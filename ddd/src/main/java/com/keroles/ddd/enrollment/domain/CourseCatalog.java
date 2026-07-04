package com.keroles.ddd.enrollment.domain;

public interface CourseCatalog {

    boolean exists(Long courseId);

    boolean isPublished(Long courseId);
}
