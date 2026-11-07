package com.himalaya.sabor.repository;

import com.himalaya.sabor.model.Category;
import com.himalaya.sabor.model.Product;
import com.himalaya.sabor.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategoryAndStatus(Category category, ProductStatus status);
    List<Product> findByCategory(Category category);
}
