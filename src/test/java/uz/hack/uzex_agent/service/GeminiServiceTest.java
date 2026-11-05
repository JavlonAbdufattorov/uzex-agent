package uz.hack.uzex_agent.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GeminiServiceTest {

    @Test
    void askShouldFailClearlyWhenApiKeyIsMissing() {
        GeminiService service = new GeminiService("", "gemini-3.1-flash-lite", null, new ObjectMapper());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.ask("hello"));

        assertTrue(ex.getMessage().contains("GEMINI_API_KEY"));
    }
}
