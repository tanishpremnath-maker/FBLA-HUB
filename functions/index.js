const functions = require("firebase-functions");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const Anthropic = require("@anthropic-ai/sdk").default;
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

const ANTHROPIC_API_KEY = "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";

// Helper: fetch user prefs and send FCM notification if allowed
async function sendToUser(uid, prefKey, title, body, channelId) {
  try {
    const userSnap = await db.collection("users").doc(uid).get();
    if (!userSnap.exists) return;
    const data = userSnap.data();
    const token = data.fcmToken;
    if (!token) return;
    if (data.notifEnabled === false) return;
    if (prefKey && data[prefKey] === false) return;

    await messaging.send({
      token,
      notification: { title, body },
      android: { notification: { channelId, sound: "default" }, priority: "high" },
      data: { channel: channelId },
    });
  } catch (err) {
    console.error(`sendToUser(${uid}) failed:`, err.message);
  }
}

// 1. Direct message notification
exports.onNewMessage = onDocumentCreated("messages/{messageId}", async (event) => {
  const msg = event.data.data();
  if (!msg || !msg.receiverId || !msg.senderId || msg.receiverId === msg.senderId) return;
  const text = msg.message || (msg.attachmentType === "image" ? "Photo" : "File");
  await sendToUser(msg.receiverId, "notifMessages", msg.senderName || "New message", text, "chat_messages");
});

// 2. Group chat message notification
exports.onNewGroupMessage = onDocumentCreated(
  "groupChats/{groupId}/messages/{messageId}",
  async (event) => {
    const msg = event.data.data();
    if (!msg || !msg.senderId) return;
    const groupSnap = await db.collection("groupChats").doc(event.params.groupId).get();
    if (!groupSnap.exists) return;
    const group = groupSnap.data();
    const memberIds = (group.memberIds || []).filter((uid) => uid !== msg.senderId);
    const text = msg.message || (msg.attachmentType === "image" ? "Photo" : "File");
    await Promise.all(
      memberIds.map((uid) =>
        sendToUser(uid, "notifGroupMessages", group.name || "Group", `${msg.senderName || "Someone"}: ${text}`, "group_messages")
      )
    );
  }
);

// 3. Connection request notification
exports.onConnectionRequest = onDocumentCreated("connectionRequests/{reqId}", async (event) => {
  const req = event.data.data();
  if (!req || !req.toUid || !req.fromName) return;
  await sendToUser(req.toUid, "notifConnectionRequests", "New Connection Request", `${req.fromName} wants to connect with you`, "connections");
});

// 4. Connection accepted notification
exports.onConnectionAccepted = onDocumentUpdated("connectionRequests/{reqId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!before || !after || before.status === "accepted" || after.status !== "accepted") return;
  const accepterSnap = await db.collection("users").doc(after.toUid).get();
  const accepterName = accepterSnap.exists ? (accepterSnap.data().name || "Someone") : "Someone";
  await sendToUser(after.fromUid, "notifConnectionAccepted", "Connection Accepted!", `${accepterName} accepted your connection request`, "connections");
});

// 5. Event reminder check — runs every 15 minutes
exports.onEventReminderCheck = onSchedule("every 15 minutes", async () => {
  const now = Date.now();
  const window = 15 * 60 * 1000;
  const usersSnap = await db.collection("users").get();
  const sends = [];
  usersSnap.forEach((doc) => {
    const data = doc.data();
    if (!data.fcmToken || data.notifEnabled === false || data.notifEventReminders === false) return;
    (data.reminders || []).forEach((r) => {
      const diff = (r.timestamp || 0) - now;
      // ~1 hour out
      if (diff > 0 && diff <= 60 * 60 * 1000 + window && diff > 60 * 60 * 1000 - window) {
        sends.push(sendToUser(doc.id, "notifEventReminders", "Competition in 1 Hour!", `${r.name} starts soon — good luck!`, "event_reminders"));
      }
      // ~24 hours out
      if (diff > 0 && diff <= 24 * 60 * 60 * 1000 + window && diff > 24 * 60 * 60 * 1000 - window) {
        sends.push(sendToUser(doc.id, "notifEventReminders", "Competition Tomorrow", `${r.name} is in about 24 hours — you've got this!`, "event_reminders"));
      }
    });
  });
  await Promise.all(sends);
});

