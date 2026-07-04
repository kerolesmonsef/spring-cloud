package com.keroles.ddd.catalog.domain;

import com.keroles.ddd.sharedkernel.DomainException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "catalog_courses")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price"))
    private Money price;

    private Long teacherId;

    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    private Course(String title, String description, Money price, Long teacherId) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.teacherId = teacherId;
        this.status = CourseStatus.DRAFT;
    }

    public static Course author(String title, String description, Money price, Long teacherId) {
        if (title == null || title.isBlank()) {
            throw new DomainException("Course title cannot be blank");
        }
        if (price == null) {
            throw new DomainException("Course price is required");
        }
        if (teacherId == null) {
            throw new DomainException("Course must belong to a teacher");
        }
        return new Course(title.trim(), description, price, teacherId);
    }

    public void publish() {
        if (status == CourseStatus.PUBLISHED) {
            throw new DomainException("Course is already published");
        }
        this.status = CourseStatus.PUBLISHED;
    }

    public void changePrice(Money newPrice) {
        if (newPrice == null) {
            throw new DomainException("Course price is required");
        }
        this.price = newPrice;
    }

    public boolean isPublished() {
        return status == CourseStatus.PUBLISHED;
    }
}
