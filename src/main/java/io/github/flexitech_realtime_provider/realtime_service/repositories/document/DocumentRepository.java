package io.github.flexitech_realtime_provider.realtime_service.repositories.document;

import io.github.flexitech_realtime_provider.realtime_service.models.document.DocumentModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentRepository extends ReactiveMongoRepository<DocumentModel, String> {
    Flux<DocumentModel> getByCollectionId(String collectionId);
}
