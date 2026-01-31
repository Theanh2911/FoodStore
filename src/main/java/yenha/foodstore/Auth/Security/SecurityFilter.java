package yenha.foodstore.Auth.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import yenha.foodstore.Auth.Exceptions.CustomAccessDeniedHandler;
import yenha.foodstore.Auth.Exceptions.CustomAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityFilter {
        private final AuthFilter authFilter;
        private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;
        private final CorsConfig corsConfig;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

                httpSecurity
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                        .requestMatchers(HttpMethod.POST, "/api/session/**").permitAll()

                                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/client-register").permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                                        .requestMatchers(HttpMethod.PUT, "/api/auth/update-password").authenticated()

                                        .requestMatchers(HttpMethod.GET, "/api/auth/logout").permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/payment/webhook/**").permitAll()

                                        .requestMatchers(HttpMethod.GET, "/api/menu/categories/**").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/menu/products/**").permitAll()

                                        .requestMatchers(HttpMethod.GET, "/api/banks/active").permitAll()

                                        .requestMatchers(HttpMethod.POST, "/api/orders/create").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/orders/{orderId}").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/orders/{orderId}/stream").permitAll()

                                        .requestMatchers(HttpMethod.GET,"api/payment/events/**").permitAll()
                                        
                                        // Inventory endpoints - MUST BE BEFORE other rules
                                        .requestMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/inventory/**").permitAll()
                                        .requestMatchers(HttpMethod.PUT, "/api/inventory/**").permitAll()
                                        
                                        .requestMatchers(HttpMethod.GET, "/api/orders/stream")
                                        .hasAnyRole("ADMIN", "STAFF")

                                        .requestMatchers("/api/menu/**").hasRole("ADMIN")
                                        .requestMatchers("/api/auth/admin-register").hasRole("ADMIN")

                                        .requestMatchers("/api/orders/**").hasAnyRole("ADMIN", "STAFF")
                                        .requestMatchers("/api/ai/suggestion/**").permitAll()

                                        .requestMatchers(HttpMethod.POST, "/api/ratings/**").hasRole("CLIENT")
                                        .requestMatchers(HttpMethod.GET, "/api/ratings").hasRole("ADMIN")

                                        .anyRequest().authenticated())

                                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(customAuthenticationEntryPoint)
                                                .accessDeniedHandler(customAccessDeniedHandler))

                                .headers(headers -> headers
                                                .contentTypeOptions(contentTypeOptions -> {
                                                })
                                                .frameOptions(frameOptions -> frameOptions.deny())
                                                .xssProtection(xss -> xss
                                                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                );

                return httpSecurity.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

}
