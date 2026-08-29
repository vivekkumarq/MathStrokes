package com.mathstrokes.catalog.entity;

import com.mathstrokes.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /** Stable machine-facing key, e.g. MATH. Lets seeds and imports reference a subject safely. */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
