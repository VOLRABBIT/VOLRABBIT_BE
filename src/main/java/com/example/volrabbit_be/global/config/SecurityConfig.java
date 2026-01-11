package com.example.volrabbit_be.global.config;

import com.example.volrabbit_be.global.jwt.JwtFilter;
import com.example.volrabbit_be.global.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF 해제 (REST API)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청 URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 1. 누구나 접근 가능 (회원가입, 재발급, Swagger)
                        .requestMatchers(
                                "/api/users/guest",
                                "/api/users/reissue",
                                "/api/users/nickname/check",
                                "/api/auth/**",

                                // Swagger 관련 경로 허용
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**"
                        ).permitAll()

                        // 2. 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    // CORS 허용 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 프론트엔드 도메인 (개발 중에는 "*" 또는 localhost 설정)
        // 실제 배포 시에는 "http://my-domain.com" 처럼 구체적으로 지정
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 2. 허용할 HTTP 메서드 (GET, POST, PUT, DELETE 등)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. 허용할 헤더 (Authorization, Content-Type 등)
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 자격 증명 허용 (쿠키, 인증 헤더 등을 포함한 요청 허용 시 true)
        configuration.setAllowCredentials(true);

        // 5. 브라우저에 노출할 헤더 (클라이언트에서 Authorization 헤더를 읽을 수 있게 함)
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}