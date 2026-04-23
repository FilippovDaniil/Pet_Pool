package com.billiardclub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "billiard_tables")
public class BilliardTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(1)
    @Column(unique = true, nullable = false)
    private int number;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableType type;

    public int getPricePerHour() {
        return type == TableType.RUSSIAN ? 500 : 700;
    }
}
