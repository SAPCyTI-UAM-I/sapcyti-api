package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.dao.DataAccessResourceFailureException;

import mx.uam.sapcyti.configuration.domain.exception.MailPolicyVersionConflictException;
import mx.uam.sapcyti.configuration.domain.model.MailAudience;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchCommand;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchOutcome;
import mx.uam.sapcyti.configuration.domain.model.MailEnvelope;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;
import mx.uam.sapcyti.configuration.domain.port.in.MailDispatchPort.MailSender;
import mx.uam.sapcyti.configuration.domain.port.out.MailPolicyRepositoryPort;
import mx.uam.sapcyti.identity.application.model.AuthenticatedUser;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;

@Timeout(5)
class MailDispatchGateServiceTest {

    private static final Instant CHANGED_AT = Instant.parse("2026-10-12T00:00:00Z");
    private static final MailEnvelope INTENDED = new MailEnvelope(
            List.of("alumno.real@uam.mx", "otro@uam.mx"),
            List.of("copia@uam.mx"),
            List.of("oculta@uam.mx"));

    private PolicyStore store;
    private MailDispatchGateService gate;
    private AuthenticatedUserResolver users;
    private UpdateMailPolicyUseCase useCase;

    @BeforeEach
    void setUp() {
        store = new PolicyStore();
        gate = new MailDispatchGateService(store, Duration.ofSeconds(2));
        users = mock(AuthenticatedUserResolver.class);
        useCase = new UpdateMailPolicyUseCase(store, users, gate);
    }

    @AfterEach
    void tearDown() {
        gate.shutdownProviderCalls();
    }

    @Test
    @DisplayName("Three modes and audience: effective To/Cc/Bcc, no call when disabled or unknown")
    void appliesModeAndAudience() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<MailEnvelope> sent = new AtomicReference<>();
        MailSender sender = envelope -> {
            calls.incrementAndGet();
            sent.set(envelope);
        };

        store.current.set(MailPolicy.initial());
        assertThat(gate.dispatch(command(MailAudience.STUDENT), sender))
                .isEqualTo(MailDispatchOutcome.SUPPRESSED);
        assertThat(calls).hasValue(0);

        store.current.set(policy(MailMode.TEST));
        assertThat(gate.dispatch(command(MailAudience.STUDENT), sender))
                .isEqualTo(MailDispatchOutcome.SENT);
        assertThat(sent.get().to()).containsExactly("sapcytialumno@gmail.com");
        assertThat(sent.get().cc()).isEmpty();
        assertThat(sent.get().bcc()).isEmpty();
        assertThat(sent.get().to()).doesNotContain("alumno.real@uam.mx", "otro@uam.mx", "copia@uam.mx");

        assertThat(gate.dispatch(command(MailAudience.STAFF), sender))
                .isEqualTo(MailDispatchOutcome.SENT);
        assertThat(sent.get().to()).containsExactly("sapcytiprofesor@gmail.com");
        assertThat(sent.get().cc()).isEmpty();
        assertThat(sent.get().bcc()).isEmpty();
        assertThat(calls).hasValue(2);

        store.current.set(policy(MailMode.ENABLED));
        assertThat(gate.dispatch(command(MailAudience.STAFF), sender))
                .isEqualTo(MailDispatchOutcome.SENT);
        assertThat(sent.get().to()).containsExactly("alumno.real@uam.mx", "otro@uam.mx");
        assertThat(sent.get().cc()).containsExactly("copia@uam.mx");
        assertThat(sent.get().bcc()).containsExactly("oculta@uam.mx");

        assertThat(gate.dispatch(new MailDispatchCommand(null, INTENDED), sender))
                .isEqualTo(MailDispatchOutcome.BLOCKED);
        assertThat(calls).hasValue(3);

