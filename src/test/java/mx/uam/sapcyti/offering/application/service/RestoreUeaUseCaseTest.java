package mx.uam.sapcyti.offering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyActiveException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreUeaUseCaseTest {

    @Mock
    private UeaRepositoryPort ueaRepository;

    @InjectMocks
    private RestoreUeaUseCase useCase;

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("restores inactive UEA in tenant")
    void restoreSuccess() {
        UEA existing = sampleUea();
        existing.deactivate();

        when(ueaRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(existing));
        when(ueaRepository.save(any(UEA.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UEA result = useCase.execute(10L);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getClave()).isEqualTo("2156041");

        ArgumentCaptor<UEA> captor = ArgumentCaptor.forClass(UEA.class);
        verify(ueaRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("rejects already active UEA")
    void alreadyActive() {
        UEA existing = sampleUea();

        when(ueaRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(10L))
                .isInstanceOf(UeaAlreadyActiveException.class);
        verify(ueaRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects UEA outside tenant")
    void rejectNotFound() {
        when(ueaRepository.findByIdAndGraduateProgramId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L))
                .isInstanceOf(UeaNotFoundException.class);
        verify(ueaRepository, never()).save(any());
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
