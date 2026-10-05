package com.vidalvl.app

import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MissionType
import com.vidalvl.app.domain.suggestions.FallbackSuggestions
import com.vidalvl.app.domain.suggestions.MissionSuggester
import com.vidalvl.app.domain.suggestions.SuggestedMission
import com.vidalvl.app.domain.suggestions.SuggestionBatch
import com.vidalvl.app.domain.suggestions.SuggestionContext
import com.vidalvl.app.domain.suggestions.SuggestionOutcome
import com.vidalvl.app.domain.suggestions.SuggestionResult
import com.vidalvl.app.domain.suggestions.SuggestionService
import com.vidalvl.app.domain.suggestions.SuggestionSettings
import com.vidalvl.app.domain.suggestions.SuggestionStore
import com.vidalvl.app.domain.verification.FailureKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionServiceTest {
    // 2026-10-05 is a Monday (epoch day 20731); 20733 is the Wednesday of the same week.
    private val monday = 20731L

    private fun idea(title: String) = SuggestedMission(title, null, Attribute.MIND)

    private class FakeStore(val existing: List<String> = emptyList()) : SuggestionStore {
        val inserted = mutableListOf<Triple<MissionType, Long, List<SuggestedMission>>>()
        var expired: Pair<Long, Long>? = null
        override suspend fun context() = SuggestionContext(
            goals = setOf(Goal.LEARN), dailyMinutes = 15, attributes = emptyList(), streak = 0,
            existingTitles = existing, recentTitles = emptyList()
        )

        override suspend fun expire(today: Long, mondayOfWeek: Long) {
            expired = today to mondayOfWeek
        }

        override suspend fun insert(missions: List<SuggestedMission>, type: MissionType, generatedFor: Long) {
            inserted += Triple(type, generatedFor, missions)
        }
    }

    private class FakeSuggester(var result: SuggestionResult) : MissionSuggester {
        var calls = 0
        var lastCounts = 0 to 0
        override suspend fun suggest(context: SuggestionContext, dailyCount: Int, weeklyCount: Int, language: String): SuggestionResult {
            calls++
            lastCounts = dailyCount to weeklyCount
            return result
        }
    }

    private class FakeFallback : FallbackSuggestions {
        override fun pick(goals: Set<Goal>, type: MissionType, count: Int, seed: Long, exclude: Set<String>) =
            List(count) { SuggestedMission("Local ${type.name} ${it + 1}", null, Attribute.MIND) }
    }

    private class FakeSettings(override var aiEnabled: Boolean?) : SuggestionSettings {
        override var lastDailyDay = -1L
        override var lastWeeklyMonday = -1L
    }

    private fun service(
        store: FakeStore,
        suggester: FakeSuggester,
        settings: FakeSettings,
        day: Long = monday
    ) = SuggestionService(store, suggester, FakeFallback(), settings, { "Spanish" }, { day })

    private fun aiBatch() = SuggestionResult.Success(
        SuggestionBatch(
            daily = listOf(idea("Leer 10 páginas"), idea("Repasar apuntes"), idea("Aprender una palabra")),
            weekly = listOf(idea("Terminar un capítulo"), idea("Resumir lo aprendido"))
        )
    )

    @Test
    fun without_an_answer_from_the_user_nothing_is_generated() = runBlocking {
        val store = FakeStore()
        val suggester = FakeSuggester(aiBatch())
        val outcome = service(store, suggester, FakeSettings(aiEnabled = null)).ensureCurrent()

        assertEquals(SuggestionOutcome.NEEDS_CONSENT, outcome)
        assertTrue(store.inserted.isEmpty())
        assertEquals(0, suggester.calls)
    }

    @Test
    fun ai_ideas_fill_the_day_and_the_week() = runBlocking {
        val store = FakeStore()
        val settings = FakeSettings(aiEnabled = true)
        val outcome = service(store, FakeSuggester(aiBatch()), settings).ensureCurrent()

        assertEquals(SuggestionOutcome.GENERATED_WITH_AI, outcome)
        assertEquals(2, store.inserted.size)
        assertEquals(3, store.inserted.first { it.first == MissionType.DAILY }.third.size)
        assertEquals(2, store.inserted.first { it.first == MissionType.WEEKLY }.third.size)
        assertEquals(monday, settings.lastDailyDay)
        assertEquals(monday, settings.lastWeeklyMonday)
    }

    @Test
    fun it_generates_only_once_per_day_and_week() = runBlocking {
        val store = FakeStore()
        val suggester = FakeSuggester(aiBatch())
        val service = service(store, suggester, FakeSettings(aiEnabled = true))

        service.ensureCurrent()
        val second = service.ensureCurrent()

        assertEquals(SuggestionOutcome.NOTHING_TO_DO, second)
        assertEquals(1, suggester.calls)
    }

    @Test
    fun a_new_day_in_the_same_week_asks_only_for_daily_missions() = runBlocking {
        val store = FakeStore()
        val suggester = FakeSuggester(aiBatch())
        val settings = FakeSettings(aiEnabled = true)
        service(store, suggester, settings, day = monday).ensureCurrent()
        store.inserted.clear()

        service(store, suggester, settings, day = monday + 2).ensureCurrent()

        assertEquals(3 to 0, suggester.lastCounts)
        assertEquals(listOf(MissionType.DAILY), store.inserted.map { it.first })
    }

    @Test
    fun a_new_week_asks_for_both() = runBlocking {
        val store = FakeStore()
        val suggester = FakeSuggester(aiBatch())
        val settings = FakeSettings(aiEnabled = true)
        service(store, suggester, settings, day = monday).ensureCurrent()

        service(store, suggester, settings, day = monday + 7).ensureCurrent()

        assertEquals(3 to 2, suggester.lastCounts)
    }

    @Test
    fun when_the_ai_fails_the_built_in_ideas_are_used() = runBlocking {
        val store = FakeStore()
        val failing = FakeSuggester(SuggestionResult.Failed(FailureKind.NETWORK))
        val outcome = service(store, failing, FakeSettings(aiEnabled = true)).ensureCurrent()

        assertEquals(SuggestionOutcome.GENERATED_LOCALLY, outcome)
        assertTrue(store.inserted.all { it.third.all { m -> m.title.startsWith("Local") } })
        assertEquals(5, store.inserted.sumOf { it.third.size })
    }

    @Test
    fun with_the_ai_turned_off_it_is_never_called() = runBlocking {
        val store = FakeStore()
        val suggester = FakeSuggester(aiBatch())
        val outcome = service(store, suggester, FakeSettings(aiEnabled = false)).ensureCurrent()

        assertEquals(SuggestionOutcome.GENERATED_LOCALLY, outcome)
        assertEquals(0, suggester.calls)
    }

    @Test
    fun missing_ideas_are_topped_up_and_duplicates_of_existing_missions_are_dropped() = runBlocking {
        val store = FakeStore(existing = listOf("leer 10 PAGINAS"))
        val partial = SuggestionResult.Success(
            SuggestionBatch(daily = listOf(idea("Leer 10 páginas"), idea("Repasar apuntes")), weekly = emptyList())
        )
        service(store, FakeSuggester(partial), FakeSettings(aiEnabled = true)).ensureCurrent()

        val daily = store.inserted.first { it.first == MissionType.DAILY }.third
        assertEquals(3, daily.size)
        assertEquals("Repasar apuntes", daily.first().title)
        assertTrue(daily.none { it.title.equals("Leer 10 páginas", ignoreCase = true) })
    }

    @Test
    fun expired_suggestions_are_cleared_every_time() = runBlocking {
        val store = FakeStore()
        service(store, FakeSuggester(aiBatch()), FakeSettings(aiEnabled = true), day = monday + 3).ensureCurrent()
        assertEquals((monday + 3) to monday, store.expired)
    }
}
