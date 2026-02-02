package yenha.foodstore.ai.suggestion.DTO.ChoiceReponse;

public record GroqMessageResponse(
    String role,
    String content,
    String reasoning
) {}