exports.fetchInstagramPosts = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const username = (payload.username || "").replace(/^@/, "").trim().toLowerCase();
  if (!username) throw new functions.https.HttpsError("invalid-argument", "username is required");
  try {
    const res = await fetch(
      `https://i.instagram.com/api/v1/users/web_profile_info/?username=${username}`,
      {
        headers: {
          "x-ig-app-id": "936619743392459",
          "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
          "Accept": "application/json",
          "Accept-Language": "en-US,en;q=0.9",
          "Referer": "https://www.instagram.com/",
          "Origin": "https://www.instagram.com",
        },
      }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json();
    const user = json?.data?.user;
    if (!user) throw new Error("User not found or account is private");
    const posts = (user.edge_owner_to_timeline_media?.edges || [])
      .slice(0, 12)
      .map(({ node }) => ({
        id: node.id || "",
        imageUrl: node.display_url || "",
        caption: node.edge_media_to_caption?.edges?.[0]?.node?.text || "",
        timestamp: (node.taken_at_timestamp || 0) * 1000,
        likeCount: node.edge_liked_by?.count || 0,
        commentCount: node.edge_media_to_comment?.count || 0,
        postUrl: `https://www.instagram.com/p/${node.shortcode}/`,
        isVideo: node.is_video || false,
      }));
    return {
      username: user.username || username,
      fullName: user.full_name || "",
      bio: user.biography || "",
      profilePicUrl: user.profile_pic_url || "",
      followerCount: user.edge_followed_by?.count || 0,
      postCount: user.edge_owner_to_timeline_media?.count || 0,
      posts,
    };
  } catch (err) {
    console.error(`fetchInstagramPosts(${username}):`, err.message);
    throw new functions.https.HttpsError("internal", err.message);
  }
});

exports.checkOutfitDressCode = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const { imageBase64, mimeType } = payload;
  if (!imageBase64) {
    throw new functions.https.HttpsError("invalid-argument", "imageBase64 is required");
  }

  const rawBase64 = imageBase64.includes(",") ? imageBase64.split(",")[1] : imageBase64;
  const mediaType = mimeType || "image/jpeg";

  const client = new Anthropic({ apiKey: ANTHROPIC_API_KEY });

  let response;
  try {
    response = await client.messages.create({
      model: "claude-sonnet-4-6",
      max_tokens: 1024,
      messages: [
        {
          role: "user",
          content: [
            {
              type: "image",
              source: { type: "base64", media_type: mediaType, data: rawBase64 },
            },
            {
              type: "text",
              text: `You are an FBLA dress code expert. Analyze this outfit photo against FBLA official dress code standards.

FBLA Dress Code Standards:
- Business professional attire is required at all competitive events and conferences
- Men: Business suit (jacket + matching dress slacks), dress shirt, tie, dress shoes and socks. Alternatively a blazer with dress pants, collared dress shirt, and tie.
- Women: Business suit (jacket + matching skirt or slacks), business dress, or blazer with a dress skirt or slacks. Professional blouse/top. Closed-toe dress shoes.
- Colors should be conservative and professional (navy, black, grey, white, etc.)
- Absolutely no jeans, t-shirts, athletic/casual wear, shorts, tank tops, hoodies, or sneakers
- Clothing must be clean, neat, pressed, and well-fitted (not too tight or too loose)
- Skirts and dresses must reach at or below the knee
- Hair should be neat and professional
- Accessories should be minimal and professional
- No excessive or distracting jewelry or accessories

Respond ONLY with valid JSON (no markdown, no code blocks) in this exact format:
{
  "acceptable": true,
  "verdict": "One sentence summary",
  "explanation": "2-3 sentences explaining the verdict in detail",
  "tips": ["specific tip 1", "specific tip 2"]
}

If you cannot clearly see the outfit or the image quality is too low, set acceptable to false and explain in the verdict. Be encouraging but honest and specific.`,
            },
          ],
        },
      ],
    });
  } catch (err) {
    console.error("Anthropic API error:", err.status, err.message);
    throw new functions.https.HttpsError(
      "internal",
      `AI service error (${err.status || "unknown"}): ${err.message}`
    );
  }

  const rawText = response.content[0].text.trim();

  try {
    const jsonMatch = rawText.match(/\{[\s\S]*\}/);
    if (!jsonMatch) throw new Error("No JSON found");
    return JSON.parse(jsonMatch[0]);
  } catch {
    return {
      acceptable: false,
      verdict: "Could not parse analysis result.",
      explanation: rawText,
      tips: ["Please try again with a clearer photo of your full outfit."],
    };
  }
});

