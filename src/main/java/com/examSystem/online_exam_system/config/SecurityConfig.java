package com.examSystem.online_exam_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration tells Spring Boot this class contains configuration
// @EnableWebSecurity enables Spring Security for the app
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // disable Spring Security's built-in CSRF for now
                // since we're using session-based auth ourselves
                .csrf(csrf -> csrf.disable())

                // allow ALL requests through Spring Security
                // we handle our own access control in controllers via SessionUtils
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // disable Spring Security's default login page
                // since we have our own login page
                .formLogin(form -> form.disable())

                // disable Spring Security's default logout
                // since we handle logout ourselves in UserController
                .logout(logout -> logout.disable())

                // allow H2 console to load in iframe
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                );

        return http.build();
    }
}