        assertThatThrownBy(() -> gate.dispatch(command(MailAudience.STUDENT), envelope -> {
            throw new IllegalStateException("rejected");
        })).isInstanceOf(IllegalStateException.class).hasMessage("rejected");
    }

    @Test
    @DisplayName("An unreadable policy blocks dispatch and does not call the provider")
    void unreadablePolicyIsBlocked() {
        AtomicInteger calls = new AtomicInteger();
        MailSender sender = envelope -> calls.incrementAndGet();
        store.current.set(policy(MailMode.ENABLED));
        store.loadFailure = new DataAccessResourceFailureException("down");

        assertThat(gate.dispatch(command(MailAudience.STUDENT), sender))
                .isEqualTo(MailDispatchOutcome.BLOCKED);
        assertThat(calls).hasValue(0);

        store.loadFailure = new IllegalStateException("Mail policy row is missing");
        assertThat(gate.dispatch(command(MailAudience.STAFF), sender))
                .isEqualTo(MailDispatchOutcome.BLOCKED);
        assertThat(calls).hasValue(0);

        MailDispatchGateService nullPolicy = new MailDispatchGateService(
                new MailPolicyRepositoryPort() {
                    @Override
                    public MailPolicy load() {
                        return null;
                    }

                    @Override
                    public MailPolicy compareAndSwap(
                            UpdateMailPolicyCommand command,
                            Long actorUserId,
                            Instant occurredAt) {
                        throw new UnsupportedOperationException();
                    }
                },
                Duration.ofSeconds(2));
        try {
            assertThat(nullPolicy.dispatch(command(MailAudience.STUDENT), sender))
                    .isEqualTo(MailDispatchOutcome.BLOCKED);
        } finally {
            nullPolicy.shutdownProviderCalls();
        }
        assertThat(calls).hasValue(0);
    }

    @Test
    @DisplayName("A send admitted before PUT can finish; a later send sees the confirmed mode")
    void admittedSendFinishesAndLaterSendSeesConfirmedMode() throws Exception {
        store.current.set(policy(MailMode.ENABLED));
        when(users.resolve()).thenReturn(coordinator());

        CountDownLatch inProvider = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<MailEnvelope> admitted = new AtomicReference<>();
        AtomicReference<MailDispatchOutcome> admittedOutcome = new AtomicReference<>();
        AtomicReference<Throwable> admittedFailure = new AtomicReference<>();
        AtomicReference<MailPolicy> updated = new AtomicReference<>();
        AtomicReference<Throwable> updateFailure = new AtomicReference<>();

        Thread dispatch = new Thread(() -> {
            try {
                admittedOutcome.set(gate.dispatch(command(MailAudience.STUDENT), envelope -> {
                    admitted.set(envelope);
                    calls.incrementAndGet();
                    inProvider.countDown();
                    awaitLatch(releaseProvider);
                }));
            } catch (Throwable ex) {
                admittedFailure.set(ex);
            }
        });
        dispatch.start();
        assertThat(inProvider.await(2, TimeUnit.SECONDS)).isTrue();

        Thread put = new Thread(() -> {
            try {
                updated.set(useCase.update(new UpdateMailPolicyCommand(MailMode.DISABLED, 1)));
            } catch (Throwable ex) {
                updateFailure.set(ex);
            }
        });
        put.start();
        await().atMost(Duration.ofSeconds(2))
                .until(() -> store.swapEntered.get() || gate.hasQueuedThreads());
        assertThat(store.swapEntered).isFalse();
        assertThat(store.transactionOpen).isFalse();

        releaseProvider.countDown();
        dispatch.join(2_000);
        put.join(2_000);

        assertThat(admittedFailure.get()).isNull();
        assertThat(updateFailure.get()).isNull();
        assertThat(admittedOutcome.get()).isEqualTo(MailDispatchOutcome.SENT);
        assertThat(admitted.get().to()).containsExactly("alumno.real@uam.mx", "otro@uam.mx");
        assertThat(admitted.get().cc()).containsExactly("copia@uam.mx");
        assertThat(admitted.get().bcc()).containsExactly("oculta@uam.mx");
        assertThat(updated.get().getMode()).isEqualTo(MailMode.DISABLED);
        assertThat(store.transactionOpen).isFalse();

        MailDispatchOutcome later = gate.dispatch(
                command(MailAudience.STUDENT),
                envelope -> calls.incrementAndGet());
        assertThat(later).isEqualTo(MailDispatchOutcome.SUPPRESSED);
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("A suppressed attempt is not sent again when the mode changes")
    void suppressedAttemptIsNotReplayed() {
        store.current.set(MailPolicy.initial());
        when(users.resolve()).thenReturn(coordinator());
        AtomicInteger calls = new AtomicInteger();

        assertThat(gate.dispatch(command(MailAudience.STUDENT), envelope -> calls.incrementAndGet()))
                .isEqualTo(MailDispatchOutcome.SUPPRESSED);
        assertThat(calls).hasValue(0);

        useCase.update(new UpdateMailPolicyCommand(MailMode.ENABLED, 0));
        assertThat(calls).hasValue(0);

        AtomicReference<MailEnvelope> sent = new AtomicReference<>();
        assertThat(gate.dispatch(command(MailAudience.STUDENT), sent::set))
                .isEqualTo(MailDispatchOutcome.SENT);
        assertThat(calls).hasValue(0);
        assertThat(sent.get().to()).containsExactlyElementsOf(INTENDED.to());
    }

    @Test
    @DisplayName("Provider timeout releases the read lock so PUT is not stuck behind SMTP")
    void timeoutReleasesTheReadLock() {
        store.current.set(policy(MailMode.ENABLED));
        when(users.resolve()).thenReturn(coordinator());
        MailDispatchGateService shortTimeout = new MailDispatchGateService(store, Duration.ofMillis(50));
        UpdateMailPolicyUseCase shortUpdate = new UpdateMailPolicyUseCase(store, users, shortTimeout);
        try {
            assertThatThrownBy(() -> shortTimeout.dispatch(command(MailAudience.STUDENT), envelope -> {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            })).isInstanceOf(IllegalStateException.class).hasMessage("Mail provider timed out");

            MailPolicy updated = shortUpdate.update(new UpdateMailPolicyCommand(MailMode.DISABLED, 1));
            assertThat(updated.getMode()).isEqualTo(MailMode.DISABLED);
        } finally {
            shortTimeout.shutdownProviderCalls();
        }
    }

    private static MailDispatchCommand command(MailAudience audience) {
        return new MailDispatchCommand(audience, INTENDED);
    }

    private static MailPolicy policy(MailMode mode) {
        MailPolicy policy = MailPolicy.initial();
        if (mode != MailMode.DISABLED) {
            policy.changeMode(mode, 4L, CHANGED_AT);
        }
        return policy;
    }

    private static AuthenticatedUser coordinator() {
        return AuthenticatedUser.builder()
                .userId(7L)
                .role(RoleType.COORDINATOR)
                .build();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the provider");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the provider was in progress", ex);
        }
    }

    private static final class PolicyStore implements MailPolicyRepositoryPort {

        private final AtomicReference<MailPolicy> current = new AtomicReference<>();
        private final AtomicBoolean swapEntered = new AtomicBoolean();
        private final AtomicBoolean transactionOpen = new AtomicBoolean();
        private volatile RuntimeException loadFailure;

        @Override
        public MailPolicy load() {
            if (loadFailure != null) {
                throw loadFailure;
            }
            MailPolicy policy = current.get();
            if (policy == null) {
                throw new IllegalStateException("Mail policy row is missing");
            }
            return policy;
        }

        @Override
        public MailPolicy compareAndSwap(
                UpdateMailPolicyCommand command,
                Long actorUserId,
                Instant occurredAt) {
            swapEntered.set(true);
            transactionOpen.set(true);
            try {
                MailPolicy policy = current.get();
                if (policy.getVersion() != command.version()) {
                    throw new MailPolicyVersionConflictException(policy.getVersion());
                }
                if (policy.getMode() != command.mode()) {
                    policy.changeMode(command.mode(), actorUserId, occurredAt);
                }
                return policy;
            } finally {
                transactionOpen.set(false);
            }
        }
    }
}
