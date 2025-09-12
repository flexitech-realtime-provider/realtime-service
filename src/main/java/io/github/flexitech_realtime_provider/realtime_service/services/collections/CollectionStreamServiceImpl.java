package io.github.flexitech_realtime_provider.realtime_service.services.collections;

import io.github.flexitech_realtime_provider.common.api.response.stream.StreamResponse;
import io.github.flexitech_realtime_provider.common.dtos.collections.CollectionDTO;
import io.github.flexitech_realtime_provider.common.dtos.document.DocumentDTO;
import io.github.flexitech_realtime_provider.common.enums.OperationType;
import io.github.flexitech_realtime_provider.realtime_service.models.collection.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Service
public class CollectionStreamServiceImpl implements CollectionStreamService {

    private final Logger log = LogManager.getLogger(getClass());

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public CollectionStreamServiceImpl(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @Override
    public Flux<ServerSentEvent<StreamResponse<CollectionDTO>>> stream(String userId, String moduleId) {
        ChangeStreamOptions options = ChangeStreamOptions.builder()
                .filter(Aggregation.newAggregation(
                        Aggregation.match(
                                Criteria.where("fullDocument.ownerId").is(userId)
                                        .and("fullDocument.moduleId").is(moduleId)
                        )
                ))
                .returnFullDocumentOnUpdate()
                .build();

        return reactiveMongoTemplate.changeStream("collections", options, Collection.class)
                .map(event -> {
                    String operationType = Objects.requireNonNull(event.getOperationType()).getValue();
                    System.out.println("Operation Type: " + operationType);

                    StreamResponse<CollectionDTO> response = new StreamResponse<>();

                    if ("delete".equals(operationType)) {
                        // Handle delete operation - get the ID from the document key
                        BsonDocument documentKey = Objects.requireNonNull(event.getRaw()).getDocumentKey();
                        String documentId = Objects.requireNonNull(documentKey).getObjectId("_id").toString();
                        response.setType(OperationType.DELETE);
                        response.setData(CollectionDTO.builder().id(documentId).build());
                    } else {
                        // Handle insert/update operations
                        if (event.getBody() == null) {
                            return null; // Skip if body is null for non-delete operations
                        }

                        response.setType(OperationType.MODIFIED);
                        response.setData(this.mapToDTO(event.getBody()));
                    }

                    return response;
                })
                .filter(Objects::nonNull)
                .map(response -> ServerSentEvent.builder(response)
                        .event("collection-change")
                        .build())
                .onErrorResume(e -> {
                    log.error("Stream error", e);
                    return Flux.empty();
                });
    }

    private CollectionDTO mapToDTO(Collection collection){
        return CollectionDTO.builder()
                .id(collection.getId())
                .title(collection.getTitle())
                .description(collection.getDescription())
                .ownerId(collection.getOwnerId())
                .moduleId(collection.getModuleId())  // Added if needed
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }
}
