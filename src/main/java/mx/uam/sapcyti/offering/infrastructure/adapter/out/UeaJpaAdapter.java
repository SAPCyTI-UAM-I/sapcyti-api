package mx.uam.sapcyti.offering.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.offering.infrastructure.adapter.out.repository.SpringDataUeaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UeaJpaAdapter implements UeaRepositoryPort {

    private final SpringDataUeaRepository jpaRepository;

    @Override
    @Transactional
    public UEA save(UEA uea) {
        return jpaRepository.save(uea);
    }

    @Override
    @Transactional
    public List<UEA> saveAll(List<UEA> ueas) {
        return jpaRepository.saveAll(ueas);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClaveAndGraduateProgramId(String clave, Long graduateProgramId) {
        return jpaRepository.existsByClaveIgnoreCaseAndGraduateProgramId(clave, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UEA> findByClaveAndGraduateProgramId(String clave, Long graduateProgramId) {
        return jpaRepository.findByClaveIgnoreCaseAndGraduateProgramId(clave, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UEA> findByIdAndGraduateProgramId(Long id, Long graduateProgramId) {
        return jpaRepository.findByIdAndGraduateProgramId(id, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UEA> findByGraduateProgramId(
            Long graduateProgramId, String search, Boolean active, Pageable pageable) {
        return jpaRepository.searchByProgram(graduateProgramId, search, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UEA> findActiveByGraduateProgramId(Long graduateProgramId) {
        return jpaRepository.findByGraduateProgramIdAndActiveTrueOrderByClaveAsc(graduateProgramId);
    }
}