exports.chatWithAI = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const { messages, query } = payload;
  if (!query) {
    throw new functions.https.HttpsError("invalid-argument", "query is required");
  }

  const client = new Anthropic({ apiKey: ANTHROPIC_API_KEY });

  const history = (messages || []).map((msg) => ({
    role: msg.isUser ? "user" : "assistant",
    content: msg.text,
  }));

  while (history.length > 0 && history[0].role !== "user") {
    history.shift();
  }

  history.push({ role: "user", content: query });

  const response = await client.messages.create({
    model: "claude-opus-4-7",
    // 8192 so the quiz/Q&A generators (which ask for many JSON questions at once)
    // don't get truncated mid-array. 1024 was cutting off the JSON and breaking parsing.
    max_tokens: 8192,
    system: `You are FBLA Hub AI, the built-in assistant for the FBLA Hub mobile app — an app built for FBLA (Future Business Leaders of America) members.

App screens and features:
- Home: dashboard with upcoming competition reminders, countdown timers, and motivational tips. AI chat button launches you.
- Events & Calendar (2nd tab): browse FBLA competitions by state, filter by date, add reminders, see countdowns.
- News (4th tab): live FBLA.org articles, auto-tagged by category, tap to read, share button.
- Resources (5th tab): official FBLA guidelines, toolkit, dress code info, state/national social media links.
- Profile (1st tab): edit name, email, chapter, graduation year, state; upload photo; manage connections; toggle public/private.
- Messages: real-time chat with connections. Access via Profile tab or Home search.
- Settings (via Profile → settings icon): accent color picker, background darkness slider, text size scale, Simplified UI toggle, light/dark mode, account management, 2FA, tutorial launcher.
- Find Peers: search FBLA members by name/chapter, send connection requests, message once connected.

You help with: navigating the app, FBLA competition prep (any of the 70+ events), conference info (Regional → SLC → NLC), networking and career tips, FBLA basics (joining, officer roles, scholarships), and general business knowledge.

Keep responses concise, friendly, and practical. Use markdown formatting (bold, bullet points) when it helps clarity. If you don't know something specific about this app or FBLA, say so honestly.`,
    messages: history,
  });

  return { reply: response.content[0].text };
});

// EmailJS — requires "Allow API for non-browser applications" in the EmailJS dashboard:
// https://dashboard.emailjs.com/admin/account/security
const EMAILJS_SERVICE_ID = "service_cbcdanj";
const EMAILJS_PUBLIC_KEY = "wCNL4V8dkOjhd1fq4";
const EMAILJS_PRIVATE_KEY = "YYWc_oUdXfv4cN1yaReyA";
const EMAILJS_TEMPLATE_2FA = "2fa";
const EMAILJS_TEMPLATE_WELCOME = "welcome";
const EMAILJS_TEMPLATE_PASSWORD_RESET = "password_reset";
const EMAILJS_TEMPLATE_ACCOUNT_DELETION = "account_deletion";
const EMAILJS_TEMPLATE_CONTACT_US = "contact_us";

