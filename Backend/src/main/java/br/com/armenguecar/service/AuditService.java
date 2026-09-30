package br.com.armenguecar.service;

import br.com.armenguecar.entity.AuditLog;
import br.com.armenguecar.repository.AuditLogRepository;
import br.com.armenguecar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> list() {
        return auditLogRepository.findAll();
    }

    @Transactional
    public AuditLog log(UUID userId, String action, String target, String detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(userRepository.findById(userId).orElse(null));
        auditLog.setAction(action);
        auditLog.setTarget(target);
        auditLog.setDetail(detail);
        return auditLogRepository.save(auditLog);
    }
}
