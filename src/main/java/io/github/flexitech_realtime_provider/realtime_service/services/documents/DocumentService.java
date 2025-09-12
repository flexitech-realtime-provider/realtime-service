package io.github.flexitech_realtime_provider.realtime_service.services.documents;

import io.github.flexitech_realtime_provider.common.api.request.document.DocumentCreateRequest;
import io.github.flexitech_realtime_provider.common.dtos.document.DocumentDTO;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentService {
    Mono<DocumentDTO> manageDocument(DocumentCreateRequest request, ClientDetailDTO clientDetailDTO);
    Mono<DocumentDTO> getById(String documentId);
    Flux<DocumentDTO> getAllDocumentByCollectionId(String collectionId);
    Mono<String> deleteDocument(String documentId);

}
