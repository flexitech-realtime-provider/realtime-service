package io.github.flexitech_realtime_provider.realtime_service.repositories.collection;

import io.github.flexitech_realtime_provider.realtime_service.models.collection.Collection;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface CollectionRepository extends ReactiveMongoRepository<Collection, String> {
    Flux<Collection> findByOwnerId(String ownerId);

    Flux<Collection> findByOwnerIdAndModuleId(String ownerId, String moduleId);

    Flux<Collection> findByOwnerIdAndModuleIdOrderByCreatedAtDesc(String ownerId, String moduleId);
}
