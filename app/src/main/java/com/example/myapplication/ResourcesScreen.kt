package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File


@Composable
fun EventPrepHub(
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val reduceMotion = LocalReduceMotion.current
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)
    val scope = rememberCoroutineScope()

    data class FBLAEvent(val name: String, val category: String, val icon: ImageVector)

    val events = remember {
        listOf(
            FBLAEvent("Accounting I", "INDIVIDUAL", Icons.Default.Calculate),
            FBLAEvent("Accounting II", "INDIVIDUAL", Icons.Default.Calculate),
            FBLAEvent("Agribusiness", "INDIVIDUAL", Icons.Default.Agriculture),
            FBLAEvent("Banking & Financial Systems", "INDIVIDUAL", Icons.Default.AccountBalance),
            FBLAEvent("Business Calculations", "INDIVIDUAL", Icons.Default.Functions),
            FBLAEvent("Business Communication", "INDIVIDUAL", Icons.Default.Forum),
            FBLAEvent("Business Law", "INDIVIDUAL", Icons.Default.Gavel),
            FBLAEvent("Business Math", "INDIVIDUAL", Icons.Default.Calculate),
            FBLAEvent("Computer Applications", "INDIVIDUAL", Icons.Default.Computer),
            FBLAEvent("Computer Problem Solving", "INDIVIDUAL", Icons.Default.Code),
            FBLAEvent("Cybersecurity", "INDIVIDUAL", Icons.Default.Security),
            FBLAEvent("Database Design & Applications", "INDIVIDUAL", Icons.Default.Storage),
            FBLAEvent("Economics", "INDIVIDUAL", Icons.Default.TrendingUp),
            FBLAEvent("Electronic Career Portfolio", "INDIVIDUAL", Icons.Default.WorkspacePremium),
            FBLAEvent("Entrepreneurship", "INDIVIDUAL", Icons.Default.Lightbulb),
            FBLAEvent("Global Business", "INDIVIDUAL", Icons.Default.Language),
            FBLAEvent("Healthcare Administration", "INDIVIDUAL", Icons.Default.LocalHospital),
            FBLAEvent("Help Desk", "INDIVIDUAL", Icons.Default.HeadsetMic),
            FBLAEvent("Hospitality & Event Management", "INDIVIDUAL", Icons.Default.Hotel),
            FBLAEvent("Human Resource Management", "INDIVIDUAL", Icons.Default.People),
            FBLAEvent("Insurance & Risk Management", "INDIVIDUAL", Icons.Default.VerifiedUser),
            FBLAEvent("Introduction to Business", "INDIVIDUAL", Icons.Default.Business),
            FBLAEvent("Introduction to Business Communication", "INDIVIDUAL", Icons.Default.RecordVoiceOver),
            FBLAEvent("Introduction to Financial Math", "INDIVIDUAL", Icons.Default.AttachMoney),
            FBLAEvent("Introduction to Information Technology", "INDIVIDUAL", Icons.Default.Devices),
            FBLAEvent("Introduction to Parliamentary Procedure", "INDIVIDUAL", Icons.Default.Gavel),
            FBLAEvent("Job Interview", "INDIVIDUAL", Icons.Default.Badge),
            FBLAEvent("Management Decision Making", "TEAM", Icons.Default.Psychology),
            FBLAEvent("Marketing", "INDIVIDUAL", Icons.Default.Campaign),
            FBLAEvent("Network Design", "TEAM", Icons.Default.AccountTree),
            FBLAEvent("Networking Concepts", "INDIVIDUAL", Icons.Default.Hub),
            FBLAEvent("Parliamentary Procedure", "TEAM", Icons.Default.HowToVote),
            FBLAEvent("Personal Finance", "INDIVIDUAL", Icons.Default.AccountBalanceWallet),
            FBLAEvent("Political Science", "INDIVIDUAL", Icons.Default.Balance),
            FBLAEvent("Public Speaking", "INDIVIDUAL", Icons.Default.Mic),
            FBLAEvent("Securities & Investments", "INDIVIDUAL", Icons.Default.ShowChart),
            FBLAEvent("Social Media Strategies", "INDIVIDUAL", Icons.Default.Share),
            FBLAEvent("Sports & Entertainment Management", "INDIVIDUAL", Icons.Default.EmojiEvents),
            FBLAEvent("Supply Chain Management", "INDIVIDUAL", Icons.Default.LocalShipping),
            FBLAEvent("UX Design", "INDIVIDUAL", Icons.Default.Palette),
            FBLAEvent("Word Processing", "INDIVIDUAL", Icons.Default.Description),
            FBLAEvent("Business Ethics", "TEAM", Icons.Default.Handshake),
            FBLAEvent("Business Plan", "TEAM", Icons.AutoMirrored.Filled.Assignment),
            FBLAEvent("Client Service", "TEAM", Icons.Default.SupportAgent),
            FBLAEvent("Community Service Project", "TEAM", Icons.Default.VolunteerActivism),
            FBLAEvent("Digital Video Production", "TEAM", Icons.Default.VideoLibrary),
            FBLAEvent("E-business", "TEAM", Icons.Default.ShoppingCart),
            FBLAEvent("Emerging Business Issues", "TEAM", Icons.Default.Insights),
            FBLAEvent("Future Business Educator", "INDIVIDUAL", Icons.Default.School),
            FBLAEvent("Graphic Design", "TEAM", Icons.Default.Brush),
            FBLAEvent("Impromptu Speaking", "INDIVIDUAL", Icons.Default.Bolt),
            FBLAEvent("Mobile Application Development", "TEAM", Icons.Default.PhoneAndroid),
            FBLAEvent("Sales Presentation", "TEAM", Icons.Default.Sell),
            FBLAEvent("Website Design", "TEAM", Icons.Default.Web)
        )
    }

    var selectedEvent by remember { mutableStateOf<FBLAEvent?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf("ALL") }

    // Prep screen state
    var prepContent by remember { mutableStateOf("") }
    var isLoadingPrep by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0=Overview 1=Questions 2=Tips

    val filteredEvents = remember(searchQuery, activeCategory) {
        events.filter { event ->
            (searchQuery.isEmpty() || event.name.contains(searchQuery, ignoreCase = true)) &&
                    (activeCategory == "ALL" || event.category == activeCategory)
        }
    }

    fun getPrepContent(event: FBLAEvent, tab: Int): String {
        val n = event.name
        val isTeam = event.category == "TEAM"

        if (tab == 0) return when {
            n == "Business Law" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Contract law (offer, acceptance, consideration, capacity)\n• Business organizations (sole proprietorship, partnership, LLC, corporation)\n• Agency relationships and authority types\n• Uniform Commercial Code (UCC) — sale of goods\n• Tort law and negligence\n• Intellectual property (copyright, trademark, patent)\n• Employment and labor law\n• Consumer protection and bankruptcy (Ch. 7, Ch. 11)\n\n**Scoring**\nNumber correct out of 100. Top scorers advance to the next round.\n\n**Key Resources**\nFBLA Business Law study guide at fbla.org, Cliffs Notes Business Law."
            n == "Economics" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Supply and demand (shifts, elasticity, equilibrium price)\n• GDP, inflation, CPI, and unemployment measurement\n• Monetary policy and the Federal Reserve\n• Fiscal policy — government spending and taxes\n• Market structures (perfect competition, monopoly, oligopoly, monopolistic competition)\n• International trade, tariffs, comparative advantage\n• Opportunity cost, trade-offs, and scarcity\n• Business cycle phases (expansion, peak, contraction, trough)\n• Banking and financial markets\n\n**Scoring**\nNumber correct out of 100. Ties broken by timed scoring.\n\n**Key Resources**\nFBLA Economics study guide, Khan Academy Macroeconomics."
            n == "Accounting I" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Accounting equation: Assets = Liabilities + Owner's Equity\n• Journalizing and posting transactions (debits and credits)\n• Normal balances for all account types\n• Trial balance preparation\n• Income statement and balance sheet\n• Adjusting entries and closing entries\n• Cash and banking procedures\n• Payroll accounting basics\n• GAAP principles (matching, revenue recognition)\n\n**Scoring**\nNumber correct out of 100.\n\n**Key Resources**\nCentury 21 Accounting textbook, FBLA Accounting I study guide."
            n == "Accounting II" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Departmental accounting and cost allocation\n• Partnership accounting (formation, profit sharing, dissolution)\n• Corporate accounting (stock issuance, dividends, retained earnings)\n• Manufacturing cost accounting (job order, process costing)\n• Budgeting and variance analysis\n• Financial statement analysis (ratios: liquidity, profitability, solvency)\n• Long-term assets and depreciation methods\n• Bonds payable and notes payable\n\n**Scoring**\nNumber correct out of 100."
            n == "Personal Finance" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Budgeting, cash flow, and the 50/30/20 rule\n• Banking (checking, savings, interest types)\n• Credit scores, credit cards, and debt management\n• Investing (stocks, bonds, mutual funds, ETFs, index funds)\n• Retirement accounts (401k, Roth IRA, Traditional IRA)\n• Insurance types (health, auto, life, homeowners, renters)\n• Taxes (W-2, 1040, deductions, credits)\n• Rule of 72 and compound interest\n• Net worth = Assets − Liabilities\n• Consumer rights and protection laws\n\n**Scoring**\nNumber correct out of 100."
            n == "Marketing" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• The 4 Ps (Product, Price, Place, Promotion)\n• Market segmentation and target market identification\n• Consumer behavior and buying decision process\n• Brand equity and brand management\n• Pricing strategies (penetration, skimming, cost-plus, loss leader)\n• Distribution channels and logistics\n• Promotional mix (advertising, PR, sales promotion, personal selling)\n• Digital marketing, social media, and SEO\n• Market research methods (primary vs. secondary)\n• SWOT analysis in a marketing context\n\n**Scoring**\nNumber correct out of 100."
            n == "Cybersecurity" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• CIA Triad (Confidentiality, Integrity, Availability)\n• Malware types (virus, worm, ransomware, trojan, spyware, adware)\n• Network security (firewalls, IDS/IPS, VPNs, DMZ)\n• Cryptography basics and encryption (symmetric vs. asymmetric)\n• Authentication methods (MFA, biometrics, tokens)\n• Social engineering (phishing, vishing, pretexting, baiting)\n• Security frameworks (NIST, ISO 27001)\n• Incident response lifecycle\n• Wireless security (WEP, WPA2, WPA3)\n• Common vulnerabilities (SQL injection, XSS, buffer overflow)\n\n**Scoring**\nNumber correct out of 100."
            n == "Introduction to Business" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Forms of business ownership (sole prop, partnership, LLC, corp)\n• Core business functions (marketing, finance, HR, operations)\n• Supply and demand fundamentals\n• Management functions (planning, organizing, leading, controlling)\n• Entrepreneurship and small business formation\n• Business ethics and social responsibility\n• International business and globalization\n• Business math (markup, break-even, ROI)\n• Human resources basics and employment law\n• E-commerce and technology in business\n\n**Scoring**\nNumber correct out of 100."
            n == "Business Communication" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• The 7 Cs of communication (Clear, Concise, Concrete, Correct, Coherent, Complete, Courteous)\n• Business writing formats (letters, memos, emails, reports)\n• Verbal vs. nonverbal communication\n• Active listening techniques and barriers\n• Presentation skills and visual aids\n• Meeting management and agenda writing\n• Grammar, punctuation, and mechanics\n• Proofreading and editing strategies\n• Cross-cultural communication\n• Digital communication etiquette\n\n**Scoring**\nNumber correct out of 100."
            n == "Business Calculations" -> "**Format**\nObjective test — 100 questions, 50 minutes. Calculator typically permitted.\n\n**Topics Covered**\n• Markup and markdown (retail pricing math)\n• Gross and net profit margin calculations\n• Simple interest: I = PRT\n• Compound interest: A = P(1 + r)^t\n• Commission, wages, and payroll calculations\n• Sales tax and income tax computations\n• Trade discounts and cash discounts\n• Break-even analysis: Fixed Costs ÷ (Price − Variable Cost)\n• Depreciation (straight-line, double-declining)\n• Statistics: mean, median, mode, range\n\n**Scoring**\nNumber correct out of 100."
            n == "Networking Concepts" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• OSI model — all 7 layers and their functions\n• TCP/IP model and key protocols (HTTP/S, FTP, SSH, SMTP, DNS, DHCP)\n• IP addressing: IPv4, IPv6, subnetting, CIDR notation\n• Network devices: router, switch, hub, firewall, access point\n• Network topologies (bus, star, ring, mesh, hybrid)\n• Wireless standards (802.11a/b/g/n/ac/ax)\n• Private vs. public IP ranges (RFC 1918)\n• Cloud computing and virtualization concepts\n• Troubleshooting tools: ping, traceroute, ipconfig/ifconfig\n• Network security: VPN, firewall, IDS/IPS, NAT\n\n**Scoring**\nNumber correct out of 100."
            n == "Public Speaking" -> "**Format**\nPrepared speech — 5 to 7 minutes on a business topic chosen by the competitor. No notes allowed during delivery.\n\n**Choosing Your Topic**\nPick a current business issue you genuinely care about:\n• Leadership and organizational culture\n• Technology's impact on business\n• Entrepreneurship and innovation\n• Environmental sustainability in business\n• Ethics and corporate responsibility\n\n**Scoring Criteria**\n• Introduction and conclusion strength\n• Organization and logical flow\n• Research quality and supporting evidence\n• Delivery (eye contact, vocal variety, posture, gestures)\n• Language use and vocabulary\n• Overall effectiveness and persuasiveness"
            n == "Job Interview" -> "**Format**\nSimulated job interview with judges acting as employers — 7 to 10 minutes total.\n\n**What to Expect**\n• Receive a job description before competition\n• Dress formally — business professional is required\n• Bring multiple printed resume copies\n• Judges score from the moment you walk in\n\n**Skills Tested**\n• Self-presentation and professional appearance\n• Communication and listening skills\n• Industry and role knowledge\n• Handling behavioral and situational questions\n• Asking thoughtful questions to the interviewer\n\n**Scoring**\nPoise, appearance, communication, knowledge of the role, resume quality, and closing."
            n == "Entrepreneurship" -> "**Format**\nObjective test (100 questions) + case study presentation to judges (7 minutes + Q&A).\n\n**Objective Test Topics**\n• Entrepreneurship process and opportunity recognition\n• Business plan components (all sections)\n• Financing options (bootstrapping, angel, VC, crowdfunding, SBA loans)\n• Marketing for startups and growth strategies\n• Operations, supply chain, and business law basics\n\n**Case Study**\nYou receive a business scenario and must present a structured solution including problem analysis, proposed solutions, recommended action, and implementation plan.\n\n**Scoring**\nObjective test score + presentation score are combined."
            n == "Business Plan" -> "**Format**\nWritten business plan (submitted in advance) + oral presentation (10 minutes) + Q&A with judges. Teams of 1–3.\n\n**Required Plan Sections**\n• Executive Summary (write this last, present it first)\n• Company Description and Mission\n• Market Analysis (target market, competitors, market size)\n• Organization & Management structure\n• Products/Services description\n• Marketing & Sales Strategy\n• Financial Projections (income statement, cash flow, balance sheet)\n• Funding Request and use of funds\n\n**Scoring**\nWritten plan quality (50%), oral presentation (30%), Q&A responses (20%)."
            n == "Impromptu Speaking" -> "**Format**\nNo preparation allowed — receive a topic and speak for 1–3 minutes immediately after a brief draw period.\n\n**What to Expect**\n• Business-related topics (ethics, leadership, economics, technology, etc.)\n• 1–2 minutes to collect your thoughts (varies by level)\n• Speak immediately — no notes during delivery\n\n**Scoring**\n• Organization (clear intro, body, conclusion)\n• Delivery confidence and vocal variety\n• Vocabulary and language use\n• Coverage and insight on the topic\n\n**The Core Skill**\nStructuring thoughts instantly under pressure — this is almost entirely a trainable skill."
            else -> "**Event Overview: $n**\n${if (isTeam) "Team event — 2–3 members collaborate on this." else "Individual event — just you."}\n\n**Typical Format**\n${if (n.contains("Objective", true) || listOf("Accounting", "Economics", "Marketing", "Law", "Math", "Finance", "Cybersecurity", "Networking", "Business Calc", "Introduction").any { n.contains(it, true) }) "Objective test — approximately 100 questions, 50 minutes." else if (listOf("Speaking", "Interview", "Presentation", "Sales").any { n.contains(it, true) }) "Performance event — you present to a panel of judges. Dress professionally." else if (listOf("Plan", "Portfolio", "Project", "Design", "Video", "Production").any { n.contains(it, true) }) "Project-based event — submit a written/digital project plus an oral presentation." else "Check fbla.org for the exact format — it may be an objective test, project, or performance event."}\n\n**Topics & Skills**\n• Download the official topic outline from fbla.org → Competitive Events → $n\n• Study your school's textbook chapters relevant to this subject area\n• Practice with past FBLA sample tests if available on fbla.org\n${if (isTeam) "• Assign clear roles to each team member and rehearse together" else ""}\n\n**Scoring**\nJudges use official FBLA rubrics. Ask your adviser for the current scoring guide for this event."
        }

        if (tab == 1) return when {
            n == "Business Law" -> "**Master the Big Four Chapters**\nContracts, business organizations, UCC, and torts make up the majority of questions. Spend 60% of your study time here.\n\n**Know Rules AND Exceptions**\nFBLA loves testing exceptions (e.g., which contracts don't require writing, when minors CAN'T void contracts). Memorize the rule, then memorize its exceptions.\n\n**Use Process of Elimination**\nWhen two answers look plausible, focus on the legal nuance — usually one option is slightly too broad or too narrow. Eliminate the extremes first.\n\n**Practice Past Tests**\nDownload FBLA past objective tests at fbla.org. The question style and topic weighting repeat — familiarity with the format is a real advantage.\n\n**Time Management on Test Day**\n~30 seconds per question on a 100-question, 50-minute test. Flag hard ones and come back — never stall on a single question."
            n == "Economics" -> "**Understand Cause and Effect Deeply**\nDon't memorize facts in isolation — know WHY demand curves shift, WHY the Fed raises rates, WHY tariffs cause deadweight loss. FBLA questions test understanding, not recall.\n\n**Master Supply & Demand First**\nHalf of economics questions trace back to supply/demand dynamics. Know every shifter for both curves and how equilibrium changes as a result.\n\n**Know the Big Numbers**\nFamiliarize yourself with what \"normal\" looks like: healthy GDP growth (~2–3%), natural unemployment rate (~4–5%), Fed inflation target (2%). Questions often test recognition of abnormal conditions.\n\n**Use Elimination on Math Questions**\nFor elasticity and GDP calculation questions, eliminate obviously wrong magnitudes first. Then verify with the formula.\n\n**Study Recent Monetary Policy**\nFBLA Economics questions frequently reference current economic conditions. Know what the Fed has been doing recently and why."
            n == "Public Speaking" -> "**Open with Power, Not Your Name**\nNever start with \"Hi, my name is...\" Open with a gripping hook: shocking statistic, vivid story, or bold question. Your first 15 seconds set the judges' impression.\n\n**Record Every Practice Session**\nWatch with sound off (gestures, eye contact, posture) then listen without looking (filler words, pacing, volume). Do this at least 5 times before competition.\n\n**No Notes = Weeks of Practice, Not Days**\nPractice no-notes delivery at least 10 days before competition. Last-minute memorization causes on-stage freezing. The speech should feel automatic.\n\n**Time to the Second**\nBeing under 5:00 or over 7:00 can mean disqualification. In practice, hit 5:45–6:15 every single time.\n\n**Transitions Are What Make You Sound Polished**\nSmooth transitions between points signal preparation and make your speech feel cohesive, not choppy."
            n == "Job Interview" -> "**Research the Job Description Cold**\nKnow every line of the job description given to you. Be ready to connect your skills and experiences to each requirement. Generic answers lose to specific ones every time.\n\n**Dress One Level Above Business Casual**\nBusiness professional — suit jacket, dress pants or skirt, dress shoes — is the floor, not the ceiling. First impression scoring starts the second you walk in.\n\n**Prepare 5 STAR Stories**\nHave 5 adaptable stories ready that can answer behavioral questions from different angles. Practice delivering each one in under 90 seconds.\n\n**Firm Handshake + Sustained Eye Contact**\nJudges score poise from the first moment. Firm handshake, direct eye contact, genuine smile.\n\n**Practice Live, Not Just in Your Head**\nDo 3 full mock interviews with a real person before competition. The nervousness of being questioned live is very different from rehearsing alone."
            n == "Business Plan" -> "**Know Your Financials from Memory**\nJudges will drill your revenue projections. Know your break-even, year 1 revenue, and cost structure from memory — stumbling on your own numbers is a massive red flag.\n\n**Lead with the Problem, Not the Product**\nProblem → Solution → Market → Business Model → Financials → Ask. Judges are more convinced when they feel the pain before you present the cure.\n\n**Prepare for the 3 Hardest Judge Questions**\n\"How did you arrive at this market size?\" / \"Who are your top competitors and what's your real advantage?\" / \"What if revenue assumptions are 50% wrong?\"\n\n**Every Team Member Answers Every Section**\nJudges intentionally direct questions to the person who didn't present that section.\n\n**Over-Practice Q&A, Not the Script**\nMost teams over-rehearse the script and under-rehearse Q&A. Flip this — Q&A is where placements are won."
            n == "Entrepreneurship" -> "**Ace the Objective Test First**\nThe objective test score gives you a buffer going into the case study. Know entrepreneurship fundamentals and all business plan components cold.\n\n**Use a Framework for Case Studies**\nIdentify the core problem → Analyze root cause → Propose 2–3 solutions → Recommend one with rationale → Implementation and risks.\n\n**Give Specific, Not Generic Answers**\nWeak: \"Increase marketing.\" Strong: \"Launch targeted LinkedIn ads with a \$2K/month budget, A/B testing two messages, measuring leads vs. cost per lead over 60 days.\"\n\n**Know Real-World Examples**\nJudges respond to references to real companies and market disruptions. This shows you engage with entrepreneurship beyond the classroom.\n\n**Show Genuine Excitement**\nEntrepreneurship judges respond to energy and passion. If you're solving a problem you care about, let that show."
            n == "Impromptu Speaking" -> "**Lock in Your Instant Framework**\nThe second you hear the topic: Position statement → Point 1 + example → Point 2 + example → Restate position with impact. Commit to this structure before you speak.\n\n**Start Confident, Never with Filler**\nPause silently for 2–3 seconds, then open with your position — not \"Um, well, this is a great topic...\"\n\n**Use Concrete Examples**\nJudges score higher when you reference a real company, event, or statistic. Vague statements score lower than specific ones.\n\n**Practice Daily in the Two Weeks Before Competition**\nHave a friend give you random topics and speak for 90 seconds on the spot. This is almost entirely a learnable skill.\n\n**Hit 2:00–2:45 Every Time**\nFinishing at 1:20 looks underprepared. Over 3:00 is a time penalty."
            else -> "**Master the Official Topic Outline**\nDownload the official FBLA topic outline for $n from fbla.org. This is literally what the test or rubric is built from. Cross off each topic as you master it.\n\n**${if (isTeam) "Assign Roles, Then Cross-Train\nEach team member should own specific sections AND be able to speak about all sections. Judges target the person who looks least prepared." else "Use Active Recall, Not Passive Re-reading\nMake flashcards, do practice questions, and have someone quiz you. Active recall builds memory far more effectively than re-reading notes."}\n\n**Study the Scoring Rubric**\nFor performance events, the scoring guide tells you exactly what judges are looking for and how many points each element is worth. Practice specifically hitting each criterion.\n\n**Simulate Competition Conditions**\n${if (isTeam) "Do a full run-through with your team, timed, standing up, in presentation mode. Practice Q&A responses as a team — make sure transitions between speakers are smooth." else "Take at least 2 full timed practice tests or run-throughs before competition day. Identify weak areas and spend extra time there in the final week."}\n\n**Day-Of Mindset**\nArrive early, stay hydrated, and review your key notes one last time (no new material). Confidence comes from preparation — trust your work and commit to every answer."
        }

        return ""
    }

    data class EventFormatInfo(
        val formatType: String,
        val teamSize: String,
        val duration: String,
        val structure: String,
        val materials: String,
        val scoring: String,
        val keyDetails: List<String>
    )

    fun getFormatInfo(event: FBLAEvent): EventFormatInfo {
        val n = event.name
        val calcTest = setOf("Business Calculations", "Business Math")
        val pureObjTest = setOf(
            "Accounting I", "Accounting II", "Agribusiness", "Banking & Financial Systems",
            "Business Communication", "Business Law", "Computer Problem Solving", "Cybersecurity",
            "Economics", "Healthcare Administration", "Insurance & Risk Management",
            "Introduction to Business", "Introduction to Financial Math",
            "Introduction to Information Technology", "Marketing", "Networking Concepts",
            "Personal Finance", "Political Science", "Securities & Investments",
            "Supply Chain Management"
        )
        if (n in pureObjTest || n in calcTest) return EventFormatInfo(
            formatType = "Objective Test",
            teamSize = "Individual",
            duration = "50 minutes",
            structure = "100 multiple-choice questions",
            materials = if (n in calcTest) "Silent, non-printing calculator" else "None",
            scoring = "Number correct out of 100. Top scorers from each state qualify for nationals.",
            keyDetails = listOf(
                "100 questions, machine-graded at the conference testing center",
                if (n in calcTest) "Calculator required — confirm if provided or bring a silent one" else "No notes, textbooks, or devices allowed",
                "~30 seconds per question — skip hard ones and return at the end",
                "No penalty for guessing — always fill in every answer",
                "Top scorers from state advance; ties broken by completion time"
            )
        )
        val mixedIndividual = setOf(
            "Entrepreneurship", "Global Business", "Hospitality & Event Management",
            "Human Resource Management", "Sports & Entertainment Management"
        )
        if (n in mixedIndividual) return EventFormatInfo(
            formatType = "Mixed",
            teamSize = "Individual",
            duration = "50 min test + 7–10 min presentation",
            structure = "Objective test followed by a case study oral presentation",
            materials = "Case study packet provided ~30 min before presentation",
            scoring = "Combined: objective test score + judges' oral presentation rubric.",
            keyDetails = listOf(
                "Part 1: 100-question objective test (50 minutes)",
                "Part 2: Receive case study ~30 minutes before your presentation slot",
                "Presentation: up to 7–10 minutes + 3–5 min Q&A with judges",
                "No outside notes during presentation — only provided case materials",
                "Business professional attire required"
            )
        )
        return when (n) {
            "Public Speaking" -> EventFormatInfo("Performance", "Individual", "5–7 minutes", "Prepared speech on a business topic of your choice", "No notes allowed on stage", "Judges score on content, organization, delivery, and language use.", listOf("Memorized delivery — absolutely no notes allowed on stage", "Time penalties apply under 5:00 or over 7:00 — target 5:45–6:15", "Topic must relate to a current business issue", "Business professional attire required", "Judged on hook/intro, organization, evidence, delivery, and closing"))
            "Impromptu Speaking" -> EventFormatInfo("Performance", "Individual", "1–3 minutes", "Speak immediately on an assigned business topic", "1–2 min to collect thoughts — no notes during delivery", "Judges score on organization, confidence, vocabulary, and topic depth.", listOf("Topic assigned on the spot — no advance preparation allowed", "1–2 minutes to think silently before you begin speaking", "No notes of any kind during delivery", "Target 2:00–2:45 — finishing under 1:30 looks underprepared", "Business professional attire required"))
            "Job Interview" -> EventFormatInfo("Performance", "Individual", "7–10 minutes", "Simulated job interview with a judge panel", "Printed resume copies (bring at least 3)", "Judges score on appearance, communication, knowledge, and overall poise.", listOf("Job description provided before competition — know every line cold", "Scoring begins the moment you enter the room", "Bring at least 3 printed copies of your resume", "Firm handshake, sustained eye contact, and a confident close matter", "Business professional attire is mandatory"))
            "Introduction to Parliamentary Procedure" -> EventFormatInfo("Performance", "Individual", "~10–15 minutes", "Demonstrate parliamentary procedure knowledge individually", "None", "Judges score on motion accuracy, procedure knowledge, and delivery.", listOf("Demonstrate motions, voting procedures, and proper parliamentary decorum", "Study Robert's Rules of Order thoroughly — know every main motion", "Common errors: incorrect seconding, wrong voting thresholds", "Business professional attire required", "Practice with a partner calling random scenarios at you"))
            "Parliamentary Procedure" -> EventFormatInfo("Performance", "Team: 5–8 members", "~20–30 minutes", "Team conducts a full simulated parliamentary meeting", "Official agenda provided at event", "Entire team scored on procedure accuracy, roles, and execution.", listOf("Team of 5–8 conducts a complete meeting following Robert's Rules of Order", "Roles: Chairperson, Secretary, and floor members — all are judged", "Every member must know ALL motions — judges target the quiet ones", "Practice running complete mock meetings from opening to adjournment", "Business professional attire required for all team members"))
            "Help Desk" -> EventFormatInfo("Role Play", "Individual", "~15–30 minutes", "Simulated IT help desk scenario with an evaluator acting as the customer", "Scenario packet provided at the event", "Evaluator scores on technical knowledge, problem-solving, and communication.", listOf("Given a technical support scenario with an evaluator playing a customer", "Diagnose the problem and walk the 'customer' through a solution", "Scored on accuracy, communication clarity, and professional conduct", "Requires strong IT knowledge: networking, software, security, hardware", "Business professional attire required"))
            "Computer Applications" -> EventFormatInfo("Production Task", "Individual", "~45–60 minutes", "Complete assigned computer production tasks on provided equipment", "Computer and Microsoft Office provided at testing center", "Scored on accuracy, formatting compliance, and task completion.", listOf("Tasks include word processing, spreadsheets, and presentation work", "Completed on a provided computer — software is Microsoft Office", "Formatting precision matters — follow every instruction exactly", "Practice speed: both accuracy and time factor into scoring", "Business casual or professional attire"))
            "Word Processing" -> EventFormatInfo("Production Task", "Individual", "~45 minutes", "Complete timed word processing production tasks", "Computer and word processing software provided at testing center", "Speed and accuracy both scored — errors are penalized by point deductions.", listOf("Tasks include letters, memos, tables, and formatted reports", "Both speed AND accuracy scored — errors deduct points", "Know business letter, memo, and report formatting cold", "MLA and APA formatting knowledge is also helpful", "Business professional attire required"))
            "Future Business Educator" -> EventFormatInfo("Performance", "Individual", "~30 min lesson + interview", "Teach a business lesson to a judge panel + complete an objective test", "Lesson plan and teaching materials you create in advance", "Scored on lesson design, instructional delivery, and objective test score.", listOf("Prepare and deliver a full business lesson to a panel of judges", "Also includes an objective test component on business education content", "Create your own lesson plan, activities, and student-facing materials", "Scored on content accuracy, student engagement, and instructional design", "Business professional attire required"))
            "Electronic Career Portfolio" -> EventFormatInfo("Project", "Individual", "Portfolio submitted before conference + on-site interview", "Build a digital career portfolio, then defend it in a live interview", "Portfolio submitted digitally before the conference deadline", "Combined: pre-scored portfolio quality + live interview performance at conference.", listOf("Build a professional online portfolio: resume, career goals, work samples, references", "Portfolio is scored before the conference on completeness and quality", "On-site interview at conference adds to your final score", "Treat the interview like a real job interview — business professional attire required", "Use a clean, professional portfolio platform and proofread everything"))
            "Database Design & Applications" -> EventFormatInfo("Project", "Individual", "Project submitted before conference + on-site component", "Design and build a functional relational database for a given scenario", "Project files and documentation submitted before conference deadline", "Scored on database design quality, functionality, relationships, and documentation.", listOf("Design a relational database solving a real-world business problem", "Submit database files and full documentation before the deadline", "May include a live demonstration component at the conference", "Relationships, queries, forms, and reports are all scored", "Business professional attire"))
            "Graphic Design" -> EventFormatInfo("Project", "1–3 members", "Project submitted before conference", "Design a graphic piece meeting given topic and specifications", "Project submitted digitally before the conference deadline", "Scored on design quality, visual principles, creativity, and specification adherence.", listOf("Design topic and specifications provided at the start of the competitive year", "Submit final design files before the conference deadline", "Judged on visual hierarchy, typography, color, creativity, and spec compliance", "Common tools: Adobe Creative Suite, Canva, Figma", "Business professional attire for any conference appearance"))
            "Digital Video Production" -> EventFormatInfo("Project", "1–3 members", "3–5 minute video submitted before conference", "Produce a short professional business video on an assigned topic", "Video submitted digitally before the conference deadline", "Scored on production quality, content accuracy, editing, and creativity.", listOf("Topic assigned at the start of the competitive year", "Produce a 3–5 minute professional-quality business video", "Scored on script, production quality, audio, editing, and message clarity", "Clearly define team roles: director, editor, on-camera talent", "Submit video file and all required documentation before the deadline"))
            "Social Media Strategies" -> EventFormatInfo("Project", "Individual", "Project submitted before conference", "Develop a complete social media marketing strategy for a given scenario", "Plan submitted digitally before the conference deadline", "Scored on strategy depth, creativity, feasibility, and professional presentation.", listOf("Topic/scenario provided at the start of the competitive year", "Create a comprehensive social media marketing plan", "Include: platform selection rationale, content calendar, and KPI tracking plan", "Scored on strategy quality, creativity, and professional document formatting", "Submit project before the conference deadline"))
            "UX Design" -> EventFormatInfo("Project", "Individual", "Project submitted before conference + on-site presentation", "Design a UX solution for a given business scenario", "Project submitted digitally; visual aids allowed for the presentation", "Scored on UX process rigor, prototype quality, and presentation clarity.", listOf("Design scenario provided at the start of the competitive year", "Create wireframes, interactive prototypes, and UX documentation", "Submit project files before the conference deadline", "Present and defend design decisions to judges at the conference", "Scored on research, usability, visual design quality, and rationale"))
            "Mobile Application Development" -> EventFormatInfo("Project", "1–3 members", "App submitted before conference + oral presentation at conference", "Build a fully working mobile app for a business use case", "App and documentation submitted before the conference deadline", "Scored on app functionality, UI/UX design, code quality, and oral presentation.", listOf("Develop a fully functional iOS or Android application", "Topic/scenario provided at the start of the competitive year", "Submit app files and complete documentation before the deadline", "Live presentation + Q&A with judges at conference (~10 minutes)", "Scored on functionality, design, technical quality, and presentation"))
            "Website Design" -> EventFormatInfo("Project", "1–3 members", "Website submitted before conference + oral presentation at conference", "Design and build a website for a given business scenario", "Website submitted before conference; visual aids allowed during presentation", "Scored on site design, functionality, and oral presentation quality.", listOf("Scenario and specifications provided at the start of the competitive year", "Build a fully functional, live website and submit the URL + documentation", "Oral presentation + Q&A with judges at conference (7–10 minutes)", "Scored on design, usability, technical quality, and content accuracy", "Business professional attire required"))
            "Business Plan" -> EventFormatInfo("Project + Presentation", "1–3 members", "Written plan submitted + 10 min oral presentation", "Write a full business plan and present it to a judge panel", "Plan submitted before conference; visual aids and handouts allowed", "Combined score: written plan + oral presentation + Q&A performance.", listOf("Submit a complete business plan before the conference deadline", "Oral presentation: up to 10 minutes + Q&A with judges", "Plan must cover all sections — exec summary through financial projections", "Know your financials from memory — judges will drill your numbers", "Business professional attire required"))
            "Business Ethics" -> EventFormatInfo("Project + Presentation", "1–3 members", "Research project submitted + 10 min oral presentation", "Research a real business ethics case and present findings to judges", "Research project submitted before conference; visual aids allowed", "Combined score: written project quality + oral presentation + Q&A.", listOf("Research and analyze a real-world business ethics situation", "Submit research project and full documentation before the deadline", "Oral presentation: up to 10 minutes + Q&A with judges", "Scored on research depth, ethical analysis quality, and delivery", "Business professional attire required"))
            "E-business" -> EventFormatInfo("Project + Presentation", "1–3 members", "Project submitted before conference + 7 min oral presentation", "Develop an e-business strategy or solution and present it to judges", "Project submitted before conference; visual aids allowed", "Combined score: project quality + oral presentation + Q&A.", listOf("Create a comprehensive e-business plan or functional website", "Submit project before the conference deadline", "Oral presentation: up to 7 minutes + Q&A with judges", "Address a real e-commerce business opportunity with detailed analysis", "Business professional attire required"))
            "Network Design" -> EventFormatInfo("Project + Presentation", "1–3 members", "Project submitted before conference + oral presentation at conference", "Design a complete network infrastructure solution for a given scenario", "Project submitted before conference; visual aids allowed", "Scored on design accuracy, scalability, security, and oral presentation.", listOf("Network infrastructure scenario provided at the start of the competitive year", "Submit detailed network diagrams and documentation before the deadline", "Oral presentation + Q&A explaining every design decision to judges", "Scored on technical accuracy, scalability, security, and cost-effectiveness", "Business professional attire required"))
            "Community Service Project" -> EventFormatInfo("Project + Presentation", "Chapter team", "Project submitted before conference + 10 min oral presentation", "Execute a community service project, document it, and present to judges", "Project portfolio submitted; visual aids and display boards allowed", "Scored on community impact, documentation quality, and presentation delivery.", listOf("Entire chapter participates in a meaningful community service initiative", "Document everything: photos, volunteer hours, impact metrics, and reports", "Submit project portfolio before the conference deadline", "Representative team presents at the conference — up to 10 min + Q&A", "Scored on project scope, measurable community impact, and documentation"))
            "Management Decision Making" -> EventFormatInfo("Case Study", "1–3 members", "30 min prep + 7 min presentation + Q&A", "Analyze a business case study and present your solution to judges", "Case study packet provided — no outside materials allowed", "Judges score on analysis depth, solution quality, and presentation delivery.", listOf("Receive the business case study 30 minutes before your presentation slot", "Work with your team to analyze the problem and build a structured solution", "Present to judges: 7 minutes + Q&A — stay composed under questioning", "No outside notes or resources — only the provided case materials", "Business professional attire required"))
            "Emerging Business Issues" -> EventFormatInfo("Mixed", "1–3 members", "50 min test + 7 min presentation + Q&A", "Objective test on current business issues + team case study presentation", "Case study packet provided for the presentation phase", "Combined: objective test score + judges' presentation rubric.", listOf("Part 1: 100-question objective test on current business issues (50 min)", "Part 2: Case study distributed 30 min before your presentation slot", "Team presentation: 7 minutes + Q&A with judges", "Covers technology trends, global business, ethics, and economic issues", "Business professional attire required"))
            "Client Service" -> EventFormatInfo("Role Play", "1–3 members", "~15 minutes", "Simulated customer service scenario with an evaluator playing a customer", "Scenario provided at the event", "Evaluator scores on customer handling, communication, and problem resolution.", listOf("Given a customer service scenario with an evaluator playing the customer", "Resolve the customer's problem or complaint professionally and empathetically", "Scored on professionalism, empathy, communication, and resolution quality", "Active listening and de-escalation skills are critical", "Business professional attire required"))
            "Sales Presentation" -> EventFormatInfo("Performance", "1–3 members", "10–12 minutes", "Prepared sales presentation delivered to a panel of judge-buyers", "Visual aids, demonstrations, and product samples allowed and encouraged", "Judges score on sales technique, product knowledge, objection handling, and close.", listOf("Present a complete sales pitch for an assigned or chosen product/service", "Judges act as potential buyers — handle every objection professionally", "Visual aids, live demos, and handouts are strongly encouraged", "Scored on opening, needs assessment, benefits pitch, objection handling, and close", "Business professional attire required"))
            "Introduction to Business Communication" -> EventFormatInfo("Performance", "Individual", "3–5 minutes", "Prepared business presentation on a topic announced for the competitive year", "Limited notes may be allowed — check current official guidelines", "Scored on organization, delivery, eye contact, and overall communication.", listOf("Topic announced at the start of the competitive year", "Introductory-level prepared speech — organize clearly and deliver confidently", "Practice timing to hit the middle of the range (around 4 minutes)", "Scored on organization, eye contact, vocal variety, and content quality", "Business professional attire required"))
            else -> EventFormatInfo(
                formatType = if (event.category == "TEAM") "Team Event" else "Individual Event",
                teamSize = if (event.category == "TEAM") "Team" else "Individual",
                duration = "Varies",
                structure = "See official FBLA guidelines at fbla.org",
                materials = "See the official event guidelines from your chapter adviser",
                scoring = "Official FBLA scoring rubric — available from your chapter adviser.",
                keyDetails = listOf("Download the official topic outline from fbla.org", "Ask your chapter adviser for the current event guidelines packet", "Business professional attire is standard for all competitive events", "Practice under realistic competition conditions well before the event")
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        if (selectedEvent == null) {
            // ── EVENT SELECTION SCREEN ──────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 0.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack, tint = textColor)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EVENT PREP HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                        Text("Choose Your Event", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                    }
                    // Search toggle
                    Surface(shape = CircleShape, color = cardBg, border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))) {
                        IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                            Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, null, tint = primaryColor)
                        }
                    }
                }

                // Search bar
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSearch,
                    enter = if (reduceMotion) androidx.compose.animation.EnterTransition.None else androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = if (reduceMotion) androidx.compose.animation.ExitTransition.None else androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search events...", color = textColorTertiary) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = primaryColor.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) }
                    )
                }

                // Category filter chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("ALL", "INDIVIDUAL", "TEAM").forEach { cat ->
                        val isActive = activeCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isActive) primaryColor else cardBg,
                            border = BorderStroke(1.dp, if (isActive) primaryColor else primaryColor.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { activeCategory = cat }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(when (cat) { "ALL" -> Icons.Default.GridView; "INDIVIDUAL" -> Icons.Default.Person; else -> Icons.Default.Group }, null, tint = if (isActive) Color.Black else primaryColor, modifier = Modifier.size(14.dp))
                                Text(when (cat) { "ALL" -> "All"; "INDIVIDUAL" -> "Individual"; else -> "Team" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp), color = if (isActive) Color.Black else textColor)
                            }
                        }
                    }
                }

                // Count
                Text(
                    "${filteredEvents.size} events",
                    color = textColorTertiary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                // Event grid
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    filteredEvents.chunked(2).forEach { rowItems ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { event ->
                                    Card(
                                        modifier = Modifier.weight(1f).height(126.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        onClick = {
                                            selectedEvent = event
                                            isLoadingPrep = true
                                            activeTab = 0
                                            scope.launch {
                                                prepContent = withContext(Dispatchers.Default) { getPrepContent(event, 0) }
                                                isLoadingPrep = false
                                            }
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Icon(event.icon, null, tint = primaryColor, modifier = Modifier.size((26 * textSizeMultiplier).dp))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    event.name,
                                                    color = textColor,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    event.category,
                                                    color = primaryColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp)
                                                )
                                            }
                                            val fmtLabel = when (getFormatInfo(event).formatType) {
                                                "Objective Test" -> "Obj. Test"
                                                "Project + Presentation" -> "Proj. + Pres."
                                                "Case Study" -> "Case Study"
                                                "Role Play" -> "Role Play"
                                                "Performance" -> "Performance"
                                                "Mixed" -> "Mixed"
                                                else -> getFormatInfo(event).formatType
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = primaryColor.copy(alpha = 0.13f)
                                            ) {
                                                Text(
                                                    fmtLabel,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = primaryColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (9 * textSizeMultiplier).sp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        } else {
            // ── EVENT PREP DETAIL SCREEN ────────────────────────────────────
            val event = selectedEvent!!
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with back + event name
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 0.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = { selectedEvent = null; prepContent = "" }, tint = textColor)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EVENT PREP HUB", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (10 * textSizeMultiplier).sp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(event.icon, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            Text(event.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (19 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            event.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp),
                            color = primaryColor
                        )
                    }
                }

                // Tab row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data class TabDef(val label: String, val icon: ImageVector)
                    listOf(TabDef("Overview", Icons.Default.Info), TabDef("Format", Icons.Default.TableChart), TabDef("Win Tips", Icons.Default.EmojiEvents)).forEachIndexed { index, tabDef ->
                        val isActive = activeTab == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) primaryColor else cardBg,
                            border = BorderStroke(1.dp, if (isActive) primaryColor else primaryColor.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f).clickable {
                                if (activeTab != index) {
                                    activeTab = index
                                    if (index != 1) {
                                        isLoadingPrep = true
                                        val tabArg = if (index == 0) 0 else 1
                                        scope.launch {
                                            prepContent = withContext(Dispatchers.Default) { getPrepContent(event, tabArg) }
                                            isLoadingPrep = false
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(tabDef.icon, null, tint = if (isActive) Color.Black else primaryColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tabDef.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = if (isActive) Color.Black else textColorSecondary)
                            }
                        }
                    }
                }

                // Content area
                if (isLoadingPrep) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(40.dp))
                            Text("Loading prep content...", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                        }
                    }
                } else if (activeTab == 1) {
                    val fmt = getFormatInfo(event)
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            // Header card — format type + stat chips
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Default.TableChart, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                        Text(fmt.formatType, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = primaryColor)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            Triple(Icons.Default.Group, "TEAM SIZE", fmt.teamSize),
                                            Triple(Icons.Default.Timer, "DURATION", fmt.duration)
                                        ).forEach { (icon, label, value) ->
                                            Surface(shape = RoundedCornerShape(10.dp), color = cardBg, border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)), modifier = Modifier.weight(1f)) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(icon, null, tint = primaryColor, modifier = Modifier.size(12.dp))
                                                        Text(label, color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (9 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold))
                                                    }
                                                    Text(value, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            // Details card — structure, materials, scoring
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    @Composable
                                    fun DetailSection(icon: ImageVector, label: String, body: String) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                                Text(label, color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp))
                                            }
                                            Text(body, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                        }
                                    }
                                    DetailSection(Icons.Default.AccountTree, "STRUCTURE", fmt.structure)
                                    androidx.compose.material3.HorizontalDivider(color = primaryColor.copy(alpha = 0.12f))
                                    DetailSection(Icons.AutoMirrored.Filled.LibraryBooks, "MATERIALS ALLOWED", fmt.materials)
                                    androidx.compose.material3.HorizontalDivider(color = primaryColor.copy(alpha = 0.12f))
                                    DetailSection(Icons.Default.Star, "SCORING", fmt.scoring)
                                }
                            }
                        }
                        item {
                            // Key details numbered list
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Lightbulb, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                        Text("KEY DETAILS", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp))
                                    }
                                    fmt.keyDetails.forEachIndexed { i, detail ->
                                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Surface(shape = CircleShape, color = primaryColor, modifier = Modifier.size(20.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${i + 1}", color = Color.Black, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (9 * textSizeMultiplier).sp))
                                                }
                                            }
                                            Text(detail, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp), modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                    // Parse and render the content with bold markdown-style text
                                    prepContent.split("\n").forEach { line ->
                                        when {
                                            line.startsWith("**") && line.endsWith("**") -> {
                                                Text(
                                                    line.removePrefix("**").removeSuffix("**"),
                                                    color = primaryColor,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp),
                                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                                )
                                            }
                                            line.startsWith("•") || line.startsWith("-") -> {
                                                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                                    Text("•", color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp), modifier = Modifier.padding(end = 8.dp, top = 1.dp))
                                                    Text(line.removePrefix("•").removePrefix("-").trim(), color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                                }
                                            }
                                            line.isNotBlank() -> {
                                                Text(line, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp), modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                            else -> Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick-action cards at the bottom
                            Text("QUICK ACTIONS", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                data class QuickAction(val icon: ImageVector, val label: String)
                                listOf(
                                    QuickAction(Icons.Default.NotificationAdd, "Reminders"),
                                    QuickAction(Icons.Default.FormatListBulleted, "All Events"),
                                    QuickAction(Icons.AutoMirrored.Filled.LibraryBooks, "Guidelines")
                                ).forEach { action ->
                                    Card(
                                        modifier = Modifier.weight(1f).height(70.dp),
                                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                                        onClick = {}
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(action.icon, null, tint = primaryColor, modifier = Modifier.size((20 * textSizeMultiplier).dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(action.label, color = textColorSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestingHub(
    onBack: () -> Unit,
    startAt: String = "home",
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)

    data class QuizQuestion(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String = "")
    data class TestEvent(val name: String, val icon: ImageVector, val questions: List<QuizQuestion>)

    val allEvents = remember {
        listOf(
            TestEvent("Business Law", Icons.Default.Gavel, listOf(
                QuizQuestion("What type of contract is formed when both parties exchange promises?", listOf("Void contract", "Bilateral contract", "Unilateral contract", "Quasi-contract"), 1, "A bilateral contract involves mutual promises — one offer, one acceptance, creating obligations on both sides."),
                QuizQuestion("Which element is NOT required for a valid contract?", listOf("Offer", "Acceptance", "Written form", "Consideration"), 2, "Contracts don't have to be in writing to be valid — most oral contracts are fully enforceable."),
                QuizQuestion("A contract signed by a minor is generally:", listOf("Void", "Voidable", "Valid and binding", "Illegal"), 1, "Contracts with minors are voidable — the minor can choose to disaffirm the contract."),
                QuizQuestion("The Statute of Frauds requires certain contracts to be:", listOf("Notarized", "In writing", "Witnessed by two people", "Registered with the state"), 1, "The Statute of Frauds requires certain contracts (like real estate sales) to be in writing to be enforceable."),
                QuizQuestion("Which business entity offers limited liability to ALL owners?", listOf("Sole proprietorship", "General partnership", "LLC", "Limited partnership"), 2, "An LLC protects all members from personal liability for business debts."),
                QuizQuestion("Bankruptcy under Chapter 11 allows a business to:", listOf("Liquidate all assets", "Reorganize and continue operating", "Transfer debts to owners", "Avoid all creditors permanently"), 1, "Chapter 11 is a reorganization bankruptcy that lets businesses restructure debt while continuing operations."),
                QuizQuestion("The Uniform Commercial Code (UCC) primarily governs:", listOf("Employment law", "Real estate transactions", "Sale of goods", "Intellectual property"), 2, "The UCC provides consistent rules for commercial transactions, especially the sale of goods."),
                QuizQuestion("Which of the following is an example of a tort?", listOf("Breach of contract", "Tax evasion", "Negligence", "Robbery"), 2, "A tort is a civil wrong — negligence (failure to exercise reasonable care) is a classic example."),
                QuizQuestion("Copyright protects:", listOf("Inventions", "Original works of authorship", "Brand names", "Trade secrets"), 1, "Copyright protects original creative works like books, music, and software automatically upon creation."),
                QuizQuestion("Apparent authority arises when:", listOf("An agent exceeds written authority", "A principal's actions make a third party reasonably believe authority exists", "An agent acts illegally", "A contract is breached"), 1, "Apparent authority exists when the principal's conduct leads a reasonable third party to believe the agent has authority.")
            )),
            TestEvent("Economics", Icons.Default.TrendingUp, listOf(
                QuizQuestion("When demand increases and supply stays constant, price will:", listOf("Decrease", "Stay the same", "Increase", "Become inelastic"), 2, "Higher demand with unchanged supply means buyers compete, driving price up."),
                QuizQuestion("GDP stands for:", listOf("General Demand Price", "Gross Domestic Product", "Government Distribution Plan", "Gross Deficit Percentage"), 1, "GDP measures the total value of all goods and services produced within a country."),
                QuizQuestion("The Federal Reserve controls inflation primarily by:", listOf("Changing tax rates", "Adjusting interest rates", "Printing currency", "Setting import tariffs"), 1, "The Fed raises interest rates to slow borrowing and spending, reducing inflation."),
                QuizQuestion("A market with only one seller is called:", listOf("Oligopoly", "Monopoly", "Perfect competition", "Monopsony"), 1, "A monopoly exists when a single company controls the entire market for a product."),
                QuizQuestion("Opportunity cost is best defined as:", listOf("The price of a good", "The cost of production", "The value of the next best alternative foregone", "Total sunk costs"), 2, "Opportunity cost is what you give up when choosing one option over the next best alternative."),
                QuizQuestion("A command economy is controlled by:", listOf("Free markets", "Consumer choices", "The government", "Corporations"), 2, "In a command economy, the government makes all major economic decisions about production and distribution."),
                QuizQuestion("Inflation is best described as:", listOf("A decrease in GDP", "A general rise in price levels", "An increase in unemployment", "A drop in interest rates"), 1, "Inflation is the rate at which the general level of prices rises over time, reducing purchasing power."),
                QuizQuestion("The law of diminishing returns states that:", listOf("Profits always increase with production", "Adding more of one input eventually yields smaller gains", "Costs decrease as output rises", "Supply always meets demand"), 1, "After a point, adding more of one factor of production yields progressively smaller output gains."),
                QuizQuestion("A tariff is:", listOf("A domestic production subsidy", "A tax on imported goods", "A limit on the quantity of imports", "A trade agreement"), 1, "A tariff is a tax imposed on goods imported from another country, making them more expensive."),
                QuizQuestion("For normal goods, as consumer income increases, demand typically:", listOf("Decreases", "Stays the same", "Increases", "Becomes perfectly inelastic"), 2, "For normal goods, higher income means consumers can afford more, so demand increases.")
            )),
            TestEvent("Accounting I", Icons.Default.Calculate, listOf(
                QuizQuestion("The accounting equation is:", listOf("Assets = Revenue - Expenses", "Assets = Liabilities + Owner's Equity", "Revenue = Assets + Liabilities", "Equity = Assets - Revenue"), 1, "Assets = Liabilities + Owner's Equity — this must always balance."),
                QuizQuestion("Which statement shows revenues and expenses?", listOf("Balance sheet", "Cash flow statement", "Income statement", "Statement of equity"), 2, "The income statement summarizes revenues, costs, and expenses during a specific period."),
                QuizQuestion("A debit to an asset account will:", listOf("Decrease it", "Increase it", "Have no effect", "Close it"), 1, "Assets have a normal debit balance, so debiting an asset account increases it."),
                QuizQuestion("Accounts Receivable is classified as:", listOf("A liability", "An equity account", "A current asset", "A long-term asset"), 2, "Accounts Receivable is money owed to the business, expected within a year — a current asset."),
                QuizQuestion("The matching principle states that:", listOf("Assets match liabilities", "Expenses are recorded in the same period as related revenues", "Revenue equals expenses", "Cash equals net income"), 1, "The matching principle ensures expenses are recognized in the same period as the revenues they generate."),
                QuizQuestion("Accumulated Depreciation appears on the balance sheet as:", listOf("A liability", "A contra-asset", "Owner's equity", "An expense"), 1, "Accumulated Depreciation is a contra-asset that reduces the asset's book value."),
                QuizQuestion("Which of the following is a liability?", listOf("Prepaid insurance", "Accounts payable", "Equipment", "Retained earnings"), 1, "Accounts Payable is money owed by the business to suppliers — a current liability."),
                QuizQuestion("A trial balance verifies that:", listOf("Net income is correct", "Total debits equal total credits", "Taxes are paid", "Inventory is accurate"), 1, "A trial balance lists all accounts and confirms that total debits equal total credits."),
                QuizQuestion("GAAP stands for:", listOf("General Accounting and Auditing Principles", "Generally Accepted Accounting Principles", "Government Approved Accounting Procedures", "General Asset Allocation Plan"), 1, "GAAP is the standard framework of accounting rules used in the United States."),
                QuizQuestion("Owner withdrawals are recorded by:", listOf("Debiting Cash, crediting Capital", "Debiting Drawings, crediting Cash", "Debiting Revenue, crediting Cash", "Crediting Drawings, debiting Capital"), 1, "Owner withdrawals: Debit Drawings (reduces equity) and Credit Cash (reduces assets).")
            )),
            TestEvent("Personal Finance", Icons.Default.AccountBalanceWallet, listOf(
                QuizQuestion("A credit score is primarily determined by:", listOf("Your income level", "Payment history and credit utilization", "Number of bank accounts", "Employment status"), 1, "Payment history (35%) and credit utilization (30%) are the two biggest FICO score factors."),
                QuizQuestion("The Rule of 72 estimates:", listOf("Loan repayment time", "How long to double an investment", "Monthly budget allocations", "Credit card interest"), 1, "Divide 72 by the annual interest rate to estimate years for an investment to double."),
                QuizQuestion("Which investment typically has the highest risk AND highest potential return?", listOf("Savings account", "Government bonds", "Stocks", "Certificates of deposit"), 2, "Stocks historically offer the highest returns but carry the most volatility and risk."),
                QuizQuestion("Roth IRA withdrawals in retirement are:", listOf("Fully taxed", "Tax-free", "Penalized", "Limited to $500/month"), 1, "Roth IRA contributions are after-tax, so qualified withdrawals in retirement are completely tax-free."),
                QuizQuestion("Net worth is calculated as:", listOf("Income - Expenses", "Assets - Liabilities", "Revenue - Costs", "Savings - Debt"), 1, "Net worth = Total Assets - Total Liabilities, representing your true financial position."),
                QuizQuestion("Collision insurance covers:", listOf("Damage to another person's car", "Damage to your car in an accident", "Medical bills after an accident", "Theft of your vehicle"), 1, "Collision insurance covers damage to your own vehicle regardless of fault."),
                QuizQuestion("Compound interest differs from simple interest because:", listOf("It uses a lower rate", "Interest is earned on interest", "It only applies to savings", "It has fewer fees"), 1, "Compound interest calculates interest on the principal AND previously earned interest."),
                QuizQuestion("An emergency fund should cover:", listOf("1 week of expenses", "1 month of expenses", "3–6 months of expenses", "1 year of expenses"), 2, "Financial experts recommend 3–6 months of living expenses for a proper emergency fund."),
                QuizQuestion("A 401(k) plan is:", listOf("A government pension program", "An employer-sponsored retirement savings plan", "A type of savings account", "Federal health insurance"), 1, "A 401(k) is an employer-sponsored retirement plan with pre-tax contributions, often employer-matched."),
                QuizQuestion("Diversification in investing means:", listOf("Investing only in bonds", "Putting all money in one stock", "Spreading investments across different assets", "Avoiding international markets"), 2, "Diversification reduces risk by spreading investments across various asset types, sectors, and geographies.")
            )),
            TestEvent("Marketing", Icons.Default.Campaign, listOf(
                QuizQuestion("The 4 Ps of marketing are:", listOf("People, Process, Planning, Profit", "Product, Price, Place, Promotion", "Product, Profit, Public, Positioning", "Price, Placement, People, Platform"), 1, "The marketing mix: Product, Price, Place (distribution), and Promotion."),
                QuizQuestion("A target market is:", listOf("The largest possible audience", "A specific group of consumers a business aims at", "All people who buy a product", "The media used for advertising"), 1, "A target market is the defined segment most likely to want and buy your product."),
                QuizQuestion("Brand equity refers to:", listOf("The cash value of a brand", "The additional value a brand name gives a product", "The cost to advertise", "Legal trademark protection"), 1, "Brand equity is the premium value a product gains from a recognizable brand name."),
                QuizQuestion("A loss leader pricing strategy involves:", listOf("Pricing above competitors", "Selling a product below cost to attract customers", "Reducing production costs", "Bundling products together"), 1, "Loss leader pricing draws customers in with a below-cost product, expecting profitable secondary purchases."),
                QuizQuestion("SWOT analysis stands for:", listOf("Sales, Work, Operations, Trends", "Strengths, Weaknesses, Opportunities, Threats", "Strategy, Win, Optimize, Target", "Speed, Width, Output, Technology"), 1, "SWOT examines internal Strengths/Weaknesses and external Opportunities/Threats."),
                QuizQuestion("SEO aims to:", listOf("Buy ad space on Google", "Improve organic search rankings", "Create viral social media posts", "Design better websites"), 1, "SEO improves a website's visibility in organic (unpaid) search engine results."),
                QuizQuestion("Market segmentation divides consumers by:", listOf("Random selection", "Demographics, psychographics, behavior, or geography", "Income levels only", "Government regulations"), 1, "Segmentation groups consumers by demographics (age, gender), psychographics, behavior, or geography."),
                QuizQuestion("Customer lifetime value (CLV) measures:", listOf("One-time purchase value", "Total revenue from a customer over the relationship", "Cost to acquire a customer", "Monthly sales average"), 1, "CLV is the total net profit expected from a customer throughout their entire relationship with a business."),
                QuizQuestion("A push distribution strategy:", listOf("Pulls consumers to buy", "Pushes product through the supply chain to retailers", "Focuses on online sales only", "Eliminates intermediaries"), 1, "Push strategy promotes products through the supply chain — manufacturer to distributor to retailer."),
                QuizQuestion("Advertising differs from publicity in that advertising is:", listOf("Free", "Paid and from an identified sponsor", "Always digital", "Word of mouth"), 1, "Advertising is paid, non-personal communication from an identified sponsor through media channels.")
            )),
            TestEvent("Introduction to Business", Icons.Default.Business, listOf(
                QuizQuestion("Which business form is easiest to start?", listOf("Corporation", "Partnership", "Sole proprietorship", "LLC"), 2, "A sole proprietorship requires minimal legal formalities — one person owns and operates the business."),
                QuizQuestion("A corporation's owners are called:", listOf("Partners", "Members", "Shareholders", "Proprietors"), 2, "Corporations issue stock; those who own that stock are shareholders."),
                QuizQuestion("Working capital is:", listOf("Total assets - Total liabilities", "Current assets - Current liabilities", "Revenue - Expenses", "Cash + Receivables"), 1, "Working capital = Current Assets - Current Liabilities, measuring short-term financial health."),
                QuizQuestion("The primary goal of most businesses is to:", listOf("Create jobs", "Pay taxes", "Earn a profit", "Help the community"), 2, "While businesses have many stakeholders, generating profit for owners is the primary financial objective."),
                QuizQuestion("Human Resources manages:", listOf("Product development", "Financial reporting", "Hiring and employee relations", "Supply chain"), 2, "HR handles recruitment, hiring, training, benefits, and employee relations."),
                QuizQuestion("E-commerce refers to:", listOf("Electronic accounting", "Buying and selling over the internet", "Email marketing", "Stock trading"), 1, "E-commerce is the buying and selling of goods and services via the internet."),
                QuizQuestion("A fixed cost:", listOf("Changes with production volume", "Stays constant regardless of output", "Is always zero at zero output", "Is the same as variable cost"), 1, "Fixed costs (like rent) remain constant regardless of how much is produced."),
                QuizQuestion("Business ethics means:", listOf("Maximizing profit at any cost", "Acting in accordance with moral principles", "Avoiding all regulations", "Prioritizing only shareholders"), 1, "Business ethics involves fair, legally compliant decisions that are morally responsible to all stakeholders."),
                QuizQuestion("Supply chain management oversees:", listOf("Employee pay scales", "Flow of goods from supplier to customer", "Setting product prices", "Customer complaints"), 1, "Supply chain management coordinates product movement from raw materials through production to the final customer."),
                QuizQuestion("Entrepreneurship involves:", listOf("Managing an existing large company", "Creating and running a new business venture", "Working for the government", "Only investing in stocks"), 1, "Entrepreneurs identify opportunities, take calculated risks, and create new business ventures.")
            )),
            TestEvent("Cybersecurity", Icons.Default.Security, listOf(
                QuizQuestion("Phishing attacks typically occur through:", listOf("Physical theft", "Deceptive emails or websites", "Software bugs", "Hardware failures"), 1, "Phishing uses fake emails or websites that appear legitimate to steal sensitive information."),
                QuizQuestion("A firewall primarily:", listOf("Encrypts data", "Monitors and filters network traffic", "Backs up data", "Removes viruses"), 1, "A firewall monitors incoming and outgoing network traffic and blocks unauthorized access."),
                QuizQuestion("Multi-factor authentication requires:", listOf("A very long password", "Multiple independent verification methods", "A physical key only", "Biometrics only"), 1, "MFA requires two or more verification factors (password + phone code, etc.) to increase security."),
                QuizQuestion("A DDoS attack:", listOf("Steals data silently", "Floods a server with traffic to shut it down", "Installs ransomware", "Intercepts emails"), 1, "A DDoS (Distributed Denial of Service) attack overwhelms a server with traffic from multiple sources."),
                QuizQuestion("Encryption converts data into:", listOf("Compressed files", "An unreadable format without the key", "Smaller file sizes", "Backup copies"), 1, "Encryption scrambles data so only someone with the correct decryption key can read it."),
                QuizQuestion("Social engineering exploits:", listOf("Software vulnerabilities", "Human psychology and trust", "Hardware weaknesses", "Network protocols"), 1, "Social engineering manipulates people rather than systems — tricking users into giving access or information."),
                QuizQuestion("Ransomware:", listOf("Slows down a computer", "Encrypts files and demands payment", "Steals passwords silently", "Deletes system logs"), 1, "Ransomware encrypts a victim's files and demands a ransom in exchange for the decryption key."),
                QuizQuestion("The CIA Triad stands for:", listOf("Computers, Internet, Access", "Confidentiality, Integrity, Availability", "Cyber, Intelligence, Authentication", "Control, Identify, Audit"), 1, "The CIA Triad is the foundational security model: Confidentiality, Integrity, and Availability."),
                QuizQuestion("A VPN provides:", listOf("Faster internet speed", "Encrypted and private network connections", "Free internet access", "Cloud storage"), 1, "A VPN (Virtual Private Network) creates an encrypted tunnel for internet traffic, protecting privacy."),
                QuizQuestion("Best practice for password security is:", listOf("Use the same password everywhere", "Use short memorable passwords", "Use long unique passwords with a password manager", "Share passwords with trusted colleagues"), 2, "Strong password hygiene: unique, long passwords for each account, managed by a password manager.")
            )),
            TestEvent("Business Communication", Icons.Default.Forum, listOf(
                QuizQuestion("The most formal written business communication is:", listOf("Text message", "Email", "Business letter", "Instant message"), 2, "A formal business letter on letterhead follows strict formatting conventions — the most formal written form."),
                QuizQuestion("Active listening involves:", listOf("Waiting for your turn to speak", "Fully concentrating, understanding, and responding thoughtfully", "Only taking detailed notes", "Agreeing with everything"), 1, "Active listening means fully engaging: eye contact, no distractions, and thoughtful responses."),
                QuizQuestion("An executive summary:", listOf("Replaces the full report", "Provides a concise overview of a longer document", "Lists all data sources", "Includes all appendices"), 1, "An executive summary gives busy readers a quick overview of key points and recommendations."),
                QuizQuestion("The subject line of a business email should:", listOf("Be left blank", "Be vague to build curiosity", "Clearly summarize the email's purpose", "Include the sender's name"), 2, "A clear, concise subject line immediately tells the recipient what the email is about."),
                QuizQuestion("The 7 Cs of effective communication include:", listOf("Clear, Concise, Concrete, Correct, Coherent, Complete, Courteous", "Creative, Candid, Concise, Caring, Clear, Correct, Cohesive", "Careful, Clear, Consistent, Correct, Complete, Constructive, Courteous", "Concise, Creative, Clear, Casual, Complete, Correct, Consistent"), 0, "The 7 Cs: Clear, Concise, Concrete, Correct, Coherent, Complete, and Courteous."),
                QuizQuestion("A memo is used for:", listOf("External customer communications", "Internal organizational communication", "Legal contracts", "Marketing campaigns"), 1, "Memos are internal documents used within an organization to share information or directives."),
                QuizQuestion("Jargon should be avoided in business writing because:", listOf("It adds professionalism", "It may confuse readers unfamiliar with the terms", "It makes writing longer", "It is always grammatically incorrect"), 1, "Jargon can exclude or confuse readers who don't share the specialized vocabulary."),
                QuizQuestion("For sensitive negative news, the best channel is:", listOf("Mass email", "Social media post", "Face-to-face meeting", "Text message"), 2, "Sensitive or complex messages warrant face-to-face delivery to allow immediate feedback and empathy."),
                QuizQuestion("Nonverbal communication includes:", listOf("Only written messages", "Body language, facial expressions, and tone", "Email and texts only", "Formal presentations"), 1, "Nonverbal communication includes body language, gestures, eye contact, expressions, and vocal tone."),
                QuizQuestion("Plagiarism in business writing means:", listOf("Using technical language", "Using someone's work without attribution", "Writing too informally", "Including too many facts"), 1, "Plagiarism is using another person's words or ideas without proper credit — unethical and potentially illegal.")
            )),
            TestEvent("Business Calculations", Icons.Default.Functions, listOf(
                QuizQuestion("An item costs \$80 with a 25% markup. Selling price is:", listOf("\$95", "\$100", "\$105", "\$110"), 1, "Selling price = \$80 × 1.25 = \$100."),
                QuizQuestion("Gross profit margin on \$200 cost / \$250 selling price:", listOf("20%", "25%", "50%", "80%"), 0, "Gross profit margin = (\$250 - \$200) / \$250 = \$50 / \$250 = 20%."),
                QuizQuestion("Simple interest on \$1,000 at 5% for 3 years:", listOf("\$50", "\$150", "\$157.63", "\$1,150"), 1, "Simple interest = Principal × Rate × Time = \$1,000 × 0.05 × 3 = \$150."),
                QuizQuestion("6% commission on \$15,000 in sales equals:", listOf("\$600", "\$900", "\$750", "\$1,050"), 1, "Commission = \$15,000 × 0.06 = \$900."),
                QuizQuestion("\$5,000 at 4% compounded annually for 2 years equals:", listOf("\$5,200", "\$5,400", "\$5,408", "\$5,416"), 2, "A = \$5,000 × (1.04)² = \$5,000 × 1.0816 = \$5,408."),
                QuizQuestion("20% discount on \$75 gives a sale price of:", listOf("\$55", "\$60", "\$65", "\$70"), 1, "Discount = \$75 × 0.20 = \$15. Sale price = \$75 - \$15 = \$60."),
                QuizQuestion("Payroll tax at 7.65% on \$2,500:", listOf("\$159.25", "\$191.25", "\$200.00", "\$215.75"), 1, "Payroll tax = \$2,500 × 0.0765 = \$191.25."),
                QuizQuestion("Break-even: Fixed costs \$10,000, price \$50, variable cost \$30:", listOf("200 units", "333 units", "500 units", "667 units"), 2, "Break-even = \$10,000 / (\$50 - \$30) = \$10,000 / \$20 = 500 units."),
                QuizQuestion("Inventory turnover of 4 with COGS \$120,000 gives avg inventory of:", listOf("\$20,000", "\$30,000", "\$40,000", "\$50,000"), 1, "Average Inventory = COGS / Turnover = \$120,000 / 4 = \$30,000."),
                QuizQuestion("15% of \$340 equals:", listOf("\$45", "\$51", "\$48", "\$56"), 1, "15% × \$340 = 0.15 × \$340 = \$51.")
            )),
            TestEvent("Entrepreneurship", Icons.Default.Lightbulb, listOf(
                QuizQuestion("An entrepreneur's primary role is:", listOf("Managing an existing company", "Identifying opportunities and starting new ventures", "Working in government", "Investing in stocks"), 1, "Entrepreneurs spot market gaps, take calculated risks, and build new businesses from the ground up."),
                QuizQuestion("A business plan's executive summary should appear:", listOf("At the end", "In the appendix", "At the beginning", "In the financial section"), 2, "The executive summary comes first and gives readers a concise overview of the entire business plan."),
                QuizQuestion("Market research helps entrepreneurs:", listOf("Avoid hiring employees", "Understand customer needs and competition", "Set employee salaries", "File taxes"), 1, "Market research gathers information about target customers, competitors, and industry trends to validate business ideas."),
                QuizQuestion("A sole proprietor has:", listOf("Limited personal liability", "Unlimited personal liability", "No tax obligations", "Shared ownership"), 1, "Sole proprietors are personally liable for all business debts — there's no legal separation between owner and business."),
                QuizQuestion("Angel investors typically provide:", listOf("Bank loans at low rates", "Early-stage capital in exchange for equity", "Government grants", "Equipment leases"), 1, "Angel investors are individuals who invest personal capital in early-stage startups, usually for an equity stake."),
                QuizQuestion("Break-even analysis tells a business:", listOf("Its profit margin", "How many units it must sell to cover all costs", "Its market share", "How to price above competitors"), 1, "Break-even analysis calculates the sales volume needed to cover all fixed and variable costs."),
                QuizQuestion("A competitive advantage means:", listOf("Having the most employees", "Something that makes your business better than rivals in customers' eyes", "Being the oldest company", "Having the lowest price always"), 1, "A sustainable competitive advantage is a unique strength — quality, cost, or innovation — that rivals can't easily copy."),
                QuizQuestion("Bootstrapping a startup means:", listOf("Getting a large bank loan", "Receiving venture capital", "Self-funding without outside investors", "Filing for an IPO"), 2, "Bootstrapping means funding a business using personal savings or revenue, avoiding outside investors."),
                QuizQuestion("A franchise business model means:", listOf("Starting a brand-new concept", "Licensing an established brand's system and name", "Merging with a competitor", "Going public on a stock exchange"), 1, "Franchising lets an entrepreneur operate under an established brand by paying fees and following the franchisor's system."),
                QuizQuestion("A value proposition is:", listOf("The company's stock value", "A clear statement of how your product solves a customer problem", "A financial forecast", "A legal document"), 1, "A value proposition explains specifically why customers should buy from you — the unique benefit you offer.")
            )),
            TestEvent("Securities & Investments", Icons.Default.ShowChart, listOf(
                QuizQuestion("A stock represents:", listOf("A loan to a company", "Partial ownership in a company", "A guaranteed fixed return", "Government debt"), 1, "Buying stock means purchasing a share of ownership in a corporation, entitling you to a portion of its profits."),
                QuizQuestion("A bond is best described as:", listOf("Ownership in a company", "A loan made to a borrower that pays interest", "A mutual fund share", "A real estate investment"), 1, "A bond is a debt instrument — you lend money to the issuer in exchange for regular interest payments and principal return."),
                QuizQuestion("The P/E ratio measures:", listOf("Profit divided by Equity", "Stock price divided by earnings per share", "Portfolio performance", "Price relative to book value"), 1, "Price-to-Earnings ratio = Stock Price / EPS. A higher P/E suggests investors expect strong future growth."),
                QuizQuestion("A bear market is characterized by:", listOf("Rising prices over 20%", "Falling prices of 20% or more", "Stable prices", "Low trading volume"), 1, "A bear market is a sustained decline of 20%+ from recent highs, typically accompanied by pessimism."),
                QuizQuestion("Diversification reduces:", listOf("Market returns", "Unsystematic (company-specific) risk", "Inflation risk", "Interest rate risk"), 1, "Diversification spreads investments so one holding's poor performance doesn't devastate the whole portfolio."),
                QuizQuestion("A mutual fund pools:", listOf("Only bonds", "Money from many investors to buy a diversified portfolio", "Only real estate assets", "Money from one investor"), 1, "Mutual funds collect money from many investors to purchase a diversified mix of stocks, bonds, or other assets."),
                QuizQuestion("Dollar-cost averaging means:", listOf("Buying at the lowest price only", "Investing a fixed amount at regular intervals regardless of price", "Selling at the highest price", "Borrowing to invest"), 1, "Dollar-cost averaging invests a fixed dollar amount on a schedule, buying more shares when prices are low."),
                QuizQuestion("The SEC primarily:", listOf("Sets interest rates", "Regulates securities markets and protects investors", "Manages the federal budget", "Controls the money supply"), 1, "The Securities and Exchange Commission enforces securities laws, ensuring fair and transparent markets."),
                QuizQuestion("Preferred stock differs from common stock in that preferred stockholders:", listOf("Vote on company decisions", "Receive dividends first and have priority in liquidation", "Get higher capital gains", "Bear more risk"), 1, "Preferred stockholders receive dividends before common stockholders and have priority if the company liquidates."),
                QuizQuestion("An index fund tracks:", listOf("A single company's stock", "A market index like the S&P 500", "Only government bonds", "Real estate values"), 1, "Index funds passively replicate the holdings and performance of a market index, offering broad diversification at low cost.")
            )),
            TestEvent("Accounting II", Icons.Default.Calculate, listOf(
                QuizQuestion("Under FIFO during inflation, ending inventory is:", listOf("Lower", "Higher", "Unchanged", "Equal to LIFO"), 1, "FIFO assumes oldest costs are used first, so remaining inventory reflects more recent (higher) prices."),
                QuizQuestion("The current ratio measures:", listOf("Profitability", "Short-term liquidity", "Long-term debt", "Asset efficiency"), 1, "Current Ratio = Current Assets / Current Liabilities. Values above 1 indicate positive short-term liquidity."),
                QuizQuestion("Goodwill is recorded when:", listOf("A company buys equipment", "A business is acquired for more than fair market value of net assets", "Revenue exceeds expenses", "Depreciation is recorded"), 1, "Goodwill represents the premium paid above the fair value of identifiable net assets in a business acquisition."),
                QuizQuestion("The debt-to-equity ratio tells investors:", listOf("Profit margin", "How much debt is used relative to equity financing", "Dividend yield", "Earnings per share"), 1, "Debt-to-Equity = Total Liabilities / Shareholders' Equity. Higher ratios mean more financial leverage and risk."),
                QuizQuestion("A contra-revenue account is:", listOf("Unearned revenue", "Sales returns and allowances", "Accounts receivable", "Prepaid expenses"), 1, "Sales Returns and Allowances is a contra-revenue account that reduces gross revenue to arrive at net revenue."),
                QuizQuestion("Under the accrual method, revenue is recognized:", listOf("When cash is received", "When earned, regardless of payment", "When the invoice is sent", "At year-end only"), 1, "Accrual accounting recognizes revenue when earned and expenses when incurred, regardless of cash flow timing."),
                QuizQuestion("Straight-line depreciation spreads cost:", listOf("Based on usage", "Equally over the asset's useful life", "Faster in early years", "Based on revenue"), 1, "Straight-line depreciation allocates equal expense each year: (Cost - Salvage Value) / Useful Life."),
                QuizQuestion("A deferred revenue liability means:", listOf("Revenue was earned but not received", "Cash was received before the service was delivered", "Expenses were paid early", "A customer owes money"), 1, "Deferred revenue is a liability because the company received payment but hasn't yet provided the product or service."),
                QuizQuestion("Return on equity (ROE) equals:", listOf("Net Income / Total Assets", "Net Income / Shareholders' Equity", "Gross Profit / Revenue", "Operating Income / Sales"), 1, "ROE = Net Income / Shareholders' Equity. It measures how effectively management uses equity to generate profit."),
                QuizQuestion("A cash flow statement shows:", listOf("Net income for the period", "Cash inflows and outflows from operating, investing, and financing activities", "Stockholder equity changes", "Revenue and expenses only"), 1, "The cash flow statement tracks actual cash movements across three activities: operating, investing, and financing.")
            )),
            TestEvent("Management Decision Making", Icons.Default.Psychology, listOf(
                QuizQuestion("SWOT analysis is used to:", listOf("Calculate financial ratios", "Evaluate internal strengths/weaknesses and external opportunities/threats", "Create org charts", "Measure employee satisfaction"), 1, "SWOT maps a company's internal Strengths and Weaknesses against external Opportunities and Threats to guide strategy."),
                QuizQuestion("Management by objectives (MBO) focuses on:", listOf("Micromanaging tasks", "Setting specific measurable goals employees help define", "Strict rule enforcement", "Maximizing short-term profit"), 1, "MBO aligns employee goals with organizational objectives through participative goal-setting and performance reviews."),
                QuizQuestion("The span of control refers to:", listOf("Financial budget limits", "Number of employees a manager directly supervises", "Geographic territory", "Product line width"), 1, "Span of control is the number of direct reports a manager oversees — wider spans mean fewer management layers."),
                QuizQuestion("A matrix organizational structure:", listOf("Has only vertical reporting lines", "Has employees reporting to both a functional and project manager", "Eliminates middle management", "Is only used in small businesses"), 1, "Matrix structures create dual reporting lines — employees answer to both a functional department head and a project manager."),
                QuizQuestion("Herzberg's two-factor theory says:", listOf("Motivation and hygiene factors both motivate equally", "Hygiene factors prevent dissatisfaction; motivators create satisfaction", "Money is the top motivator", "Fear is the best management tool"), 1, "Hygiene factors (pay, conditions) prevent dissatisfaction. True motivation comes from achievement, recognition, and growth."),
                QuizQuestion("Decision trees help managers:", listOf("Create org charts", "Visualize choices and probable outcomes with expected values", "Track cash flow", "Monitor employee hours"), 1, "Decision trees map choices and their possible outcomes with probabilities, helping compare expected values of options."),
                QuizQuestion("A PESTEL analysis examines:", listOf("Internal company strengths", "Political, Economic, Social, Technological, Environmental, and Legal factors", "Product pricing strategies", "Employee performance metrics"), 1, "PESTEL is an external macro-environment analysis tool covering six categories that affect business strategy."),
                QuizQuestion("Maslow's hierarchy places which need at the top?", listOf("Safety", "Belonging", "Esteem", "Self-actualization"), 3, "Maslow's hierarchy: Physiological → Safety → Belonging → Esteem → Self-actualization (the highest-level need)."),
                QuizQuestion("Benchmarking involves:", listOf("Setting financial budgets", "Comparing your performance metrics against best-in-class competitors", "Creating employee schedules", "Auditing financial statements"), 1, "Benchmarking compares your processes and performance to industry leaders or best practices to identify improvement areas."),
                QuizQuestion("A critical path in project management is:", listOf("The shortest path in a network", "The longest sequence of dependent tasks that determines project duration", "The most expensive set of tasks", "Any path through a Gantt chart"), 1, "The critical path is the longest chain of dependent tasks — any delay on it delays the entire project.")
            )),
            TestEvent("Networking Concepts", Icons.Default.Hub, listOf(
                QuizQuestion("IP stands for:", listOf("Internet Protocol", "Internal Process", "Integrated Platform", "Information Packet"), 0, "IP (Internet Protocol) is the fundamental protocol for addressing and routing data packets."),
                QuizQuestion("Which OSI layer handles routing between networks?", listOf("Physical", "Data Link", "Network", "Transport"), 2, "The Network layer (Layer 3) handles logical addressing (IP) and routing between networks."),
                QuizQuestion("DNS translates:", listOf("IP addresses to MAC addresses", "Domain names to IP addresses", "Packets into frames", "HTTP to HTTPS"), 1, "DNS (Domain Name System) converts human-readable domain names (google.com) into IP addresses."),
                QuizQuestion("HTTPS provides security through:", listOf("Faster routing", "SSL/TLS encryption", "Stronger passwords", "VPN tunneling"), 1, "HTTPS uses SSL/TLS encryption to secure communication between a browser and web server."),
                QuizQuestion("A LAN covers:", listOf("A country", "A continent", "A city", "A small local area like a building or campus"), 3, "A LAN (Local Area Network) connects devices in a limited geographic area like a home, office, or school."),
                QuizQuestion("The default gateway is:", listOf("The internet's main server", "A router connecting local network to external networks", "A DNS server", "A firewall"), 1, "The default gateway is the router that forwards traffic from your local network to external networks."),
                QuizQuestion("Which is a private IP address?", listOf("8.8.8.8", "172.16.0.1", "44.192.140.1", "208.67.222.222"), 1, "192.168.x.x, 10.x.x.x, and 172.16–31.x.x are private IP ranges not routable on the public internet."),
                QuizQuestion("TCP differs from UDP in that TCP:", listOf("Is faster and connectionless", "Guarantees delivery and is connection-oriented", "Is only for video streaming", "Does not use IP"), 1, "TCP provides reliable, ordered, error-checked delivery. UDP is faster but does not guarantee delivery."),
                QuizQuestion("A subnet mask is used to:", listOf("Encrypt network traffic", "Divide an IP address into network and host portions", "Assign MAC addresses", "Block malicious traffic"), 1, "A subnet mask identifies which part of an IP address is the network and which is the host."),
                QuizQuestion("A MAC address is:", listOf("A software license key", "A unique hardware identifier for a network interface", "A type of IP address", "A wireless security protocol"), 1, "A MAC (Media Access Control) address is a unique hardware identifier assigned to a network interface card.")
            )),
            TestEvent("Business Ethics", Icons.Default.Balance, listOf(
                QuizQuestion("Utilitarianism judges an action's ethics based on:", listOf("The intent of the actor", "The rules it follows", "The outcomes for all affected", "Religious doctrine"), 2, "Utilitarianism evaluates actions by their consequences — the greatest good for the greatest number."),
                QuizQuestion("Whistleblowing refers to:", listOf("Reporting a coworker's tardiness", "Exposing unethical or illegal activity within an organization", "Filing a wage complaint", "Alerting managers to safety hazards"), 1, "Whistleblowing means reporting wrongdoing inside an organization, often at personal risk."),
                QuizQuestion("Conflict of interest occurs when:", listOf("An employee disagrees with a manager", "Personal interests could improperly influence professional decisions", "Two businesses compete for the same customer", "An employee refuses overtime"), 1, "A conflict of interest exists when personal gain could compromise professional judgment."),
                QuizQuestion("The Sarbanes-Oxley Act was enacted to:", listOf("Reduce corporate taxes", "Improve corporate governance and accounting integrity after scandals", "Regulate international trade", "Protect employee benefits"), 1, "SOX was passed after Enron and WorldCom scandals to increase financial transparency and executive accountability."),
                QuizQuestion("Which best reflects corporate social responsibility (CSR)?", listOf("Maximizing short-term shareholder profit", "Donating to a cause benefiting the CEO's family", "Reducing emissions beyond legal requirements", "Lobbying to weaken environmental laws"), 2, "CSR means going beyond legal obligations to benefit society, the environment, and stakeholders."),
                QuizQuestion("Insider trading involves:", listOf("Hiring employees from a competitor", "Trading stocks based on non-public information", "Selling company assets below market value", "Moving company funds offshore"), 1, "Insider trading is the illegal buying or selling of securities based on material non-public information."),
                QuizQuestion("The 'golden rule' in ethics means:", listOf("Whoever has the gold makes the rules", "Treat others as you would want to be treated", "Always pursue maximum profit", "Follow the law exactly and no more"), 1, "The golden rule — reciprocity — is one of the most universal ethical principles across cultures."),
                QuizQuestion("A bribe is best defined as:", listOf("A performance bonus", "Something offered to improperly influence a decision", "A commission on a completed sale", "A legal finder's fee"), 1, "A bribe is something of value given to improperly influence the decision or action of an official or individual."),
                QuizQuestion("Moral courage in business means:", listOf("Taking the safe, popular path", "Being willing to act ethically even when it is difficult or costly", "Following orders without question", "Prioritizing personal gain over ethics"), 1, "Moral courage is the willingness to do the right thing even when it involves risk or personal cost."),
                QuizQuestion("Ethical relativism holds that:", listOf("Ethics are universal and absolute", "What is ethical depends on the culture or individual", "All actions are equally ethical", "Ethics apply only to public figures"), 1, "Ethical relativism argues that moral standards vary by culture, society, or individual — there are no universal rules.")
            )),
            TestEvent("Computer Applications", Icons.Default.Computer, listOf(
                QuizQuestion("In a spreadsheet, an absolute cell reference is denoted by:", listOf("A colon (:)", "An asterisk (*)", "A dollar sign (\$)", "A hashtag (#)"), 2, "A \$ before the column and/or row (e.g., \$A\$1) locks that reference when copying the formula."),
                QuizQuestion("Which format is best for sharing a non-editable document?", listOf("DOCX", "TXT", "PDF", "XLS"), 2, "PDF preserves formatting and is not easily edited, making it ideal for distributing final documents."),
                QuizQuestion("Mail merge combines:", listOf("Two email accounts", "A database of records with a document template", "Multiple Word documents into one", "Spreadsheet formulas with text"), 1, "Mail merge pulls data from a spreadsheet or database to personalize a document template for each record."),
                QuizQuestion("A pivot table is used to:", listOf("Create charts only", "Summarize and analyze large data sets", "Merge cells", "Sort alphabetically"), 1, "Pivot tables allow users to reorganize and summarize large amounts of data quickly without formulas."),
                QuizQuestion("Which keyboard shortcut copies selected content?", listOf("Ctrl+V", "Ctrl+X", "Ctrl+C", "Ctrl+Z"), 2, "Ctrl+C copies selected content to the clipboard; Ctrl+V pastes it."),
                QuizQuestion("Track Changes in word processing:", listOf("Monitors file download speed", "Records edits so they can be accepted or rejected", "Autosaves the document", "Converts text to a table"), 1, "Track Changes marks insertions, deletions, and formatting changes so editors can review and approve each edit."),
                QuizQuestion("Which chart type best shows data as a proportion of a whole?", listOf("Bar chart", "Line chart", "Scatter plot", "Pie chart"), 3, "Pie charts show each category as a slice of the whole, ideal for illustrating percentage breakdown."),
                QuizQuestion("A macro in office applications is:", listOf("A keyboard shortcut only", "A large font size setting", "A recorded sequence of commands that automates tasks", "A type of spreadsheet formula"), 2, "Macros record and replay a series of actions to automate repetitive tasks in office software."),
                QuizQuestion("The function =SUM(A1:A10) calculates:", listOf("Only cells A1 and A10", "All cells from A1 through A10", "All cells in column A", "10 added to the value in A1"), 1, "The colon (:) in A1:A10 means all cells in that range — SUM adds them all together."),
                QuizQuestion("Conditional formatting in a spreadsheet:", listOf("Changes cell size based on value", "Applies formatting rules based on cell content", "Locks cells from editing", "Links cells across sheets"), 1, "Conditional formatting automatically applies styles (color, bold, etc.) when cells meet specified criteria.")
            )),
            TestEvent("Global Business", Icons.Default.Public, listOf(
                QuizQuestion("Currency appreciation means a country's currency:", listOf("Weakens against foreign currencies", "Strengthens against foreign currencies", "Is pegged to gold", "Eliminates trade deficits"), 1, "Appreciation means the currency buys more foreign currency — it has become relatively stronger."),
                QuizQuestion("Comparative advantage means a country should produce:", listOf("Everything it possibly can", "What it's best at regardless of trade", "Goods it can produce at lower opportunity cost than trading partners", "Only luxury goods"), 2, "Comparative advantage drives specialization — produce what you give up the least to make, then trade."),
                QuizQuestion("The WTO primarily:", listOf("Provides loans to developing nations", "Regulates global currency exchange", "Sets rules for international trade and resolves disputes", "Manages international shipping"), 2, "The World Trade Organization establishes trade rules, reduces trade barriers, and mediates disputes between nations."),
                QuizQuestion("A multinational corporation (MNC) is a company that:", listOf("Only imports goods", "Operates in multiple countries", "Is government-owned", "Only sells in one country"), 1, "MNCs have operations, investments, or subsidiaries in multiple countries beyond their home nation."),
                QuizQuestion("Dumping in international trade means:", listOf("Disposing of hazardous waste abroad", "Selling goods in a foreign market below cost to undercut rivals", "Refusing to import foreign goods", "Placing high tariffs on competing goods"), 1, "Dumping is selling exports at artificially low prices to gain market share — considered an unfair trade practice."),
                QuizQuestion("A weaker domestic currency tends to:", listOf("Decrease exports", "Increase exports", "Increase imports", "Strengthen trade partners' economies"), 1, "A weaker currency makes exports cheaper for foreign buyers and imports more expensive domestically, boosting exports."),
                QuizQuestion("Cultural intelligence (CQ) refers to:", listOf("The ability to translate foreign languages instantly", "The capability to function effectively across cultural contexts", "Memorizing international laws", "Fluency in at least two languages"), 1, "CQ is the ability to adapt to different cultural norms, communication styles, and business practices."),
                QuizQuestion("A joint venture in international business involves:", listOf("One company buying another outright", "Two or more companies partnering to share risk and resources", "A government nationalizing a foreign firm", "Licensing a brand to a third party"), 1, "A joint venture is a business arrangement where two or more companies pool resources for a specific project."),
                QuizQuestion("Balance of trade is the difference between a country's:", listOf("Imports and exports of goods", "Total government revenue and spending", "GDP and GNP", "Foreign debt and domestic debt"), 0, "The balance of trade compares the value of exports to imports — a surplus means more exports than imports."),
                QuizQuestion("Globalization refers to:", listOf("The decline of multinational corporations", "Increasing interconnection of world economies, cultures, and trade", "Rise of protectionist trade policies worldwide", "Elimination of all international borders"), 1, "Globalization describes the growing interdependence of countries through trade, investment, and communication.")
            )),
            TestEvent("Healthcare Administration", Icons.Default.Favorite, listOf(
                QuizQuestion("HIPAA primarily protects:", listOf("Hospital profit margins", "Patient health information privacy", "Medical device safety standards", "Healthcare worker wages"), 1, "HIPAA (Health Insurance Portability and Accountability Act) establishes national standards for protecting sensitive patient health information."),
                QuizQuestion("Informed consent means:", listOf("The patient signed any form presented", "The patient agreed after being fully informed of risks and alternatives", "A doctor approved a treatment plan", "Insurance pre-approved a procedure"), 1, "Informed consent requires patients to receive clear information about a procedure's risks, benefits, and alternatives before agreeing."),
                QuizQuestion("Medicare primarily serves:", listOf("Low-income individuals", "Adults 65+ and people with certain disabilities", "Active military personnel", "Children under 18"), 1, "Medicare is a federal health insurance program for Americans 65 and older and for certain younger people with disabilities."),
                QuizQuestion("In healthcare, a deductible is:", listOf("Monthly insurance premium", "The amount paid out-of-pocket before insurance begins covering costs", "Maximum total coverage", "Co-payment per visit"), 1, "The deductible is what you pay before insurance kicks in — after meeting it, insurance covers the agreed percentage."),
                QuizQuestion("Electronic Health Records (EHRs) primarily improve:", listOf("Hospital building design", "Coordination of care and information sharing", "Pharmaceutical pricing", "Insurance denial rates"), 1, "EHRs allow healthcare providers to instantly access and share patient records, reducing errors and improving coordination."),
                QuizQuestion("A hospital's Chief Medical Officer (CMO) oversees:", listOf("Managing hospital finances", "Medical staff and clinical quality standards", "Marketing hospital services", "Negotiating with suppliers"), 1, "The CMO ensures clinical excellence, coordinates physician activities, and sets standards for patient care quality."),
                QuizQuestion("Continuity of care refers to:", listOf("Paying for care continuously each month", "Consistent, coordinated care as a patient moves through the system", "Keeping the same nurse for every visit", "Uninterrupted hospital staffing"), 1, "Continuity of care ensures patients receive seamless, consistent treatment across providers, settings, and time."),
                QuizQuestion("Managed care organizations aim to:", listOf("Maximize hospital profits", "Control healthcare costs while maintaining quality", "Eliminate health insurance", "Focus only on emergency services"), 1, "Managed care uses networks, pre-authorization, and care coordination to control costs and maintain quality outcomes."),
                QuizQuestion("A formulary in healthcare is:", listOf("A hospital floor plan", "Standard medical billing forms", "A list of drugs covered by an insurance plan", "A patient satisfaction survey"), 2, "A formulary is the approved list of medications covered by an insurance plan, often tiered by cost."),
                QuizQuestion("The primary role of a healthcare administrator is:", listOf("Diagnosing patients", "Managing the operational, financial, and human aspects of healthcare organizations", "Performing surgical procedures", "Prescribing medications"), 1, "Healthcare administrators run the business side — budgets, staffing, compliance, and strategic planning — of health organizations.")
            )),
            TestEvent("Help Desk", Icons.Default.HeadsetMic, listOf(
                QuizQuestion("First call resolution (FCR) means:", listOf("The customer called for the first time", "The issue is resolved during initial contact without escalation", "A manager handles the first call", "The call lasted under one minute"), 1, "FCR measures how often issues are solved on the first contact — a key indicator of help desk efficiency."),
                QuizQuestion("A ticketing system is used to:", listOf("Store employee timesheets", "Track, assign, and manage support requests", "Schedule training sessions", "Monitor network traffic"), 1, "Ticketing systems create a record for each support request, enabling tracking, prioritization, and accountability."),
                QuizQuestion("The ITIL framework is:", listOf("A programming language", "A set of best practices for IT service management", "A hardware certification", "An internet security protocol"), 1, "ITIL (Information Technology Infrastructure Library) provides a framework for aligning IT services with business needs."),
                QuizQuestion("Escalation in help desk support means:", listOf("Hanging up on a difficult caller", "Passing an unresolved issue to a higher support level", "Charging more for complex issues", "Sending a complaint to management"), 1, "Escalation transfers an issue to a more experienced technician or specialist when the current level cannot resolve it."),
                QuizQuestion("SLA stands for:", listOf("System Load Average", "Software License Agreement", "Service Level Agreement", "Secure Login Authentication"), 2, "An SLA defines the expected service standards — response times, resolution targets, and uptime — between provider and customer."),
                QuizQuestion("Active Directory is primarily used to:", listOf("Host websites", "Manage user accounts and access permissions on a network", "Back up data to the cloud", "Monitor internet usage"), 1, "Active Directory is Microsoft's directory service for managing users, computers, and permissions in a network environment."),
                QuizQuestion("Remote desktop access allows a technician to:", listOf("Physically repair a user's computer", "Control another computer over a network to provide support", "View only the screen without interacting", "Install physical hardware remotely"), 1, "Remote desktop tools let technicians take control of a user's computer to diagnose and fix issues without being on-site."),
                QuizQuestion("When a user forgets their password, best practice is to:", listOf("Give the admin password temporarily", "Verify identity before resetting the password", "Email the existing password", "Ask a coworker to share credentials"), 1, "Password resets must follow identity verification protocols to prevent unauthorized account access."),
                QuizQuestion("A knowledge base in IT support is:", listOf("A hardware storage device", "A searchable repository of known issues and solutions", "A training manual for new hires only", "The company's main database"), 1, "A knowledge base documents common problems and solutions, allowing users and technicians to self-serve for known issues."),
                QuizQuestion("Mean Time to Resolve (MTTR) measures:", listOf("Average number of support calls per hour", "Average time to fully resolve a support issue", "Mean training time for technicians", "Total monthly support volume"), 1, "MTTR is a key help desk KPI measuring how long it takes from when an issue is reported to when it is fully fixed.")
            )),
            TestEvent("Human Resource Management", Icons.Default.People, listOf(
                QuizQuestion("Job analysis defines:", listOf("Employee salary ranges", "The duties, requirements, and context of a job", "Performance review timelines", "Organizational hierarchy"), 1, "Job analysis systematically identifies and documents what a job entails — tasks, responsibilities, and required competencies."),
                QuizQuestion("Title VII of the Civil Rights Act prohibits employment discrimination based on:", listOf("Age and disability", "Race, color, religion, sex, or national origin", "Union membership", "Educational attainment"), 1, "Title VII prohibits employers from discriminating in hiring, firing, or terms of employment based on protected characteristics."),
                QuizQuestion("The FMLA provides:", listOf("Paid maternity leave for all employees", "Up to 12 weeks of unpaid, job-protected leave for qualifying reasons", "Free healthcare during leave", "Guaranteed promotion after returning"), 1, "The Family and Medical Leave Act gives eligible employees up to 12 weeks of unpaid leave while protecting their job."),
                QuizQuestion("Onboarding refers to:", listOf("Terminating an employee's contract", "Integrating a new employee into the organization", "Annual performance reviews", "Exit interviews"), 1, "Onboarding helps new employees understand culture, policies, roles, and responsibilities to become productive quickly."),
                QuizQuestion("The most effective selection tool for predicting job performance is:", listOf("References only", "Structured behavioral interviews", "Unstructured casual interviews", "Application forms alone"), 1, "Structured behavioral interviews use consistent, job-relevant questions — research shows they best predict actual job performance."),
                QuizQuestion("A 360-degree performance review collects feedback from:", listOf("Only the direct manager", "Peers, subordinates, supervisors, and sometimes customers", "HR department only", "An external auditing firm"), 1, "360-degree reviews gather feedback from all directions — giving a fuller picture of an employee's performance and impact."),
                QuizQuestion("Succession planning in HR involves:", listOf("Listing employee grievances", "Identifying and developing future leaders for key positions", "Planning the company's financial succession", "Managing layoff procedures"), 1, "Succession planning ensures organizational continuity by proactively developing internal candidates for critical roles."),
                QuizQuestion("The primary purpose of an exit interview is to:", listOf("Convince the employee to stay", "Gather feedback to improve retention and work environment", "Document legal liability waivers", "Recover company equipment"), 1, "Exit interviews provide honest insight from departing employees about what the organization could improve."),
                QuizQuestion("Employee turnover rate measures:", listOf("How quickly employees advance", "The percentage of employees who leave and must be replaced in a period", "Average length of employee tenure", "Total salary cost per employee"), 1, "Turnover rate = (# employees who left / average # employed) × 100. High turnover signals engagement or culture problems."),
                QuizQuestion("Pay equity means:", listOf("All employees earn the same salary", "Compensation is fair relative to skills, experience, and job requirements", "Entry-level employees get the highest raises", "Benefits are worth more than salary"), 1, "Pay equity ensures workers are compensated fairly relative to their qualifications and responsibilities, eliminating discriminatory gaps.")
            )),
            TestEvent("Insurance & Risk Management", Icons.Default.VerifiedUser, listOf(
                QuizQuestion("The law of large numbers in insurance states that:", listOf("Larger policies cost more", "As the number of insured grows, actual losses become more predictable", "Only large corporations need insurance", "Premiums increase with age"), 1, "With more policyholders, an insurer can more accurately predict claims, which is the foundation of insurance pricing."),
                QuizQuestion("Subrogation means:", listOf("The insured gets double payment", "The insurer assumes the legal right to recover losses from a responsible third party", "A policy is transferred to a new owner", "The insured sues their own insurance company"), 1, "After paying a claim, subrogation lets the insurer pursue the at-fault party to recover the payout."),
                QuizQuestion("Term life insurance provides coverage:", listOf("For your entire life", "Only while working", "For a specific period of time", "Until the cash value runs out"), 2, "Term life covers a set period (e.g., 20 years). If the insured outlives the term, there is no payout."),
                QuizQuestion("Risk retention means a company:", listOf("Buys extensive insurance", "Accepts the financial consequences of a risk itself", "Transfers all risk to a third party", "Eliminates all potential risks"), 1, "Risk retention (self-insurance) means accepting that a loss may occur and funding it internally instead of buying insurance."),
                QuizQuestion("A deductible in an insurance policy is:", listOf("A monthly payment to maintain coverage", "The amount the insured must pay before the insurer covers the rest", "The maximum amount the insurer will pay", "A discount for good claims history"), 1, "The deductible is the policyholder's first-dollar cost — once met, the insurer pays its share of covered losses."),
                QuizQuestion("Pure risk involves:", listOf("The possibility of gain or loss", "Only the chance of loss with no possibility of gain", "Investments in volatile assets", "Entrepreneurial ventures"), 1, "Pure risk (insurable risk) only has two outcomes: loss or no loss — there is no chance of gain."),
                QuizQuestion("Liability insurance protects against:", listOf("Damage to the policyholder's own property", "Claims from third parties for bodily injury or property damage", "The policyholder's personal health expenses", "Natural disaster damage only"), 1, "Liability coverage pays for losses the policyholder is legally responsible for causing to others."),
                QuizQuestion("Risk avoidance as a strategy means:", listOf("Buying insurance to cover the risk", "Reducing the severity of a potential loss", "Not engaging in activities that create the risk", "Accepting a small certain loss to avoid a larger uncertain one"), 2, "Risk avoidance eliminates the risk entirely by choosing not to participate in the risky activity."),
                QuizQuestion("An insurance premium is:", listOf("Money received from a claim", "The maximum coverage amount", "The periodic payment made to maintain an insurance policy", "A government tax on insurance"), 2, "The premium is what the policyholder pays (monthly, annually, etc.) to keep the insurance policy active."),
                QuizQuestion("Reinsurance is when:", listOf("An insurer cancels and reissues a policy", "An insurance company buys insurance for itself to spread large risks", "A customer buys two separate policies", "A broker resells insurance from another company"), 1, "Reinsurance lets insurers transfer portions of their risk portfolios to other insurers, protecting against catastrophic losses.")
            )),
            TestEvent("Parliamentary Procedure", Icons.Default.Gavel, listOf(
                QuizQuestion("A quorum is:", listOf("The presiding officer of a meeting", "The minimum members needed to conduct business", "A motion to end debate", "A committee for special projects"), 1, "Without a quorum, an organization cannot legally conduct official business — this protects against decisions made by an unrepresentative minority."),
                QuizQuestion("A motion 'to table' a motion means:", listOf("To discuss it immediately", "To permanently reject it", "To temporarily set it aside for later consideration", "To refer it to a committee"), 2, "Laying on the table postpones consideration of a motion until later — it is not a vote to kill the motion."),
                QuizQuestion("A point of order is raised when:", listOf("A member wants to make a motion", "A member believes a procedural rule is being violated", "A member wants to speak in favor", "The president adjourns the meeting"), 1, "A point of order calls attention to a rule violation and requires the presiding officer to make an immediate ruling."),
                QuizQuestion("Which motion has the highest priority?", listOf("Adjourn", "Reconsider", "Lay on the table", "Main motion"), 0, "The motion to adjourn takes precedence over all other motions — it can always be made and voted on immediately."),
                QuizQuestion("Amending a main motion requires:", listOf("A two-thirds vote", "A majority vote", "Unanimous consent", "Only the chair's approval"), 1, "Amendments require a majority vote — they change specific words, add language, or strike language from the main motion."),
                QuizQuestion("The presiding officer of a meeting is typically called:", listOf("The secretary", "The treasurer", "The chair or president", "The parliamentarian"), 2, "The chair (or president) presides over the meeting, recognizes speakers, and rules on points of order."),
                QuizQuestion("Calling the previous question, if adopted, means:", listOf("Going back to the previous meeting's agenda", "Immediately stopping debate and forcing a vote", "Tabling the motion indefinitely", "Referring the matter to a committee"), 1, "Previous question is a motion to close debate and vote immediately — it requires a two-thirds vote to pass."),
                QuizQuestion("Meeting minutes should include:", listOf("Members' personal opinions", "Summaries of all debate discussions", "Official actions taken and motions passed", "The chair's personal notes"), 2, "Minutes are the official record of what was done — motions made, votes taken, and actions approved — not what was said."),
                QuizQuestion("A motion requires a second because:", listOf("It shows the motion has at least two supporters", "It is required by law", "It gives the chair time to consider it", "It adds the seconder as co-sponsor"), 0, "A second indicates at least one other member believes the matter is worth the group's time — it is not an endorsement."),
                QuizQuestion("The main motion is:", listOf("Always the first motion made", "The primary item of business introduced for group consideration", "Always voted on without discussion", "A motion that overrides all other motions"), 1, "The main motion introduces a new item of business and is the lowest in precedence — all other motions take priority over it.")
            )),
            TestEvent("Public Speaking", Icons.Default.Mic, listOf(
                QuizQuestion("An effective speech conclusion should:", listOf("Introduce new arguments", "Summarize key points and provide a memorable close", "Apologize for any weaknesses", "Be longer than the body"), 1, "The conclusion reinforces the main points and ends memorably — audiences remember first and last impressions most."),
                QuizQuestion("Ethos in persuasion refers to:", listOf("Emotional appeal", "Logical evidence and data", "Credibility and character of the speaker", "The argument's structure"), 2, "Ethos establishes the speaker's trustworthiness and expertise — without credibility, even good arguments fail."),
                QuizQuestion("Extemporaneous speaking means:", listOf("Reading directly from a script", "Speaking from brief notes with limited preparation time", "Memorizing a speech word for word", "Improvising with zero preparation"), 1, "Extemporaneous speaking involves preparation and an outline, but the exact words are chosen in the moment of delivery."),
                QuizQuestion("Vocal variety refers to changing:", listOf("The topic of the speech", "Pace, pitch, volume, and tone while speaking", "The slide design", "The speaking location"), 1, "Varying pace, pitch, and volume keeps the audience engaged and emphasizes key points throughout the speech."),
                QuizQuestion("Eye contact with an audience primarily establishes:", listOf("That the speaker memorized the speech", "Connection, confidence, and engagement", "The appropriate speaking speed", "That the speaker is not using notes"), 1, "Eye contact signals confidence and makes audience members feel personally addressed, increasing trust and engagement."),
                QuizQuestion("The primacy-recency effect suggests audiences best remember:", listOf("Only the middle of the speech", "The beginning and end of the speech", "Only emotional parts", "Only statistics and data"), 1, "Audiences have stronger memory for the first and last things they hear — making the introduction and conclusion critical."),
                QuizQuestion("Transitional phrases in a speech:", listOf("Introduce the speaker's biography", "Move the audience smoothly from one idea to the next", "Summarize the entire speech", "Provide source citations"), 1, "Transitions signal movement between ideas and help audiences follow the logical flow of the speech."),
                QuizQuestion("Pathos in persuasion appeals to:", listOf("Logic and reasoning", "Trust in the speaker", "The audience's emotions", "Political views"), 2, "Pathos uses stories, vivid imagery, and emotional language to connect with the audience's feelings and values."),
                QuizQuestion("The most helpful immediate technique for speech anxiety is:", listOf("Speaking faster to finish sooner", "Avoiding eye contact", "Deep, controlled breathing before beginning", "Apologizing for nervousness upfront"), 2, "Controlled breathing activates the parasympathetic nervous system, reducing the physical symptoms of anxiety before speaking."),
                QuizQuestion("An effective attention-getter in an introduction might include:", listOf("Reading the outline aloud", "Stating all main points immediately", "A surprising statistic, story, or rhetorical question", "Defining every term you will use"), 2, "Strong attention-getters create curiosity or an emotional hook — they make the audience want to keep listening.")
            )),
            TestEvent("Social Media Strategies", Icons.Default.Share, listOf(
                QuizQuestion("Engagement rate in social media measures:", listOf("Total number of followers", "Interaction relative to reach or followers", "Revenue generated from ads", "Total posts published"), 1, "Engagement rate = (likes + comments + shares) / reach or followers — it measures how actively an audience interacts with content."),
                QuizQuestion("A social media algorithm determines:", listOf("How many ads a platform runs", "What content is shown to which users and in what order", "The cost of premium subscriptions", "The platform's storage needs"), 1, "Algorithms rank and distribute content based on relevance, engagement history, and predicted user interest."),
                QuizQuestion("User-generated content (UGC) refers to:", listOf("Content created by the brand's marketing team", "Content created by customers and fans about the brand", "Automated AI-generated posts", "Sponsored posts from influencers"), 1, "UGC is authentic content created by actual users — it builds trust and social proof more effectively than brand-created content."),
                QuizQuestion("A/B testing in social media involves:", listOf("Testing two different platforms simultaneously", "Comparing two versions of content to see which performs better", "Running two campaigns for different products", "Testing content in two countries"), 1, "A/B testing shows one version to one audience segment and another version to a different segment, then compares performance."),
                QuizQuestion("Hashtags on social media are used to:", listOf("Notify specific followers", "Tag a brand's official account", "Categorize content and increase discoverability", "Mark content as sponsored"), 2, "Hashtags group related content, allowing users to discover posts on specific topics even from accounts they don't follow."),
                QuizQuestion("Reach in social media analytics means:", listOf("Total number of likes on all posts", "The number of unique users who saw the content", "Revenue earned from the platform", "How many times content was shared"), 1, "Reach measures the unique number of people who saw a post — impressions counts total views including repeats."),
                QuizQuestion("Influencer marketing works by:", listOf("Paying traditional celebrities only", "Using individuals with loyal followings to authentically promote products", "Creating viral content without real people", "Targeting only high-income demographics"), 1, "Influencers have built trust with their audience — their recommendations carry more weight than direct brand advertising."),
                QuizQuestion("Social listening refers to:", listOf("Recording competitor phone calls", "Monitoring social media for mentions, trends, and conversations about a brand", "Playing music during a live broadcast", "Tracking employee social media use"), 1, "Social listening uses tools to track brand mentions, competitor activity, and industry trends across social platforms."),
                QuizQuestion("Ideal posting frequency on social media should be based on:", listOf("Posting as often as possible", "Platform norms, audience behavior, and content quality", "Matching exactly what competitors post", "Only posting on weekdays"), 1, "Quality over quantity — posting frequency should be guided by what the audience responds to and the platform's best practices."),
                QuizQuestion("A content calendar helps a social media manager:", listOf("Design graphics for posts", "Plan, organize, and schedule content in advance", "Respond to comments automatically", "Track follower demographics"), 1, "A content calendar ensures consistent posting, strategic timing, and alignment with campaigns and key dates.")
            )),
            TestEvent("Sports & Entertainment Management", Icons.Default.EmojiEvents, listOf(
                QuizQuestion("Sponsorship in sports is primarily used to:", listOf("Reduce player salaries", "Provide financial support in exchange for marketing exposure", "Control ticket pricing", "Eliminate the need for media coverage"), 1, "Sponsorships are mutually beneficial — companies fund teams or events in exchange for brand visibility and association."),
                QuizQuestion("A naming rights deal involves:", listOf("Sponsors naming the team's lineup", "A company paying to name a stadium or arena", "Athletes naming signature merchandise", "Media companies naming broadcast rights holders"), 1, "Naming rights allow a company to associate its brand with a venue in exchange for significant long-term fees."),
                QuizQuestion("The primary revenue streams for professional sports teams include:", listOf("Only player trades", "Ticket sales, broadcasting rights, sponsorships, and merchandise", "Government subsidies only", "Player contracts and agent fees"), 1, "Modern sports teams diversify revenue across gate receipts, media deals, corporate partnerships, and licensed merchandise."),
                QuizQuestion("A talent agency's primary function is to:", listOf("Train athletes in their sport", "Represent and negotiate contracts for artists or athletes", "Produce sporting events", "Manage stadium operations"), 1, "Agents negotiate contracts, secure endorsements, and manage career opportunities on behalf of their athlete or artist clients."),
                QuizQuestion("Broadcasting rights in sports refer to:", listOf("A team's right to host home games", "Permission granted to media companies to air events", "Athletes' rights to stream games personally", "Rights to stream games free on personal devices"), 1, "Broadcast rights are lucrative agreements that allow networks to televise games — a major revenue source for leagues and teams."),
                QuizQuestion("Return on investment (ROI) for a sports sponsorship is measured by:", listOf("Number of fans in attendance", "Financial return generated relative to sponsorship cost", "Number of games the team wins", "Size of the team's social media following"), 1, "Sponsors measure ROI through brand exposure value, sales lift, lead generation, and consumer sentiment changes."),
                QuizQuestion("Event management includes all of the following EXCEPT:", listOf("Venue selection and logistics", "Marketing and promotion", "Coaching athlete performance", "Budget management"), 2, "Event managers handle logistics, marketing, budgets, and operations — but coaching athletes is the team's or sport's responsibility."),
                QuizQuestion("Ticket scalping refers to:", listOf("Stadium cleaning after events", "Reselling event tickets above face value", "Issuing complimentary tickets to sponsors", "Season ticket renewal programs"), 1, "Scalping is the resale of tickets at inflated prices — legal in some jurisdictions but controversial and regulated in others."),
                QuizQuestion("Ambush marketing in sports refers to:", listOf("Official sponsor advertising at an event", "A non-sponsor associating itself with an event without paying official fees", "Guerrilla advertising in a local market", "Marketing to younger demographics only"), 1, "Ambush marketing lets competitors piggyback on event excitement without paying sponsorship fees, undermining official sponsors."),
                QuizQuestion("A mega-event like the Super Bowl generates significant value through:", listOf("Low-cost local community impact", "Global commercial reach, advertising, and cultural impact", "Minimal sponsor interest", "Regional ticket sales only"), 1, "The Super Bowl commands the highest TV ad rates and generates billions in economic activity — a premier marketing platform.")
            )),
            TestEvent("Supply Chain Management", Icons.Default.LocalShipping, listOf(
                QuizQuestion("Just-in-time (JIT) inventory aims to:", listOf("Store as much inventory as possible", "Receive goods only when needed to minimize storage costs", "Produce goods just before they expire", "Order supplies just before a sale ends"), 1, "JIT reduces inventory carrying costs by synchronizing supply with demand — requiring reliable suppliers and accurate forecasting."),
                QuizQuestion("The bullwhip effect in supply chains refers to:", listOf("A warehouse safety hazard", "Increasing order variability as you move upstream in the supply chain", "Price increases caused by transportation costs", "A strategy to increase supply chain speed"), 1, "Small demand fluctuations at the retail level amplify into large order swings at the manufacturer level — the bullwhip effect."),
                QuizQuestion("Third-party logistics (3PL) providers offer:", listOf("Raw materials to manufacturers", "Outsourced supply chain functions like warehousing and shipping", "Direct retail services to consumers", "Financial services for supply chain transactions"), 1, "3PLs take over logistics operations — transportation, warehousing, fulfillment — allowing companies to focus on core competencies."),
                QuizQuestion("Vertical integration means a company:", listOf("Focuses solely on one product", "Controls multiple stages of its supply chain", "Outsources all production", "Only sells products online"), 1, "Vertically integrated companies own upstream suppliers or downstream distributors, gaining control and reducing dependence."),
                QuizQuestion("RFID technology in supply chain is used to:", listOf("Communicate with suppliers via radio", "Track and identify inventory using radio frequency signals", "Remotely manage warehouse staff", "Monitor supplier financial performance"), 1, "RFID tags on products or pallets allow automatic, real-time tracking without line-of-sight scanning — far faster than barcodes."),
                QuizQuestion("Vendor-managed inventory (VMI) means:", listOf("Customers manage their own orders", "The supplier monitors and replenishes the buyer's inventory", "A third party audits the vendor", "The buyer manages the supplier's production schedule"), 1, "In VMI, the supplier takes responsibility for monitoring stock levels at the buyer's location and automatically restocking."),
                QuizQuestion("Lead time in supply chain refers to:", listOf("The time a supplier takes to prepare and deliver an order", "Time products spend in store before being sold", "The number of suppliers in the chain", "The cost of the first shipment"), 0, "Lead time is the total time from order placement to receipt — shorter lead times reduce the need for safety stock."),
                QuizQuestion("Supply chain resilience is the ability to:", listOf("Negotiate lower prices from suppliers", "Recover from disruptions and maintain operations", "Increase the number of suppliers indefinitely", "Reduce delivery times to zero"), 1, "Resilient supply chains anticipate disruptions (natural disasters, pandemics, shortages) and adapt quickly to maintain flow."),
                QuizQuestion("Cold chain logistics manages:", listOf("Blockchain records for supply chains", "Temperature-sensitive products like food and pharmaceuticals", "High-value items requiring security escorts", "Oversized industrial equipment transport"), 1, "Cold chain ensures temperature-sensitive goods stay within required temperature ranges throughout storage and transportation."),
                QuizQuestion("A supply chain map is used to:", listOf("Plot delivery routes for drivers", "Visualize all entities, flows, and relationships in the supply chain", "Track individual package locations", "Create warehouse floor plans"), 1, "Supply chain mapping identifies every supplier, manufacturer, distributor, and retailer — enabling risk assessment and optimization.")
            )),
            TestEvent("Word Processing", Icons.Default.Description, listOf(
                QuizQuestion("'Widow and orphan control' in word processing prevents:", listOf("Formatting errors in headers", "Single lines of a paragraph appearing alone at a page break", "Unintended deletion of text", "Double-spacing errors"), 1, "Widow/orphan control keeps at least two lines of a paragraph together at the top or bottom of a page for better readability."),
                QuizQuestion("A style in word processing is:", listOf("A visual theme for the document background", "A set of formatting characteristics applied consistently to text", "The document's page orientation", "A macro that runs automatically"), 1, "Styles (Heading 1, Normal, etc.) apply consistent formatting — changing a style updates all text using it throughout the document."),
                QuizQuestion("Track Changes records:", listOf("Multiple document versions simultaneously", "All edits made so they can be accepted or rejected", "Comparisons of two different documents", "Automatic grammar corrections"), 1, "Track Changes marks each insertion, deletion, and format change with the editor's name, enabling collaborative review."),
                QuizQuestion("Double spacing provides how much space between lines compared to single?", listOf("The same amount", "50% more", "Twice as much", "Three times as much"), 2, "Double spacing places one blank line between each line of text — twice the space of single spacing."),
                QuizQuestion("A table of contents in Word is best created using:", listOf("Manual page numbering", "Built-in heading styles and the TOC tool", "The footer section", "Text boxes on each page"), 1, "Word's TOC tool reads Heading styles to automatically build and update a table of contents with accurate page numbers."),
                QuizQuestion("Section breaks allow:", listOf("Inserting images between paragraphs", "Different headers, footers, or page layouts within one document", "Breaking the document into separate files", "Creating automatic bullet lists"), 1, "Section breaks divide a document into independently formatted sections — allowing landscape in one section and portrait in another."),
                QuizQuestion("Mail merge is used to:", listOf("Combine two Word documents permanently", "Create personalized documents using a data source", "Merge formatting from two documents", "Email a document directly from Word"), 1, "Mail merge generates individualized letters, labels, or emails by filling template fields with data from a spreadsheet or database."),
                QuizQuestion("A watermark in word processing is:", listOf("A header image on every page", "Text or an image printed lightly behind main content", "A footer with page numbers", "A stamp applied when printing"), 1, "Watermarks appear as faint background text or images ('DRAFT', 'CONFIDENTIAL') on every page without interfering with content."),
                QuizQuestion("'Justified' text alignment means:", listOf("Text aligns left only", "Text aligns right only", "Text is centered on the page", "Text aligns to both the left and right margins"), 3, "Justified text stretches each line to align with both margins — common in books, newspapers, and formal documents."),
                QuizQuestion("A document template is:", listOf("A pre-designed format used as a reusable starting point", "A backup copy of a document", "A merged version of multiple documents", "A compressed file format"), 0, "Templates (.dotx) store styles, layout, and placeholder content so users can quickly create consistently formatted documents.")
            )),
            TestEvent("Website Design", Icons.Default.Web, listOf(
                QuizQuestion("HTML stands for:", listOf("HyperText Markup Language", "High Tech Media Language", "Hyperlink and Text Manipulation Language", "HyperText Management Layout"), 0, "HTML is the standard language for creating the structure and content of web pages using elements and tags."),
                QuizQuestion("CSS is primarily used to:", listOf("Define the structure of a webpage", "Control presentation, layout, and styling of webpages", "Handle server-side logic and databases", "Create interactive animations only"), 1, "CSS (Cascading Style Sheets) separates design from content — controlling colors, fonts, spacing, and layout."),
                QuizQuestion("Responsive web design ensures:", listOf("Websites load faster on all devices", "A website looks and works well on all screen sizes", "All content is always visible", "Websites are secured against hackers"), 1, "Responsive design uses flexible grids, images, and CSS media queries to adapt layout to any device or screen size."),
                QuizQuestion("SEO helps websites:", listOf("Load faster", "Rank higher in organic search results", "Become more secure", "Generate more email subscribers"), 1, "Search Engine Optimization improves a site's visibility in unpaid search results through content, structure, and link-building."),
                QuizQuestion("White space in web design refers to:", listOf("The literal color white used in backgrounds", "Empty space between elements that improves readability", "Areas reserved for advertising", "Unpublished pages on the site"), 1, "White space (negative space) reduces clutter, guides the eye, and gives content room to breathe — essential for clean design."),
                QuizQuestion("A wireframe in web design is:", listOf("A technical blueprint of a page's structure and layout", "The final visual design of a website", "The website's HTML code", "A security framework"), 0, "Wireframes are low-fidelity sketches of page layout showing element placement without colors or detailed design — the blueprint stage."),
                QuizQuestion("The above-the-fold area of a webpage is:", listOf("The footer section", "The area visible without scrolling", "The navigation menu", "The sidebar content"), 1, "Above the fold is the portion of a page immediately visible — prime real estate for calls to action, headlines, and key content."),
                QuizQuestion("Alt text for images primarily serves:", listOf("Aesthetic purposes", "Accessibility for visually impaired users and SEO purposes", "Reducing image file sizes", "Preventing image theft"), 1, "Alt text describes images for screen readers used by visually impaired users and helps search engines understand image content."),
                QuizQuestion("A CTA (Call to Action) in web design is:", listOf("A customer complaint form", "A prompt encouraging users to take a specific action", "A required legal disclaimer", "The website's contact information"), 1, "CTAs (Buy Now, Sign Up, Learn More) direct users toward desired actions — critical for conversion and user flow."),
                QuizQuestion("RGB color model is used for:", listOf("Printed materials only", "Physical paint mixing", "Screens and digital displays", "Pantone color matching"), 2, "RGB (Red, Green, Blue) is an additive color model used for screens — combining light to create colors, unlike CMYK for print.")
            )),
            TestEvent("Introduction to IT", Icons.Default.Computer, listOf(
                QuizQuestion("Binary number system uses only:", listOf("0 and 1", "0, 1, and 2", "0 through 9", "A through F"), 0, "Binary is base-2 — all data in a computer is ultimately stored and processed as combinations of 0s and 1s."),
                QuizQuestion("RAM is best described as:", listOf("Permanent storage for the operating system", "Temporary, fast storage for currently running programs", "Long-term file storage", "The CPU's processing cache"), 1, "RAM (Random Access Memory) holds currently active data and programs — it is wiped when the computer is powered off."),
                QuizQuestion("An operating system's primary role is to:", listOf("Store user files permanently", "Manage hardware resources and provide services to software", "Create application programs", "Connect to the internet"), 1, "The OS is the master control program — managing the CPU, memory, storage, and input/output for all applications."),
                QuizQuestion("The Internet of Things (IoT) refers to:", listOf("A network of office computers", "Physical devices with sensors that connect and exchange data over the internet", "A global internet service provider network", "Social media platforms collectively"), 1, "IoT connects everyday objects (thermostats, vehicles, appliances) to the internet, enabling remote monitoring and automation."),
                QuizQuestion("Cloud computing means:", listOf("Using computers in cloudy weather", "Accessing computing resources and services over the internet instead of locally", "A type of weather-forecasting software", "Programming using cloud-shaped diagrams"), 1, "Cloud computing delivers on-demand access to computing power, storage, and services over the internet — on a pay-as-you-go basis."),
                QuizQuestion("Machine learning is best described as:", listOf("Programming computers with explicit rules for every situation", "Systems that learn and improve automatically from data", "A type of computer hardware", "A user interface design method"), 1, "Machine learning uses algorithms that find patterns in data and improve their own accuracy without being explicitly reprogrammed."),
                QuizQuestion("A motherboard in a computer is:", listOf("External storage device", "The main circuit board connecting all major components", "The computer's power supply", "The display adapter"), 1, "The motherboard is the central hub — CPU, RAM, storage, and peripherals all connect to and communicate through it."),
                QuizQuestion("The primary purpose of a firewall is to:", listOf("Speed up internet connections", "Monitor and control incoming and outgoing network traffic", "Encrypt files stored on a hard drive", "Back up data automatically"), 1, "A firewall acts as a security barrier, filtering network traffic based on defined rules to block unauthorized access."),
                QuizQuestion("Open source software means:", listOf("Software that is always free to use", "Software whose source code is publicly available for viewing and modification", "Software made for open-air environments", "Software that works across all platforms"), 1, "Open source software exposes its source code — anyone can view, modify, and distribute it (though not always free of charge)."),
                QuizQuestion("Bandwidth in networking refers to:", listOf("The physical width of a network cable", "The maximum data that can be transmitted in a given time", "The number of users on a network", "The cost of internet service"), 1, "Bandwidth measures network capacity — typically in Mbps or Gbps. Higher bandwidth supports more simultaneous data transmission.")
            ))
        )
    }

    var screen by remember { mutableStateOf(startAt) }
    var selectedEvent by remember { mutableStateOf<TestEvent?>(null) }
    var isTimed by remember { mutableStateOf(false) }
    var isFullTest by remember { mutableStateOf(false) }
    var questionCount by remember { mutableStateOf(10) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf(-1) }
    var showFeedback by remember { mutableStateOf(false) }
    var userAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var timeLeftSeconds by remember { mutableStateOf(0) }
    var shuffleQuestions by remember { mutableStateOf(true) }
    var showExplanations by remember { mutableStateOf(true) }
    var timerMode by remember { mutableStateOf("total") }
    var secondsPerQuestion by remember { mutableStateOf(30) }
    var timerRestartSignal by remember { mutableStateOf(0) }
    var qaSelectedEvent by remember { mutableStateOf("") }
    var qaQuestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var qaLoadingQuestions by remember { mutableStateOf(false) }
    var qaRevealedAnswers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var qaError by remember { mutableStateOf("") }
    var qaFetchTrigger by remember { mutableStateOf(0) }
    var aiQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var isGeneratingAI by remember { mutableStateOf(false) }
    var aiGenError by remember { mutableStateOf("") }
    var qaEventSearch by remember { mutableStateOf("") }
    var testEventSearch by remember { mutableStateOf("") }
    val qaEventList = remember {
        listOf(
            "Accounting I", "Accounting II", "Agribusiness", "Banking & Financial Systems",
            "Business Calculations", "Business Communication", "Business Law", "Business Math",
            "Computer Applications", "Computer Problem Solving", "Cybersecurity",
            "Database Design & Applications", "Economics", "Electronic Career Portfolio",
            "Entrepreneurship", "Global Business", "Healthcare Administration", "Help Desk",
            "Hospitality & Event Management", "Human Resource Management",
            "Insurance & Risk Management", "Introduction to Business",
            "Introduction to Business Communication", "Introduction to Financial Math",
            "Introduction to Information Technology", "Introduction to Parliamentary Procedure",
            "Job Interview", "Management Decision Making", "Marketing", "Network Design",
            "Networking Concepts", "Parliamentary Procedure", "Personal Finance",
            "Political Science", "Public Speaking", "Securities & Investments",
            "Social Media Strategies", "Sports & Entertainment Management",
            "Supply Chain Management", "UX Design", "Word Processing",
            "Business Ethics", "Business Plan", "Client Service",
            "Digital Video Production", "E-business", "Emerging Business Issues",
            "Future Business Educator", "Graphic Design", "Impromptu Speaking",
            "Mobile Application Development", "Sales Presentation", "Website Design"
        )
    }

    LaunchedEffect(screen, timerRestartSignal) {
        if (screen == "quiz" && isTimed && timeLeftSeconds > 0) {
            while (timeLeftSeconds > 0) {
                delay(1000L)
                timeLeftSeconds = maxOf(0, timeLeftSeconds - 1)
            }
            if (screen == "quiz") {
                if (timerMode == "per_question" && currentIndex < questions.size - 1) {
                    if (selectedAnswer != -1) userAnswers = userAnswers + (currentIndex to selectedAnswer)
                    currentIndex++
                    selectedAnswer = -1
                    showFeedback = false
                    timeLeftSeconds = secondsPerQuestion
                    timerRestartSignal++
                } else {
                    if (selectedAnswer != -1) userAnswers = userAnswers + (currentIndex to selectedAnswer)
                    screen = "results"
                }
            }
        }
    }

    LaunchedEffect(screen, qaSelectedEvent, qaFetchTrigger) {
        if (screen != "qaPrep" || qaSelectedEvent.isEmpty() || qaQuestions.isNotEmpty()) return@LaunchedEffect
        qaLoadingQuestions = true
        qaError = ""
        try {
            val data = hashMapOf<String, Any>(
                "messages" to emptyList<Any>(),
                "query" to "Generate 7 judge interview questions for the FBLA $qaSelectedEvent competitive event. These are questions real FBLA judges ask competitors during oral/interview portions. Return ONLY valid JSON (no markdown, no code blocks) in this exact format: [{\"question\":\"...\",\"answer\":\"...\"},...]. Make the questions specific to the $qaSelectedEvent event content, format, and required skills. Each model answer should be 2-3 clear, specific sentences a competitor could actually say."
            )
            val result = FirebaseFunctions.getInstance()
                .getHttpsCallable("chatWithAI")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any>
            val reply = (map?.get("reply") as? String ?: "").trim()
            val clean = reply.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val arr = org.json.JSONArray(clean)
            val parsed = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsed.add(Pair(obj.optString("question", ""), obj.optString("answer", "")))
            }
            qaQuestions = parsed.filter { it.first.isNotEmpty() }
            if (qaQuestions.isEmpty()) qaError = "Could not parse AI response. Please try again."
        } catch (e: Exception) {
            qaError = "Could not generate questions. Check your connection and try again."
        }
        qaLoadingQuestions = false
    }

    LaunchedEffect(selectedEvent) {
        val ev = selectedEvent ?: return@LaunchedEffect
        if (ev.questions.size >= 20) return@LaunchedEffect
        aiQuestions = emptyList()
        isGeneratingAI = true
        aiGenError = ""
        try {
            val needed = 20 - ev.questions.size
            val coveredTopics = ev.questions.take(5).joinToString("; ") { it.question.take(60) }
            val data = hashMapOf<String, Any>(
                "messages" to emptyList<Any>(),
                "query" to "Generate $needed multiple-choice quiz questions for the FBLA ${ev.name} competitive event. Return ONLY valid JSON (no markdown, no code blocks) in this exact format: [{\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctIndex\":0,\"explanation\":\"...\"},...]. Each question must have exactly 4 answer options. correctIndex is 0-based (0=A,1=B,2=C,3=D). FBLA exam difficulty. Do not repeat these already-covered topics: $coveredTopics"
            )
            val result = FirebaseFunctions.getInstance()
                .getHttpsCallable("chatWithAI")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any>
            val reply = (map?.get("reply") as? String ?: "").trim()
            val stripped = reply.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val startIdx = stripped.indexOf('[')
            val endIdx = stripped.lastIndexOf(']')
            val clean = if (startIdx >= 0 && endIdx > startIdx) stripped.substring(startIdx, endIdx + 1) else stripped
            val arr = org.json.JSONArray(clean)
            val parsed = mutableListOf<QuizQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.optJSONArray("options")
                val opts = mutableListOf<String>()
                if (optsArr != null) for (j in 0 until optsArr.length()) opts.add(optsArr.optString(j, ""))
                if (opts.size == 4 && obj.optString("question", "").isNotEmpty()) {
                    parsed.add(QuizQuestion(
                        question = obj.optString("question", ""),
                        options = opts,
                        correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3),
                        explanation = obj.optString("explanation", "")
                    ))
                }
            }
            aiQuestions = parsed
            if (parsed.isEmpty()) aiGenError = "Could not load extra questions."
        } catch (e: Exception) {
            aiGenError = "Could not load extra questions. Check your connection."
        }
        isGeneratingAI = false
    }

    val score = userAnswers.entries.count { (i, ans) -> questions.getOrNull(i)?.correctIndex == ans }

    fun startQuiz() {
        val ev = selectedEvent ?: return
        val combined = ev.questions + aiQuestions
        val pool = if (shuffleQuestions) combined.shuffled() else combined
        questions = pool.take(questionCount.coerceAtMost(pool.size))
        currentIndex = 0
        selectedAnswer = -1
        showFeedback = false
        userAnswers = emptyMap()
        timeLeftSeconds = when {
            !isTimed -> 0
            timerMode == "per_question" -> secondsPerQuestion
            else -> questionCount * 60
        }
        timerRestartSignal++
        screen = "quiz"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        when (screen) {

            "home" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 4.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = onBack, tint = textColor)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TESTING HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Text("Choose Your Mode", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                        }
                    }
                    data class ModeTile(val label: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)
                    val modes = listOf(
                        ModeTile("Practice Mode", "Pick a topic — get feedback after each answer", Icons.Default.AutoStories) { isFullTest = false; screen = "selectEvent" },
                        ModeTile("Full Test", "Answer all questions, then see your score", Icons.Default.Assignment) { isFullTest = true; screen = "selectEvent" },
                        ModeTile("Q&A Prep", "AI-generated judge questions for any event", Icons.Default.QuestionAnswer) { screen = "qaSelectEvent" }
                    )
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        modes.forEach { mode ->
                            Surface(
                                onClick = mode.onClick,
                                modifier = Modifier.fillMaxWidth().pressScale(),
                                shape = RoundedCornerShape(20.dp),
                                color = cardBg
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(46.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                        Icon(mode.icon, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mode.label, color = textColor, fontWeight = FontWeight.Bold, fontSize = (15 * textSizeMultiplier).sp)
                                        Text(mode.subtitle, color = textColor.copy(alpha = 0.55f), fontSize = (12 * textSizeMultiplier).sp)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            "selectEvent" -> {
                val filteredEvents = remember(testEventSearch) {
                    if (testEventSearch.isBlank()) allEvents
                    else allEvents.filter { it.name.contains(testEventSearch, ignoreCase = true) }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = { if (startAt == "home") screen = "home" else onBack() }, tint = textColor)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isFullTest) "FULL TEST" else "PRACTICE MODE", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Text("Pick a Topic", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                        }
                    }
                    OutlinedTextField(
                        value = testEventSearch,
                        onValueChange = { testEventSearch = it },
                        placeholder = { Text("Search events...", color = textColorSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = textColorSecondary) },
                        trailingIcon = { if (testEventSearch.isNotEmpty()) IconButton(onClick = { testEventSearch = "" }) { Icon(Icons.Default.Close, null, tint = textColorSecondary) } },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor, unfocusedBorderColor = primaryColor.copy(alpha = 0.3f), focusedContainerColor = cardBg, unfocusedContainerColor = cardBg),
                        singleLine = true
                    )
                    if (filteredEvents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No events match \"$testEventSearch\"", color = textColorSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredEvents) { ev ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(165.dp).pressScale(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    onClick = { selectedEvent = ev; screen = "config" }
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size(42.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                            Icon(ev.icon, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        }
                                        Column {
                                            Text(ev.name, color = textColor, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text("${if (ev.questions.size < 20) "Up to 20" else ev.questions.size.toString()} questions", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "config" -> {
                val ev = selectedEvent ?: return@Box
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = { screen = "selectEvent" }, tint = textColor)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CONFIGURE", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(ev.icon, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                Text(ev.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (20 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                Text("NUMBER OF QUESTIONS", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp))
                                if (isGeneratingAI) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = primaryColor, strokeWidth = 2.dp)
                                    Text("Loading more questions...", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                }
                            }
                            val totalAvailable = (ev.questions + aiQuestions).size
                            val countOptions = listOf(5, 10, 20).filter { it <= totalAvailable }.let { base ->
                                if (totalAvailable !in base && totalAvailable > 0) base + totalAvailable else base
                            }.distinct()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                countOptions.forEach { count ->
                                    val isSelected = questionCount == count
                                    Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).clickable { questionCount = count }) {
                                        Column(modifier = Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$count", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = (20 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                            Text(if (count == totalAvailable) "All" else "Questions", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                        }
                                    }
                                }
                            }
                            if (aiGenError.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(aiGenError, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("TIMER", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                data class TimerOpt(val label: String, val sub: String, val timed: Boolean, val mode: String)
                                listOf(
                                    TimerOpt("Untimed", "No limit", false, "total"),
                                    TimerOpt("Total Time", "${questionCount} min total", true, "total"),
                                    TimerOpt("Per Question", "Configurable", true, "per_question")
                                ).forEach { opt ->
                                    val isSelected = isTimed == opt.timed && (!opt.timed || timerMode == opt.mode)
                                    Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).clickable { isTimed = opt.timed; timerMode = opt.mode }) {
                                        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(opt.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor, textAlign = TextAlign.Center)
                                            Text(opt.sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                            if (isTimed && timerMode == "per_question") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("SECONDS PER QUESTION", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(15 to "15s", 30 to "30s", 60 to "60s").forEach { (secs, label) ->
                                        val isSelected = secondsPerQuestion == secs
                                        Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).clickable { secondsPerQuestion = secs }) {
                                            Column(modifier = Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                                Text("per question", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("QUESTION ORDER", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(true to Pair("Shuffled", "Random order"), false to Pair("Sequential", "Original order")).forEach { (shuffle, labels) ->
                                    val isSelected = shuffleQuestions == shuffle
                                    Surface(shape = RoundedCornerShape(14.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).height(76.dp).clickable { shuffleQuestions = shuffle }) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(labels.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                            Text(labels.second, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("EXPLANATIONS", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(true to Pair("After Each", "See tips as you go"), false to Pair("Results Only", "Review after test")).forEach { (showEach, labels) ->
                                    val isSelected = showExplanations == showEach
                                    Surface(shape = RoundedCornerShape(14.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).height(76.dp).clickable { showExplanations = showEach }) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(labels.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                            Text(labels.second, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("SUMMARY", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (10 * textSizeMultiplier).sp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    listOf(
                                        "Topic" to ev.name,
                                        "Mode" to if (isFullTest) "Full Test" else "Practice",
                                        "Questions" to "$questionCount",
                                        "Order" to if (shuffleQuestions) "Shuffled" else "Sequential",
                                        "Timer" to when { !isTimed -> "None"; timerMode == "per_question" -> "${secondsPerQuestion}s / question"; else -> "${questionCount} min total" },
                                        "Explanations" to if (showExplanations) "After each" else "Results only"
                                    ).forEach { (k, v) ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(k, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                                            Text(v, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { startQuiz() }, modifier = Modifier.fillMaxWidth().height(54.dp).pressScale(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                                Text("Start ${if (isFullTest) "Test" else "Practice"}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            "quiz" -> {
                if (questions.isEmpty()) { screen = "home"; return@Box }
                val q = questions[currentIndex]
                val progress = (currentIndex + 1).toFloat() / questions.size
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = { if (startAt == "home") screen = "home" else onBack() }, tint = textColor)
                        Column(modifier = Modifier.weight(1f)) {
                            Text((selectedEvent?.name ?: "").uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (10 * textSizeMultiplier).sp))
                            Text("Question ${currentIndex + 1} of ${questions.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (16 * textSizeMultiplier).sp))
                        }
                        if (isTimed) {
                            val mins = timeLeftSeconds / 60
                            val secs = timeLeftSeconds % 60
                            val warningThreshold = if (timerMode == "per_question") 10 else 60
                            val isWarning = timeLeftSeconds < warningThreshold
                            Surface(shape = RoundedCornerShape(10.dp), color = if (isWarning) Color.Red.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.15f), border = BorderStroke(1.dp, if (isWarning) Color.Red else primaryColor)) {
                                Text(text = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), color = if (isWarning) Color.Red else primaryColor)
                            }
                        }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 16.dp), color = primaryColor, trackColor = primaryColor.copy(alpha = 0.2f))
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBg), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))) {
                                Text(q.question, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            q.options.forEachIndexed { idx, option ->
                                val isSelected = selectedAnswer == idx
                                val isCorrect = idx == q.correctIndex
                                val bgColor = when {
                                    showFeedback && isCorrect -> Color(0xFF22C55E).copy(alpha = 0.18f)
                                    showFeedback && isSelected && !isCorrect -> Color.Red.copy(alpha = 0.18f)
                                    isSelected -> primaryColor.copy(alpha = 0.18f)
                                    else -> cardBg
                                }
                                val borderColor = when {
                                    showFeedback && isCorrect -> Color(0xFF22C55E)
                                    showFeedback && isSelected && !isCorrect -> Color.Red
                                    isSelected -> primaryColor
                                    else -> primaryColor.copy(alpha = 0.15f)
                                }
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).pressScale(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = bgColor), border = BorderStroke(1.5.dp, borderColor), onClick = { if (!showFeedback) selectedAnswer = idx }) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Surface(shape = CircleShape, color = if (isSelected || (showFeedback && isCorrect)) borderColor else primaryColor.copy(alpha = 0.1f), modifier = Modifier.size(28.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                when {
                                                    showFeedback && isCorrect -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    showFeedback && isSelected && !isCorrect -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    else -> Text(listOf("A", "B", "C", "D")[idx], style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = (12 * textSizeMultiplier).sp), color = if (isSelected) Color.White else textColorSecondary)
                                                }
                                            }
                                        }
                                        Text(option, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = (14 * textSizeMultiplier).sp), color = textColor)
                                    }
                                }
                            }
                            if (showFeedback && showExplanations && q.explanation.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF22C55E).copy(alpha = 0.08f)), border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp).padding(top = 1.dp))
                                        Text(q.explanation, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (isFullTest) {
                                        if (selectedAnswer != -1) userAnswers = userAnswers + (currentIndex to selectedAnswer)
                                        if (currentIndex < questions.size - 1) { currentIndex++; selectedAnswer = -1; if (isTimed && timerMode == "per_question") { timeLeftSeconds = secondsPerQuestion; timerRestartSignal++ } } else { screen = "results" }
                                    } else {
                                        if (!showFeedback) {
                                            if (selectedAnswer != -1) { userAnswers = userAnswers + (currentIndex to selectedAnswer); showFeedback = true }
                                        } else {
                                            if (currentIndex < questions.size - 1) { currentIndex++; selectedAnswer = -1; showFeedback = false; if (isTimed && timerMode == "per_question") { timeLeftSeconds = secondsPerQuestion; timerRestartSignal++ } } else { screen = "results" }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                enabled = selectedAnswer != -1 || showFeedback
                            ) {
                                Text(
                                    text = when {
                                        isFullTest && currentIndex < questions.size - 1 -> "Next →"
                                        isFullTest -> "Submit Test"
                                        !showFeedback -> "Check Answer"
                                        currentIndex < questions.size - 1 -> "Next Question →"
                                        else -> "See Results"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp), color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            "results" -> {
                val pct = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
                val gradeColor = when { pct >= 90 -> Color(0xFF22C55E); pct >= 80 -> Color(0xFF84CC16); pct >= 70 -> Color(0xFFF59E0B); pct >= 60 -> Color(0xFFF97316); else -> Color.Red }
                val gradeLetter = when { pct >= 90 -> "A"; pct >= 80 -> "B"; pct >= 70 -> "C"; pct >= 60 -> "D"; else -> "F" }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = { if (startAt == "home") screen = "home" else onBack() }, tint = textColor)
                        Text("Results", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.5f))) {
                                Box(modifier = Modifier.fillMaxWidth().background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(gradeColor.copy(alpha = 0.18f), gradeColor.copy(alpha = 0.04f))), shape = RoundedCornerShape(24.dp))) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(gradeLetter, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = (72 * textSizeMultiplier).sp), color = gradeColor)
                                        Text("$pct%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = (28 * textSizeMultiplier).sp), color = textColor)
                                        Text("$score / ${questions.size} correct", style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textSizeMultiplier).sp), color = textColorSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(when { pct >= 90 -> "Outstanding! You're ready to compete!"; pct >= 80 -> "Great job! A little more practice and you'll crush it!"; pct >= 70 -> "Good effort! Review what you missed and try again!"; pct >= 60 -> "Keep at it — you're getting there!"; else -> "Don't give up! Review the material and try again!" }, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { startQuiz() }, modifier = Modifier.weight(1f).height(50.dp).pressScale(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                                    Text("Try Again", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), color = Color.Black)
                                }
                                OutlinedButton(onClick = { screen = "home" }, modifier = Modifier.weight(1f).height(50.dp).pressScale(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, primaryColor)) {
                                    Text("New Test", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), color = primaryColor)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("ANSWER REVIEW", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                        }
                        questions.forEachIndexed { i, q ->
                            item {
                                val userAns = userAnswers[i]
                                val isCorrect = userAns == q.correctIndex
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFF22C55E).copy(alpha = 0.07f) else Color.Red.copy(alpha = 0.07f)), border = BorderStroke(1.dp, if (isCorrect) Color(0xFF22C55E).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f))) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (isCorrect) Color(0xFF22C55E) else Color.Red, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                                            Text("Q${i + 1}: ${q.question}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Correct: ${q.options[q.correctIndex]}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp), color = Color(0xFF22C55E))
                                        if (!isCorrect && userAns != null) Text("Your answer: ${q.options[userAns]}", style = MaterialTheme.typography.labelMedium.copy(fontSize = (12 * textSizeMultiplier).sp), color = Color.Red.copy(alpha = 0.8f))
                                        else if (userAns == null) Text("Not answered (time expired)", style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = textColorSecondary)
                                        if (q.explanation.isNotEmpty()) { Spacer(modifier = Modifier.height(4.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lightbulb, null, tint = textColorSecondary, modifier = Modifier.size(14.dp).padding(top = 1.dp)); Text(q.explanation, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = textColorSecondary) } }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }

            "qaSelectEvent" -> {
                fun qaIcon(name: String): ImageVector = when {
                    name.contains("Account") -> Icons.Default.Calculate
                    name.contains("Agri") -> Icons.Default.Grass
                    name.contains("Banking") -> Icons.Default.AccountBalance
                    name.contains("Business Law") -> Icons.Default.Gavel
                    name.contains("Calculation") || name.contains("Math") -> Icons.Default.Functions
                    name.contains("Communication") -> Icons.Default.Forum
                    name.contains("Computer") || name.contains("IT") -> Icons.Default.Computer
                    name.contains("Cyber") -> Icons.Default.Security
                    name.contains("Database") -> Icons.Default.Storage
                    name.contains("Economics") -> Icons.Default.TrendingUp
                    name.contains("Entrepreneur") -> Icons.Default.Lightbulb
                    name.contains("Global") -> Icons.Default.Public
                    name.contains("Healthcare") -> Icons.Default.Favorite
                    name.contains("Help Desk") -> Icons.Default.HeadsetMic
                    name.contains("Hospitality") -> Icons.Default.Star
                    name.contains("Human Resource") -> Icons.Default.People
                    name.contains("Insurance") -> Icons.Default.VerifiedUser
                    name.contains("Interview") -> Icons.Default.RecordVoiceOver
                    name.contains("Management") -> Icons.Default.Psychology
                    name.contains("Marketing") -> Icons.Default.Campaign
                    name.contains("Network") -> Icons.Default.Hub
                    name.contains("Parliamentary") -> Icons.Default.Gavel
                    name.contains("Personal Finance") -> Icons.Default.AccountBalanceWallet
                    name.contains("Speaking") -> Icons.Default.Mic
                    name.contains("Securities") || name.contains("Investments") -> Icons.Default.ShowChart
                    name.contains("Social Media") -> Icons.Default.Share
                    name.contains("Sports") -> Icons.Default.EmojiEvents
                    name.contains("Supply Chain") -> Icons.Default.LocalShipping
                    name.contains("UX") -> Icons.Default.DesignServices
                    name.contains("Video") -> Icons.Default.Videocam
                    name.contains("Word Processing") -> Icons.Default.Description
                    name.contains("Website") || name.contains("E-business") -> Icons.Default.Web
                    name.contains("Sales") -> Icons.Default.Storefront
                    name.contains("Ethics") -> Icons.Default.Balance
                    name.contains("Portfolio") -> Icons.Default.WorkspacePremium
                    name.contains("Mobile") -> Icons.Default.PhoneAndroid
                    name.contains("Graphic") -> Icons.Default.Palette
                    name.contains("Business Plan") -> Icons.Default.Assignment
                    name.contains("Client") -> Icons.Default.SupportAgent
                    name.contains("Digital") -> Icons.Default.VideoLibrary
                    name.contains("Emerging") -> Icons.Default.Bolt
                    name.contains("Future Business") || name.contains("Educator") -> Icons.Default.School
                    name.contains("Introduction") -> Icons.Default.MenuBook
                    else -> Icons.Default.EmojiEvents
                }
                val filteredQaEvents = remember(qaEventSearch) {
                    if (qaEventSearch.isBlank()) qaEventList
                    else qaEventList.filter { it.contains(qaEventSearch, ignoreCase = true) }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = { if (startAt == "home") screen = "home" else onBack() }, tint = textColor)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Q&A PREP", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Text("Select Your Event", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                        }
                    }
                    OutlinedTextField(
                        value = qaEventSearch,
                        onValueChange = { qaEventSearch = it },
                        placeholder = { Text("Search events...", color = textColor.copy(alpha = 0.4f), fontSize = (13 * textSizeMultiplier).sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (qaEventSearch.isNotEmpty()) IconButton(onClick = { qaEventSearch = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedContainerColor = cardBg, unfocusedContainerColor = cardBg,
                            cursorColor = primaryColor
                        )
                    )
                    if (filteredQaEvents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No events match \"$qaEventSearch\"", color = textColorSecondary, fontSize = (13 * textSizeMultiplier).sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredQaEvents) { eventName ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(120.dp).pressScale(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    onClick = {
                                        qaSelectedEvent = eventName
                                        qaQuestions = emptyList()
                                        qaRevealedAnswers = emptySet()
                                        qaError = ""
                                        qaEventSearch = ""
                                        screen = "qaPrep"
                                    }
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                            Box(modifier = Modifier.size(34.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                                Icon(qaIcon(eventName), null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                            }
                                            Column {
                                                Text(eventName, color = textColor, fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp, lineHeight = (16 * textSizeMultiplier).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Text("Judge Q&A", color = primaryColor.copy(alpha = 0.7f), fontSize = (10 * textSizeMultiplier).sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }

            "qaPrep" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(onClick = { screen = "qaSelectEvent" }, tint = textColor)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Q&A PREP", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Text(qaSelectedEvent, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (20 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (qaQuestions.isNotEmpty() && !qaLoadingQuestions) {
                            IconButton(onClick = { qaQuestions = emptyList(); qaRevealedAnswers = emptySet(); qaError = ""; qaFetchTrigger++ }) {
                                Icon(Icons.Default.Refresh, null, tint = primaryColor)
                            }
                        }
                    }
                    when {
                        qaLoadingQuestions -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(48.dp))
                                    Text("Generating judge questions...", color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                    Text("Powered by Claude AI", color = textColorSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                                }
                            }
                        }
                        qaError.isNotEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
                                    Text(qaError, color = textColorSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                    Button(onClick = { qaError = ""; qaFetchTrigger++ }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(12.dp)) {
                                        Text("Try Again", color = Color.Black, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = (14 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lightbulb, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                            Text("Tap any card to reveal a model answer. Tap again to hide.", style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                    }
                                }
                                itemsIndexed(qaQuestions) { index, (question, answer) ->
                                    val revealed = index in qaRevealedAnswers
                                    Card(
                                        modifier = Modifier.fillMaxWidth().pressScale(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (revealed) primaryColor.copy(alpha = 0.1f) else cardBg),
                                        border = BorderStroke(1.5.dp, if (revealed) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.2f)),
                                        onClick = { qaRevealedAnswers = if (revealed) qaRevealedAnswers - index else qaRevealedAnswers + index }
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = primaryColor.copy(alpha = 0.15f), modifier = Modifier.size(28.dp)) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = (12 * textSizeMultiplier).sp), color = primaryColor)
                                                    }
                                                }
                                                Text(question, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (14 * textSizeMultiplier).sp, lineHeight = (20 * textSizeMultiplier).sp), color = textColor, modifier = Modifier.weight(1f))
                                            }
                                            androidx.compose.animation.AnimatedVisibility(visible = revealed) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    HorizontalDivider(color = primaryColor.copy(alpha = 0.25f))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CheckCircle, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                                        Text("MODEL ANSWER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = (10 * textSizeMultiplier).sp), color = primaryColor)
                                                    }
                                                    Text(answer, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (13 * textSizeMultiplier).sp, lineHeight = (19 * textSizeMultiplier).sp), color = textColorSecondary)
                                                }
                                            }
                                            if (!revealed) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Tap to reveal answer", style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = primaryColor.copy(alpha = 0.6f))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HubSelectorScreen(
    onPracticeTest: () -> Unit,
    onQnA: () -> Unit,
    onExternalTests: () -> Unit,
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)

    data class HubTile(val label: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)
    val tiles = listOf(
        HubTile("Practice Test", "Pick a category, get instant feedback", Icons.Default.Quiz, onPracticeTest),
        HubTile("Q&A Prep", "AI judge questions for any event", Icons.Default.QuestionAnswer, onQnA),
        HubTile("External Tests", "Official FBLA practice test PDFs", Icons.AutoMirrored.Filled.LibraryBooks, onExternalTests)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 4.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack, tint = textColor)
                Column(modifier = Modifier.weight(1f)) {
                    Text("STUDY HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                    Text("What are we doing?", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tiles.forEach { tile ->
                    Surface(
                        onClick = tile.onClick,
                        modifier = Modifier.fillMaxWidth().pressScale(),
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(46.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(tile.icon, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tile.label, color = textColor, fontWeight = FontWeight.Bold, fontSize = (15 * textSizeMultiplier).sp)
                                Text(tile.subtitle, color = textColor.copy(alpha = 0.55f), fontSize = (12 * textSizeMultiplier).sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JudgeReadyScreen(
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)

    data class JudgeTip(val title: String, val body: String, val icon: ImageVector)
    val tips = listOf(
        JudgeTip("First Impressions", "Judges form opinions within seconds. Arrive in full business professional attire, make eye contact when entering the room, and introduce yourself with a firm handshake and a confident smile.", Icons.Default.Verified),
        JudgeTip("Know Your Event Cold", "Read your event's topic outline 3+ times. Judges expect you to know every term, formula, and concept — not just the surface level. Surprises in the room should come from you, not to you.", Icons.Default.MenuBook),
        JudgeTip("Speak Clearly & Confidently", "Slow down. Most competitors talk too fast when nervous. Pause between points, use confident language (\"I believe\" not \"I think maybe\"), and never say \"um\" or \"like\" more than once.", Icons.Default.Mic),
        JudgeTip("Answer What Was Asked", "When a judge asks a question, listen completely before responding. Answer directly, then support with detail. If you don't know, say \"That's a great question — here's how I'd approach it.\"", Icons.Default.QuestionAnswer),
        JudgeTip("Parliamentary Procedure", "If your event uses parliamentary procedure, know Robert's Rules cold — motions, seconds, voting, points of order. Judges penalize hesitation and procedural errors immediately.", Icons.Default.Gavel),
        JudgeTip("Time Management", "Most events have strict time limits. Practice finishing 10–15 seconds early so you never get cut off. Running over time can disqualify or heavily penalize your score.", Icons.Default.Timer),
        JudgeTip("Professional Body Language", "Stand or sit up straight, keep hands visible and still, and nod to show you're listening. Avoid crossing arms, looking at the floor, or fidgeting with your materials.", Icons.Default.EmojiPeople),
        JudgeTip("Close Strong", "End with a clear summary statement or call to action. The last thing judges hear sticks most. \"In conclusion...\" followed by one punchy sentence can be the difference between 1st and 2nd.", Icons.Default.Flag)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 4.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack, tint = textColor)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("STUDY HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                        Text("Judge Ready", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                    }
                }
                Text(
                    "What separates good from first place.",
                    color = textColor.copy(alpha = 0.55f),
                    fontSize = (13 * textSizeMultiplier).sp,
                    modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 20.dp)
                )
            }
            items(tips) { tip ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(tip.icon, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tip.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = (14 * textSizeMultiplier).sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(tip.body, color = textColor.copy(alpha = 0.7f), fontSize = (12 * textSizeMultiplier).sp, lineHeight = (18 * textSizeMultiplier).sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourcesScreen(primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false, userState: String = "", userSchool: String = "", chapterInstagramHandle: String = "") {
// Dynamic text colors
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)
    val tileBg = if (lightMode) Color.White else Color(0xFF0F1E38)
    val tileBorder = if (lightMode) Color(0xFFDDE3F0) else primaryColor.copy(alpha = 0.18f)

    val context = LocalContext.current
    var activeHub by remember { mutableStateOf("") }
    var activeResourceCategory by remember { mutableStateOf("") }

    data class IgPost(val id: String, val imageUrl: String, val caption: String, val timestamp: Long, val likeCount: Int, val commentCount: Int, val postUrl: String, val isVideo: Boolean)
    data class IgProfile(val username: String, val fullName: String, val bio: String, val profilePicUrl: String, val followerCount: Int, val postCount: Int, val posts: List<IgPost>)

    var igProfile by remember { mutableStateOf<IgProfile?>(null) }
    var igLoading by remember { mutableStateOf(false) }
    var igError by remember { mutableStateOf("") }
    var igSelectedPost by remember { mutableStateOf<IgPost?>(null) }

    var natIgProfile by remember { mutableStateOf<IgProfile?>(null) }
    var natIgLoading by remember { mutableStateOf(false) }
    var natIgError by remember { mutableStateOf("") }
    var natIgSelectedPost by remember { mutableStateOf<IgPost?>(null) }

    LaunchedEffect(chapterInstagramHandle) {
        if (chapterInstagramHandle.isBlank()) { igProfile = null; igError = ""; return@LaunchedEffect }
        igLoading = true
        igError = ""
        try {
            val fetched = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://i.instagram.com/api/v1/users/web_profile_info/?username=${chapterInstagramHandle.trim()}")
                    .header("x-ig-app-id", "936619743392459")
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.instagram.com/")
                    .header("Origin", "https://www.instagram.com")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body?.string() ?: throw Exception("Empty response")
                val root = org.json.JSONObject(body)
                val user = root.getJSONObject("data").getJSONObject("user")
                val timeline = user.getJSONObject("edge_owner_to_timeline_media")
                val edges = timeline.getJSONArray("edges")
                val posts = mutableListOf<IgPost>()
                for (i in 0 until minOf(edges.length(), 12)) {
                    val node = edges.getJSONObject(i).getJSONObject("node")
                    val captionEdges = node.optJSONObject("edge_media_to_caption")?.optJSONArray("edges")
                    val caption = captionEdges?.takeIf { it.length() > 0 }?.getJSONObject(0)?.getJSONObject("node")?.optString("text") ?: ""
                    posts.add(IgPost(
                        id = node.optString("id"),
                        imageUrl = node.optString("display_url"),
                        caption = caption,
                        timestamp = node.optLong("taken_at_timestamp", 0L) * 1000L,
                        likeCount = node.optJSONObject("edge_liked_by")?.optInt("count") ?: 0,
                        commentCount = node.optJSONObject("edge_media_to_comment")?.optInt("count") ?: 0,
                        postUrl = "https://www.instagram.com/p/${node.optString("shortcode")}/",
                        isVideo = node.optBoolean("is_video")
                    ))
                }
                IgProfile(
                    username = user.optString("username", chapterInstagramHandle),
                    fullName = user.optString("full_name"),
                    bio = user.optString("biography"),
                    profilePicUrl = user.optString("profile_pic_url"),
                    followerCount = user.optJSONObject("edge_followed_by")?.optInt("count") ?: 0,
                    postCount = timeline.optInt("count"),
                    posts = posts
                )
            }
            igProfile = fetched
        } catch (e: Exception) {
            igError = "Could not load @$chapterInstagramHandle"
            android.util.Log.e("Instagram", "fetch failed: ${e.message}")
        } finally {
            igLoading = false
        }
    }

    LaunchedEffect(Unit) {
        natIgLoading = true
        try {
            val fetched = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://i.instagram.com/api/v1/users/web_profile_info/?username=fbla_national")
                    .header("x-ig-app-id", "936619743392459")
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.instagram.com/")
                    .header("Origin", "https://www.instagram.com")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body?.string() ?: throw Exception("Empty response")
                val root = org.json.JSONObject(body)
                val user = root.getJSONObject("data").getJSONObject("user")
                val timeline = user.getJSONObject("edge_owner_to_timeline_media")
                val edges = timeline.getJSONArray("edges")
                val posts = mutableListOf<IgPost>()
                for (i in 0 until minOf(edges.length(), 6)) {
                    val node = edges.getJSONObject(i).getJSONObject("node")
                    val captionEdges = node.optJSONObject("edge_media_to_caption")?.optJSONArray("edges")
                    val caption = captionEdges?.takeIf { it.length() > 0 }?.getJSONObject(0)?.getJSONObject("node")?.optString("text") ?: ""
                    posts.add(IgPost(
                        id = node.optString("id"),
                        imageUrl = node.optString("display_url"),
                        caption = caption,
                        timestamp = node.optLong("taken_at_timestamp", 0L) * 1000L,
                        likeCount = node.optJSONObject("edge_liked_by")?.optInt("count") ?: 0,
                        commentCount = node.optJSONObject("edge_media_to_comment")?.optInt("count") ?: 0,
                        postUrl = "https://www.instagram.com/p/${node.optString("shortcode")}/",
                        isVideo = node.optBoolean("is_video")
                    ))
                }
                IgProfile(
                    username = user.optString("username", "fbla_national"),
                    fullName = user.optString("full_name"),
                    bio = user.optString("biography"),
                    profilePicUrl = user.optString("profile_pic_url"),
                    followerCount = user.optJSONObject("edge_followed_by")?.optInt("count") ?: 0,
                    postCount = timeline.optInt("count"),
                    posts = posts
                )
            }
            natIgProfile = fetched
        } catch (e: Exception) {
            natIgError = "Could not load @fbla_national"
            android.util.Log.e("Instagram", "national fetch failed: ${e.message}")
        } finally {
            natIgLoading = false
        }
    }

    var resourceSearch by remember { mutableStateOf("") }

    when (activeHub) {
        "prep" -> { EventPrepHub(onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "testing_practice" -> { TestingHub(onBack = { activeHub = "selector" }, startAt = "selectEvent", primaryColor = primaryColor, bgOpacity = bgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "testing_qna" -> { TestingHub(onBack = { activeHub = "selector" }, startAt = "qaSelectEvent", primaryColor = primaryColor, bgOpacity = bgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "selector" -> { HubSelectorScreen(onPracticeTest = { activeHub = "testing_practice" }, onQnA = { activeHub = "testing_qna" }, onExternalTests = { activeHub = "from_selector"; activeResourceCategory = "practice_tests"; resourceSearch = "" }, onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "dress_code" -> { DressCodeCheckerScreen(onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
    }

    data class ResourceLink(val title: String, val description: String, val url: String)


    val resourceLinks = remember {
        listOf(
            // ── Official FBLA ──────────────────────────────────────────────────
            ResourceLink("FBLA National", "Official FBLA homepage — news, events, and announcements.", "https://www.fbla.org/"),
            ResourceLink("Member Portal", "Log in to your FBLA account, manage membership, and access resources.", "https://portal.fbla.org/"),
            ResourceLink("Competitive Events", "Browse all HS competitive events, rules, and topic outlines.", "https://www.fbla.org/high-school/competitive-events/"),
            ResourceLink("NLC Information", "National Leadership Conference — eligibility, schedule, and registration.", "https://www.fbla.org/high-school/national-leadership-conference/"),
            ResourceLink("SLC Information", "State Leadership Conference details, dates, and registration info.", "https://www.fbla.org/high-school/state-leadership-conference/"),
            ResourceLink("Scholarships", "National and state-level scholarship opportunities for FBLA members.", "https://www.fbla.org/high-school/scholarships/"),
            ResourceLink("Brand Center", "Download official FBLA logos, colors, and brand usage guidelines.", "https://www.fbla.org/brand-center/"),
            ResourceLink("Dress Code", "Official business professional attire standards for all conferences.", "https://www.fbla.org/dress-code/"),
            ResourceLink("State Chapters", "Find your state's FBLA chapter page for local events and contacts.", "https://www.fbla.org/chapters/"),
            ResourceLink("Community Service", "FBLA's March of Dimes partnership and service hour requirements.", "https://www.fbla.org/march-of-dimes/"),
            ResourceLink("Chapter Officer Resources", "Handbooks, training materials, and tools for chapter officers.", "https://www.fbla.org/high-school/chapter-management/"),
            ResourceLink("FBLA Store", "Order official FBLA merchandise, awards, and chapter supplies.", "https://www.fblastore.org/"),
            ResourceLink("FBLA Blog / News", "Latest FBLA news, member spotlights, and competition recaps.", "https://www.fbla.org/news/"),
            // ── Study & Test Prep ─────────────────────────────────────────────
            ResourceLink("Quizlet — FBLA Sets", "Community-made flashcard sets for almost every FBLA event.", "https://quizlet.com/subject/fbla/"),
            ResourceLink("Khan Academy Business", "Free courses on accounting, finance, economics, and entrepreneurship.", "https://www.khanacademy.org/economics-finance-domain"),
            ResourceLink("Investopedia", "Plain-English definitions and guides for business and finance terms.", "https://www.investopedia.com/"),
            ResourceLink("Robert's Rules Online", "Full text of parliamentary procedure rules used in FBLA competitions.", "https://www.rulesonline.com/"),
            // ── Career & Professional Development ─────────────────────────────
            ResourceLink("LinkedIn", "Build your professional profile and connect with FBLA alumni.", "https://www.linkedin.com/"),
            ResourceLink("Indeed Career Guide", "Resume, cover letter, and interview tips from Indeed's career experts.", "https://www.indeed.com/career-advice"),
            ResourceLink("SBA — Small Business Guide", "U.S. Small Business Administration resources for entrepreneurs.", "https://www.sba.gov/business-guide"),
            ResourceLink("Harvard Business Review", "Articles on leadership, strategy, and management for business students.", "https://hbr.org/"),
            ResourceLink("Forbes — Business News", "Breaking business news, entrepreneur profiles, and market trends.", "https://www.forbes.com/")
        )
    }

    val practicePdfs = remember {
        listOf(
            ResourceLink("FBLA Knowledgemaster", "Official FBLA-endorsed test prep platform with practice questions for every event.", "https://www.fblaknowledgemaster.com/"),
            ResourceLink("Business Law Practice Test (Quizlet)", "Community flashcard and practice test set covering contracts, torts, UCC, and more.", "https://quizlet.com/subject/fbla-business-law/"),
            ResourceLink("Economics Practice Test (Quizlet)", "Micro and macro economics practice questions aligned to FBLA topic outlines.", "https://quizlet.com/subject/fbla-economics/"),
            ResourceLink("Accounting I Practice Test (Quizlet)", "Debits, credits, financial statements, GAAP, and adjusting entries practice.", "https://quizlet.com/subject/fbla-accounting/"),
            ResourceLink("Personal Finance Practice Test (Quizlet)", "Credit scores, investing, insurance, budgeting, and retirement accounts.", "https://quizlet.com/subject/fbla-personal-finance/"),
            ResourceLink("Marketing Practice Test (Quizlet)", "4 Ps, target markets, brand equity, SEO, and market segmentation practice.", "https://quizlet.com/subject/fbla-marketing/"),
            ResourceLink("Business Calculations Quizlet", "Markup, commission, interest, break-even, and payroll calculations.", "https://quizlet.com/subject/fbla-business-calculations/"),
            ResourceLink("Cybersecurity Practice Test (Quizlet)", "Phishing, firewalls, CIA triad, encryption, and network security concepts.", "https://quizlet.com/subject/fbla-cybersecurity/"),
            ResourceLink("Parliamentary Procedure Study Guide", "Robert's Rules of Order guide with practice scenarios for FBLA competitions.", "https://www.fbla.org/media/2021/09/Parliamentary-Procedure-Guide.pdf"),
            ResourceLink("All FBLA Topic Outlines (Official)", "Official FBLA topic outlines for every competitive event — the definitive study source.", "https://www.fbla.org/high-school/competitive-events/"),
            ResourceLink("FBLA Past Tests (Archive.org)", "Archived collection of past FBLA practice tests and study materials.", "https://archive.org/search?query=fbla+practice+test"),
            ResourceLink("Networking Concepts Quizlet", "OSI model, TCP/IP, DNS, subnetting, firewalls, and network security.", "https://quizlet.com/subject/fbla-networking/"),
            ResourceLink("Introduction to Business Quizlet", "Business structures, HR, supply chain, ethics, and e-commerce practice.", "https://quizlet.com/subject/fbla-introduction-to-business/"),
            ResourceLink("Securities and Investments Quizlet", "Stocks, bonds, mutual funds, options, and market indicators practice.", "https://quizlet.com/subject/fbla-securities/")
        )
    }

    val resourcePdfs = remember {
        listOf(
            // ── Chapter Management ────────────────────────────────────────────
            ResourceLink("HS 101 Toolkit", "Everything a new chapter needs — setup, officers, meetings, and more.", "https://www.fbla.org/media/2023/05/FBLA-High-School-101-Toolkit.pdf"),
            ResourceLink("Chapter Officer Handbook", "Roles, responsibilities, and best practices for every chapter officer position.", "https://www.fbla.org/media/2022/08/Chapter-Officer-Handbook.pdf"),
            ResourceLink("Membership & Dues Info", "How to join FBLA, dues structure, and membership benefits explained.", "https://www.fbla.org/high-school/membership/"),
            ResourceLink("Community Service Guide", "March of Dimes partnership guide and community service hour requirements.", "https://www.fbla.org/march-of-dimes/"),
            // ── Competition Prep ──────────────────────────────────────────────
            ResourceLink("Competitive Event Topic Outlines", "PDF topic outlines and study checklists for every competitive event.", "https://www.fbla.org/high-school/competitive-events/"),
            ResourceLink("Parliamentary Procedure Guide", "Full guide to Robert's Rules of Order as used in FBLA competitions.", "https://www.fbla.org/media/2021/09/Parliamentary-Procedure-Guide.pdf"),
            ResourceLink("Business Plan Guidelines", "Official rubric and format requirements for FBLA business plan events.", "https://www.fbla.org/media/2023/05/Business-Plan-Guidelines.pdf"),
            ResourceLink("Public Speaking Tips", "FBLA-specific tips for prepared and impromptu speaking competitions.", "https://www.fbla.org/media/2022/10/Public-Speaking-Tips.pdf"),
            // ── Conference Prep ───────────────────────────────────────────────
            ResourceLink("NLC Delegate Handbook", "Rules, schedule overview, and tips for NLC delegates.", "https://www.fbla.org/media/2023/06/NLC-Delegate-Handbook.pdf"),
            ResourceLink("Dress Code & Appearance Guide", "Detailed visual guide to FBLA business professional dress standards.", "https://www.fbla.org/media/2022/03/Dress-Code-Guide.pdf"),
            // ── Brand & Identity ──────────────────────────────────────────────
            ResourceLink("FBLA Brand Standards Guide", "Full brand guidelines — logo usage, official colors, and typography.", "https://www.fbla.org/brand-center/"),
            // ── Career & Finance ──────────────────────────────────────────────
            ResourceLink("Resume Template (FBLA Style)", "Clean, competition-ready resume template approved for FBLA events.", "https://www.fbla.org/media/2023/04/Resume-Template.pdf"),
            ResourceLink("Financial Literacy Workbook", "Personal finance basics — budgeting, saving, investing, and credit.", "https://www.fbla.org/media/2022/11/Financial-Literacy-Workbook.pdf")
        )
    }

    data class SocialMedia(val name: String, val handle: String, val url: String)
    val socialMediaList = remember {
        listOf(
            SocialMedia("Instagram", "@fbla_national", "https://www.instagram.com/fbla_national/"),
            SocialMedia("X (Twitter)", "@FBLA_National", "https://twitter.com/FBLA_National"),
            SocialMedia("Facebook", "FutureBusinessLeaders", "https://www.facebook.com/FutureBusinessLeaders/"),
            SocialMedia("LinkedIn", "FBLA", "https://www.linkedin.com/company/future-business-leaders-america")
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "RESOURCES", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = primaryColor, fontSize = (11 * textSizeMultiplier).sp))
                Text(text = "FBLA Hub", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (MaterialTheme.typography.displaySmall.fontSize.value * textSizeMultiplier).sp))
                Spacer(modifier = Modifier.height(20.dp))

                val grayCard = cardBg

                if (activeResourceCategory.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Study Hub
                        Surface(onClick = { activeHub = "selector" }, modifier = Modifier.fillMaxWidth().pressScale(), shape = RoundedCornerShape(16.dp), color = grayCard) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.School, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Study Hub", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                    Text("Practice tests, Q&A, and more", color = textColor.copy(alpha = 0.55f), fontSize = (11 * textSizeMultiplier).sp)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                        // Outfit Checker
                        Surface(onClick = { activeHub = "dress_code" }, modifier = Modifier.fillMaxWidth().pressScale(), shape = RoundedCornerShape(16.dp), color = grayCard) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Checkroom, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Outfit Checker", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                    Text("AI dress code analysis", color = textColor.copy(alpha = 0.55f), fontSize = (11 * textSizeMultiplier).sp)
                                }
                                InfoIconButton("Take or upload a photo of your outfit and the AI will check if it meets FBLA's business professional dress code.\n\nIt gives specific feedback on what to fix before a conference.", primaryColor, lightMode)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Links + PDFs — side by side gray cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.weight(1f).height(130.dp).pressScale(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = grayCard),
                            onClick = { activeResourceCategory = "links"; resourceSearch = "" }
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Language, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Links", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                    Text("${resourceLinks.size} websites", color = textColor.copy(alpha = 0.5f), fontSize = (11 * textSizeMultiplier).sp)
                                }
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f).height(130.dp).pressScale(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = grayCard),
                            onClick = { activeResourceCategory = "pdfs"; resourceSearch = "" }
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("PDFs", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                    Text("${resourcePdfs.size} documents", color = textColor.copy(alpha = 0.5f), fontSize = (11 * textSizeMultiplier).sp)
                                }
                            }
                        }
                    }
                } else {
                    val allItems = when (activeResourceCategory) {
                        "links" -> resourceLinks
                        "pdfs" -> resourcePdfs
                        else -> practicePdfs
                    }
                    val categoryTitle = when (activeResourceCategory) {
                        "links" -> "Links"
                        "pdfs" -> "PDFs"
                        else -> "Practice Tests"
                    }
                    val categoryIcon = when (activeResourceCategory) {
                        "links" -> Icons.Default.Language
                        "pdfs" -> Icons.AutoMirrored.Filled.LibraryBooks
                        else -> Icons.Default.Quiz
                    }
                    val filteredItems = if (resourceSearch.isBlank()) allItems else allItems.filter {
                        it.title.contains(resourceSearch, ignoreCase = true) || it.description.contains(resourceSearch, ignoreCase = true)
                    }

                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        if (activeHub == "from_selector") { activeHub = "selector"; activeResourceCategory = ""; resourceSearch = "" }
                        else { activeResourceCategory = ""; resourceSearch = "" }
                    }.padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColorSecondary, modifier = Modifier.size(18.dp))
                        Text("Back", color = textColorSecondary, fontSize = (13 * textSizeMultiplier).sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                        Icon(categoryIcon, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        Text(categoryTitle, color = textColor, fontWeight = FontWeight.Bold, fontSize = (18 * textSizeMultiplier).sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${allItems.size} total", color = textColorSecondary, fontSize = (11 * textSizeMultiplier).sp)
                    }
                    // Search bar
                    OutlinedTextField(
                        value = resourceSearch,
                        onValueChange = { resourceSearch = it },
                        placeholder = { Text("Search...", color = textColor.copy(alpha = 0.4f), fontSize = (13 * textSizeMultiplier).sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (resourceSearch.isNotEmpty()) {
                                IconButton(onClick = { resourceSearch = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedContainerColor = tileBg, unfocusedContainerColor = tileBg,
                            cursorColor = primaryColor
                        )
                    )
                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No results for \"$resourceSearch\"", color = textColorSecondary, fontSize = (13 * textSizeMultiplier).sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredItems.forEach { item ->
                                Surface(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                                    modifier = Modifier.fillMaxWidth().pressScale(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(item.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                            Text(item.description, color = textColor.copy(alpha = 0.55f), fontSize = (11 * textSizeMultiplier).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(Icons.Default.OpenInNew, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

// Social Media Section — redesigned
            if (activeResourceCategory.isEmpty()) item {
                // Header text
                Text(
                    "STAY CONNECTED",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp),
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Want to stay connected?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (22 * textSizeMultiplier).sp),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Follow FBLA national for competition updates, NLC news, and community highlights.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp),
                    color = textColorSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Competition graphic card
                Card(
                    modifier = Modifier.fillMaxWidth().height(185.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background gradient
                        Box(modifier = Modifier.fillMaxSize().background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF0A1628), primaryColor.copy(alpha = 0.3f), Color(0xFF0D1B3E))
                            )
                        ))
                        // Decorative circles
                        Box(modifier = Modifier.size(150.dp).align(Alignment.TopStart).background(primaryColor.copy(alpha = 0.06f), CircleShape))
                        Box(modifier = Modifier.size(110.dp).align(Alignment.BottomEnd).background(primaryColor.copy(alpha = 0.07f), CircleShape))

                        // Content
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top badge
                            Surface(shape = RoundedCornerShape(8.dp), color = primaryColor.copy(alpha = 0.18f)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(13.dp))
                                    Text("FBLA NATIONAL", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (9 * textSizeMultiplier).sp, letterSpacing = 2.sp))
                                }
                            }

                            // Center icon cluster
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Groups, null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(38.dp))
                                Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(54.dp))
                                Icon(Icons.Default.Groups, null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(38.dp))
                            }

                            // Bottom text
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("National Leadership Conference", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp))
                                Text("Future Business Leaders of America", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // "Follow Us" label
                Text(
                    "FOLLOW US",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp),
                    color = textColorTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 2-column social media grid
                socialMediaList.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { social ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(14.dp),
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(social.url))) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SocialMediaLogo(platform = social.name, modifier = Modifier.size(24.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(social.name, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(social.handle, color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.OpenInNew, null, tint = primaryColor.copy(alpha = 0.55f), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

// National Instagram Feed
            if (activeResourceCategory.isEmpty()) item {
                Text(
                    "NATIONAL INSTAGRAM",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp),
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    when {
                        natIgLoading -> {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(32.dp))
                            }
                        }
                        natIgError.isNotEmpty() -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                                    Text(natIgError, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                        natIgProfile != null -> {
                            val profile = natIgProfile!!
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    AsyncImage(
                                        model = profile.profilePicUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("@${profile.username}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp), color = textColor)
                                        if (profile.fullName.isNotEmpty()) Text(profile.fullName, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary)
                                    }
                                    IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/${profile.username}/"))) }) {
                                        Icon(Icons.Default.OpenInNew, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(profile.postCount.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                                        Text("Posts", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val followers = profile.followerCount
                                        val followersStr = if (followers >= 1000) "${followers / 1000}K" else followers.toString()
                                        Text(followersStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                                        Text("Followers", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                    }
                                }
                                if (profile.bio.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(profile.bio, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                if (profile.posts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    profile.posts.chunked(3).forEach { row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            row.forEach { post ->
                                                Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).clickable { natIgSelectedPost = post }) {
                                                    AsyncImage(model = post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                    if (post.isVideo) {
                                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                                        }
                                                    }
                                                }
                                            }
                                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

// Chapter Instagram Feed
            if (activeResourceCategory.isEmpty() && chapterInstagramHandle.isNotBlank()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "CHAPTER INSTAGRAM",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp),
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        when {
                            igLoading -> {
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(32.dp))
                                }
                            }
                            igError.isNotEmpty() -> {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.ErrorOutline, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                                        Text(igError, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            igProfile != null -> {
                                val profile = igProfile!!
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Profile header
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        AsyncImage(
                                            model = profile.profilePicUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("@${profile.username}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp), color = textColor)
                                            if (profile.fullName.isNotEmpty()) Text(profile.fullName, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                        // Open in Instagram button
                                        IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/${profile.username}/"))) }) {
                                            Icon(Icons.Default.OpenInNew, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    // Stats row
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(profile.postCount.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                                            Text("Posts", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val followers = profile.followerCount
                                            val followersStr = if (followers >= 1000) "${followers / 1000}K" else followers.toString()
                                            Text(followersStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                                            Text("Followers", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                    }
                                    if (profile.bio.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(profile.bio, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                    // Post grid 3-column
                                    if (profile.posts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        val rows = profile.posts.chunked(3)
                                        rows.forEach { row ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                row.forEach { post ->
                                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).clickable { igSelectedPost = post }) {
                                                        AsyncImage(model = post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                        if (post.isVideo) {
                                                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                                // fill remaining cols if row < 3
                                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Post detail dialog
        igSelectedPost?.let { post ->
            Dialog(onDismissRequest = { igSelectedPost = null }) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A2540))) {
                    Column {
                        AsyncImage(model = post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFE1306C), modifier = Modifier.size(16.dp))
                                    Text(post.likeCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ChatBubbleOutline, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                    Text(post.commentCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                                }
                            }
                            if (post.caption.isNotEmpty()) {
                                Text(post.caption, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.postUrl))); igSelectedPost = null }, modifier = Modifier.fillMaxWidth()) {
                                Text("View on Instagram", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // National post detail dialog
        natIgSelectedPost?.let { post ->
            Dialog(onDismissRequest = { natIgSelectedPost = null }) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A2540))) {
                    Column {
                        AsyncImage(model = post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFE1306C), modifier = Modifier.size(16.dp))
                                    Text(post.likeCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ChatBubbleOutline, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                    Text(post.commentCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                                }
                            }
                            if (post.caption.isNotEmpty()) {
                                Text(post.caption, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.postUrl))); natIgSelectedPost = null }, modifier = Modifier.fillMaxWidth()) {
                                Text("View on Instagram", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SocialMediaLogo(platform: String, modifier: Modifier = Modifier) {
    when (platform) {
        "Instagram" -> {
            Image(
                painter = painterResource(id = R.drawable.instagram_logo),
                contentDescription = "Instagram",
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .clip(CircleShape)
            )
        }
        "X (Twitter)" -> {
            Box(
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color(0xFF000000), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "𝕏",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        "Facebook" -> {
            Box(
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color(0xFF1877F2), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "f",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }
        "LinkedIn" -> {
            Box(
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color(0xFF0A66C2), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "in",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun getStateFBLASocialMedia(state: String): Map<String, String> {
    return when (state.trim().lowercase()) {
        "wisconsin" -> mapOf(
            "Instagram" to "https://www.instagram.com/wifbla/",
            "Facebook" to "https://www.facebook.com/wifbla/",
            "X (Twitter)" to "https://twitter.com/wifbla"
        )
        "california" -> mapOf(
            "Instagram" to "https://www.instagram.com/cafbla/",
            "Facebook" to "https://www.facebook.com/cafbla/",
            "X (Twitter)" to "https://twitter.com/cafbla"
        )
        "texas" -> mapOf(
            "Instagram" to "https://www.instagram.com/texasfbla/",
            "Facebook" to "https://www.facebook.com/txfbla/",
            "X (Twitter)" to "https://twitter.com/txfbla"
        )
        "florida" -> mapOf(
            "Instagram" to "https://www.instagram.com/floridafbla/",
            "Facebook" to "https://www.facebook.com/floridafbla/",
            "X (Twitter)" to "https://twitter.com/floridafbla"
        )
        "new york" -> mapOf(
            "Instagram" to "https://www.instagram.com/nyfbla/",
            "Facebook" to "https://www.facebook.com/nyfbla/",
            "X (Twitter)" to "https://twitter.com/nyfbla"
        )
        "pennsylvania" -> mapOf(
            "Instagram" to "https://www.instagram.com/pafbla/",
            "Facebook" to "https://www.facebook.com/pafbla/",
            "X (Twitter)" to "https://twitter.com/pafbla"
        )
        "illinois" -> mapOf(
            "Instagram" to "https://www.instagram.com/ilfbla/",
            "Facebook" to "https://www.facebook.com/ilfbla/",
            "X (Twitter)" to "https://twitter.com/ilfbla"
        )
        "ohio" -> mapOf(
            "Instagram" to "https://www.instagram.com/ohiofbla/",
            "Facebook" to "https://www.facebook.com/ohiofbla/",
            "X (Twitter)" to "https://twitter.com/ohiofbla"
        )
        "georgia" -> mapOf(
            "Instagram" to "https://www.instagram.com/gafbla/",
            "Facebook" to "https://www.facebook.com/gafbla/",
            "X (Twitter)" to "https://twitter.com/gafbla"
        )
        "north carolina" -> mapOf(
            "Instagram" to "https://www.instagram.com/ncfbla/",
            "Facebook" to "https://www.facebook.com/ncfbla/",
            "X (Twitter)" to "https://twitter.com/ncfbla"
        )
        "michigan" -> mapOf(
            "Instagram" to "https://www.instagram.com/mifbla/",
            "Facebook" to "https://www.facebook.com/mifbla/",
            "X (Twitter)" to "https://twitter.com/mifbla"
        )
        "virginia" -> mapOf(
            "Instagram" to "https://www.instagram.com/vafbla/",
            "Facebook" to "https://www.facebook.com/vafbla/",
            "X (Twitter)" to "https://twitter.com/vafbla"
        )
        else -> emptyMap() // No state-specific links for other states
    }
}

// Helper function to get Sun Prairie FBLA chapter social media links

fun getSunPrairieChapterSocialMedia(): Map<String, String> {
    return mapOf(
        "Instagram" to "https://www.instagram.com/spw_fbla/",
        "X (Twitter)" to "https://twitter.com/sunprairieFBLA",
        "Facebook" to "https://www.facebook.com/SPWHSFBLA"
    )
}

data class OutfitAnalysisResult(
    val acceptable: Boolean,
    val verdict: String,
    val explanation: String,
    val tips: List<String>
)

@Composable
fun DressCodeCheckerScreen(
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val context = LocalContext.current
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val cardColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.15f)

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBase64 by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<OutfitAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Shared handling for both gallery pick and camera capture: decode, downscale, base64.
    fun processOutfitImage(uri: Uri) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream) ?: throw Exception("Unable to decode image")
            val maxDim = 1024
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
            val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val resized = Bitmap.createScaledBitmap(bitmap, w, h, true)
            selectedBitmap = resized
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
            selectedBase64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            result = null
            errorMessage = ""
        } catch (e: Exception) {
            errorMessage = "Failed to load image: ${e.message}"
        }
    }

    var showOutfitPhotoOptions by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processOutfitImage(it) }
    }
    // Take a photo on the spot with the camera.
    var outfitCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    val outfitCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) outfitCameraUri?.let { processOutfitImage(it) }
    }
    fun openOutfitCamera() {
        try {
            val photoFile = File.createTempFile("outfit_", ".jpg", context.cacheDir)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            outfitCameraUri = uri
            outfitCameraLauncher.launch(uri)
        } catch (e: Exception) {
            errorMessage = "Camera not available: ${e.message}"
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openOutfitCamera() else showCameraPermissionDialog = true
    }
    fun launchOutfitCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openOutfitCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack, tint = primaryColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "DRESS CODE CHECKER",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp),
                            color = primaryColor
                        )
                        Text(
                            "AI-Powered FBLA Standard Analysis",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (18 * textSizeMultiplier).sp),
                            color = textColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Upload a full-body photo of your outfit and AI will check if it meets FBLA business professional standards.",
                    color = textColorSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    onClick = { showOutfitPhotoOptions = true }
                ) {
                    if (selectedBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected outfit",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = primaryColor, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Tap to take or select a photo", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textSizeMultiplier).sp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Full-body outfit photo works best", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                            }
                        }
                    }
                }

                if (showOutfitPhotoOptions) {
                    Dialog(onDismissRequest = { showOutfitPhotoOptions = false }) {
                        Surface(shape = RoundedCornerShape(16.dp), color = if (lightMode) Color.White else Color(0xFF1A2744)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                TextButton(
                                    onClick = { showOutfitPhotoOptions = false; launchOutfitCamera() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Take Photo", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Medium)
                                }
                                TextButton(
                                    onClick = { showOutfitPhotoOptions = false; imagePicker.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Image, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Choose from Gallery", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                if (showCameraPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showCameraPermissionDialog = false },
                        containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                        icon = { Icon(Icons.Default.CameraAlt, null, tint = primaryColor) },
                        title = { Text("Camera access needed", color = textColor, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "To take a photo, FBLA Hub needs permission to use your camera. You can turn it on in Settings.",
                                color = textColorSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showCameraPermissionDialog = false
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    errorMessage = "Couldn't open settings: ${e.message}"
                                }
                            }) {
                                Text("OPEN SETTINGS", color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCameraPermissionDialog = false }) {
                                Text("CANCEL", color = textColorSecondary)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedBitmap != null && result == null) {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(44.dp).pressScale(),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Photo", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (13 * textSizeMultiplier).sp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isAnalyzing = true
                            result = null
                            errorMessage = ""
                            val data = hashMapOf("imageBase64" to selectedBase64, "mimeType" to "image/jpeg")
                            FirebaseFunctions.getInstance()
                                .getHttpsCallable("checkOutfitDressCode")
                                .call(data)
                                .addOnSuccessListener { taskResult ->
                                    isAnalyzing = false
                                    @Suppress("UNCHECKED_CAST")
                                    val map = taskResult.getData() as? Map<String, Any>
                                    if (map != null) {
                                        result = OutfitAnalysisResult(
                                            acceptable = map["acceptable"] as? Boolean ?: false,
                                            verdict = map["verdict"] as? String ?: "",
                                            explanation = map["explanation"] as? String ?: "",
                                            tips = (map["tips"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                        )
                                    } else {
                                        errorMessage = "Unexpected response format. Please try again."
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isAnalyzing = false
                                    errorMessage = "Analysis failed: ${e.message}"
                                }
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)).pressScale(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing...", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ANALYZE OUTFIT", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                        }
                    }
                } else if (selectedBitmap == null) {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)).pressScale(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("SELECT PHOTO", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = Color(0xFFE74C3C), style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = result != null,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn()
                ) {
                    result?.let { res ->
                        Column {
                        val isOk = res.acceptable
                        val accentColor = if (isOk) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                        val resultCardBg = if (lightMode) {
                            if (isOk) Color(0xFFE8F8EF) else Color(0xFFFDECEC)
                        } else {
                            if (isOk) Color(0xFF0D2B1A) else Color(0xFF2B0D0D)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = resultCardBg)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        null,
                                        tint = accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            if (isOk) "DRESS CODE COMPLIANT" else "DOES NOT MEET STANDARDS",
                                            color = accentColor,
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = (11 * textSizeMultiplier).sp)
                                        )
                                        Text(res.verdict, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(res.explanation, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                if (res.tips.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        if (isOk) "WHAT YOU DID WELL" else "HOW TO IMPROVE",
                                        color = accentColor,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = (10 * textSizeMultiplier).sp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    res.tips.forEach { tip ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                            Text("• ", color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                            Text(tip, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { selectedBitmap = null; selectedBase64 = ""; result = null; errorMessage = "" },
                            modifier = Modifier.fillMaxWidth().height(44.dp).pressScale(),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                        ) {
                            Text("Try Another Photo", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (13 * textSizeMultiplier).sp))
                        }
                        } // close Column
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = primaryColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("FBLA Dress Code Basics", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                "Business suit or blazer with dress pants/skirt",
                                "Collared dress shirt with tie (men)",
                                "Closed-toe dress shoes",
                                "Skirts/dresses at or below the knee",
                                "No jeans, sneakers, or casual wear"
                            ).forEach { rule ->
                                Text("• $rule", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(vertical = 1.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
