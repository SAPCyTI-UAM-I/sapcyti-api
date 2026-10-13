package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface SpringDataExamRequestRepository extends JpaRepository<ExamRequestEntity, Long> {

    Optional<ExamRequestEntity> findByStudentIdAndExamType(Long studentId, ExamType examType);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT request FROM ExamRequestEntity request
            WHERE request.studentId = :studentId AND request.examType = :examType
            """)
    Optional<ExamRequestEntity> lockByStudentAndType(
            @Param("studentId") Long studentId,
            @Param("examType") ExamType examType);
}
