package io.github.flexitech_realtime_provider.realtime_service.controllers.document;

import io.github.flexitech_realtime_provider.common.api.request.document.DocumentCreateRequest;
import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.dtos.document.DocumentDTO;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.common.utils.CommonUtil;
import io.github.flexitech_realtime_provider.common.utils.validation.CommonValidator;
import io.github.flexitech_realtime_provider.common.utils.validation.ValidationUtil;
import io.github.flexitech_realtime_provider.realtime_service.services.documents.DocumentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/collections/{collectionId}/documents")
@Slf4j
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public Mono<ResponseEntity<? extends ApiResponse<?>>> getDocuments(@PathVariable String collectionId, @RequestParam(required = false) String id) {
        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMap(client -> {
                    if (CommonValidator.validString(id)) {
                        return documentService.getById(id)
                                .map(doc -> {
                                    return ApiResponse.success(doc, "Get document success!");
                                });
                    } else {
                        return documentService.getAllDocumentByCollectionId(collectionId)
                                .collectList()
                                .map(docList -> {
                                    return ApiResponse.success(docList, "Get documents success.");
                                });
                    }
                }).onErrorResume(ex -> {
                    if (ex instanceof AuthenticationCredentialsNotFoundException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Authentication required"));
                    }
                    if (ex instanceof AccessDeniedException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Insufficient privileges"));
                    }
                    return Mono.just(ApiResponse.internalServerError(null, "Server error"));
                });
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Object>>> createDocument(
            @RequestBody @Valid DocumentCreateRequest request) {
        log.info("Creating document...");
        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMap(clientDetails -> {
                    return documentService.manageDocument(request, clientDetails)
                            .map(saved -> ApiResponse.success((Object)saved, "Document created successfully"));
                })
                .onErrorResume(WebExchangeBindException.class, ex ->
                        Mono.just(ApiResponse.badRequest(CommonUtil.collectFieldErrors(ex), "Please check all fields!"))
                )
                .onErrorResume(ex -> {
                    if (ex instanceof AuthenticationCredentialsNotFoundException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Authentication required"));
                    }
                    if (ex instanceof AccessDeniedException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Insufficient privileges"));
                    }
                    return Mono.just(ApiResponse.internalServerError(null, "Server error"));
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Object>>> updateDocument(@RequestBody @Valid DocumentCreateRequest request, @PathVariable String id){
        log.info("Updating document...");
        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMap(clientDetails -> {
                            request.setId(id);
                            return documentService.manageDocument(request, clientDetails)
                                    .map(saved -> ApiResponse.success((Object)saved, "Document created successfully"))
                                    .onErrorResume(ex ->
                                            Mono.just(ApiResponse.internalServerError(null, ex.getMessage())));
                        }

                )
                .onErrorResume(WebExchangeBindException.class, ex ->
                        Mono.just(ApiResponse.badRequest(CommonUtil.collectFieldErrors(ex), "Please check all fields!"))
                )
                .onErrorResume(ex -> {
                    if (ex instanceof AuthenticationCredentialsNotFoundException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Authentication required"));
                    }
                    if (ex instanceof AccessDeniedException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Insufficient privileges"));
                    }
                    return Mono.just(ApiResponse.internalServerError(null, "Server error"));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Object>>> deleteDocument(
            @PathVariable("id") String id) {
        log.info("Deleting document with id: {}", id);

        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMap(clientDetails ->
                        documentService.deleteDocument(id)
                                .map((deletedId)->(ApiResponse.success((Object) deletedId, "Document deleted successfully")))

                )
                .onErrorResume(ex -> {
                    if (ex instanceof AuthenticationCredentialsNotFoundException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Authentication required"));
                    }
                    if (ex instanceof AccessDeniedException) {
                        return Mono.just(ApiResponse.unauthorized(null, "Insufficient privileges"));
                    }
                    ex.printStackTrace();
                    return Mono.just(ApiResponse.internalServerError(null, "Server error"));
                });
    }

}
