package com.beauty_store.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double price;

    @Column
    private Double originalPrice;

    @Column(name = "image", length = 1000)
    private String image;

    @Column(nullable = false)
    private String category;

    @Column
    private String tags; // Chỉ giữ 1 cột tags

    @Column
    private Double rating; // Điểm trung bình sao

    @Column(name = "review_count")
    private Integer reviewCount; // Số lượt đánh giá
}
