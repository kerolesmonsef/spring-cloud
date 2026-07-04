package com.keroles.ddd.enrollment.application.eventlistener;

import com.keroles.ddd.enrollment.domain.StudentEnrolledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StudentEnrolledEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(StudentEnrolledEvent event) {
        log.info("Student {} enrolled in course {} at {}",
                event.studentId(), event.courseId(), event.occurredOn());
    }
}
