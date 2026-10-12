package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyVersionConflictException;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.MailPolicyAuditEntry;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SPEC-040 persistence: effective change, no-op, version conflict, and audit rollback.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:mail_policy_adapter;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
@Import(MailPolicyJpaAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MailPolicyJpaAdapterTest {

    private static final Long ACTOR_USER_ID = 15L;
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-12T18:30:00Z");
    private static final Instant LATER = Instant.parse("2026-10-12T19:00:00Z");

    @Autowired
    private MailPolicyJpaAdapter adapter;

    @Autowired
    private SpringDataMailPolicyRepository policies;

    @Autowired
    private SpringDataMailPolicyAuditRepository auditEntries;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        resetPolicy();
    }

    @AfterEach
    void tearDown() {
        resetPolicy();
    }

    @Test
    @DisplayName("effective change commits version + 1 and one audit entry; a failed audit keeps the previous mode")
    void effectiveChangeAndAuditAreAtomic() {
        MailPolicy updated = adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.TEST, 0),
                ACTOR_USER_ID,
                OCCURRED_AT);

        assertThat(updated.getMode()).isEqualTo(MailMode.TEST);
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getUpdatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(updated.getUpdatedByUserId()).isEqualTo(ACTOR_USER_ID);

        List<MailPolicyAuditEntry> entries = auditEntries.findAll();
        assertThat(entries).hasSize(1);
        MailPolicyAuditEntry entry = entries.get(0);
        assertThat(entry.getPreviousMode()).isEqualTo(MailMode.DISABLED);
        assertThat(entry.getNewMode()).isEqualTo(MailMode.TEST);
        assertThat(entry.getPreviousVersion()).isZero();
        assertThat(entry.getNewVersion()).isEqualTo(1);
        assertThat(entry.getActorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(entry.getOccurredAt()).isEqualTo(OCCURRED_AT);

        rejectFurtherAuditInserts();

        assertThatThrownBy(() -> adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.ENABLED, 1),
                99L,
                LATER))
                .isInstanceOf(RuntimeException.class);

        MailPolicy stored = adapter.load();
        assertThat(stored.getMode()).isEqualTo(MailMode.TEST);
        assertThat(stored.getVersion()).isEqualTo(1);
        assertThat(stored.getUpdatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(stored.getUpdatedByUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(auditEntries.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("same mode and current version is a no-op")
    void sameModeAndCurrentVersionIsANoOp() {
        adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.TEST, 0),
                ACTOR_USER_ID,
                OCCURRED_AT);

        MailPolicy unchanged = adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.TEST, 1),
                99L,
                LATER);

        assertThat(unchanged.getMode()).isEqualTo(MailMode.TEST);
        assertThat(unchanged.getVersion()).isEqualTo(1);
        assertThat(unchanged.getUpdatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(unchanged.getUpdatedByUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(auditEntries.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("an obsolete or higher version is a conflict and writes nothing")
    void obsoleteOrHigherVersionIsAConflict() {
        adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.TEST, 0),
                ACTOR_USER_ID,
                OCCURRED_AT);
        adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.ENABLED, 1),
                ACTOR_USER_ID,
                LATER);

        assertThatThrownBy(() -> adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.DISABLED, 1),
                ACTOR_USER_ID,
                LATER))
                .isInstanceOf(MailPolicyVersionConflictException.class);

        MailPolicy stored = adapter.load();
        assertThat(stored.getMode()).isEqualTo(MailMode.ENABLED);
        assertThat(stored.getVersion()).isEqualTo(2);
        assertThat(auditEntries.count()).isEqualTo(2);

        resetPolicy();

        assertThatThrownBy(() -> adapter.compareAndSwap(
                new UpdateMailPolicyCommand(MailMode.TEST, 4),
                ACTOR_USER_ID,
                OCCURRED_AT))
                .isInstanceOf(MailPolicyVersionConflictException.class);

        MailPolicy initial = adapter.load();
        assertThat(initial.getMode()).isEqualTo(MailMode.DISABLED);
        assertThat(initial.getVersion()).isZero();
        assertThat(initial.getUpdatedAt()).isNull();
        assertThat(initial.getUpdatedByUserId()).isNull();
        assertThat(auditEntries.count()).isZero();
    }

    private void resetPolicy() {
        execute("ALTER TABLE mail_policy_audit_entries DROP CONSTRAINT IF EXISTS ck_mail_policy_audit_fail");
        transactionTemplate.executeWithoutResult(status -> {
            auditEntries.deleteAll();
            policies.deleteAll();
            policies.saveAndFlush(MailPolicy.initial());
        });
    }

    private void rejectFurtherAuditInserts() {
        execute("""
                ALTER TABLE mail_policy_audit_entries
                ADD CONSTRAINT ck_mail_policy_audit_fail CHECK (actor_user_id = %d)
                """.formatted(ACTOR_USER_ID));
    }

    private void execute(String sql) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException(sql, exception);
        }
    }
}
