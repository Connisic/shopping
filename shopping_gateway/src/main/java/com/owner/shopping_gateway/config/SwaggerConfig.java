package com.owner.shopping_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public RouteLocator swaggerRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("user-service-swagger", r -> r.path("/user-service/v3/api-docs")
						.filters(f -> f.rewritePath("/user-service/(?<segment>.*)", "/${segment}"))
						.uri("lb://user-service"))
				.route("product-service-swagger", r -> r.path("/product-service/v3/api-docs")
						.filters(f -> f.rewritePath("/product-service/(?<segment>.*)", "/${segment}"))
						.uri("lb://product-service"))
				.route("order-service-swagger", r -> r.path("/order-service/v3/api-docs")
						.filters(f -> f.rewritePath("/order-service/(?<segment>.*)", "/${segment}"))
						.uri("lb://order-service"))
				.build();
	}
} 