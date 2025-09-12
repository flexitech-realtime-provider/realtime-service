package io.github.flexitech_realtime_provider.realtime_service.services.collections;

import io.github.flexitech_realtime_provider.common.api.request.collection.CreateCollectionRequest;
import io.github.flexitech_realtime_provider.common.dtos.collections.CollectionDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CollectionService {
    Mono<CollectionDTO> createCollection(CreateCollectionRequest request, String ownerId, String moduleId);
    Mono<CollectionDTO> updateCollection(String collectionId, CollectionDTO collectionDTO, String ownerId);
    Mono<CollectionDTO> getCollectionById(String collectionId, String ownerId, String moduleId);
    Flux<CollectionDTO> getAllCollectionsByUser(String ownerId, String moduleId);
    Mono<Void> deleteCollection(String collectionId, String ownerId);
}
