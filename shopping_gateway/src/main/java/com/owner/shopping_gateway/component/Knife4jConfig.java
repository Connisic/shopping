package com.owner.shopping_gateway.component;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

@Configuration
public class Knife4jConfig {
	@Bean
	public RouterFunction<ServerResponse> knife4jRoutes() {
		return RouterFunctions.route(
				RequestPredicates.GET("/v3/api-docs"),
				request -> ServerResponse.temporaryRedirect(URI.create("/v3/api-docs/default")).build()
		);
	}
}
