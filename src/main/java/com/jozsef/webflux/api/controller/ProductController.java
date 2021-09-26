package com.jozsef.webflux.api.controller;

import com.jozsef.webflux.model.Product;
import com.jozsef.webflux.model.ProductEvent;
import com.jozsef.webflux.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Flux<Product> getAllProducts() {
        return this.productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getProduct(@PathVariable String id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(product))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> saveProduct(@RequestBody Product product) {
        return productService.saveProduct(product);
    }

    @PutMapping("{id}")
    public Mono<ResponseEntity<Product>> updateProduct(@PathVariable String id, @RequestBody Product product) {
        return productService.updateProduct(id, product)
                .map(updatedProduct -> ResponseEntity.ok(updatedProduct))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id).then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public Mono<Void> deleteAllProducts() {
        return productService.deleteAllProducts();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductEvent> getProductEvents() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(val -> new ProductEvent(val, "Product Event"));
    }

}
