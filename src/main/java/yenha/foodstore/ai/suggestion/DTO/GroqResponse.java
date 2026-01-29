package yenha.foodstore.ai.suggestion.DTO;

import java.util.List;

public record GroqResponse(
    String id,
    String object,
    Long created,
    String model,
    List<GroqChoice> choices,
    Object usage,
    Object usage_breakdown,
    String system_fingerprint,
    Object x_groq,
    String service_tier
) {}
