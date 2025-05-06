package io.github.flexitech_realtime_provider.realtime_service.clients.external;

import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "auth-service",
        url = "${external-services.auth-service.url}"
)
public interface AuthServiceClient {
    @GetMapping("/oauth2/v1/token")
    ResponseEntity<ApiResponse<Object>> getTokenInfo(@RequestParam String token);
}
