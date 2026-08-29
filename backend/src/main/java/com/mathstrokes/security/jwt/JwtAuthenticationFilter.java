package com.mathstrokes.security.jwt;

import java.io.IOException;
import java.util.List;

import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.security.service.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer access token and populates the security context.
 *
 * The principal is rebuilt purely from token claims, so a request costs no database round trip.
 * A malformed or expired token is not an error here: the context is simply left empty and the
 * entry point produces the 401, which keeps anonymous endpoints reachable.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token, TokenType.ACCESS);
                UserPrincipal principal = toPrincipal(claims);
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ApiException ex) {
                SecurityContextHolder.clearContext();
                logger.debug("Rejected bearer token: " + ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private UserPrincipal toPrincipal(Claims claims) {
        Long userId = claims.get(JwtService.CLAIM_USER_ID, Number.class).longValue();
        List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class) == null
                ? List.of()
                : (List<String>) claims.get(JwtService.CLAIM_ROLES, List.class);
        return new UserPrincipal(userId, claims.getSubject(), null, null, true, roles);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String value = header.substring(BEARER_PREFIX.length()).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}
