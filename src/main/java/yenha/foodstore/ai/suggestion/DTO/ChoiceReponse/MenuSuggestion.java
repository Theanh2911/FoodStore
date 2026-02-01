package yenha.foodstore.ai.suggestion.DTO.ChoiceReponse;

public record MenuSuggestion(
    String main_dish,
    String side_dish,
    String drink,
    String reason
) {}
