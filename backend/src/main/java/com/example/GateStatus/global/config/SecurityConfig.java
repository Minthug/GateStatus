package com.example.GateStatus.global.config;

import com.example.GateStatus.domain.admin.GateAdmin;
import com.example.GateStatus.domain.admin.Role;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity // 메소드 레벨 보안을 위한 설정
public class SecurityConfig {

//    private static final String[] USER_ADMIN_AUTHORITIES = {"USER", "ADMIN"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/v1/figures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/v1/figures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/figures/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 조회는 모든 사용자 허용
                        .requestMatchers(HttpMethod.GET, "/v1/**").permitAll()
                        .requestMatchers("/","/index", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login-process")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/v1/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\": \"관리자 권한이 필요합니다\"}");
                            } else if (request.getRequestURI().startsWith("/admin/")) {
                                response.sendRedirect("/admin/login");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/v1/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\": \"관리자 권한이 필요합니다\"}");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
