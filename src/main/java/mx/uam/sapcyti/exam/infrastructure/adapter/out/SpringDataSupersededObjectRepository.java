package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSupersededObjectRepository extends JpaRepository<SupersededObjectEntity, Long> {
}
