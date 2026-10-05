package com.vidalvl.app

import com.vidalvl.app.data.remote.GeminiMissionProtocol
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.suggestions.AttributeSnapshot
import com.vidalvl.app.domain.suggestions.SuggestionContext
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiMissionProtocolTest {
    private val context = SuggestionContext(
        goals = setOf(Goal.SLEEP, Goal.LEARN),
        dailyMinutes = 15,
        attributes = listOf(AttributeSnapshot(Attribute.BODY, 3, 0.4f), AttributeSnapshot(Attribute.MIND, 2, 1.0f)),
        streak = 4,
        existingTitles = listOf("Tender la cama"),
        recentTitles = listOf("Leer 30 minutos")
    )

    private fun answer(text: String) =
        """{"candidates":[{"content":{"parts":[{"text":${JSONObject.quote(text)}}]}}]}"""

    @Test
    fun prompt_carries_goals_time_streak_and_known_titles() {
        val prompt = GeminiMissionProtocol.buildPrompt(context, 3, 2, "Spanish")
        assertTrue(prompt.contains("sleep better"))
        assertTrue(prompt.contains("learn and study"))
        assertTrue(prompt.contains("15 minutes"))
        assertTrue(prompt.contains("4 days"))
        assertTrue(prompt.contains("Tender la cama"))
        assertTrue(prompt.contains("Leer 30 minutos"))
        assertTrue(prompt.contains("exactly 3 daily missions and 2 weekly missions"))
        assertTrue(prompt.contains("Spanish"))
    }

    @Test
    fun prompt_forbids_risky_advice_and_shaming() {
        val prompt = GeminiMissionProtocol.buildPrompt(context, 3, 0, "English")
        assertTrue(prompt.contains("No medical, dieting or fasting advice"))
        assertTrue(prompt.contains("Never shame"))
    }

    @Test
    fun prompt_marks_an_empty_list() {
        val empty = context.copy(existingTitles = emptyList(), recentTitles = emptyList())
        assertTrue(GeminiMissionProtocol.buildPrompt(empty, 3, 2, "English").contains("(none)"))
    }

    @Test
    fun request_is_text_only_with_a_schema_for_both_lists() {
        val json = JSONObject(GeminiMissionProtocol.buildRequestJson("hola"))
        val parts = json.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        assertEquals(1, parts.length())
        assertEquals("hola", parts.getJSONObject(0).getString("text"))
        val properties = json.getJSONObject("generationConfig").getJSONObject("response_schema").getJSONObject("properties")
        assertTrue(properties.has("daily") && properties.has("weekly"))
        val enum = properties.getJSONObject("daily").getJSONObject("items").getJSONObject("properties")
            .getJSONObject("attribute").getJSONArray("enum")
        assertEquals(Attribute.entries.size, enum.length())
    }

    @Test
    fun a_good_answer_is_read_into_both_lists() {
        val text = """{"daily":[{"title":"Leer 10 páginas","description":"Con calma","attribute":"MIND"}],
            "weekly":[{"title":"Dormir a la misma hora","attribute":"body"}]}"""
        val batch = GeminiMissionProtocol.parseBatch(answer(text), Attribute.DISCIPLINE)!!
        assertEquals("Leer 10 páginas", batch.daily.single().title)
        assertEquals("Con calma", batch.daily.single().description)
        assertEquals(Attribute.MIND, batch.daily.single().attribute)
        assertEquals(Attribute.BODY, batch.weekly.single().attribute)
        assertNull(batch.weekly.single().description)
    }

    @Test
    fun an_unknown_attribute_falls_back_to_the_default() {
        val text = """{"daily":[{"title":"Algo","attribute":"MAGIC"}],"weekly":[]}"""
        val batch = GeminiMissionProtocol.parseBatch(answer(text), Attribute.SOCIAL)!!
        assertEquals(Attribute.SOCIAL, batch.daily.single().attribute)
    }

    @Test
    fun items_without_a_title_are_skipped() {
        val text = """{"daily":[{"title":"  ","attribute":"MIND"},{"attribute":"MIND"},{"title":"Ok","attribute":"MIND"}],"weekly":[]}"""
        val batch = GeminiMissionProtocol.parseBatch(answer(text), Attribute.MIND)!!
        assertEquals(listOf("Ok"), batch.daily.map { it.title })
    }

    @Test
    fun missing_lists_are_empty_not_an_error() {
        val batch = GeminiMissionProtocol.parseBatch(answer("""{"daily":[]}"""), Attribute.MIND)
        assertNotNull(batch)
        assertTrue(batch!!.weekly.isEmpty())
    }

    @Test
    fun json_in_markdown_fences_is_accepted() {
        val text = "```json\n{\"daily\":[{\"title\":\"A\",\"attribute\":\"MIND\"}],\"weekly\":[]}\n```"
        assertEquals("A", GeminiMissionProtocol.parseBatch(answer(text), Attribute.MIND)!!.daily.single().title)
    }

    @Test
    fun garbage_is_null() {
        assertNull(GeminiMissionProtocol.parseBatch("no es json", Attribute.MIND))
        assertNull(GeminiMissionProtocol.parseBatch("""{"candidates": []}""", Attribute.MIND))
        assertNull(GeminiMissionProtocol.parseBatch(answer("texto sin json"), Attribute.MIND))
    }
}
