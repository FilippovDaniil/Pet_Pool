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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Admin-only: staff management
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Admin-only: table management (write operations)
                .requestMatchers(HttpMethod.POST, "/tables").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/tables/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/tables/**").hasRole("ADMIN")
                .requestMatchers("/tables/add", "/tables/*/edit").hasRole("ADMIN")
                // Admin-only: client management (create/delete)
                .requestMatchers(HttpMethod.POST, "/clients").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/clients/**").hasRole("ADMIN")
                .requestMatchers("/clients/add").hasRole("ADMIN")
                // Admin-only: tournament record moderation
                .requestMatchers(HttpMethod.DELETE, "/tournament/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/tournament/**").hasRole("ADMIN")
                .requestMatchers("/tournament/record/*/edit").hasRole("ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
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
