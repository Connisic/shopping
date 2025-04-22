package com.owner.shopping_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayConfig {

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				// 用户API路由
				.route("user-api", r -> r.path("/api/user/**")
						.uri("lb://shopping-user-customer-api"))
				// 商品API路由
				.route("goods-api", r -> r.path("/api/goods/**")
						.uri("lb://shopping-goods-api"))
				// 订单API路由
				.route("order-api", r -> r.path("/api/order/**")
						.uri("lb://shopping-order-customer-api"))
				// 购物车API路由
				.route("cart-api", r -> r.path("/api/cart/**")
						.uri("lb://shopping-cart-customer-api"))
				// 搜索API路由
				.route("search-api", r -> r.path("/api/search/**")
						.uri("lb://shopping-search-customer-api"))
				// 分类API路由
				.route("category-api", r -> r.path("/api/category/**")
						.uri("lb://shopping-category-customer-api"))
				// 秒杀API路由
				.route("seckill-api", r -> r.path("/api/seckill/**")
						.uri("lb://shopping-seckill-customer-api"))
				// 推荐API路由
				.route("recommend-api", r -> r.path("/api/recommend/**")
						.uri("lb://shopping-recommend-api"))
				.build();
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return new CorsWebFilter(source);
	}
} 