async function sendEmailViaEmailJS(templateId, templateParams) {
  const response = await fetch("https://api.emailjs.com/api/v1.0/email/send", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      service_id: EMAILJS_SERVICE_ID,
      template_id: templateId,
      user_id: EMAILJS_PUBLIC_KEY,
      accessToken: EMAILJS_PRIVATE_KEY,
      template_params: templateParams,
    }),
  });

  if (!response.ok) {
    const text = await response.text();
    console.error(`EmailJS error (template=${templateId}):`, text);
    throw new functions.https.HttpsError("internal", `EmailJS: ${text}`);
  }
}

exports.send2FAEmail = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = (payload.email || "").trim();
  const code = payload.code;
  if (!email || !code) {
    console.error("Missing params, received:", JSON.stringify(data));
    throw new functions.https.HttpsError("invalid-argument", "Email and code are required");
  }

  await sendEmailViaEmailJS(EMAILJS_TEMPLATE_2FA, {
    email,
    to_email: email,
    passcode: code,
    verification_code: code,
    code,
    time: "5 minutes",
  });

  return { success: true, message: "Email sent successfully" };
});

exports.sendWelcomeEmail = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = (payload.email || "").trim();
  const name = (payload.name || "").trim();
  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required");
  }

  await sendEmailViaEmailJS(EMAILJS_TEMPLATE_WELCOME, {
    email,
    to_email: email,
    name: name || "Member",
  });

  return { success: true, message: "Welcome email sent." };
});

exports.sendAccountDeletionEmail = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = (payload.email || "").trim();
  const name = (payload.name || "").trim();
  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required");
  }

  await sendEmailViaEmailJS(EMAILJS_TEMPLATE_ACCOUNT_DELETION, {
    email,
    to_email: email,
    name: name || "Member",
  });

  return { success: true, message: "Account deletion email sent." };
});

exports.sendContactUsAutoReply = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = (payload.email || "").trim();
  const name = (payload.name || "").trim();
  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required");
  }

  await sendEmailViaEmailJS(EMAILJS_TEMPLATE_CONTACT_US, {
    email,
    to_email: email,
    name,
  });

  return { success: true, message: "Contact auto-reply sent." };
});

// ── Password reset (OTP-based, custom flow) ────────────────────────────────────
// Flow: sendPasswordReset emails an 8-digit OTP. The app collects that code
// (verifyPasswordResetCode), then the user chooses a new password
// (confirmPasswordReset), which must be strong and different from the old one.

const PASSWORD_RESET_TTL_MS = 15 * 60 * 1000; // 15 minutes
// Web API key for this Firebase project (same key shipped in the Android app).
const FIREBASE_WEB_API_KEY = "AIzaSyAcObzTAYNCEzq9tWJdu4yjC9Ad6ZSxq_0";

function normalizeEmail(email) {
  return (email || "").trim().toLowerCase();
}

function generateResetOtp() {
  return String(Math.floor(10000000 + Math.random() * 90000000));
}

// Shared strength check — kept identical on the client for live feedback.
function passwordStrengthError(pw) {
  if (!pw || pw.length < 8) return "Password must be at least 8 characters.";
  if (!/[A-Z]/.test(pw)) return "Password must include an uppercase letter.";
  if (!/[a-z]/.test(pw)) return "Password must include a lowercase letter.";
  if (!/[0-9]/.test(pw)) return "Password must include a number.";
  return null;
}

