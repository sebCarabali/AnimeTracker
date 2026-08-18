package com.animetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.animetracker.auth.AniListOAuth2UserService;
import com.animetracker.auth.OAuthLoginSuccessHandler;

/**
 * Única fuente de verdad de la configuración de seguridad de la app.
 * Rutas públicas: /login y estáticos. Todo lo demás requiere autenticación
 * OAuth2 con AniList. El login-page y el failure-url apuntan a /login para
 * que la falla transitoria de OAuth se resuelva en la misma pantalla.
 */
@Configuration
public class SecurityConfig {

    private final AniListOAuth2UserService aniListOAuth2UserService;
    private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;

    public SecurityConfig(AniListOAuth2UserService aniListOAuth2UserService,
            OAuthLoginSuccessHandler oAuthLoginSuccessHandler) {
        this.aniListOAuth2UserService = aniListOAuth2UserService;
        this.oAuthLoginSuccessHandler = oAuthLoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/login/**", "/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .successHandler(oAuthLoginSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userService(aniListOAuth2UserService)));

        return http.build();
    }
}
