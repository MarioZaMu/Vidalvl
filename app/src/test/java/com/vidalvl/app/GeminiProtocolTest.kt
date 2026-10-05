package com.vidalvl.app

import com.vidalvl.app.data.remote.GeminiProtocol
import com.vidalvl.app.domain.verification.FailureKind
import com.vidalvl.app.domain.verification.VerificationResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiProtocolTest {
    /** Wraps [text] the way generateContent returns it. */
    private fun answer(text: String, vararg extraParts: String): String {
        val parts = extraParts.joinToString("") { """{"text":${JSONObject.quote(it)},"thought":true},""" }
        return """{"candidates":[{"content":{"parts":[$parts{"text":${JSONObject.quote(text)}}]}}]}"""
    }

    @Test
    fun prompt_carries_title_details_and_language() {
        val prompt = GeminiProtocol.buildPrompt("Tender la cama", "Con sábana", "Spanish")
        assertTrue(prompt.contains("Tender la cama"))
        assertTrue(prompt.contains("Con sábana"))
        assertTrue(prompt.contains("Spanish"))
    }

    @Test
    fun prompt_demands_the_finished_state_and_a_description_first() {
        val prompt = GeminiProtocol.buildPrompt("Tender la cama", null, "Spanish")
        assertTrue(prompt.contains("RESULT"))
        assertTrue(prompt.contains("still undone is NOT proof"))
        assertTrue(prompt.contains("observation"))
    }

    @Test
    fun schema_asks_for_the_observation_before_the_verdict() {
        val schema = JSONObject(GeminiProtocol.buildRequestJson("p", "QUJD"))
            .getJSONObject("generationConfig").getJSONObject("response_schema")
        val order = schema.getJSONArray("propertyOrdering")
        assertEquals("observation", order.getString(0))
        assertEquals("verified", order.getString(1))
    }

    @Test
    fun answers_with_an_observation_are_still_read() {
        val text = """{"observation": "Una cama con las sábanas revueltas", "verified": false, "reason": "Estira las sábanas"}"""
        assertEquals(
            VerificationResult.Rejected("Estira las sábanas"),
            GeminiProtocol.parseVerdict(answer(text))
        )
    }

    @Test
    fun prompt_skips_blank_details() {
        val prompt = GeminiProtocol.buildPrompt("Leer", "  ", "English")
        assertTrue(!prompt.contains("Mission details"))
    }

    @Test
    fun request_has_text_photo_and_json_schema() {
        val json = JSONObject(GeminiProtocol.buildRequestJson("hola", "QUJD"))
        val parts = json.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        assertEquals("hola", parts.getJSONObject(0).getString("text"))
        val inline = parts.getJSONObject(1).getJSONObject("inline_data")
        assertEquals("image/jpeg", inline.getString("mime_type"))
        assertEquals("QUJD", inline.getString("data"))
        val config = json.getJSONObject("generationConfig")
        assertEquals("application/json", config.getString("response_mime_type"))
        assertEquals("OBJECT", config.getJSONObject("response_schema").getString("type"))
    }

    @Test
    fun positive_verdict_is_verified() {
        val result = GeminiProtocol.parseVerdict(answer("""{"verified": true, "reason": "Se ve la cama"}"""))
        assertEquals(VerificationResult.Verified, result)
    }

    @Test
    fun negative_verdict_keeps_the_reason() {
        val result = GeminiProtocol.parseVerdict(answer("""{"verified": false, "reason": " No se ve la cama "}"""))
        assertEquals(VerificationResult.Rejected("No se ve la cama"), result)
    }

    @Test
    fun json_inside_markdown_fences_is_accepted() {
        val text = "```json\n{\"verified\": true, \"reason\": \"ok\"}\n```"
        assertEquals(VerificationResult.Verified, GeminiProtocol.parseVerdict(answer(text)))
    }

    @Test
    fun reasoning_parts_are_ignored() {
        val result = GeminiProtocol.parseVerdict(
            answer("""{"verified": false, "reason": "Borrosa"}""", "Pensando... {\"verified\": true}")
        )
        assertEquals(VerificationResult.Rejected("Borrosa"), result)
    }

    @Test
    fun anything_unexpected_is_an_unreadable_answer() {
        val unreadable = VerificationResult.Failed(FailureKind.UNREADABLE_ANSWER)
        assertEquals(unreadable, GeminiProtocol.parseVerdict("no es json"))
        assertEquals(unreadable, GeminiProtocol.parseVerdict("""{"candidates": []}"""))
        assertEquals(unreadable, GeminiProtocol.parseVerdict("""{"promptFeedback": {"blockReason": "SAFETY"}}"""))
        assertEquals(unreadable, GeminiProtocol.parseVerdict(answer("""{"reason": "falta el campo"}""")))
        assertEquals(unreadable, GeminiProtocol.parseVerdict(answer("texto sin json")))
    }
}
