package com.jozsef.webflux.service;

import com.jozsef.webflux.model.Product;
import com.jozsef.webflux.repository.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Mono<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    @Override
    public Mono<Product> saveProduct(Product product) {
        return productRepository.insert(product);
    }

    @Override
    public Mono<Product> updateProduct(String id, Product product) {
        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    existingProduct.setName(product.getName());
                    existingProduct.setPrice(product.getPrice());

                    return productRepository.save(product);
                });
    }

    @Override
    public Mono<Void> deleteProduct(String id) {
        return productRepository.findById(id)
                .flatMap(productRepository::delete);
    }

    @Override
    public Mono<Void> deleteAllProducts() {
        return productRepository.deleteAll();
    }

}
