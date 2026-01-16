package yenha.foodstore.Auth.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import yenha.foodstore.Auth.Service.TokenBlacklistService;

import java.io.IOException;
    @Component
    @Slf4j
    @RequiredArgsConstructor
    public class AuthFilter extends OncePerRequestFilter {
        private final JwtUtils jwtUtils;
        private final CustomUserDetailsService customUserDetailsService;
        private final TokenBlacklistService tokenBlacklistService;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            String token = getTokenFromRequest(request);

            if (token != null) {
                try {
                    if (tokenBlacklistService.isTokenBlacklisted(token)) {
                        log.warn("Token is blacklisted");
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    String phoneNumber = jwtUtils.getUsernameFromToken(token);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(phoneNumber);

                    if (StringUtils.hasText(phoneNumber) && jwtUtils.isTokenValid(token, userDetails)) {
                        log.info("Token is valid for phone number: {}", phoneNumber);

                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                } catch (Exception e) {
                    log.error("Error validating token: {}", e.getMessage());
                }
            }

            try {
                filterChain.doFilter(request, response);

            } catch (Exception e) {
                log.error("Error occurred in AuthFilter: {}", e.getMessage());
            }

        }

        private String getTokenFromRequest(HttpServletRequest request) {
            String tokenWithBearer = request.getHeader("Authorization");
            if (tokenWithBearer != null && tokenWithBearer.startsWith("Bearer ")) {
                return tokenWithBearer.substring(7);
            }
            return null;
        }
    }


