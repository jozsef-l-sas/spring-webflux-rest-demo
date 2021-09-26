package com.jozsef.webflux;

import com.jozsef.webflux.handler.ProductHandler;
import com.jozsef.webflux.model.Product;
import com.jozsef.webflux.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@Slf4j
public class SpringWebfluxDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringWebfluxDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(ProductRepository repository) {
        return args -> {
            Flux<Product> productFlux = Flux.just(
                    Product.builder().name("Big Latte").price(2.99).build(),
                    Product.builder().name("Big Decaf").price(2.49).build(),
                    Product.builder().name("Green Tea").price(1.99).build()
                    ).flatMap(repository::insert);

            productFlux.thenMany(repository.findAll())
                       .subscribe(p -> log.info("Inserted product: {}", p));
        };
    }

    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {
//        return route()
//                .GET("/functional/products/events", accept(TEXT_EVENT_STREAM), handler::getProductEvents)
//                .GET("/functional/products/{id}", accept(APPLICATION_JSON), handler::getProduct)
//                .GET("/functional/products", accept(APPLICATION_JSON), handler::getAllProducts)
//                .PUT("/functional/products/{id}", accept(APPLICATION_JSON), handler::updateProduct)
//                .POST("/functional/products", accept(APPLICATION_JSON), handler::saveProduct)
//                .DELETE("/functional/products/{id}", accept(APPLICATION_JSON), handler::deleteProduct)
//                .DELETE("/functional/products", accept(APPLICATION_JSON), handler::deleteAllProducts)
//                .build();

        return route()
                .path("functional/products", builder -> builder
                        .nest(accept(APPLICATION_JSON).or(contentType(APPLICATION_JSON)).or(accept(TEXT_EVENT_STREAM)),
                                nestedBuilder -> nestedBuilder.GET("/events", handler::getProductEvents)
                                                              .GET("/{id}", handler::getProduct)
                                                              .GET(handler::getAllProducts)
                                                              .PUT("/{id}", handler::updateProduct)
                                                              .POST(handler::saveProduct)
                        )
                        .DELETE("/{id}", handler::deleteProduct)
                        .DELETE(handler::deleteAllProducts)
                )
                .build();
    }

}
