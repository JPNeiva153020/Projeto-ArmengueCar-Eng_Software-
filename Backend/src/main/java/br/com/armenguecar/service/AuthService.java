package br.com.armenguecar.service;

import br.com.armenguecar.dto.AuthDtos.LoginRequest;
import br.com.armenguecar.dto.AuthDtos.LoginResponse;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.repository.UserRepository;
import br.com.armenguecar.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(
                        request.password(),
                        candidate.getPasswordHash()
                ))
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        String token = jwtService.generate(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPermissions()
        );
    }
}
