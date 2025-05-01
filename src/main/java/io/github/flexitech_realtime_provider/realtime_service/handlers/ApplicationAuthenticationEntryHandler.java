package io.github.flexitech_realtime_provider.realtime_service.handlers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ApplicationAuthenticationEntryHandler implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.error("Unauthorized error: {}", authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String errorResponse = """
            {
              "success": false,
              "status": 401,
              "message": "%s",
              "path": "%s",
              "timestamp": "%s"
            }
            """.formatted(
                authException.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        response.getWriter().write(errorResponse);
    }
}
