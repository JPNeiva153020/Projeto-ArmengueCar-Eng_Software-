package br.com.armenguecar.controller;

import br.com.armenguecar.entity.Notification;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<Notification> list(Authentication authentication) {
        User user = currentUser(authentication);

        return notificationRepository.findAll().stream()
                .filter(notification -> notification.getUser().getId().equals(user.getId()))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .toList();
    }

    @PatchMapping("/{id}/read")
    public Notification markAsRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notificação não encontrada"
                ));

        User user = currentUser(authentication);
        ensureOwner(notification, user);

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @PostMapping("/read-all")
    public void markAllAsRead(Authentication authentication) {
        User user = currentUser(authentication);

        notificationRepository.findAll().stream()
                .filter(notification -> notification.getUser().getId().equals(user.getId()))
                .filter(notification -> !notification.isRead())
                .forEach(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    private void ensureOwner(Notification notification, User user) {
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
    }
}
