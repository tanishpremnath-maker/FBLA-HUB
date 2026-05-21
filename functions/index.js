const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineString } = require("firebase-functions/params");
const sgMail = require("@sendgrid/mail");
const Anthropic = require("@anthropic-ai/sdk");

// Define API key parameters
const sendgridKey = defineString("SENDGRID_API_KEY");
const anthropicKey = defineString("ANTHROPIC_API_KEY");

exports.checkOutfitDressCode = onCall({ timeoutSeconds: 60 }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const { imageBase64, mimeType } = request.data;
  if (!imageBase64) {
    throw new HttpsError("invalid-argument", "imageBase64 is required");
  }

  // Strip data-URL prefix if present (e.g. "data:image/jpeg;base64,")
  const rawBase64 = imageBase64.includes(",") ? imageBase64.split(",")[1] : imageBase64;
  const mediaType = mimeType || "image/jpeg";

  const client = new Anthropic.default({ apiKey: anthropicKey.value() });

  const response = await client.messages.create({
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

  const rawText = response.content[0].text.trim();

  try {
    // Extract JSON even if Claude wraps it in markdown code fences
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

exports.chatWithAI = onCall({ timeoutSeconds: 60 }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const { messages, query } = request.data;
  if (!query) {
    throw new HttpsError("invalid-argument", "query is required");
  }

  const client = new Anthropic.default({ apiKey: anthropicKey.value() });

  // Build messages array — Claude requires alternating user/assistant, starting with user
  const history = (messages || []).map((msg) => ({
    role: msg.isUser ? "user" : "assistant",
    content: msg.text,
  }));

  // Drop any leading assistant turns so the array starts with a user message
  while (history.length > 0 && history[0].role !== "user") {
    history.shift();
  }

  history.push({ role: "user", content: query });

  const response = await client.messages.create({
    model: "claude-opus-4-7",
    max_tokens: 1024,
    system: `You are FBLA Hub AI, the built-in assistant for the Lead Link mobile app — an app built for FBLA (Future Business Leaders of America) members.

App screens and features:
- Home: dashboard with upcoming competition reminders, countdown timers, motivational tip, and quick-access search (✦ icon top-right). AI chat button launches you.
- Events & Calendar (2nd tab): browse FBLA competitions by state, filter by date, add reminders, see countdowns.
- News (4th tab): live FBLA.org articles, auto-tagged by category, tap to read, share button.
- Resources (5th tab): official FBLA guidelines, toolkit, dress code info, state/national social media links.
- Profile (1st tab): edit name, email, chapter, graduation year, state; upload photo; manage connections; toggle public/private.
- Messages: real-time chat with connections. Access via Profile tab or Home search.
- Settings (via Profile → settings icon): accent color picker, background darkness slider, text size scale, Simplified UI toggle, light/dark mode, account management, 2FA, tutorial launcher.
- Find Peers: search FBLA members by name/chapter, send connection requests, message once connected.

You help with: navigating the app, FBLA competition prep (any of the 70+ events), conference info (Regional → SLC → NLC), networking and career tips, FBLA basics (joining, officer roles, scholarships), and general business knowledge.

Keep responses concise, friendly, and practical. Use markdown formatting (bold, bullet points) when it helps clarity. If you don't know something specific about this app or FBLA, say so honestly. No need to repeat the user's question back to them.`,
    messages: history,
  });

  return { reply: response.content[0].text };
});

exports.send2FAEmail = onCall(async (request) => {
  // Initialize SendGrid with the API key
  sgMail.setApiKey(sendgridKey.value());

  // Verify user is authenticated
  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "User must be authenticated"
    );
  }

  const { email, code } = request.data;

  // Validate inputs
  if (!email || !code) {
    throw new HttpsError(
      "invalid-argument",
      "Email and code are required"
    );
  }

  const msg = {
    to: email,
    from: "leadlink2026@gmail.com",
    subject: "Your Lead Link Verification Code",
    text: `Your verification code is: ${code}. This code will expire in 5 minutes.`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f5;">
        <div style="background-color: #000D1A; padding: 30px; border-radius: 10px; text-align: center;">
          <h1 style="color: #FFB800; margin: 0; font-size: 28px;">Lead Link</h1>
          <p style="color: white; margin-top: 10px;">FBLA Mobile App</p>
        </div>
        <div style="background-color: white; padding: 30px; border-radius: 10px; margin-top: 20px;">
          <p style="color: #333; font-size: 16px;">Your verification code is:</p>
          <div style="background-color: #FFB800; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
            <h1 style="color: #000; font-size: 36px; letter-spacing: 8px; margin: 0;">${code}</h1>
          </div>
          <p style="color: #666; font-size: 14px;">This code will expire in 5 minutes.</p>
          <p style="color: #999; font-size: 12px; margin-top: 30px; border-top: 1px solid #eee; padding-top: 20px;">
            If you didn't request this code, please ignore this email.<br>
            This is an automated message from Lead Link FBLA Mobile App.
          </p>
        </div>
      </div>
    `,
  };

  try {
    await sgMail.send(msg);
    console.log("Email sent successfully to:", email);
    return { success: true, message: "Email sent successfully" };
  } catch (error) {
    console.error("SendGrid error:", error.response ? error.response.body : error);
    throw new HttpsError("internal", "Failed to send email");
  }
});
