package yenha.foodstore.Auth.DTO;

public class AuthResponse {
    private Long userId;
    private String name;
    private String phoneNumber;
    private String message;
    private String token;
    private String refreshToken;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String message) {
        this.message = message;
    }

    public AuthResponse(Long userId, String name, String phoneNumber, String message, String token, String role) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.token = token;
        this.role = role;
    }
    
    public AuthResponse(Long userId, String name, String phoneNumber, String message, String token, String refreshToken, String role) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.token = token;
        this.refreshToken = refreshToken;
        this.role = role;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

