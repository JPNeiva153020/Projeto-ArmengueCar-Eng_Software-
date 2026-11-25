package br.com.armenguecar.service;

import br.com.armenguecar.dto.UserDtos.CreateUserRequest;
import br.com.armenguecar.dto.UserDtos.UpdateUserRequest;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.enums.Role;
import br.com.armenguecar.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final br.com.armenguecar.repository.ClientRepository clients;
    public UserService(UserRepository users, PasswordEncoder encoder, AuditService audit, br.com.armenguecar.repository.ClientRepository clients) {
        this.users = users; this.encoder = encoder; this.audit = audit; this.clients = clients;
    }
    @Transactional(readOnly = true)
    public List<User> list() { return users.findAll(); }
    @Transactional
    public User create(CreateUserRequest request, UUID actor) {
        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        ensureEmailAvailable(email, null);
        User user = new User(request.name().trim(), email, encoder.encode(request.password()), request.role());
        User saved = users.save(user);
        audit.log(actor, "USUARIO_CRIADO", saved.getId().toString(), "Perfil: " + saved.getRole());
        return saved;
    }
    @Transactional
    public User update(UUID id, UpdateUserRequest request, UUID actor) {
        User user = find(id);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, id);
        if (id.equals(actor) && request.role() != Role.ADMIN) {
            throw new IllegalArgumentException("Não é possível remover o próprio perfil de administrador");
        }
        if (user.isActive() && user.getRole() == Role.ADMIN && request.role() != Role.ADMIN
                && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("É necessário manter um administrador ativo");
        }
        if (request.role() != Role.CLIENTE && clients.existsByUserId(id))
            throw new IllegalArgumentException("Remova o vínculo com o cliente antes de alterar o perfil");
        boolean accessChanged = !user.getEmail().equalsIgnoreCase(email) || user.getRole() != request.role();
        user.setName(request.name().trim()); user.setEmail(email); user.setRole(request.role());
        if (accessChanged) user.setAuthVersion(user.getAuthVersion() + 1);
        audit.log(actor, "USUARIO_ATUALIZADO", id.toString(), "Perfil: " + user.getRole());
        return users.save(user);
    }
    @Transactional
    public User toggle(UUID id, UUID actor) {
        User user = find(id);
        if (user.isActive() && id.equals(actor)) throw new IllegalArgumentException("Não é possível desativar a própria conta");
        if (user.isActive() && user.getRole() == Role.ADMIN && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1)
            throw new IllegalArgumentException("É necessário manter um administrador ativo");
        user.setActive(!user.isActive());
        user.setAuthVersion(user.getAuthVersion() + 1);
        audit.log(actor, user.isActive() ? "USUARIO_ATIVADO" : "USUARIO_DESATIVADO", id.toString(), null);
        return users.save(user);
    }
    @Transactional
    public void resetPassword(UUID id, String password, UUID actor) {
        User user = find(id);
        validatePassword(password);
        user.setPasswordHash(encoder.encode(password));
        user.setAuthVersion(user.getAuthVersion() + 1);
        users.save(user);
        audit.log(actor, "SENHA_REDEFINIDA", id.toString(), null);
    }
    private void validatePassword(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("A senha deve ocupar no máximo 72 bytes em UTF-8");
    }
    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private void ensureEmailAvailable(String email, UUID id) {
        users.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(id)).ifPresent(u -> {
            throw new IllegalArgumentException("E-mail já cadastrado");
        });
    }
}
