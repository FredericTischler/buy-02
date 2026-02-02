package com.ecommerce.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        jwtAuthenticationFilter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(securityConfig, "jwtAuthenticationFilter", jwtAuthenticationFilter);
    }

    @Test
    void securityFilterChain_shouldContainJwtFilter() throws Exception {
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsProperty", "*");

        SecurityFilterChain chain = securityConfig.securityFilterChain(createHttpSecurity());

        assertThat(chain.getFilters()).anyMatch(filter -> filter == jwtAuthenticationFilter);
    }

    @Test
    void corsConfigurationSource_shouldParseOrigins() {
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsProperty", "https://app.com, https://admin.com");

        CorsConfiguration configuration = resolveCorsConfiguration();

        assertThat(configuration.getAllowedOrigins()).containsExactly("https://app.com", "https://admin.com");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "DELETE", "PUT", "OPTIONS");
    }

    @Test
    void passwordEncoder_shouldHashPasswords() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String hashed = encoder.encode("password123");

        assertThat(encoder.matches("password123", hashed)).isTrue();
    }

    private HttpSecurity createHttpSecurity() throws Exception {
        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        HashMap<Class<?>, Object> sharedObjects = new HashMap<>();
        ApplicationContext context = new StaticApplicationContext();
        sharedObjects.put(ApplicationContext.class, context);
        return new HttpSecurity(objectPostProcessor, authBuilder, sharedObjects);
    }

    private CorsConfiguration resolveCorsConfiguration() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        return source.getCorsConfiguration(new MockHttpServletRequest());
    }
}
