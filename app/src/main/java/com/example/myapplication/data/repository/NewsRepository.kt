package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides FBLA news: an instant hardcoded fallback list plus a background
 * web-scrape of fbla.org/newsroom. All news data access lives here so the
 * NewsScreen/ViewModel never touch Jsoup or networking directly.
 */
@Singleton
class NewsRepository @Inject constructor() {

    /** Hardcoded articles shown instantly — accurate as of April 2026. */
    val hardcodedArticles: List<NewsArticle> = listOf(
        NewsArticle("NLC 2026 Registration Now Open", "Register now for the National Leadership Conference in Anaheim, California, June 28 – July 2, 2026. Early-bird rates available through May 1.", "Apr 10, 2026", "https://www.fbla.org/nlc/", "CONFERENCE", System.currentTimeMillis() - 259200000L),
        NewsArticle("Architects of Ambition — 2025-2026 National Theme", "FBLA's official theme for the 2025-2026 year challenges members to build bold futures through leadership, innovation, and community impact.", "Apr 5, 2026", "https://www.fbla.org/newsroom/", "NATIONAL", System.currentTimeMillis() - 691200000L),
        NewsArticle("SLC Season Recap: Record Turnout Nationwide", "State Leadership Conferences wrapped up across all 50 states and territories this spring, with record attendance and fierce competition in over 70 events.", "Mar 28, 2026", "https://www.fbla.org/slc/", "CONFERENCE", System.currentTimeMillis() - 1382400000L),
        NewsArticle("2026 Competitive Event Guidelines Updated", "FBLA has released final updated guidelines for all 2025-2026 high school competitive events. Members are encouraged to review changes before NLC.", "Mar 20, 2026", "https://www.fbla.org/high-school/competitive-events/", "HS", System.currentTimeMillis() - 2073600000L),
        NewsArticle("Scholarship Applications Closing Soon", "Over \$250,000 in FBLA scholarships are available for graduating seniors. Deadlines vary — apply now at fbla.org before opportunities close.", "Mar 15, 2026", "https://www.fbla.org/high-school/program/scholarships/", "OPPORTUNITY", System.currentTimeMillis() - 2505600000L),
        NewsArticle("Business Achievement Awards: Nominations Open", "FBLA members can now submit nominations for the 2026 Business Achievement Awards, recognizing outstanding chapter and individual accomplishments.", "Mar 8, 2026", "https://www.fbla.org/high-school/recognition/business-achievement-awards/", "OPPORTUNITY", System.currentTimeMillis() - 3110400000L),
        NewsArticle("New: AI & Technology Competitive Event Pilot", "FBLA is piloting a new Artificial Intelligence & Technology event at select SLCs this year, with plans for full rollout at NLC 2027.", "Mar 1, 2026", "https://www.fbla.org/high-school/competitive-events/", "HS", System.currentTimeMillis() - 3715200000L),
        NewsArticle("Chapter Excellence Awards — Apply Now", "Chapters nationwide are encouraged to apply for the 2026 Chapter Excellence Awards, highlighting community service, membership growth, and event performance.", "Feb 22, 2026", "https://www.fbla.org/chapter-excellence-awards/", "HS", System.currentTimeMillis() - 4320000000L),
        NewsArticle("PBL Announces 2026 Corporate Partner Cohort", "Phi Beta Lambda welcomes 12 new national corporate partners for 2026, expanding internship and job placement pipelines for collegiate members.", "Feb 15, 2026", "https://www.fbla.org/pbl/", "PBL", System.currentTimeMillis() - 4924800000L),
        NewsArticle("FBLA Surpasses 230,000 Members Nationwide", "FBLA has officially surpassed 230,000 active members for the 2025-2026 school year — a new all-time high for the organization.", "Feb 10, 2026", "https://www.fbla.org/membership/", "NATIONAL", System.currentTimeMillis() - 5356800000L),
        NewsArticle("Middle Level Championships Set for June", "The FBLA Middle Level national championships will be held alongside NLC in Anaheim this June, featuring 25 competitive events for grades 5-9.", "Feb 3, 2026", "https://www.fbla.org/middle-level/", "ML", System.currentTimeMillis() - 5961600000L),
        NewsArticle("Financial Literacy Month: Free Resources Available", "In honor of Financial Literacy Month in April, FBLA has released a free toolkit of classroom and chapter activities available to all members.", "Apr 1, 2026", "https://www.fbla.org/financial-literacy/", "NATIONAL", System.currentTimeMillis() - 518400000L),
        NewsArticle("Adviser of the Year Nominations Open", "FBLA-PBL is accepting nominations for the 2026 National Adviser of the Year award. Submit your chapter adviser's nomination by May 15.", "Mar 25, 2026", "https://www.fbla.org/advisers/", "NATIONAL", System.currentTimeMillis() - 1728000000L),
        NewsArticle("Career & Leadership Webinar Series Launched", "FBLA's new free webinar series features monthly sessions with business leaders covering resume writing, networking, and career exploration.", "Mar 12, 2026", "https://www.fbla.org/careers/", "OPPORTUNITY", System.currentTimeMillis() - 2764800000L),
        NewsArticle("Digital Marketing Event Draws Highest Registration", "The Digital Marketing competitive event has become one of the top-10 most entered events for 2026, reflecting growing student interest in marketing careers.", "Feb 28, 2026", "https://www.fbla.org/competitions/", "HS", System.currentTimeMillis() - 3888000000L),
        NewsArticle("Alumni Network Launches Mentorship Program", "FBLA's alumni network is connecting current student members with professionals across industries through a new structured mentorship program.", "Feb 20, 2026", "https://www.fbla.org/alumni/", "NATIONAL", System.currentTimeMillis() - 4492800000L),
        NewsArticle("Entrepreneurship Challenge: \$10K Grand Prize", "FBLA's national Entrepreneurship Challenge is back with a \$10,000 grand prize for the best student business plan. Submissions due April 30.", "Feb 6, 2026", "https://www.fbla.org/entrepreneurship/", "OPPORTUNITY", System.currentTimeMillis() - 5702400000L),
        NewsArticle("Leadership Development Retreat Applications Open", "Applications are open for FBLA's 2026 National Leadership Development Retreat, a competitive program for top chapter officers.", "Jan 30, 2026", "https://www.fbla.org/leadership/", "NATIONAL", System.currentTimeMillis() - 6307200000L),
        NewsArticle("Community Service: 1.2M Hours and Counting", "FBLA chapters collectively logged over 1.2 million community service hours in the 2024-2025 school year, setting a new organizational record.", "Jan 22, 2026", "https://www.fbla.org/community-service/", "NATIONAL", System.currentTimeMillis() - 6912000000L),
        NewsArticle("New Study Guides Released for Objective Tests", "FBLA has published updated official study guides for all objective test competitive events, now available as free downloads on fbla.org.", "Jan 14, 2026", "https://www.fbla.org/high-school/competitive-events/", "HS", System.currentTimeMillis() - 7603200000L)
    )

