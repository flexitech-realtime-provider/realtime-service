package io.github.flexitech_realtime_provider.realtime_service.controllers.auth;

import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.api.response.auth.AuthInfoResponse;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/realtime")
public class AuthController {

    @GetMapping("/authorized-info")
    public ResponseEntity<ApiResponse<Object>> authorizedInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            // For JWT-based authentication with custom details
            ClientDetailDTO userDetails = (ClientDetailDTO) authentication.getPrincipal();

            // Build response DTO
            AuthInfoResponse response = new AuthInfoResponse(
                    userDetails.username(),
                    authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            );

            return ApiResponse.success(response, "Authorization info retrieved successfully");
        }
        return ApiResponse.unauthorized("No active authentication found");
    }
}
