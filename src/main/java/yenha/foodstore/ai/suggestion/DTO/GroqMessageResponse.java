package yenha.foodstore.ai.suggestion.DTO;

public record GroqMessageResponse(
    String role,
    String content,
    String reasoning
) {}
