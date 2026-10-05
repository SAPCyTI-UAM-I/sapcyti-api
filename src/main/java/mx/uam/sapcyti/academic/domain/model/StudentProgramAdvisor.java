package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Join row linking a {@link StudentProgram} to an advisor {@link Professor}.
 */
@Entity
@Table(name = "student_program_advisors")
@IdClass(StudentProgramAdvisor.StudentProgramAdvisorId.class)
public class StudentProgramAdvisor {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_program_id", nullable = false)
    private StudentProgram studentProgram;

    @Id
    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "position", nullable = false)
    private int position;

    protected StudentProgramAdvisor() {
        // For JPA
    }

    StudentProgramAdvisor(StudentProgram studentProgram, Long professorId, int position) {
        this.studentProgram = studentProgram;
        this.professorId = professorId;
        this.position = position;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public int getPosition() {
        return position;
    }

    public static class StudentProgramAdvisorId implements Serializable {

        private Long studentProgram;
        private Long professorId;

        protected StudentProgramAdvisorId() {
            // For JPA
        }

        public StudentProgramAdvisorId(Long studentProgramId, Long professorId) {
            this.studentProgram = studentProgramId;
            this.professorId = professorId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StudentProgramAdvisorId that)) {
                return false;
            }
            return Objects.equals(studentProgram, that.studentProgram)
                    && Objects.equals(professorId, that.professorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentProgram, professorId);
        }
    }
}
