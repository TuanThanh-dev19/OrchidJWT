package fu.se.lab06jwt.responses;

public record LoginResponse(String token, long expiresIn) {}
