package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**", "/vendor/**").permitAll()
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard/**", "/api/v1/dashboard/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/auditoria/**", "/api/auditoria/**", "/api/v1/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/pedidos/*/cancelar", "/api/pedidos/*/desconto",
                                         "/api/v1/pedidos/*/cancelar", "/api/v1/pedidos/*/desconto").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/pedidos/**", "/api/pedidos/**", "/api/v1/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/**", "/api/pedidos/**", "/api/v1/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/clientes/**", "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/produtos", "/api/produtos/**").authenticated()
                        .requestMatchers("/produtos/**", "/api/produtos/**").hasRole("GERENTE")
                        .anyRequest().authenticated()
                )
                // API REST sem sessão autenticada responde 401 (JSON) em vez de redirecionar para /login
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}
