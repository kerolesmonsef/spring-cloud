package com.keroles.ddd.enrollment.domain;

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
@Table(name = "enrollment_students")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name"))
    private PersonName name;

    private Student(PersonName name) {
        this.name = name;
    }

    public static Student register(PersonName name) {
        return new Student(name);
    }
}
