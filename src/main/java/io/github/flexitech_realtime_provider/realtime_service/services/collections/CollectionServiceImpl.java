package io.github.flexitech_realtime_provider.realtime_service.services.collections;

import io.github.flexitech_realtime_provider.common.api.request.collection.CreateCollectionRequest;
import io.github.flexitech_realtime_provider.common.dtos.collections.CollectionDTO;
import io.github.flexitech_realtime_provider.realtime_service.models.collection.Collection;
import io.github.flexitech_realtime_provider.realtime_service.repositories.collection.CollectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@Slf4j
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionServiceImpl(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public Mono<CollectionDTO> createCollection(CreateCollectionRequest request, String ownerId, String moduleId) {
        Collection doc = new Collection();
        doc.setTitle(request.getTitle());
        doc.setDescription(request.getDescription());
        doc.setOwnerId(ownerId);
        doc.setModuleId(moduleId);
        doc.setCreatedAt(Instant.now());

        return this.collectionRepository.save(doc).doOnSuccess(saved->{
            log.info("Document with title {} saved successfully!", saved.getTitle());
        }).doOnError(e->{
            log.error("Failed to save document with title {}.", request.getTitle());
        }).map(this::mapToDTO);
    }

    @Override
    public Mono<CollectionDTO> updateCollection(String collectionId, CollectionDTO collectionDTO, String ownerId) {
        return this.collectionRepository.findById(collectionId)
                .flatMap(existingDoc -> {
                    existingDoc.setTitle(collectionDTO.getTitle());
                    existingDoc.setDescription(collectionDTO.getDescription());
                    existingDoc.setOwnerId(ownerId);
                    existingDoc.setUpdatedAt(Instant.now());
                    return collectionRepository.save(existingDoc)
                            .doOnSuccess(doc -> {
                                log.info("updated success!");
                            }).map(this::mapToDTO);
                }).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<CollectionDTO> getCollectionById(String collectionId, String ownerId, String moduleId) {
        return collectionRepository.findById(collectionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(doc -> {
                    if (!doc.getOwnerId().equals(ownerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                    if(!doc.getModuleId().equals(moduleId)){
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                    return Mono.just(doc);
                }).map(this::mapToDTO);
    }

    @Override
    public Flux<CollectionDTO> getAllCollectionsByUser(String ownerId, String moduleId) {
        return collectionRepository.findByOwnerIdAndModuleIdOrderByCreatedAtDesc(ownerId, moduleId).map(this::mapToDTO);
    }

    @Override
    public Mono<Void> deleteCollection(String collectionId, String ownerId) {
        return collectionRepository.findById(collectionId)
                .flatMap(doc -> {
                    if (!doc.getOwnerId().equals(ownerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                    return collectionRepository.deleteById(collectionId)
                            .doOnSuccess(v ->
                                    log.info("delete document success!")
                            );
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    private CollectionDTO mapToDTO(Collection document){
        return CollectionDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .ownerId(document.getOwnerId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
