package com.jozsef.webflux.service;

import com.jozsef.webflux.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {

    Flux<Product> getAllProducts();

    Mono<Product> getProductById(String id);

    Mono<Product> saveProduct(Product product);

    Mono<Product> updateProduct(String id, Product product);

    Mono<Void> deleteProduct(String id);

    Mono<Void> deleteAllProducts();

}