// Returns true if `password` is the account's CURRENT password (used to enforce
// that the new password is actually different from the old one).
async function isCurrentPassword(email, password) {
  try {
    const res = await fetch(
      `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${FIREBASE_WEB_API_KEY}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, returnSecureToken: false }),
      }
    );
    return res.ok; // 200 means the password matched the current one.
  } catch (err) {
    console.error("isCurrentPassword error:", err.message);
    return false;
  }
}

exports.sendPasswordReset = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = normalizeEmail(payload.email);
  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required");
  }

  let displayName = "";
  try {
    const userRecord = await admin.auth().getUserByEmail(email);
    displayName = userRecord.displayName || "";
  } catch (err) {
    // Genuinely-unknown email: succeed silently (avoids user enumeration).
    if (err.code === "auth/user-not-found") {
      console.warn(`sendPasswordReset(${email}): no such user`);
      return { success: true, message: "If an account exists, a reset email was sent." };
    }
    // Anything else (e.g. missing IAM permission) is a real bug — surface it.
    console.error(`sendPasswordReset(${email}) admin error:`, err.message);
    throw new functions.https.HttpsError("internal", `Auth error: ${err.message}`);
  }

  const otp = generateResetOtp();
  await db.collection("passwordResetCodes").doc(email).set({
    code: otp,
    email,
    expiresAt: Date.now() + PASSWORD_RESET_TTL_MS,
    used: false,
  });

  await sendEmailViaEmailJS(EMAILJS_TEMPLATE_PASSWORD_RESET, {
    email,
    to_email: email,
    name: displayName,
    passcode: otp,
    verification_code: otp,
    code: otp,
    reset_code: otp,
    otp,
    time: "15 minutes",
  });

  return { success: true, message: "Password reset email sent." };
});

exports.verifyPasswordResetCode = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = normalizeEmail(payload.email);
  const code = (payload.code || "").trim();
  if (!email || !code) {
    throw new functions.https.HttpsError("invalid-argument", "Email and code are required");
  }

  const snap = await db.collection("passwordResetCodes").doc(email).get();
  if (!snap.exists) {
    return { valid: false, reason: "No reset request found. Please request a new code." };
  }
  const rec = snap.data();
  if (rec.used) return { valid: false, reason: "This code was already used. Request a new one." };
  if (Date.now() > (rec.expiresAt || 0)) return { valid: false, reason: "This code has expired. Request a new one." };
  if (rec.code !== code) return { valid: false, reason: "Incorrect code. Please check your email." };

  return { valid: true };
});

exports.confirmPasswordReset = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = normalizeEmail(payload.email);
  const code = (payload.code || "").trim();
  const newPassword = payload.newPassword || "";
  if (!email || !code || !newPassword) {
    throw new functions.https.HttpsError("invalid-argument", "Email, code, and new password are required");
  }

  // 1. Re-validate the reset code server-side.
  const ref = db.collection("passwordResetCodes").doc(email);
  const snap = await ref.get();
  if (!snap.exists) throw new functions.https.HttpsError("not-found", "No reset request found.");
  const rec = snap.data();
  if (rec.used) throw new functions.https.HttpsError("failed-precondition", "This code was already used.");
  if (Date.now() > (rec.expiresAt || 0)) throw new functions.https.HttpsError("deadline-exceeded", "This code has expired.");
  if (rec.code !== code) throw new functions.https.HttpsError("permission-denied", "Incorrect code.");

  // 2. Enforce strength requirements.
  const strengthErr = passwordStrengthError(newPassword);
  if (strengthErr) throw new functions.https.HttpsError("invalid-argument", strengthErr);

  // 3. Enforce that the new password differs from the old one.
  if (await isCurrentPassword(email, newPassword)) {
    throw new functions.https.HttpsError("already-exists", "New password must be different from your old password.");
  }

  // 4. Update the password and consume the code.
  try {
    const userRecord = await admin.auth().getUserByEmail(email);
    await admin.auth().updateUser(userRecord.uid, { password: newPassword });
  } catch (err) {
    console.error("confirmPasswordReset updateUser error:", err.message);
    throw new functions.https.HttpsError("internal", "Could not update password. Please try again.");
  }
  await ref.update({ used: true });

  return { success: true, message: "Password updated successfully." };
});