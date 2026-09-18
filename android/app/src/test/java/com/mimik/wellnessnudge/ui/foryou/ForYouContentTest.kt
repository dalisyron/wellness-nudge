package com.mimik.wellnessnudge.ui.foryou

import com.mimik.wellnessnudge.api.TipCard
import com.mimik.wellnessnudge.api.TipNudge
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Test

class ForYouContentTest {

    @Test
    fun placeholdersShowUntilTheFirstLoadFails() {
        assertEquals(ForYouContent.Loading, forYouContent(null, failed = false))
        assertEquals(ForYouContent.Failed, forYouContent(null, failed = true))
    }

    @Test
    fun loadedTipsStayOnScreenWhenAReloadFails() {
        val content = forYouContent(PreviewData.tips, failed = true) as ForYouContent.Loaded

        assertEquals(PreviewData.tips.items!!.size, content.areas.size)
    }

    @Test
    fun areasKeepTheMimsOrderAndSkipCardsWithNothingHelpful() {
        val nothingHelpful = TipCard("improve-mood", "lifting your mood", "Intro", 2, PreviewData.now, emptyList())
        val tips = PreviewData.tips.copy(items = PreviewData.tips.items!! + nothingHelpful)

        val content = forYouContent(tips, failed = false) as ForYouContent.Loaded

        assertEquals(listOf("improve-sleep", "reduce-fatigue", "reduce-stress"), content.areas.map { it.category })
        assertEquals(2, content.areas.first().helpful.size)
    }

    @Test
    fun missingFieldsFallBackQuietly() {
        val sparse = TipCard(
            category = "improve-sleep",
            categoryLabel = null,
            intro = " ",
            recentMentions = null,
            lastFocusedAt = null,
            helpfulNudges = listOf(TipNudge("nudge_1", 1L, "  Dim the lights by 9:30 PM.\n")),
        )

        val area = (forYouContent(PreviewData.tips.copy(items = listOf(sparse)), failed = false) as ForYouContent.Loaded)
            .areas.single()

        assertEquals(0, area.recentMentions)
        assertEquals("Dim the lights by 9:30 PM.", area.helpful.single().text)
    }

    @Test
    fun quotesGetTypographicApostrophes() {
        val card = TipCard("improve-sleep", null, null, 1, null, listOf(TipNudge("nudge_1", 1L, "Don't scroll in bed.")))

        val quote = (forYouContent(PreviewData.tips.copy(items = listOf(card)), failed = false) as ForYouContent.Loaded)
            .areas.single().helpful.single()

        assertEquals("Don\u2019t scroll in bed.", quote.text)
    }

    @Test
    fun noTipsIsAnEmptyList() {
        val content = forYouContent(PreviewData.tipsEmpty, failed = false) as ForYouContent.Loaded

        assertEquals(emptyList<FocusArea>(), content.areas)
    }
}
