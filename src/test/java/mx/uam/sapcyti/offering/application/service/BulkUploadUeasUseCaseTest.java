package mx.uam.sapcyti.offering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort.ParsedBulkRow;
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
class BulkUploadUeasUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private UeaRepositoryPort ueaRepository;

    @Mock
    private UeaBulkFileParserPort bulkFileParser;

    @InjectMocks
    private BulkUploadUeasUseCase useCase;

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("all-or-nothing returns zero created when duplicate clave in file")
    void allOrNothingOnDuplicateInFile() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(bulkFileParser.parse("catalog.csv", sampleBytes())).thenReturn(List.of(
                row(1, "2156024"),
                row(2, "2156024")));

        BulkUploadUeasUseCase.BulkUploadResult result = useCase.execute("catalog.csv", sampleBytes());

        assertThat(result.getCreated()).isZero();
        assertThat(result.getErrors()).extracting(BulkUploadUeasUseCase.BulkUploadError::getCode)
                .contains("DUPLICATE_CLAVE");
        verify(ueaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("inserts all rows when valid")
    void bulkSuccess() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(bulkFileParser.parse("catalog.csv", sampleBytes())).thenReturn(List.of(row(1, "2156024")));
        when(ueaRepository.existsByClaveAndGraduateProgramId("2156024", 1L)).thenReturn(false);

        BulkUploadUeasUseCase.BulkUploadResult result = useCase.execute("catalog.csv", sampleBytes());

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getErrors()).isEmpty();
        verify(ueaRepository).saveAll(anyList());
    }

    private static byte[] sampleBytes() {
        return "dummy".getBytes(StandardCharsets.UTF_8);
    }

    private static ParsedBulkRow row(int rowNumber, String clave) {
        return new ParsedBulkRow(
                rowNumber,
                clave,
                "REDES Y PROTOCOLOS",
                "OBLIGATORIA",
                "MIXTA",
                "3",
                "3",
                "Basica",
                "9");
    }
}
