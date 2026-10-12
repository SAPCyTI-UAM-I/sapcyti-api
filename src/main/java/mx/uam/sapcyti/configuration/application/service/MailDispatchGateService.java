package mx.uam.sapcyti.configuration.application.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import mx.uam.sapcyti.configuration.domain.model.MailAudience;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchCommand;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchOutcome;
import mx.uam.sapcyti.configuration.domain.model.MailEnvelope;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.port.in.MailDispatchPort;
import mx.uam.sapcyti.configuration.domain.port.out.MailPolicyRepositoryPort;

/**
 * Single-process mail dispatch gate (SPEC-042).
 *
 * <p>Each attempt takes the shared read lock before loading {@link MailPolicy}
 * and holds it until the provider call finishes, fails, or hits
 * {@code app.mail-dispatch.provider-timeout}. {@link UpdateMailPolicyUseCase}
 * takes the write lock around the short compare-and-swap. Test mailboxes are
 * fixed; this service does not read them from the API.
 */
@Service
public class MailDispatchGateService implements MailDispatchPort {

    private static final String STUDENT_TEST_MAILBOX = "sapcytialumno@gmail.com";
    private static final String STAFF_TEST_MAILBOX = "sapcytiprofesor@gmail.com";

    private static final Logger LOG = LoggerFactory.getLogger(MailDispatchGateService.class);

    private final MailPolicyRepositoryPort policies;
    private final Duration providerTimeout;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final ExecutorService providerCalls = Executors.newCachedThreadPool(
            MailDispatchGateService::daemonThread);

    public MailDispatchGateService(
            MailPolicyRepositoryPort policies,
            @Value("${app.mail-dispatch.provider-timeout:30s}") Duration providerTimeout) {
        if (providerTimeout == null || !providerTimeout.isPositive()) {
            throw new IllegalArgumentException("Mail provider timeout must be positive");
        }
        this.policies = policies;
        this.providerTimeout = providerTimeout;
    }

    /**
     * Runs {@code action} while holding the write lock. The action is the short
     * SQL compare-and-swap. It must not call the mail provider.
     *
     * @param action work that commits before this method releases the lock
     * @param <T>    result of {@code action}
     * @return the result of {@code action}
     */
    public <T> T withWriteLock(Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public MailDispatchOutcome dispatch(MailDispatchCommand command, MailSender sender) {
        if (command == null || command.audience() == null) {
            return MailDispatchOutcome.BLOCKED;
        }
        boolean held = false;
        try {
            lock.readLock().lockInterruptibly();
            held = true;
            MailMode mode = readMode();
            if (mode == null) {
                return MailDispatchOutcome.BLOCKED;
            }
            return apply(mode, command, sender);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mail dispatch lock interrupted", ex);
        } finally {
            if (held) {
                lock.readLock().unlock();
            }
        }
    }

    @PreDestroy
    void shutdownProviderCalls() {
        providerCalls.shutdownNow();
    }

    boolean hasQueuedThreads() {
        return lock.hasQueuedThreads();
    }

    private MailDispatchOutcome apply(MailMode mode, MailDispatchCommand command, MailSender sender) {
        switch (mode) {
            case DISABLED:
                return MailDispatchOutcome.SUPPRESSED;
            case TEST:
                accept(sender, testEnvelope(command.audience()));
                return MailDispatchOutcome.SENT;
            case ENABLED:
                accept(sender, command.intended());
                return MailDispatchOutcome.SENT;
            default:
                return MailDispatchOutcome.BLOCKED;
        }
    }

    private MailMode readMode() {
        try {
            MailPolicy policy = policies.load();
            if (policy == null) {
                return null;
            }
            return policy.getMode();
        } catch (RuntimeException ex) {
            LOG.warn("Mail policy could not be read for dispatch", ex);
            return null;
        }
    }

    private void accept(MailSender sender, MailEnvelope envelope) {
        if (sender == null) {
            throw new IllegalArgumentException("Mail sender is required");
        }
        Future<Void> call = providerCalls.submit(() -> {
            sender.send(envelope);
            return null;
        });
        try {
            call.get(providerTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException ex) {
            call.cancel(true);
            throw new IllegalStateException("Mail provider timed out", ex);
        } catch (InterruptedException ex) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mail dispatch interrupted", ex);
        } catch (ExecutionException ex) {
            call.cancel(true);
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Mail provider failed", cause);
        }
    }

    private static MailEnvelope testEnvelope(MailAudience audience) {
        String mailbox = switch (audience) {
            case STUDENT -> STUDENT_TEST_MAILBOX;
            case STAFF -> STAFF_TEST_MAILBOX;
        };
        return new MailEnvelope(List.of(mailbox), List.of(), List.of());
    }

    private static Thread daemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "mail-dispatch");
        thread.setDaemon(true);
        return thread;
    }
}
