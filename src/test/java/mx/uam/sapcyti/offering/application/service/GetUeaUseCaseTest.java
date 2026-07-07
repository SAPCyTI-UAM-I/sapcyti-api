package mx.uam.sapcyti.offering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetUeaUseCaseTest {

    @Mock
    private UeaRepositoryPort ueaRepository;

    @InjectMocks
    private GetUeaUseCase useCase;

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("returns the UEA in the tenant")
    void returnsUea() {
        when(ueaRepository.findByIdAndGraduateProgramId(10L, 1L))
                .thenReturn(Optional.of(sampleUea()));

        UEA result = useCase.execute(10L);

        assertThat(result.getClave()).isEqualTo("2156041");
    }

    @Test
    @DisplayName("throws when the UEA is missing or outside the tenant")
    void notFound() {
        when(ueaRepository.findByIdAndGraduateProgramId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L))
                .isInstanceOf(UeaNotFoundException.class);
    }

    private static UEA sampleUea() {
        return UEA.create(
                1L,
                "2156041",
                "Nombre",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.BASICA,
                9);
    }
}
