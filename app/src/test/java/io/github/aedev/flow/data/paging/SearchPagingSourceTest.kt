package io.github.aedev.flow.data.paging

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchPagingSourceTest {

    private fun extractVideoIdReflect(url: String): String {
        val method = SearchPagingSource::class.java.getDeclaredMethod("extractVideoId", String::class.java)
        method.isAccessible = true
        val instance = SearchPagingSource(query = "test")
        return method.invoke(instance, url) as String
    }

    private fun extractVideoIdUnoptimized(url: String): String {
        val patterns =
            listOf(
                "v=([A-Za-z0-9_-]{11})".toRegex(),
                "youtu\\.be/([A-Za-z0-9_-]{11})".toRegex(),
                "shorts/([A-Za-z0-9_-]{11})".toRegex(),
            )
        for (pat in patterns) {
            val m = pat.find(url) ?: continue
            return m.groupValues[1]
        }
        return url.substringAfterLast("/").substringBefore("?").take(11)
    }

    @Test
    fun testExtractVideoIdFormats() {
        val standardUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", extractVideoIdReflect(standardUrl))

        val shortUrl = "https://youtu.be/dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", extractVideoIdReflect(shortUrl))

        val shortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", extractVideoIdReflect(shortsUrl))

        val fallbackUrl = "https://www.youtube.com/embed/dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", extractVideoIdReflect(fallbackUrl))
    }

    @Test
    fun benchmarkExtractVideoIdComparison() {
        val testUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ"
        )

        // Warmup
        repeat(1_000) {
            for (url in testUrls) {
                extractVideoIdUnoptimized(url)
                extractVideoIdReflect(url)
            }
        }

        val iterations = 10_000

        val startUnoptimized = System.nanoTime()
        repeat(iterations) {
            for (url in testUrls) {
                extractVideoIdUnoptimized(url)
            }
        }
        val elapsedUnoptimizedMs = (System.nanoTime() - startUnoptimized) / 1_000_000.0

        val startOptimized = System.nanoTime()
        repeat(iterations) {
            for (url in testUrls) {
                extractVideoIdReflect(url)
            }
        }
        val elapsedOptimizedMs = (System.nanoTime() - startOptimized) / 1_000_000.0

        println("Unoptimized (compiling regex each time): ${elapsedUnoptimizedMs} ms")
        println("Optimized (reusing static Regex constants): ${elapsedOptimizedMs} ms")
    }
}
