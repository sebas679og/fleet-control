package com.fleet.control.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fleet.control.gateway.constants.GatewayErrorCodes;
import com.fleet.control.gateway.constants.GatewayMessages;
import com.fleet.control.gateway.dto.ErrorResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final Cache<String, List<Long>> requestLog =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofSeconds(60))
                    .maximumSize(10_000)
                    .build();
    private final int limit;
    private static final long WINDOW_SECONDS = 60;
    private static final long WINDOW_MS = WINDOW_SECONDS * 1000;
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public RateLimitFilter(@Value("${gateway.rate-limit.per-minute:60}") int limit) {
        this.limit = limit;
    }



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = getClientIp(exchange);
        long now = System.currentTimeMillis();

        List<Long> requests = requestLog.get(ip, k -> new ArrayList<>());
        synchronized (requests) {
            requests.removeIf(ts -> now - ts > WINDOW_MS);

            if (requests.size() >= limit) {
                long oldestTimestamp = requests.get(0);
                long retryAfter = Math.max(1, (oldestTimestamp + WINDOW_MS - now) / 1000);

                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfter));
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                ErrorResponse body =
                        ErrorResponse.builder()
                                .error(GatewayErrorCodes.RATE_LIMIT_EXCEEDED)
                                .message(String.format(GatewayMessages.RATE_LIMIT_EXCEEDED, limit))
                                .retryAfter(retryAfter)
                                .timestamp(java.time.Instant.now())
                                .build();
                byte[] bytes;
                try {
                    bytes = objectMapper.writeValueAsBytes(body);
                } catch (JsonProcessingException e) {
                    return exchange.getResponse().setComplete();
                }
                return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }

            requests.add(now);
        }

        return chain.filter(exchange);
    }

    private String getClientIp(ServerWebExchange exchange) {
        var forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remote = exchange.getRequest().getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -1;
    }


}
