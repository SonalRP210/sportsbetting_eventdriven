package com.sportsbetting.betting.outbox;

import com.sportsbetting.betting.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void cleanupDeletesRowsOlderThanRetentionHours() {
        when(outboxEventRepository.deleteByCreatedAtBefore(any())).thenReturn(4);

        OutboxCleanup cleanup = new OutboxCleanup(outboxEventRepository, 3L);
        cleanup.cleanup();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(outboxEventRepository).deleteByCreatedAtBefore(cutoff.capture());
        Instant captured = cutoff.getValue();
        assertThat(captured).isBefore(Instant.now());
        assertThat(captured).isAfter(Instant.now().minusSeconds(3L * 3600 + 120));
    }
}
