package com.jozsef.webflux.api.controller;

import com.jozsef.webflux.model.Product;
import com.jozsef.webflux.model.ProductEvent;
import com.jozsef.webflux.repository.ProductRepository;
import com.jozsef.webflux.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ProductControllerTest {

    private WebTestClient testClient;

    private List<Product> expectedList;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        testClient = WebTestClient.bindToController(new ProductController(productService))
                .configureClient()
                .baseUrl("/products")
                .build();

        expectedList = productRepository.findAll().collectList().block();
    }

    @Test
    void testGetAllProducts() {
        testClient.get()
                  .uri("/")
                  .exchange()
                  .expectStatus().isOk()
                  .expectBodyList(Product.class).isEqualTo(expectedList);
    }

    @Test
    void testProductNotFound() {
        testClient.get()
                  .uri("/aaa")
                  .exchange()
                  .expectStatus().isNotFound();
    }

    @Test
    void testGetProduct() {
        Product expectedProduct = expectedList.get(0);
        testClient.get()
                  .uri("/{id}", expectedProduct.getId())
                  .exchange()
                  .expectStatus().isOk()
                  .expectBody(Product.class).isEqualTo(expectedProduct);
    }

    @Test
    void testCreateProduct() {
        Product expectedProduct = Product.builder()
                                         .name("Jasmine Tea")
                                         .price(0.99)
                                         .build();

        testClient.post()
                  .uri("/")
                  .body(Mono.just(expectedProduct), Product.class)
                  .exchange()
                  .expectStatus().isCreated()
                  .expectBody(Product.class)
                  .consumeWith(p -> {
                            assertThat(p.getResponseBody())
                                  .usingRecursiveComparison()
                                  .ignoringFields("id")
                                  .isEqualTo(expectedProduct);

                            assertEquals(4, productRepository.findAll().collectList().block().size());
                        }
                  );
    }

    @Test
    void testUpdateProduct() {
        Product expectedProduct = expectedList.get(0);
        expectedProduct.setName("Jasmine Tea");
        expectedProduct.setPrice(0.99);

        testClient.put()
                  .uri("/{id}", expectedProduct.getId())
                  .body(Mono.just(expectedProduct), Product.class)
                  .exchange()
                  .expectStatus().isOk()
                  .expectBody(Product.class)
                  .consumeWith(p ->
                          assertThat(p.getResponseBody())
                                  .usingRecursiveComparison()
                                  .isEqualTo(expectedProduct)
                  );
    }

    @Test
    void testUpdateProductNotFound() {
        Product expectedProduct = Product.builder()
                                         .id("mock_id")
                                         .name("Jasmine Tea")
                                         .price(0.99)
                                         .build();

        testClient.put()
                  .uri("/{id}", expectedProduct.getId())
                  .body(Mono.just(expectedProduct), Product.class)
                  .exchange()
                  .expectStatus().isNotFound();
    }

    @Test
    @Disabled("Causes flakiness, needs research for cause")
    void testDeleteProduct() {
        String id = expectedList.get(0).getId();

        testClient.delete()
                  .uri("/{id}", id)
                  .exchange()
                  .expectStatus().isOk()
                  .expectBody().consumeWith(r ->
                        assertEquals(2, productRepository.findAll().collectList().block().size())
                    );
    }

    @Test
    void testDeleteAllProducts() {
        testClient.delete()
                  .uri("/")
                  .exchange()
                  .expectStatus().isOk()
                  .expectBody().consumeWith(r ->
                        assertEquals(0, productRepository.findAll().collectList().block().size())
                );
    }

    @Test
    void testProductEvents() {
        ProductEvent expectedEvent = ProductEvent.builder().eventId(0L).eventType("Product Event").build();

        FluxExchangeResult<ProductEvent> result = testClient.get()
                .uri("/events")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(ProductEvent.class);

        StepVerifier.create(result.getResponseBody())
                    .expectNext(expectedEvent)
                    .expectNextCount(2)
                    .consumeNextWith(productEvent -> assertEquals(Long.valueOf(3), productEvent.getEventId()))
                    .thenCancel()
                    .verify();
    }

}
