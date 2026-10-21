package fu.se.lab06jwt.responses;

import java.util.Date;

public record UserResponse(Integer id, String fullName, String email, Date createdAt, Date updatedAt) {}
