package yenha.foodstore.ai.suggestion.DTO.ChoiceReponse;

import java.util.List;

public record GroqRequest(
    List<GroqMessage> messages,
    String model,
    Integer temperature,
    Integer max_completion_tokens,
    Integer top_p,
    Boolean stream,
    String reasoning_effort,
    String stop
) {
    public GroqRequest(List<GroqMessage> messages) {
        this(
            messages,
            "openai/gpt-oss-20b",
            1,
            8192,
            1,
            false,
            "medium",
            null
        );
    }
}
