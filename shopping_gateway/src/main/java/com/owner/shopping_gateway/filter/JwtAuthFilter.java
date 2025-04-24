package com.owner.shopping_gateway.filter;

import com.owner.shopping_gateway.utils.JwtUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.ignore-paths}")
    private String ignorePaths;

    private List<String> whiteList;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 初始化白名单
        if (whiteList == null) {
            whiteList = Arrays.asList(ignorePaths.split(","));
        }

        // 白名单路径直接放行
        if (isWhiteListPath(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange);
        }
        token=token.replace("Bearer ", "");
        try {
            Map<String, Object> verify = JwtUtils.verify(token);
            // 添加用户信息到请求头
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("userId", verify.get("userId").toString())
                    .header("username", verify.get("username").toString())
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private boolean isWhiteListPath(String path) {
        // 检查是否是Swagger/Knife4j相关路径
        if (path.contains("/v3/api-docs") || 
            path.contains("/doc.html") || 
            path.contains("/webjars/") || 
            path.contains("/swagger-resources") ||
            path.contains("/swagger-ui")) {
            return true;
        }

        // 检查其他白名单路径
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory()
                .wrap("{\"code\":401,\"msg\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

