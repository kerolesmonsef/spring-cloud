package com.keroles.ddd.catalog.domain;

import com.keroles.ddd.sharedkernel.PersonName;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "catalog_teachers")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name"))
    private PersonName name;

    private Teacher(PersonName name) {
        this.name = name;
    }

    public static Teacher register(PersonName name) {
        return new Teacher(name);
    }
}
