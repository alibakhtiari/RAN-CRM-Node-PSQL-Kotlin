package com.ran.crm.utils

object T9Utils {
    private val t9Map = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz"
    )

    /**
     * Converts text to T9 digit sequence
     * Example: "hello" -> "43556"
     */
    fun textToT9(text: String): String {
        return text.lowercase().mapNotNull { char ->
            t9Map.entries.find { it.value.contains(char) }?.key
        }.joinToString("")
    }

    /**
     * Checks if a name matches a T9 query
     * Example: "john" matches "5646"
     */
    fun matchesT9(text: String, query: String): Boolean {
        if (query.isEmpty()) return true
        val t9Text = textToT9(text)
        return t9Text.contains(query)
    }

    /**
     * Fuzzy match using Levenshtein distance
     * Returns true if the distance is within threshold
     */
    fun fuzzyMatch(text: String, query: String, threshold: Int = 2): Boolean {
        if (query.isEmpty()) return true
        val distance = levenshteinDistance(text.lowercase(), query.lowercase())
        return distance <= threshold
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) {
            dp[i][0] = i
        }
        for (j in 0..len2) {
            dp[0][j] = j
        }

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Advanced search combining T9 and fuzzy matching
     */
    fun advancedSearch(text: String, query: String): Boolean {
        // Check if query is all digits (T9 search)
        if (query.all { it.isDigit() }) {
            return matchesT9(text, query)
        }
        
        // Otherwise use fuzzy matching
        return text.contains(query, ignoreCase = true) || fuzzyMatch(text, query)
    }
}
