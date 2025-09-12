package io.github.flexitech_realtime_provider.realtime_service.controllers.collection;

import io.github.flexitech_realtime_provider.common.api.request.collection.CreateCollectionRequest;
import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.api.response.stream.StreamResponse;
import io.github.flexitech_realtime_provider.common.dtos.collections.CollectionDTO;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.common.utils.CommonUtil;
import io.github.flexitech_realtime_provider.common.utils.validation.CommonValidator;
import io.github.flexitech_realtime_provider.common.utils.validation.ValidationUtil;
import io.github.flexitech_realtime_provider.realtime_service.services.collections.CollectionService;
import io.github.flexitech_realtime_provider.realtime_service.services.collections.CollectionStreamService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/realtime/collections")
@Slf4j
public class CollectionController {

    private final CollectionService collectionService;
    private final CollectionStreamService collectionStreamService;

    public CollectionController(CollectionService collectionService, CollectionStreamService collectionStreamService) {
        this.collectionService = collectionService;
        this.collectionStreamService = collectionStreamService;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Object>>> createDocument(
            @Valid @RequestBody CreateCollectionRequest request
    ) {
        log.info("Creating collection...");

        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMap(clientDetails ->
                        collectionService.createCollection(
                                        request,
                                        clientDetails.userId(),
                                        clientDetails.moduleId()
                                )
                                .map(saved -> ApiResponse.success((Object) saved, "Document created successfully"))
                )
                .onErrorResume(AuthenticationCredentialsNotFoundException.class, ex ->
                        Mono.just(ApiResponse.unauthorized(null, "Authentication required"))
                )
                .onErrorResume(WebExchangeBindException.class, ex ->
                        Mono.just(ApiResponse.badRequest(CommonUtil.collectFieldErrors(ex), "Please check all fields!"))
                )
                .onErrorResume(Exception.class, ex -> {
                    log.error("Error creating collection", ex);
                    return Mono.just(ApiResponse.internalServerError(null, ex.getMessage()));
                });
    }

    @GetMapping
    public Mono<ResponseEntity<? extends ApiResponse<?>>> getDocuments(
            @RequestParam(required = false) String id
    ) {
        log.info("Getting collections...");
        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMap(clientDetails -> {
                    if (CommonValidator.validString(id)) {
                        return collectionService.getCollectionById(id, clientDetails.userId(), clientDetails.moduleId())
                                .map(collection -> ApiResponse.success(collection, "Document retrieved"));
                    }
                    return collectionService.getAllCollectionsByUser(clientDetails.userId(), clientDetails.moduleId())
                            .collectList()
                            .map(collections -> ApiResponse.success(collections, "All user collections"));
                })
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamResponse<CollectionDTO>>> streamDocuments() {
        // Create a heartbeat flux that emits every 30 seconds
        Flux<ServerSentEvent<StreamResponse<CollectionDTO>>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<StreamResponse<CollectionDTO>>builder()
                        .event("heartbeat")
                        .data(null)
                        .build());

        return CommonUtil.getReactiveAuthUser(ClientDetailDTO.class)
                .flatMapMany(clientDetails ->
                        collectionStreamService.stream(clientDetails.userId(), clientDetails.moduleId())
                                .mergeWith(heartbeat) // Merge with heartbeat stream
                )
                .onErrorResume(AuthenticationCredentialsNotFoundException.class, ex -> {
                    log.warn("Unauthorized access attempt to collection stream");
                    return Flux.just(ServerSentEvent.<StreamResponse<CollectionDTO>>builder()
                            .event("error")
                            .data(null)
                            .build());
                });
    }

}
