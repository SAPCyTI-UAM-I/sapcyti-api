package mx.uam.sapcyti.configuration.domain.model;

import java.util.List;

/**
 * Resolved mail envelope: To, Cc and Bcc only.
 *
 * <p>The dispatch gate builds this value and passes it to the sender. In test mode
 * the lists contain the fixed mailbox alone and do not keep the caller's addresses.
 */
public record MailEnvelope(List<String> to, List<String> cc, List<String> bcc) {

    public MailEnvelope {
        to = copy(to);
        cc = copy(cc);
        bcc = copy(bcc);
    }

    private static List<String> copy(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        return List.copyOf(addresses);
    }
}
