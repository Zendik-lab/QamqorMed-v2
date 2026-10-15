package com.qamqormed.config;

import com.qamqormed.security.JsonAccessDeniedHandler;
import com.qamqormed.security.JsonAuthenticationEntryPoint;
import com.qamqormed.security.CustomUserDetailsService;
import com.qamqormed.security.JwtAuthenticationFilter;
import com.qamqormed.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfig corsConfig,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/api/auth/login", "/api/auth/register").permitAll()
                        // The role-switching/catalog endpoints remain available in dev so the
                        // development UI can bootstrap, but appointment inventory and workflow
                        // actions expose cross-patient data and require a signed ADMIN session.
                        .requestMatchers("/api/dev/appointments/**", "/api/dev/notification-workflow/**", "/api/dev/audit").hasRole("ADMIN")
                        .requestMatchers("/api/dev/users/search", "/api/dev/login/**", "/api/dev/filters/**").permitAll()
                        .requestMatchers("/api/dev/users/*/relation").hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                        .requestMatchers("/api/dev/**").denyAll()
                        .requestMatchers("/api/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/patient-profiles", "/api/patients/**").hasRole("PATIENT")
                        .requestMatchers("/api/notifications/**", "/api/appointments/**").authenticated()
                        .requestMatchers("/api/ai/**").authenticated()
                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/nurse/**").hasRole("NURSE")
                        .requestMatchers("/api/doctors/me/**").hasRole("DOCTOR")
                        .requestMatchers("/api/nurses/me/**").hasRole("NURSE")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/icd10/**").hasAnyRole("DOCTOR", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
