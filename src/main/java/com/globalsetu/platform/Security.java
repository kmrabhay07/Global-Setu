package com.globalsetu.platform;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Configuration
class Security {
    static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain chain(HttpSecurity http, Sessions sessions, Accounts accounts) throws Exception {
        var filter = new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                String header=request.getHeader("Authorization");
                if(header!=null && header.startsWith("Bearer ") && header.length()<200) {
                    sessions.findById(digest(header.substring(7))).filter(s->s.expiresAt.isAfter(Instant.now())).flatMap(s->accounts.findById(s.accountId)).filter(a->a.active).ifPresent(a -> SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(a,null,List.of(new SimpleGrantedAuthority("ROLE_"+a.role)))));
                }
                chain.doFilter(request,response);
            }
        };
        // Authentication uses explicit bearer tokens, never ambient cookies.
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' blob: data:; script-src 'self'; connect-src 'self'; frame-ancestors 'none'")))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/api/auth/register","/api/auth/otp/**","/api/catalog/**").permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->s.sendError(401,"Authentication required")))
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
