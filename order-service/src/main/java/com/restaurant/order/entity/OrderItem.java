package com.restaurant.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    private Long mealId;

    @Column(name = "snapshot_name", nullable = false)
    private String snapshotName;

    @Column(name = "snapshot_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal snapshotPrice;

    @Column(name = "snapshot_image_url", length = 1024)
    private String snapshotImageUrl;

    @Column(nullable = false)
    private Integer quantity;
}
