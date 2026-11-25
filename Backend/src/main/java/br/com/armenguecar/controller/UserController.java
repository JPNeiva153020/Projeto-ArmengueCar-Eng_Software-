package br.com.armenguecar.controller;

import br.com.armenguecar.dto.UserDtos.CreateUserRequest;
import br.com.armenguecar.dto.UserDtos.UpdateUserRequest;
import br.com.armenguecar.dto.UserDtos.ResetPasswordRequest;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    private UUID actor(Authentication auth) { return ((User) auth.getPrincipal()).getId(); }
    @GetMapping public List<User> list() { return users.list(); }
    @PostMapping public User create(@Valid @RequestBody CreateUserRequest request, Authentication auth) {
        return users.create(request, actor(auth));
    }
    @PutMapping("/{id}") public User update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request, Authentication auth) {
        return users.update(id, request, actor(auth));
    }
    @PatchMapping("/{id}/toggle") public User toggle(@PathVariable UUID id, Authentication auth) {
        return users.toggle(id, actor(auth));
    }
    @PatchMapping("/{id}/password") public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request, Authentication auth) {
        users.resetPassword(id, request.password(), actor(auth));
    }
}
