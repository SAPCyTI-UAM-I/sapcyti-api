package mx.uam.sapcyti.offering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.offering.application.command.RegisterUeaCommand;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyExistsException;
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
class RegisterUeaUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private UeaRepositoryPort ueaRepository;

    @InjectMocks
    private RegisterUeaUseCase useCase;

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("registers UEA with active true")
    void registerSuccess() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(ueaRepository.existsByClaveAndGraduateProgramId("2156041", 1L)).thenReturn(false);
        when(ueaRepository.save(any(UEA.class))).thenAnswer(invocation -> {
            UEA uea = invocation.getArgument(0);
            return uea;
        });

        RegisterUeaUseCase.RegisterUeaResult result = useCase.execute(sampleCommand());

        assertThat(result.getClave()).isEqualTo("2156041");
        assertThat(result.isActive()).isTrue();
        ArgumentCaptor<UEA> captor = ArgumentCaptor.forClass(UEA.class);
        verify(ueaRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("rejects duplicate clave")
    void rejectDuplicateClave() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(ueaRepository.existsByClaveAndGraduateProgramId("2156041", 1L)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(UeaAlreadyExistsException.class);
        verify(ueaRepository, never()).save(any());
    }

    private static RegisterUeaCommand sampleCommand() {
        return new RegisterUeaCommand(
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                UeaType.OPTATIVA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.COMPLEMENTARIA,
                9);
    }
}
