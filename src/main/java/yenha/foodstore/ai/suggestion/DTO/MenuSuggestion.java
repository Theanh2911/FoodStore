package yenha.foodstore.ai.suggestion.DTO;

public record MenuSuggestion(
    String main_dish,
    String side_dish,
    String drink,
    String reason
) {}
