package mx.uam.sapcyti.identity.domain.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root. System access account. Stores credentials and activation status.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType role;

    @Column(name = "graduate_program_id")
    private Long graduateProgramId; // Nullable for global SYSTEM_ADMIN

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Embedded
    private PasswordResetToken passwordResetToken;

    protected User() {
        // For JPA
    }

    public User(String email, String passwordHash, RoleType role, Long graduateProgramId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.graduateProgramId = graduateProgramId;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public RoleType getRole() {
        return role;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public List<RefreshToken> getRefreshTokens() {
        return refreshTokens;
    }

    public PasswordResetToken getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPasswordResetToken(PasswordResetToken passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }
}
