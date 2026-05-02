package com.sportsbetting.oddsservice.ingest;

import com.sportsbetting.oddsservice.model.odds.OddsQuoteEntity;
import com.sportsbetting.oddsservice.model.odds.OddsQuoteKey;
import com.sportsbetting.oddsservice.model.odds.OddsUpdatedEvent;
import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import com.sportsbetting.oddsservice.model.outbox.OutboxEventEntity;
import com.sportsbetting.oddsservice.outbox.OutboxEventFactory;
import com.sportsbetting.oddsservice.repository.OddsQuoteRepository;
import com.sportsbetting.oddsservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies one ingest chunk inside an enclosing transaction: validate → load quotes → persist quotes + outbox rows.
 */
@Component
public class OddsFeedChunkProcessor {

    private final OddsQuoteRepository oddsQuoteRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OddsUpdateValidator oddsUpdateValidator;
    private final OutboxEventFactory outboxEventFactory;

    public OddsFeedChunkProcessor(
            OddsQuoteRepository oddsQuoteRepository,
            OutboxEventRepository outboxEventRepository,
            OddsUpdateValidator oddsUpdateValidator,
            OutboxEventFactory outboxEventFactory
    ) {
        this.oddsQuoteRepository = oddsQuoteRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.oddsUpdateValidator = oddsUpdateValidator;
        this.outboxEventFactory = outboxEventFactory;
    }

    public void process(List<OddsUpdate> chunk) {
        oddsUpdateValidator.validateAll(chunk);
        List<String> keyIds = chunk.stream()
                .map(OddsQuoteKey::from)
                .map(OddsQuoteKey::asString)
                .distinct()
                .toList();
        Map<String, OddsQuoteEntity> quotesByKey = oddsQuoteRepository.findAllByKeyIdIn(keyIds).stream()
                .collect(Collectors.toMap(OddsQuoteEntity::getKeyId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        Instant batchInstant = Instant.now();
        List<OutboxEventEntity> outboxes = new ArrayList<>(chunk.size());

        for (OddsUpdate update : chunk) {
            OddsQuoteKey key = OddsQuoteKey.from(update);
            BigDecimal normalized = OddsMoney.normalize(update.odds());
            OddsQuoteEntity quote = quotesByKey.computeIfAbsent(key.asString(), k -> new OddsQuoteEntity());
            quote.setKeyId(key.asString());
            quote.setEventId(key.eventId());
            quote.setSelection(key.selection());
            quote.setOdds(normalized);
            quote.setUpdatedAt(batchInstant);

            OddsUpdatedEvent eventPayload = OddsUpdatedEvent.from(update, batchInstant, normalized);
            outboxes.add(outboxEventFactory.createPending(OddsUpdatedEvent.EVENT_TYPE, eventPayload));
        }

        oddsQuoteRepository.saveAll(quotesByKey.values());
        outboxEventRepository.saveAll(outboxes);
    }
}
