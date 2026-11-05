package uz.hack.uzex_agent.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import uz.hack.uzex_agent.dto.AiRequest;
import uz.hack.uzex_agent.dto.AiResponse;
import uz.hack.uzex_agent.service.GeminiService;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public AiResponse ask(@Valid @RequestBody AiRequest request) {
        String answer = geminiService.ask(request.question());
        return new AiResponse(answer);
    }
}