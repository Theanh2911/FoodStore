package yenha.foodstore.ai.suggestion.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import yenha.foodstore.ai.suggestion.DTO.BusinessResponse.BusinessStrategyResponse;
import yenha.foodstore.ai.suggestion.DTO.BusinessResponse.BusinessSuggestionRequest;
import yenha.foodstore.ai.suggestion.DTO.ChoiceReponse.MenuSuggestion;
import yenha.foodstore.ai.suggestion.DTO.ChoiceReponse.SuggestionRequest;
import yenha.foodstore.ai.suggestion.Service.AiBusinessSuggestionService;
import yenha.foodstore.ai.suggestion.Service.AISuggestionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AISuggestionController {

    @Autowired
    private AISuggestionService aiSuggestionService;
    
    @Autowired
    private AiBusinessSuggestionService aiBusinessSuggestionService;

    /**
     * POST /api/ai/suggestion
     * Get AI-powered menu suggestion based on user demand
     */
    @PostMapping("/suggestion")
    public ResponseEntity<?> getMenuSuggestion(
            @Valid @RequestBody SuggestionRequest request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        
        try {
            MenuSuggestion suggestion = aiSuggestionService.getSuggestion(request.userDemand());
            return new ResponseEntity<>(suggestion, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/ai/business-suggestion
     * Get AI-powered business strategy suggestions based on product performance data
     */
    @PostMapping("/business-suggestion")
    public ResponseEntity<?> getBusinessSuggestion(
            @Valid @RequestBody BusinessSuggestionRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        try {
            List<BusinessStrategyResponse> strategies = aiBusinessSuggestionService.getBusinessSuggestion(
                request.startDate(), 
                request.endDate()
            );
            return new ResponseEntity<>(strategies, HttpStatus.OK);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
