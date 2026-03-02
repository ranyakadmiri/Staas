package com.example.demo.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {



    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /*  @Override
      protected void doFilterInternal( @NonNull HttpServletRequest request,
                                       @NonNull HttpServletResponse response,
                                       @NonNull FilterChain filterChain) throws ServletException, IOException {
          String header = request.getHeader("Authorization");
          if (header != null && header.startsWith("Bearer ")) {
              String token = header.substring(7);
              if (jwtUtils.isTokenValid(token)) {
                  String email = jwtUtils.extractUsername(token);
                  UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                  SecurityContextHolder.getContext().setAuthentication(
                          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
                  );
              }
          }
          filterChain.doFilter(request, response);
      }*/
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // ✅ Debug: Log the actual path
        System.out.println("Request URI: " + path);
        System.out.println("Context Path: " + request.getContextPath());
        System.out.println("Servlet Path: " + request.getServletPath());

        // ✅ Check for actuator endpoints (with or without context path)
        if (path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtils.isTokenValid(token)) {
                String email = jwtUtils.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
                );
            }
        }
        filterChain.doFilter(request, response);
    }
}

