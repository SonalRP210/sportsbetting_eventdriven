package com.sportsbetting.betting.outbox;

import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private OutboxSlicePublisher outboxSlicePublisher;

    @Test
    void dispatchPendingReturnsZeroWhenPollingDisabled() {
        OutboxDispatcher cut = new OutboxDispatcher(
                outboxEventRepository, transactionTemplate, outboxSlicePublisher, 25, false);

        assertThat(cut.dispatchPending()).isZero();

        verify(outboxEventRepository, never()).findTop100ByPublishedFalseOrderByCreatedAtAsc();
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    void dispatchPendingPublishesWhenPollingEnabled() {
        OutboxEventEntity row = new OutboxEventEntity();
        row.setId(UUID.randomUUID());
        row.setEventType("betting.bet.placed.v1");
        row.setPayload("{}");
        row.setMessageKey("BET-1");
        row.setPublished(false);
        row.setCreatedAt(Instant.now());

        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(row));

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        OutboxDispatcher cut = new OutboxDispatcher(
                outboxEventRepository, transactionTemplate, outboxSlicePublisher, 25, true);

        assertThat(cut.dispatchPending()).isEqualTo(1);

        verify(outboxSlicePublisher).publishSlice(argThat(slice -> slice.size() == 1 && slice.get(0) == row));
    }
}
