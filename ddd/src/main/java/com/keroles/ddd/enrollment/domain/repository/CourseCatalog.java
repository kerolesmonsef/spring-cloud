package com.keroles.ddd.enrollment.domain.repository;

public interface CourseCatalog {

    boolean exists(Long courseId);

    boolean isPublished(Long courseId);
}
