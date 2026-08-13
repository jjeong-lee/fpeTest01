package kr.ac.knue.achievement.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChangeHistoryTransactionTest {
    @Test
    void rollsBackTheBusinessChangeWhenChangeHistoryWriteFails() {
        InMemoryTransactionalCommandStore store = new InMemoryTransactionalCommandStore(true);

        assertThrows(ChangeHistoryWriteException.class,
                () -> store.change("user_account", "user-1", "before", "after", "admin", "설정 변경"));

        assertEquals("before", store.currentValue());
        assertEquals(List.of(), store.history());
    }
}
