package io.github.flexitech_realtime_provider.realtime_service.controllers.auth;

import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.api.response.auth.AuthInfoResponse;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/realtime")
@Slf4j
public class AuthController {

    @GetMapping("/authorized-info")
    public Mono<ResponseEntity<ApiResponse<AuthInfoResponse>>> authorizedInfo(Authentication authentication) {
        log.info("Getting auth info...");
        ClientDetailDTO userDetails = (ClientDetailDTO) authentication.getPrincipal();

        AuthInfoResponse response = new AuthInfoResponse(
                userDetails.username(),
                userDetails.scopes()
        );
        log.info("Getting authorized user success!");
        return Mono.just(ApiResponse.success(response, "Authorization info retrieved"));
    }
}
