package uz.hack.uzex_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record AiRequest(
        @NotBlank String question
) {}