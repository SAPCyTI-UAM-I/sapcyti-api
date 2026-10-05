package mx.uam.sapcyti.offering.application.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.offering.application.command.RegisterUeaCommand;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyExistsException;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUeaUseCase {

    private final GraduateProgramRepositoryPort programRepository;
    private final UeaRepositoryPort ueaRepository;

    @Transactional
    public RegisterUeaResult execute(RegisterUeaCommand command) {
        Long graduateProgramId = requireTenant();
        assertProgramExists(graduateProgramId);

        if (command.creditos() == null) {
            throw new IllegalArgumentException("creditos is required");
        }

        String normalizedClave = UEA.validateAndNormalizeClave(command.clave());
        if (ueaRepository.existsByClaveAndGraduateProgramId(normalizedClave, graduateProgramId)) {
            throw new UeaAlreadyExistsException();
        }

        UEA uea = UEA.create(
                graduateProgramId,
                command.clave(),
                command.nombre(),
                command.tipo(),
                command.modalidad(),
                command.horasTeoria(),
                command.horasPractica(),
                command.tipoFormacion(),
                command.creditos());

        uea = ueaRepository.save(uea);
        return RegisterUeaResult.from(uea);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }

    private void assertProgramExists(Long graduateProgramId) {
        if (programRepository.findById(graduateProgramId).isEmpty()) {
            throw new GraduateProgramNotFoundException(graduateProgramId);
        }
    }

    @Value
    @Builder
    public static class RegisterUeaResult {
        Long id;
        String clave;
        String nombre;
        mx.uam.sapcyti.offering.domain.model.UeaType tipo;
        mx.uam.sapcyti.offering.domain.model.UeaModality modalidad;
        java.math.BigDecimal horasTeoria;
        java.math.BigDecimal horasPractica;
        mx.uam.sapcyti.offering.domain.model.FormationType tipoFormacion;
        int creditos;
        boolean active;
        Long graduateProgramId;

        static RegisterUeaResult from(UEA uea) {
            return RegisterUeaResult.builder()
                    .id(uea.getId())
                    .clave(uea.getClave())
                    .nombre(uea.getNombre())
                    .tipo(uea.getTipo())
                    .modalidad(uea.getModalidad())
                    .horasTeoria(uea.getHorasTeoria())
                    .horasPractica(uea.getHorasPractica())
                    .tipoFormacion(uea.getTipoFormacion())
                    .creditos(uea.getCreditos())
                    .active(uea.isActive())
                    .graduateProgramId(uea.getGraduateProgramId())
                    .build();
        }
    }
}
