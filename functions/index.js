const functions = require("firebase-functions");
const Anthropic = require("@anthropic-ai/sdk").default;

const ANTHROPIC_API_KEY = "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";

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
    max_tokens: 1024,
    system: `You are FBLA Hub AI, the built-in assistant for the FBLA Hub mobile app — an app built for FBLA (Future Business Leaders of America) members.

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

Keep responses concise, friendly, and practical. Use markdown formatting (bold, bullet points) when it helps clarity. If you don't know something specific about this app or FBLA, say so honestly.`,
    messages: history,
  });

  return { reply: response.content[0].text };
});

exports.send2FAEmail = functions.https.onCall(async (data, context) => {
  const payload = (data && data.data) ? data.data : data;
  const email = payload.email;
  const code = payload.code;
  if (!email || !code) {
    console.error("Missing params, received:", JSON.stringify(data));
    throw new functions.https.HttpsError("invalid-argument", "Email and code are required");
  }

  const response = await fetch("https://api.emailjs.com/api/v1.0/email/send", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      service_id: "service_cbcdanj",
      template_id: "template_soygwa9",
      user_id: "wCNL4V8dkOjhd1fq4",
      accessToken: "YYWc_oUdXfv4cN1yaReyA",
      template_params: {
        email,
        to_email: email,
        passcode: code,
        verification_code: code,
        time: "5 minutes",
      },
    }),
  });

  if (!response.ok) {
    const text = await response.text();
    console.error("EmailJS error:", text);
    throw new functions.https.HttpsError("internal", "Failed to send email");
  }

  return { success: true, message: "Email sent successfully" };
});