package yenha.foodstore.ai.suggestion.DTO.BusinessResponse;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BusinessSuggestionRequest(
    @NotNull(message = "Start date is required")
    LocalDate startDate,
    
    @NotNull(message = "End date is required")
    LocalDate endDate
) {}
