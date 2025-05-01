//package io.github.flexitech_realtime_provider.realtime_service.config;
//
//import feign.RequestInterceptor;
//import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//@Configuration
//public class OpenFeignConfiguration {
//    @Bean
//    public RequestInterceptor authInterceptor(){
//        return requestTemplate -> {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if (authentication != null && authentication.isAuthenticated()) {
//                String token = ((ClientDetailDTO) authentication.getPrincipal()).token();
//                requestTemplate.header("Authorization", "Bearer " + token);
//            }
//        };
//    }
//}
