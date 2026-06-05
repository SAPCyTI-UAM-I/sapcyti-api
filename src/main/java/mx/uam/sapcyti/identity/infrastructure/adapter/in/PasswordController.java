package mx.uam.sapcyti.identity.infrastructure.adapter.in;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.application.service.ForgotPasswordUseCase;
import mx.uam.sapcyti.identity.application.service.ResetPasswordUseCase;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.ForgotPasswordRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.ForgotPasswordResponse;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordController {

    private static final String FORGOT_MESSAGE_EN =
            "If an account with that email exists, a recovery email has been sent";
    private static final String FORGOT_MESSAGE_ES =
            "Si existe una cuenta con ese correo, se ha enviado un email de recuperación";

    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        Locale locale = httpRequest.getLocale();
        forgotPasswordUseCase.execute(request.getEmail().trim().toLowerCase(), locale);
        return ResponseEntity.ok(new ForgotPasswordResponse(resolveForgotMessage(locale)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    private String resolveForgotMessage(Locale locale) {
        if (locale != null && locale.getLanguage().startsWith("en")) {
            return FORGOT_MESSAGE_EN;
        }
        return FORGOT_MESSAGE_ES;
    }
}
