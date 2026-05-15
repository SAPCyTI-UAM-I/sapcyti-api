package mx.uam.sapcyti.shared.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void set(Long graduateProgramId) {
        CURRENT.set(graduateProgramId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
