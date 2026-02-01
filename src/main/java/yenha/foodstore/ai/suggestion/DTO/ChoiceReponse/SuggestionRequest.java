package yenha.foodstore.ai.suggestion.DTO.ChoiceReponse;

import jakarta.validation.constraints.NotBlank;

public record SuggestionRequest(
    @NotBlank(message = "User demand cannot be empty")
    String userDemand
) {}
