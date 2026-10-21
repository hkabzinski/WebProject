package com.example.store.user;

import com.example.store.session.SessionService;
import com.example.store.session.UserSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthenticationService implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptPolicy loginAttemptPolicy;
    private final SessionService sessionService;

    public DefaultAuthenticationService(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder,
                                        LoginAttemptPolicy loginAttemptPolicy,
                                        SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptPolicy = loginAttemptPolicy;
        this.sessionService = sessionService;
    }

    @Override
    public UserSession login(String email, String rawPassword) {
        User user = userRepository.findByEmail(EmailAddress.normalize(email))
                .orElseThrow(InvalidCredentialsException::new);
        loginAttemptPolicy.verifyNotLocked(user);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginAttemptPolicy.registerFailedAttempt(user);
            throw new InvalidCredentialsException();
        }
        loginAttemptPolicy.registerSuccessfulAttempt(user);
        return sessionService.openSession(user);
    }
}
