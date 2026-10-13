package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.junit.jupiter.api.Test;

class EmptyExamRequestReadAdapterTest {

    private final ExamRequestReadPort adapter = new EmptyExamRequestReadAdapter();

    @Test
    void returnsEmptyForEveryExamType() {
        assertThat(adapter.findByStudentAndType(10L, ExamType.GRADO)).isEmpty();
        assertThat(adapter.findByStudentAndType(10L, ExamType.PREDOCTORAL)).isEmpty();
        assertThat(EmptyExamRequestReadAdapter.class.getDeclaredFields()).isEmpty();
    }
}
