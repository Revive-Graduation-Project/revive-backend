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
@Table(name = "custom_order_items")
public class CustomOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custom_item_seq")
    @SequenceGenerator(
            name = "custom_item_seq",
            sequenceName = "custom_order_item_id_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Column(name = "snapshot_name", nullable = false)
    private String snapshotName;

    @Column(name = "quantity_grams", nullable = false)
    private Double quantityGrams;

    @Column(name = "snapshot_price_per_100_gram", nullable = false, precision = 10, scale = 2)
    private BigDecimal snapshotPricePer100Gram;

    @Column(name = "snapshot_calories_per_100_gram")
    private Double snapshotCaloriesPer100Gram;

    @Column(name = "snapshot_protein_per_100_gram")
    private Double snapshotProteinPer100Gram;

    @Column(name = "snapshot_carbs_per_100_gram")
    private Double snapshotCarbsPer100Gram;

    @Column(name = "snapshot_fats_per_100_gram")
    private Double snapshotFatsPer100Gram;
}
