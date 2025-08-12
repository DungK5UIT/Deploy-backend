package com.beauty_store.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.model.ErrorResponse;
import com.beauty_store.backend.model.Product;
import com.beauty_store.backend.repository.ProductRepository;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/list")
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Fetching all products");
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            logger.info("Fetching product with ID: {}", id);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            logger.warn("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Không tìm thấy sản phẩm: " + e.getMessage(), HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            logger.error("Error fetching product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        try {
            logger.info("Adding new product: {}", product.getName());
            return ResponseEntity.ok(productRepository.save(product));
        } catch (Exception e) {
            logger.error("Error adding product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}