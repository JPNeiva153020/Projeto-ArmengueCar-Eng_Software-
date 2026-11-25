package br.com.armenguecar.dto;

import br.com.armenguecar.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}
    public record CreateUserRequest(@NotBlank @Size(max=255) String name,
                                    @NotBlank @Email @Size(max=255) String email,
                                    @NotBlank @Size(min=8, max=72) String password,
                                    @NotNull Role role) {}
    public record UpdateUserRequest(@NotBlank @Size(max=255) String name,
                                    @NotBlank @Email @Size(max=255) String email,
                                    @NotNull Role role) {}
    public record ResetPasswordRequest(@NotBlank @Size(min=8, max=72) String password) {}
}