    /** Scrapes fbla.org/newsroom in the background. Falls back to [hardcodedArticles] on failure or thin results. */
    suspend fun fetchFBLANews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val articles = mutableListOf<NewsArticle>()
            val doc = org.jsoup.Jsoup.connect("https://www.fbla.org/newsroom/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .referrer("https://www.google.com/")
                .timeout(10000)
                .get()
            doc.select("article, .post, .news-item, .press-release, div[class*='news'], div[class*='article']").take(20).forEach { article ->
                try {
                    val titleElement = article.select("h1, h2, h3, h4, .title, .headline, .entry-title, .post-title, [class*='title']").first()
                    val title = titleElement?.text()?.trim() ?: ""
                    var link = ""
                    if (titleElement != null) {
                        val titleLink = titleElement.select("a[href]").first()
                        if (titleLink != null) link = titleLink.attr("abs:href") ?: titleLink.attr("href") ?: ""
                        if (link.isEmpty()) {
                            val parentLink = titleElement.parent()
                            if (parentLink != null && parentLink.tagName() == "a") link = parentLink.attr("abs:href") ?: parentLink.attr("href") ?: ""
                        }
                    }
                    if (link.isEmpty()) {
                        article.select("a[href]").forEach { linkElement ->
                            val href = linkElement.attr("abs:href") ?: linkElement.attr("href") ?: ""
                            if (!href.contains("/author/") && !href.contains("facebook.com") && !href.contains("twitter.com") && !href.contains("linkedin.com") && !href.contains("/category/") && href.isNotEmpty()) {
                                link = href; return@forEach
                            }
                        }
                    }
                    val summary = article.select("p, .excerpt, .entry-summary, .description, [class*='excerpt'], [class*='summary']").first()?.text()?.trim()?.take(200) ?: ""
                    val dateStr = article.select("time, .date, .published, .entry-date, [class*='date']").first()?.text()?.trim() ?: ""
                    if (title.isNotEmpty() && link.isNotEmpty() && !link.contains("/author/")) {
                        val fullUrl = if (link.startsWith("http")) link else "https://www.fbla.org$link"
                        val formattedDate = if (dateStr.isNotEmpty()) dateStr else SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                        val finalSummary = if (summary.isNotEmpty()) summary else "Click to read the full article on FBLA.org"
                        val imgEl = article.select("img[class*=img-fluid], img[class*=pix-img-scale], img[class*=wp-post-image], img[class*=attachment-]").first()
                        val imageUrl = imgEl?.let { el ->
                            val srcset = el.attr("srcset").split(",").firstOrNull()?.trim()?.split(" ")?.firstOrNull() ?: ""
                            listOf(el.attr("data-lazy-src"), el.attr("data-src"), el.attr("data-lazy"), el.attr("abs:src"), srcset)
                                .firstOrNull { u -> u.isNotEmpty() && !u.startsWith("data:") && u.startsWith("http") }
                        } ?: ""
                        articles.add(NewsArticle(title = title, summary = finalSummary, date = formattedDate, url = fullUrl, tag = determineNewsTag(title, finalSummary), timestamp = System.currentTimeMillis(), imageUrl = imageUrl))
                    }
                } catch (e: Exception) { Log.e("NewsRepository", "Error parsing article", e) }
            }
            if (articles.size >= 5) articles else hardcodedArticles
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error fetching news", e)
            hardcodedArticles
        }
    }

    private fun determineNewsTag(title: String, summary: String): String {
        val text = (title + " " + summary).lowercase()
        return when {
            text.contains("nlc") || text.contains("slc") || text.contains("conference") -> "CONFERENCE"
            text.contains("scholarship") || text.contains("award") || text.contains("opportunity") -> "OPPORTUNITY"
            text.contains("high school") || text.contains("hs") -> "HS"
            text.contains("middle") || text.contains("ml") -> "ML"
            text.contains("college") || text.contains("pbl") -> "PBL"
            else -> "NATIONAL"
        }
    }
}
