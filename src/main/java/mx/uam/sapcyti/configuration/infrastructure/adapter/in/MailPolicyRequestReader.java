package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import mx.uam.sapcyti.configuration.domain.exception.MailPolicyFieldError;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyValidationException;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;

/**
 * Parses a PUT /api/mail-policy body against the SPEC-041 whitelist.
 * Phases stop at the first failure: shape, then mode, then version.
 */
final class MailPolicyRequestReader {

    private static final JsonFactory JSON = new JsonFactory();
    private static final BigInteger MAX_VERSION = BigInteger.valueOf(Integer.MAX_VALUE);

    private MailPolicyRequestReader() {
    }

    static UpdateMailPolicyCommand parse(byte[] raw) {
        if (isBlank(raw)) {
            throw validation(List.of(error("body", "REQUIRED", "Request body is required")));
        }
        try (JsonParser parser = JSON.createParser(raw)) {
            return parseObject(parser);
        } catch (MailPolicyValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw validation(List.of(error(
                    "body", "INVALID_TYPE", "Request body must be a JSON object")));
        }
    }

    private static UpdateMailPolicyCommand parseObject(JsonParser parser) throws IOException {
        JsonToken first = parser.nextToken();
        if (first == null) {
            throw validation(List.of(error("body", "REQUIRED", "Request body is required")));
        }
        if (first != JsonToken.START_OBJECT) {
            throw validation(List.of(error(
                    "body", "INVALID_TYPE", "Request body must be a JSON object")));
        }

        boolean modeSeen = false;
        boolean versionSeen = false;
        boolean modeNull = false;
        boolean versionNull = false;
        boolean extra = false;
        JsonToken modeToken = null;
        String modeText = null;
        JsonToken versionToken = null;
        String versionText = null;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.FIELD_NAME) {
                throw validation(List.of(error(
                        "body", "INVALID_TYPE", "Request body must be a JSON object")));
            }
            String name = parser.currentName();
            JsonToken valueToken = parser.nextToken();
            if (valueToken == null) {
                throw validation(List.of(error(
                        "body", "INVALID_TYPE", "Request body must be a JSON object")));
            }
            if ("mode".equals(name)) {
                if (modeSeen) {
                    extra = true;
                    parser.skipChildren();
                } else {
                    modeSeen = true;
                    modeToken = valueToken;
                    if (valueToken == JsonToken.VALUE_NULL) {
                        modeNull = true;
                    } else if (valueToken == JsonToken.VALUE_STRING) {
                        modeText = parser.getText();
                    } else {
                        parser.skipChildren();
                    }
                }
            } else if ("version".equals(name)) {
                if (versionSeen) {
                    extra = true;
                    parser.skipChildren();
                } else {
                    versionSeen = true;
                    versionToken = valueToken;
                    if (valueToken == JsonToken.VALUE_NULL) {
                        versionNull = true;
                    } else if (valueToken == JsonToken.VALUE_NUMBER_INT) {
                        versionText = parser.getText();
                    } else {
                        parser.skipChildren();
                    }
                }
            } else {
                extra = true;
                parser.skipChildren();
            }
        }
        if (parser.nextToken() != null) {
            throw validation(List.of(error(
                    "body", "INVALID_TYPE", "Request body must be a JSON object")));
        }

        List<MailPolicyFieldError> shapeErrors = new ArrayList<>();
        if (!modeSeen || modeNull) {
            shapeErrors.add(error("mode", "REQUIRED", "Mode is required"));
        }
        if (!versionSeen || versionNull) {
            shapeErrors.add(error("version", "REQUIRED", "Version is required"));
        }
        if (extra) {
            shapeErrors.add(error(
                    "body", "INVALID_VALUE", "Request body contains unsupported fields"));
        }
        if (!shapeErrors.isEmpty()) {
            throw validation(shapeErrors);
        }

        if (modeToken != JsonToken.VALUE_STRING) {
            throw validation(List.of(error("mode", "INVALID_TYPE", "Mode must be a string")));
        }
        MailMode mode;
        try {
            mode = MailMode.valueOf(modeText);
        } catch (IllegalArgumentException ex) {
            throw validation(List.of(error(
                    "mode", "INVALID_ENUM", "Use DISABLED, TEST or ENABLED")));
        }
        return new UpdateMailPolicyCommand(mode, parseVersion(versionToken, versionText));
    }

    private static int parseVersion(JsonToken token, String numberText) {
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw validation(List.of(error(
                    "version", "INVALID_TYPE", "Version must be an integer")));
        }
        BigInteger value;
        try {
            value = new BigInteger(numberText);
        } catch (NumberFormatException ex) {
            throw validation(List.of(error(
                    "version", "INVALID_TYPE", "Version must be an integer")));
        }
        if (value.signum() < 0 || value.compareTo(MAX_VERSION) > 0) {
            throw validation(List.of(error(
                    "version",
                    "OUT_OF_RANGE",
                    "Version must be an integer greater than or equal to 0")));
        }
        return value.intValue();
    }

    private static MailPolicyValidationException validation(List<MailPolicyFieldError> errors) {
        return new MailPolicyValidationException(errors);
    }

    private static MailPolicyFieldError error(String field, String code, String message) {
        return new MailPolicyFieldError(field, code, message);
    }

    private static boolean isBlank(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return true;
        }
        for (byte value : raw) {
            if (value != ' ' && value != '\n' && value != '\r' && value != '\t') {
                return false;
            }
        }
        return true;
    }
}
