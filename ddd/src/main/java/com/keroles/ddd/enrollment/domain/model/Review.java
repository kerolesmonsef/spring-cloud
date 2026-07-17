package com.keroles.ddd.enrollment.domain.model;

import com.keroles.ddd.sharedkernel.DomainException;
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
@Table(name = "enrollment_reviews")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private Long courseId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "rating_value"))
    private Rating rating;

    private String comment;

    private Review(Long studentId, Long courseId, Rating rating, String comment) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.rating = rating;
        this.comment = comment;
    }

    public static Review write(Long studentId, Long courseId, Rating rating, String comment) {
        if (studentId == null || courseId == null) {
            throw new DomainException("A review requires a student and a course");
        }
        if (rating == null) {
            throw new DomainException("A review requires a rating");
        }
        return new Review(studentId, courseId, rating, comment);
    }
}
