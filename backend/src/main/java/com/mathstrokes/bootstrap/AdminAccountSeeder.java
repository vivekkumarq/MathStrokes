package com.mathstrokes.bootstrap;

import com.mathstrokes.auth.service.SecurityAnswers;
import com.mathstrokes.common.enums.RoleName;
import com.mathstrokes.config.AppProperties;
import com.mathstrokes.user.entity.Role;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.repository.RoleRepository;
import com.mathstrokes.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator, once, from configuration.
 *
 * There are no credentials in this file or in any migration. The phone number, password and
 * security answer come from the environment, and if any of them is missing the seeder does
 * nothing and says so - it will not invent a default password, because a known default on a
 * deployed instance is an open door.
 *
 * It also refuses to run if an admin already exists, so restarting the application can never
 * reset a live account.
 */
@Component
@Order(1)
public class AdminAccountSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(AppProperties appProperties, UserRepository userRepository,
                              RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdmin() {
        AppProperties.Seed seed = appProperties.getSeed();
        if (!seed.isEnabled()) {
            return;
        }
        if (userRepository.countByRole(RoleName.ROLE_ADMIN) > 0) {
            log.debug("An administrator already exists; skipping admin seeding");
            return;
        }
        if (isBlank(seed.getAdminPhoneNumber()) || isBlank(seed.getAdminPassword())
                || isBlank(seed.getAdminSecurityAnswer())) {
            log.warn("No administrator exists and SEED_ADMIN_PHONE / SEED_ADMIN_PASSWORD / "
                    + "SEED_ADMIN_SECURITY_ANSWER are not all set, so none was created. "
                    + "Set them and restart to create the first admin account.");
            return;
        }
        if (userRepository.existsByPhoneNumber(seed.getAdminPhoneNumber().trim())) {
            log.warn("SEED_ADMIN_PHONE is already registered to another account; "
                    + "no administrator was created.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_ADMIN is missing. Has the reference-data migration run?"));

        User admin = new User();
        admin.setFullName(seed.getAdminFullName());
        admin.setPhoneNumber(seed.getAdminPhoneNumber().trim());
        admin.setPasswordHash(passwordEncoder.encode(seed.getAdminPassword()));
        admin.setSecurityQuestion(seed.getAdminSecurityQuestion());
        admin.setSecurityAnswerHash(passwordEncoder.encode(
                SecurityAnswers.normalise(seed.getAdminSecurityAnswer())));
        admin.setEnabled(true);
        admin.addRole(adminRole);
        userRepository.save(admin);

        log.info("Created the first administrator account for phone number {}. "
                        + "Sign in and change the password.",
                mask(admin.getPhoneNumber()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Keeps the full number out of the logs while still identifying the account. */
    private String mask(String phoneNumber) {
        if (phoneNumber.length() <= 4) {
            return "****";
        }
        return "*".repeat(phoneNumber.length() - 4)
                + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
