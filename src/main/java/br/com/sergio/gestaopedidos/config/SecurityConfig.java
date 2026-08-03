package br.com.sergio.gestaopedidos.config;

import br.com.sergio.gestaopedidos.security.JwtAuthenticationFilter;
import br.com.sergio.gestaopedidos.security.handler.CustomAccessDeniedHandler;
import br.com.sergio.gestaopedidos.security.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    /*
     * Segurança da API REST:
     * - utiliza JWT;
     * - não utiliza sessão;
     * - retorna erros em formato JSON.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http

                .securityMatcher(
                        "/api/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                )

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/login",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/api/usuarios/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/clientes/**",
                                "/api/produtos/**",
                                "/api/pedidos/**"
                        )
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    /*
     * Segurança da interface web:
     * - utiliza formulário de login;
     * - utiliza sessão;
     * - protege as páginas Thymeleaf.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/uploads/logos/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers("/usuarios/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/relatorios/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/producoes/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/insumos/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/compras/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/estoque/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/configuracoes/**")
                        .hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }
}
