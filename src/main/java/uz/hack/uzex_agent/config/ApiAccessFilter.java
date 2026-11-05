package uz.hack.uzex_agent.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiAccessFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final int MAX_TRACKED_CLIENTS = 4096;

    private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Value("${app.api-token:}")
    private String apiToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (apiToken == null || apiToken.isBlank()) {
            writeError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "API access is not configured");
            return;
        }

        String authorization = request.getHeader("Authorization");
        String suppliedToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : "";
        if (!MessageDigest.isEqual(
                apiToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        long now = System.currentTimeMillis();
        String client = request.getHeader("CF-Connecting-IP");
        if (client == null || client.isBlank()) {
            client = request.getRemoteAddr();
        }
        AtomicBoolean limited = new AtomicBoolean(false);
        requestWindows.compute(client, (key, window) -> {
            if (window == null || now - window.startedAt >= WINDOW_MS) {
                return new RequestWindow(now, 1);
            }
            if (window.count >= MAX_REQUESTS_PER_WINDOW) {
                limited.set(true);
                return window;
            }
            window.count++;
            return window;
        });

        if (requestWindows.size() > MAX_TRACKED_CLIENTS) {
            requestWindows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MS * 2);
        }
        if (limited.get()) {
            response.setHeader("Retry-After", "60");
            writeError(request, response, 429, "Too many requests");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            for (String allowedOrigin : allowedOrigins) {
                if (origin.equals(allowedOrigin.trim())) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    response.addHeader("Vary", "Origin");
                    break;
                }
            }
        }
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private static final class RequestWindow {
        private final long startedAt;
        private int count;

        private RequestWindow(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}