package com.billiardclub.config;

import com.billiardclub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: use cookie-based token so JavaScript fetch() can read it.
            // REST clients read XSRF-TOKEN cookie and send X-XSRF-TOKEN header.
            // Thymeleaf forms continue to work with the hidden _csrf field automatically.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Disable CSRF entirely for /api/search/** (public read-only endpoint)
                .ignoringRequestMatchers(new AntPathRequestMatcher("/api/search/**"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // K8s health probes must be accessible without authentication
                .requestMatchers("/actuator/health").permitAll()

                // ── OpenSearch search (public) ──────────────────────────────
                .requestMatchers("/api/search/**").permitAll()

                // ── REST API: booking operations ────────────────────────────
                // GET list/detail — any authenticated user
                .requestMatchers(HttpMethod.GET,    "/api/bookings/**").authenticated()
                // POST create, PATCH status, POST payment — any authenticated user
                .requestMatchers(HttpMethod.POST,   "/api/bookings/**").authenticated()
                .requestMatchers(HttpMethod.PATCH,  "/api/bookings/**").authenticated()

                // ── REST API: tournament ────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/tournament/**").authenticated()
                .requestMatchers(HttpMethod.PUT,    "/api/tournament/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/tournament/**").hasRole("ADMIN")

                // ── UI: admin-only ──────────────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Table management write operations
                .requestMatchers(HttpMethod.POST,   "/tables").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/tables/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/tables/**").hasRole("ADMIN")
                .requestMatchers("/tables/add", "/tables/*/edit").hasRole("ADMIN")
                // Client management write operations
                .requestMatchers(HttpMethod.POST,   "/clients").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/clients/**").hasRole("ADMIN")
                .requestMatchers("/clients/add").hasRole("ADMIN")
                // Tournament UI: edit form (GET) — admin only
                .requestMatchers("/tournament/*/edit").hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            // REST API endpoints return 401 instead of redirecting to login page
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    (request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
