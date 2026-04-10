package com.gamepulse.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // /api/** 경로에 CORS 허용 설정 적용

                .allowedOrigins(
                        "http://localhost:3000",
                        // 로컬 개발 환경
                        "https://gamepulse.vbnmzxc.shop"
                        // 프로덕션 프론트엔드 도메인
                )

                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // OPTIONS는 브라우저가 실제 요청 전에 보내는 preflight 요청
                // 없으면 POST, PUT 같은 요청이 차단됨

                .allowedHeaders("*")
                // 모든 헤더 허용

                .maxAge(3600);
        // preflight 응답을 1시간 캐싱
        // 매 요청마다 OPTIONS 요청을 안 보내도 됨
    }
}