package com.example.store.user;

import com.example.store.common.StoreProperties;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPasswordResetService implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public DefaultPasswordResetService(UserRepository userRepository,
                                       PasswordResetTokenRepository passwordResetTokenRepository,
                                       PasswordEncoder passwordEncoder,
                                       StoreProperties storeProperties,
                                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    @Override
    public PasswordResetTokenResponse requestPasswordReset(String email) {
        String normalizedEmail = EmailAddress.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));
        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID().toString(),
                user,
                clock.instant().plus(storeProperties.passwordReset().timeToLive()));
        return PasswordResetTokenResponse.of(passwordResetTokenRepository.save(token));
    }

    @Override
    public void resetPassword(String token, String newRawPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findById(token)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (!resetToken.isUsableAt(clock.instant())) {
            throw new InvalidPasswordResetTokenException();
        }
        resetToken.markUsed();
        User user = resetToken.getUser();
        user.changePassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }
}
