package io.github.flexitech_realtime_provider.realtime_service.services.documents;

import io.github.flexitech_realtime_provider.common.api.request.document.DocumentCreateRequest;
import io.github.flexitech_realtime_provider.common.dtos.document.DocumentDTO;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.common.utils.converters.BsonConverter;
import io.github.flexitech_realtime_provider.common.utils.validation.CommonValidator;
import io.github.flexitech_realtime_provider.realtime_service.models.collection.Collection;
import io.github.flexitech_realtime_provider.realtime_service.models.document.DocumentModel;
import io.github.flexitech_realtime_provider.realtime_service.repositories.collection.CollectionRepository;
import io.github.flexitech_realtime_provider.realtime_service.repositories.document.DocumentRepository;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService{

    private final DocumentRepository documentRepository;
    private final CollectionRepository collectionRepository;

    public DocumentServiceImpl(DocumentRepository documentRepository, CollectionRepository collectionRepository) {
        this.documentRepository = documentRepository;
        this.collectionRepository = collectionRepository;
    }

    @Override
    public Mono<DocumentDTO> manageDocument(DocumentCreateRequest request, ClientDetailDTO clientDetailDTO) {
        return this.collectionRepository.findById(request.getCollectionId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found")))
                .flatMap(collection -> {
                    if (CommonValidator.validString(request.getId())) {
                        return this.documentRepository.findById(request.getId())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")))
                                .flatMap(existingDoc -> {
                                    existingDoc.setName(request.getName());
                                    existingDoc.setMetaData(BsonConverter.toBson(request.getDocuments()));
                                    existingDoc.setUpdatedAt(Instant.now());
                                    return this.documentRepository.save(existingDoc)
                                            .map(this::mapToDTO)
                                            .doOnNext(dto -> log.info("Document updated successfully: {}", dto.getId()));
                                });
                    }
                    else {
                        DocumentModel newDoc = new DocumentModel();
                        newDoc.setCollectionId(collection.getId());
                        newDoc.setName(request.getName());
                        newDoc.setMetaData(BsonConverter.toBson(request.getDocuments()));
                        Instant now = Instant.now();
                        newDoc.setCreatedAt(now);
                        newDoc.setUpdatedAt(now); // optional, keeps consistency
                        return this.documentRepository.save(newDoc)
                                .map(this::mapToDTO)
                                .doOnNext(dto -> log.info("Document created successfully: {}", dto.getId()));
                    }
                });
    }

    @Override
    public Mono<DocumentDTO> getById(String documentId) {
        return this.documentRepository.findById(documentId)
                .map(this::mapToDTO);
    }


    @Override
    public Flux<DocumentDTO> getAllDocumentByCollectionId(String collectionId) {
        return this.documentRepository.getByCollectionId(collectionId)
                .map(this::mapToDTO);
    }

    @Override
    public Mono<String> deleteDocument(String documentId){
        return documentRepository.findById(documentId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found!")))
                .flatMap(doc-> documentRepository.deleteById(documentId)
                        .doOnSuccess(d->{
                            log.info("Delete document successfully.");
                        }).map(d-> documentId));
    }

    private DocumentDTO mapToDTO(DocumentModel model){
        return DocumentDTO.builder()
                .id(model.getId())
                .data(model.getMetaData())
                .documents(BsonConverter.fromBson(model.getMetaData()))
                .collectionId(model.getCollectionId())
                .name(model.getName())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
