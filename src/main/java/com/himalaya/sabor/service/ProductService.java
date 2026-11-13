package com.himalaya.sabor.service;

import com.himalaya.sabor.dto.ProductDto;
import com.himalaya.sabor.model.Category;
import com.himalaya.sabor.model.Product;
import com.himalaya.sabor.model.ProductStatus;
import com.himalaya.sabor.repository.CategoryRepository;
import com.himalaya.sabor.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final com.himalaya.sabor.repository.DailyMenuItemRepository dailyMenuItemRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          com.himalaya.sabor.repository.DailyMenuItemRepository dailyMenuItemRepository) {
        this.dailyMenuItemRepository = dailyMenuItemRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Map<Category, List<Product>> getActiveProductsGroupedByCategory() {
        List<Category> categories = categoryRepository.findAll();
        Map<Category, List<Product>> grouped = new LinkedHashMap<>();

        for (Category category : categories) {
            List<Product> products = productRepository.findByCategoryAndStatus(category, ProductStatus.ACTIVO);
            if (!products.isEmpty()) {
                grouped.put(category, products);
            }
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProductsByCategorySlug(String slug) {
        Optional<Category> optionalCategory = categoryRepository.findBySlug(slug);
        if (optionalCategory.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findByCategoryAndStatus(optionalCategory.get(), ProductStatus.ACTIVO);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProductsForAdmin() {
        return productRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado con ID: " + id));
    }

    @Transactional
    public Product saveProduct(ProductDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no válida con ID: " + dto.getCategoryId()));

        Product product;
        if (dto.getId() != null) {
            product = getProductById(dto.getId());
        } else {
            product = new Product();
        }

        product.setName(dto.getName().trim());
        product.setCategory(category);
        product.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        product.setPrice(dto.getPrice());
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : ProductStatus.ACTIVO);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateStatus(Long id, ProductStatus newStatus) {
        Product product = getProductById(id);
        // D-13: un plato que está en el menú del día (hoy o fechas posteriores) no se puede dar de baja
        if (newStatus != ProductStatus.ACTIVO
                && dailyMenuItemRepository.existsByProductIdAndDailyMenuDateGreaterThanEqual(id, java.time.LocalDate.now())) {
            throw new IllegalStateException("No se puede dar de baja '" + product.getName()
                    + "': forma parte del menú del día. Sustitúyelo primero en el menú.");
        }
        product.setStatus(newStatus);
        return productRepository.save(product);
    }

    /** Todos los productos activos, ordenados por categoría y nombre (usado por el chef para armar el menú). */
    @Transactional(readOnly = true)
    public List<Product> findAllActive() {
        return productRepository.findAllByOrderByIdAsc().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVO)
                .toList();
    }
}
