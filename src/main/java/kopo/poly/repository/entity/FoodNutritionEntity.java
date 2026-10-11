package kopo.poly.repository.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "FOOD_NUTRITION")
public class FoodNutritionEntity {

    @Id
    @Column(name = "FOOD_CD", length = 50)
    private String foodCd;

    @Column(name = "FOOD_NM", nullable = false, length = 200)
    private String foodNm;

    @Column(name = "KCAL")
    private Double kcal;

    @Column(name = "PROTEIN")
    private Double protein;

    @Column(name = "FAT")
    private Double fat;

    @Column(name = "CARBS")
    private Double carbs;
}
