package com.example.hibernatedemo.infrastructure.repository;

import com.example.hibernatedemo.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    boolean existsBySku(String sku);
}
