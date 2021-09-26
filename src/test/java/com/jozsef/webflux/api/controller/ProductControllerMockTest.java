package com.jozsef.webflux.api.controller;

import com.jozsef.webflux.model.Product;
import com.jozsef.webflux.model.ProductEvent;
import com.jozsef.webflux.repository.ProductRepository;
import com.jozsef.webflux.service.ProductService;
import com.jozsef.webflux.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ProductControllerMockTest {

    private WebTestClient testClient;

    private List<Product> expectedList;

    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);

        testClient = WebTestClient.bindToController(new ProductController(productService))
                                  .configureClient()
                                  .baseUrl("/products")
                                  .build();

        expectedList = List.of(
                Product.builder().id("1").name("Big Latte").price(2.99).build(),
                Product.builder().id("2").name("Big Decaf").price(2.49).build(),
                Product.builder().id("3").name("Green Tea").price(1.99).build()
        );
    }

    @Test
    void testGetAllProducts() {
        when(productRepository.findAll()).thenReturn(Flux.fromIterable(expectedList));

        testClient.get()
                  .uri("/")
                  .exchange()
                  .expectStatus().isOk()
                  .expectBodyList(Product.class).isEqualTo(expectedList);
    }

    @Test
    void testProductNotFound() {
        when(productRepository.findById("aaa")).thenReturn(Mono.empty());

        testClient.get()
                  .uri("/aaa")
                  .exchange()
                  .expectStatus().isNotFound();
    }

    @Test
    void testGetProduct() {
        Product expectedProduct = expectedList.get(0);

        when(productRepository.findById(expectedProduct.getId())).thenReturn(Mono.just(expectedProduct));

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

        when(productRepository.insert(expectedProduct)).thenReturn(Mono.just(expectedProduct));

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
                          }
                  );
    }

    @Test
    void testUpdateProduct() {
        Product expectedProduct = Product.builder()
                                         .id(expectedList.get(0).getId())
                                         .build();
        expectedProduct.setName("Jasmine Tea");
        expectedProduct.setPrice(0.99);

        when(productRepository.findById(expectedProduct.getId())).thenReturn(Mono.just(expectedList.get(0)));
        when(productRepository.save(expectedProduct)).thenReturn(Mono.just(expectedProduct));

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

        when(productRepository.findById(expectedProduct.getId())).thenReturn(Mono.empty());

        testClient.put()
                  .uri("/{id}", expectedProduct.getId())
                  .body(Mono.just(expectedProduct), Product.class)
                  .exchange()
                  .expectStatus().isNotFound();
    }

    @Test
    void testDeleteProduct() {
        String id = expectedList.get(0).getId();

        when(productRepository.findById(id)).thenReturn(Mono.just(expectedList.get(0)));
        when(productRepository.delete(expectedList.get(0))).thenReturn(Mono.empty());

        testClient.delete()
                  .uri("/{id}", id)
                  .exchange()
                  .expectStatus().isOk();
    }

    @Test
    void testDeleteAllProducts() {
        when(productRepository.deleteAll()).thenReturn(Mono.empty());

        testClient.delete()
                  .uri("/")
                  .exchange()
                  .expectStatus().isOk();
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
