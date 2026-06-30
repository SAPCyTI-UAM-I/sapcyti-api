package mx.uam.sapcyti.academic.infrastructure.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataStudentProgramRepository extends JpaRepository<StudentProgram, Long> {

    @Query("""
            SELECT sp FROM StudentProgram sp
            LEFT JOIN FETCH sp.advisors
            WHERE sp.studentId = :studentId AND sp.graduateProgramId = :graduateProgramId
            ORDER BY sp.programType
            """)
    List<StudentProgram> findByStudentIdAndGraduateProgramIdWithAdvisors(
            @Param("studentId") Long studentId,
            @Param("graduateProgramId") Long graduateProgramId);

    @Query("""
            SELECT sp FROM StudentProgram sp
            LEFT JOIN FETCH sp.advisors
            WHERE sp.id = :id AND sp.studentId = :studentId AND sp.graduateProgramId = :graduateProgramId
            """)
    Optional<StudentProgram> findByIdAndStudentIdAndGraduateProgramIdWithAdvisors(
            @Param("id") Long id,
            @Param("studentId") Long studentId,
            @Param("graduateProgramId") Long graduateProgramId);

    @Query("""
            SELECT CASE WHEN COUNT(sp) > 0 THEN true ELSE false END
            FROM StudentProgram sp
            LEFT JOIN sp.advisors advisor
            WHERE sp.status = mx.uam.sapcyti.academic.domain.model.ProgramStatus.ACTIVO
            AND (sp.tutorId = :professorId OR advisor.professorId = :professorId)
            """)
    boolean hasActiveAssignmentAsTutorOrAdvisor(@Param("professorId") Long professorId);
}
