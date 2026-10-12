package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SPEC-040 migration: one DISABLED row at version 0, idempotent when run again.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:mail_policy_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GlobalMailPolicyMigrationTest {

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void emptyInstallation() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS mail_policy_audit_entries");
            statement.execute("DROP TABLE IF EXISTS mail_policy");
            statement.execute("DROP TABLE IF EXISTS users");
            statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
        }
    }

    @Test
    @DisplayName("migration inserts one DISABLED version 0 row and a second run does not insert another")
    void migrationIsIdempotent() throws SQLException {
        runMigration();
        assertInitialRow();

        runMigration();
        assertInitialRow();
        assertNoGraduateProgramColumn();
        assertSingleRowConstraint();
        assertUserForeignKey();
    }

    private void runMigration() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V22__global_mail_policy.sql"));
        }
    }

    private void assertInitialRow() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT mode, version, updated_at, updated_by_user_id FROM mail_policy")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("mode")).isEqualTo("DISABLED");
            assertThat(resultSet.getInt("version")).isZero();
            assertThat(resultSet.getObject("updated_at")).isNull();
            assertThat(resultSet.getObject("updated_by_user_id")).isNull();
            assertThat(resultSet.next()).isFalse();
        }
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM mail_policy_audit_entries")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isZero();
        }
    }

    private void assertNoGraduateProgramColumn() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE LOWER(table_name) IN ('mail_policy', 'mail_policy_audit_entries')
                          AND LOWER(column_name) = 'graduate_program_id'
                        """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isZero();
        }
    }

    private void assertSingleRowConstraint() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO mail_policy (id, mode, version) VALUES (2, 'DISABLED', 0)"))
                .isInstanceOf(SQLException.class);
    }

    private void assertUserForeignKey() {
        assertThatThrownBy(() -> execute(
                "UPDATE mail_policy SET updated_by_user_id = 999 WHERE id = 1"))
                .isInstanceOf(SQLException.class);
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
