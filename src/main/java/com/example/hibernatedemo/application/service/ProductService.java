package com.example.hibernatedemo.application.service;

import com.example.hibernatedemo.application.dto.CreateProductRequest;
import com.example.hibernatedemo.application.dto.ProductResponse;
import com.example.hibernatedemo.domain.entity.Product;
import com.example.hibernatedemo.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with name: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", id);
                    return new RuntimeException("Product not found with id: " + id);
                });

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination: {}", pageable);
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        log.debug("Fetching all active products");
        return productRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", id);
                    return new RuntimeException("Product not found with id: " + id);
                });

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with id: {}", updatedProduct.getId());

        return toResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        if (!productRepository.existsById(id)) {
            log.warn("Product not found with id: {}", id);
            throw new RuntimeException("Product not found with id: " + id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted successfully with id: {}", id);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
