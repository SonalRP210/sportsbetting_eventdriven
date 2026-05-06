package com.sportsbetting.betting.outbox;

import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected collaborators")
public class OutboxDispatcher {

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxSlicePublisher outboxSlicePublisher;
    private final int dispatchChunkSize;
    private final boolean pollEnabled;

    public OutboxDispatcher(
            OutboxEventRepository outboxEventRepository,
            TransactionTemplate transactionTemplate,
            OutboxSlicePublisher outboxSlicePublisher,
            @Value("${app.outbox.dispatch.chunk-size:25}") int dispatchChunkSize,
            @Value("${app.outbox.poll.enabled:false}") boolean pollEnabled
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.transactionTemplate = transactionTemplate;
        this.outboxSlicePublisher = outboxSlicePublisher;
        this.dispatchChunkSize = Math.max(1, dispatchChunkSize);
        this.pollEnabled = pollEnabled;
    }

    /**
     * When polling is disabled, Debezium is the primary relay — do not publish from the app
     * (would duplicate messages already emitted from the WAL).
     */
    public int dispatchPending() {
        if (!pollEnabled) {
            return 0;
        }
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
