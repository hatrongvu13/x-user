package com.htv.xuser.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * AppConfig — Bean cấu hình chung
 * <p>
 * Scope hiện tại (không có Redis / Kafka):
 * - Jackson  (ObjectMapper với JavaTimeModule)
 * - i18n     (MessageSource, LocaleResolver)
 * - JPA      (AuditorAware cho @CreatedBy / @LastModifiedBy)
 * - CORS     (CorsConfigurationSource)
 * <p>
 * TODO khi thêm Redis:
 *   - Uncomment bean StringRedisTemplate, RedisCacheManager
 *   - Thêm @EnableCaching
 *   - Thay TokenStore in-memory bằng Redis implementation
 * <p>
 * TODO khi thêm Kafka:
 *   - Tạo KafkaEventPublisher bean
 *   - Inject vào AuthService để publish UserRegistered, PasswordChanged...
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GlobalConfig {


    private final UserDetailsService userDetailsService;

    // =========================================================================
    // i18n
    // =========================================================================

    @Bean
    public MessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.forLanguageTag("vi")
        ));
        return resolver;
    }

    // =========================================================================
    //  PASSWORD ENCODER
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength 12 ≈ 250ms/hash — đủ chậm để chống brute-force
        return new BCryptPasswordEncoder(12);
    }

    // =========================================================================
    // CORS
    // =========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProps) {
        var config = new CorsConfiguration();

        // Origins từ config
        List<String> origins = Arrays.asList(
                securityProps.getAllowedOrigins().split(",")
        );
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // =========================================================================
    // EVENT PUBLISHER MOCK (TODO: thay bằng Kafka khi sẵn sàng)
    // =========================================================================

    /**
     * Mock EventPublisher — log ra console.
     * Khi tích hợp Kafka: inject KafkaTemplate và publish lên topic tương ứng.
     *
     * Cách dùng trong service:
     * <pre>
     *   eventPublisher.publish("xsystem.user.registered", payload);
     * </pre>
     */
    @Bean
    public EventPublisher eventPublisher() {
        return (topic, payload) ->
                log.info("[EVENT] topic={} payload={}", topic, payload);
    }

    /**
     * Functional interface cho event publishing.
     * Production: implement với KafkaTemplate.
     */
    @FunctionalInterface
    public interface EventPublisher {
        /**
         * Publish một domain event.
         *
         * @param topic   Kafka topic (VD: "xsystem.user.registered")
         * @param payload Object sẽ được serialize thành JSON
         */
        void publish(String topic, Object payload);
    }
}
