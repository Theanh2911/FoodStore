package yenha.foodstore.Promotion.Exception;

public class PromotionException extends RuntimeException {
    
    public PromotionException(String message) {
        super(message);
    }
    
    public PromotionException(String message, Throwable cause) {
        super(message, cause);
    }
}
