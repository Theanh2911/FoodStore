package yenha.foodstore.ai.suggestion.DTO;

public record GroqChoice(
    Integer index,
    GroqMessageResponse message,
    Object logprobs,
    String finish_reason
) {}
