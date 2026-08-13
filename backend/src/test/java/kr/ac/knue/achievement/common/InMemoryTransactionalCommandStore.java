package kr.ac.knue.achievement.common;

import java.util.ArrayList;
import java.util.List;

final class InMemoryTransactionalCommandStore {
    private final boolean failHistoryWrite;
    private String currentValue = "before";
    private final List<String> history = new ArrayList<>();

    InMemoryTransactionalCommandStore(boolean failHistoryWrite) {
        this.failHistoryWrite = failHistoryWrite;
    }

    void change(String entityName, String entityId, String beforeValue, String afterValue, String actorUserId, String reason) {
        String originalValue = currentValue;
        int historySize = history.size();
        try {
            currentValue = afterValue;
            if (failHistoryWrite) {
                throw new ChangeHistoryWriteException();
            }
            history.add(entityName + ":" + entityId + ":" + beforeValue + ":" + afterValue + ":" + actorUserId + ":" + reason);
        } catch (RuntimeException exception) {
            currentValue = originalValue;
            while (history.size() > historySize) {
                history.remove(history.size() - 1);
            }
            throw exception;
        }
    }

    String currentValue() {
        return currentValue;
    }

    List<String> history() {
        return List.copyOf(history);
    }
}
