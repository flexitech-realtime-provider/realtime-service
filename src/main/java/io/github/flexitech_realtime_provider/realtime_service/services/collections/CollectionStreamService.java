package io.github.flexitech_realtime_provider.realtime_service.services.collections;

import io.github.flexitech_realtime_provider.common.api.response.stream.StreamResponse;
import io.github.flexitech_realtime_provider.common.dtos.collections.CollectionDTO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface CollectionStreamService {
    Flux<ServerSentEvent<StreamResponse<CollectionDTO>>> stream(String userId, String moduleId);
}
