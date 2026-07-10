package com.restaurant.menu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meal_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MealTemplate template;

    @Column(name = "slot_name", nullable = false)
    private String slotName;

    @Column(name = "ingredient_category", nullable = false)
    private String ingredientCategory;

    @Column(name = "max_select", nullable = false)
    private Integer maxSelect;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;
}
