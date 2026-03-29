package com.restaurant.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")
    @SequenceGenerator(
            name = "order_item_seq",
            sequenceName = "order_item_id_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "meal_id", nullable = false)
    private UUID mealId;

    @Column(name = "snapshot_name", nullable = false)
    private String snapshotName;

    @Column(name = "snapshot_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal snapshotPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "snapshot_calories")
    private Double snapshotCalories;

    @Column(name = "snapshot_protein")
    private Double snapshotProtein;

    @Column(name = "snapshot_carbs")
    private Double snapshotCarbs;

    @Column(name = "snapshot_fats")
    private Double snapshotFats;
}
