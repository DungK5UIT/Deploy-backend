package com.beauty_store.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = false)
    private String status; // e.g., "PENDING", "PAID", "FAILED"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private String fullName;

    @Column
    private String phone;

    @Column
    private String email;

    @Column
    private String address;

    @Column
    private String city;

    @Column
    private String district;

    @Column
    private String paymentMethod;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
}