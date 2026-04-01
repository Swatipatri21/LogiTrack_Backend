package com.deliverytracking.config;

import com.deliverytracking.security.AccessDeniedHandlerJwt;
import com.deliverytracking.security.AuthEntryPointJwt;
import com.deliverytracking.security.JwtAuthFilter;
import com.deliverytracking.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private AuthEntryPointJwt authEntryPointJwt;

    @Autowired
    private AccessDeniedHandlerJwt accessDeniedHandlerJwt;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public AuthenticationManager authenticationManager() {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder());
//        provider.setUserDetailsService(userDetailsService);
//        return new ProviderManager(provider);
//    }
    
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(); // Use no-args constructor
        provider.setPasswordEncoder(passwordEncoder()); // Set encoder via setter
        provider.setUserDetailsService(userDetailsService);
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(authEntryPointJwt)
                    .accessDeniedHandler(accessDeniedHandlerJwt)
                )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers( "/api/auth/**",
                        "/api/login",
                        "/api/register",
                        "/error",
                        "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/track/**").permitAll()
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/actuator/**"
                ).permitAll()
                .requestMatchers("/favicon.ico", "/static/**", "/index.html").permitAll()
                .requestMatchers("/api/shipments/**").authenticated()       // ← was ADMIN, STAFF only
                .requestMatchers("/api/delivery/**").authenticated()        // ← was ADMIN, STAFF only
//                .requestMatchers("/api/history/**").hasRole("ADMIN")
                .requestMatchers("/api/history/**").hasAnyRole("ADMIN", "HUB_MANAGER")
                .requestMatchers(HttpMethod.GET,    "/api/users/**").authenticated()  // ← was ADMIN only
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasanyRole("ADMIN","HUB_MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasanyRole("ADMIN","HUB_MANAGER")
                .requestMatchers(HttpMethod.PATCH,  "/api/users/**").authenticated()  // @PreAuthorize handles it
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
}

