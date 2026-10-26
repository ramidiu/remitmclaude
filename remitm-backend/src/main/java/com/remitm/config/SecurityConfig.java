package com.remitm.config;

import com.remitm.security.JwtAuthenticationFilter;
import com.remitm.security.RemitmPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
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
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RemitmPermissionEvaluator permissionEvaluator;

    @Value("${app.frontend-url:https://remitm.com}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // CORS origins are intentionally DECOUPLED from app.frontend-url: frontend-url drives
        // email links/logo (a public host like www.remitm.com), but the browser origin calling
        // this API can be the Docker frontend on :8096 or a local dev server. Listing frontendUrl
        // alone broke login the moment frontend-url was pointed at the prod domain.
        config.setAllowedOrigins(List.of(
            frontendUrl,
            "https://remitm.com",
            "https://www.remitm.com",
            "https://global.remitm.com",
            "https://sendmoney.remitm.com",   // live customer domain
            "https://remitm.remitz.co.uk",    // legacy domain (kept for safety)
            "http://localhost:8096",   // Docker frontend (remitm stack)
            "http://localhost:8100",
            "http://localhost:4200",
            "http://localhost",
            "https://localhost",       // Capacitor Android (androidScheme=https)
            "capacitor://localhost",   // Capacitor iOS
            "ionic://localhost"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // Public account deletion (Google Play policy) — OTP-verified, no login
                        .requestMatchers("/api/account/public/**").permitAll()
                        // Public customer-app configuration flags (feature toggles + timings)
                        .requestMatchers("/api/public/**").permitAll()
                        // PayIn endpoints
                        .requestMatchers("/api/payin/**").authenticated()
                        // Internal service-to-service endpoints — require authentication;
                        // controllers additionally restrict these to staff via @PreAuthorize.
                        .requestMatchers("/internal/**").authenticated()
                        // FX public endpoints
                        .requestMatchers("/api/fx/rates").permitAll()
                        .requestMatchers("/api/fx/rates/**").permitAll()
                        .requestMatchers("/api/fx/quote").permitAll()
                        // /api/fx/corridors/auto-create is NOT public — it creates corridors.
                        // Now authenticated + config:manage_corridors (see CorridorController).
                        .requestMatchers("/api/corridors").permitAll()
                        .requestMatchers("/api/corridors/*/delivery-methods").permitAll()
                        .requestMatchers("/api/corridors/*/limits").permitAll()
                        // Transaction public config endpoints
                        .requestMatchers("/api/transactions/config/active-countries").permitAll()
                        .requestMatchers("/api/transactions/config/active-receive-countries").permitAll()
                        .requestMatchers("/api/transactions/config/payment-methods").permitAll()
                        .requestMatchers("/api/transactions/config/payout-types").permitAll()
                        .requestMatchers("/api/transactions/banks/config/**").permitAll()
                        .requestMatchers("/api/transactions/banks/list/**").permitAll()
                        .requestMatchers("/api/transactions/banks/full/**").permitAll()
                        .requestMatchers("/api/transactions/banks/search/**").permitAll()
                        .requestMatchers("/api/transactions/lookup/mobile-services").permitAll()
                        .requestMatchers("/api/transactions/lookup/cash-points").permitAll()
                        // User public endpoints
                        // NOTE: GET /api/users/{uuid} is intentionally NOT public — it returns full
                        // profile PII. It is now authenticated + ownership-checked in UserController.
                        .requestMatchers("/api/users/kyc/**").permitAll()
                        // KYC document file GET is authenticated + ownership-checked (KycController).
                        // <img>/<iframe> previews pass the JWT via ?token= (JwtAuthenticationFilter).
                        .requestMatchers("/api/referrals/validate/**").permitAll()
                        // on-completed + wallet credit/debit are NOT public. They were
                        // service-to-service remnants with no external caller; now authenticated
                        // (wallet credit/debit additionally require wallet:manage — see WalletController).
                        // Support attachment file GET is authenticated + ownership-checked
                        // (SupportTicketController); previews pass the JWT via ?token=.
                        // Swagger / actuator
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-config").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Trust Payments callback — called by frontend after card payment redirect
                        .requestMatchers("/api/trust-payment/**").permitAll()
                        // Fire open banking — return-from-bank update + public bank list
                        .requestMatchers("/api/fire/update-payment").permitAll()
                        .requestMatchers("/api/fire/aspsps").permitAll()
                        // Nsano payout — inbound callback from Nsano servers (no JWT)
                        .requestMatchers("/nsano/callback").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        // X-Frame-Options: DENY (Spring default) blocks the KYC document <iframe>
                        // preview when the admin UI and API are on different origins (local :8098 →
                        // :8097, or split prod domains). Replace the blanket DENY with a CSP
                        // frame-ancestors allow-list so only trusted frontends can embed /file docs.
                        // <img> previews were never affected (only iframes obey X-Frame-Options),
                        // which is why images showed but PDFs failed with "refused to connect".
                        .frameOptions(frame -> frame.disable())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "frame-ancestors 'self' http://localhost:8098 http://localhost:8096 "
                                + "http://localhost:8100 http://localhost:4200 "
                                + "https://remitm.com https://www.remitm.com https://sendmoney.remitm.com "
                                + "https://global.remitm.com"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
