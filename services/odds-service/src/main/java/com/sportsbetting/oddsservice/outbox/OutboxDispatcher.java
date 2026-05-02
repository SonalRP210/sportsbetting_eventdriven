package com.sportsbetting.oddsservice.outbox;

import com.sportsbetting.oddsservice.model.outbox.OutboxEventEntity;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class OutboxDispatcher {

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxSlicePublisher outboxSlicePublisher;
    private final int dispatchChunkSize;

    public OutboxDispatcher(
            OutboxEventRepository outboxEventRepository,
            TransactionTemplate transactionTemplate,
            OutboxSlicePublisher outboxSlicePublisher,
            @Value("${app.outbox.dispatch.chunk-size:25}") int dispatchChunkSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.transactionTemplate = transactionTemplate;
        this.outboxSlicePublisher = outboxSlicePublisher;
        this.dispatchChunkSize = Math.max(1, dispatchChunkSize);
    }

    /**
     * Loads unpublished rows, publishes each slice in its own transaction with batched JDBC flush per slice.
     */
    public int dispatchPending() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        int sent = 0;
        for (int i = 0; i < pending.size(); i += dispatchChunkSize) {
            int end = Math.min(i + dispatchChunkSize, pending.size());
            List<OutboxEventEntity> slice = List.copyOf(pending.subList(i, end));
            transactionTemplate.executeWithoutResult(status -> outboxSlicePublisher.publishSlice(slice));
            sent += slice.size();
        }
        return sent;
    }
}
