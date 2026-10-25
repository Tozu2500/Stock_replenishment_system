package com.example.stockreplenishment.security;

import com.example.stockreplenishment.repository.AppUserRepository;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Rule of thumb: reading (GET) is allowed for every logged-in user, changing data needs the ADMIN role.
 * The REST API and the web pages have separate filter chains because they log in differently.
 */
@Configuration
public class SecurityConfig {
    /** REST API: HTTP Basic login on every request (e.g. from Postman), no session and no CSRF token. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /** Web pages: form login with a session. Forms are protected with CSRF tokens (added by Thymeleaf). */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/error").permitAll()
                        .requestMatchers("/h2-console/**", "/*/new", "/*/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form.defaultSuccessUrl("/", true)) // Spring Security's built-in login page
                .logout(Customizer.withDefaults())
                // The H2 console (demo profile) posts its own forms without CSRF tokens and runs in frames
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** Loads a user (with the BCrypt password hash and role) from the database. */
    @Bean
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername()).password(user.getPasswordHash())
                        .roles(user.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
