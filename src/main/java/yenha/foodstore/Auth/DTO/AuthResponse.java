package yenha.foodstore.Auth.DTO;

public class AuthResponse {
    private Long userId;
    private String name;
    private String phoneNumber;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String message) {
        this.message = message;
    }

    public AuthResponse(Long userId, String name, String phoneNumber, String message) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

