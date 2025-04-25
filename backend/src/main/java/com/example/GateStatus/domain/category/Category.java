package com.example.GateStatus.domain.category;

import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Category extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    private String iconUrl;

    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorySubject subject;

    public void update(String name, String description, String iconUrl, Integer displayOrder,
                       Boolean active, CategoryType type, CategorySubject subject) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (iconUrl != null) this.iconUrl = iconUrl;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (active != null) this.active = active;
        if (type != null) this.type = type;
        if (subject != null) this.subject = subject;
    }

}
