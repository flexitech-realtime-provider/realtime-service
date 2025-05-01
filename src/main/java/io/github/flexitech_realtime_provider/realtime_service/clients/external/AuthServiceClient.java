package io.github.flexitech_realtime_provider.realtime_service.clients.external;

import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "auth-service",
        url = "${external-services.auth-service.url}"
)
public interface AuthServiceClient {
    @GetMapping("/api/v1/auth/token")
    ResponseEntity<ApiResponse<Object>> tokenInfo(@RequestHeader("Authorization") String token);
}
