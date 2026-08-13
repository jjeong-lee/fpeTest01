package kr.ac.knue.achievement.common;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class AuditedCommandExecutor {
    private final ChangeHistoryMapper changeHistoryMapper;

    public AuditedCommandExecutor(ChangeHistoryMapper changeHistoryMapper) {
        this.changeHistoryMapper = changeHistoryMapper;
    }

    @Transactional
    public void execute(UUID actorUserId, String entityName, String entityId, String beforeValue,
            String afterValue, String reason, Runnable businessWrite) {
        businessWrite.run();
        changeHistoryMapper.insert(UUID.randomUUID(), entityName, entityId, beforeValue, afterValue,
                actorUserId, reason, Instant.now());
    }
}
