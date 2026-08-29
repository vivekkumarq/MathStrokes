package com.mathstrokes.config;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mathstrokes.app")
public class AppProperties {

    private List<String> corsAllowedOrigins = List.of("http://localhost:4200");

    private final Seed seed = new Seed();
    private final Exam exam = new Exam();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Seed {
        /** When true the bootstrap seeder creates the default admin and the sample question bank. */
        private boolean enabled = true;
        private String adminFullName = "Platform Administrator";
        private String adminPhoneNumber;
        private String adminPassword;
        private String adminSecurityQuestion = "What is your favourite mathematical constant?";
        private String adminSecurityAnswer;
    }

    @Getter
    @Setter
    public static class Exam {
        private int defaultDurationMinutes = 60;
        private int defaultQuestionCount = 25;
        /**
         * Grace period allowed between the client clock and the server clock when a submission
         * arrives fractionally after expiry. Answers are still rejected past expiresAt; this only
         * decides whether the submission is recorded as SUBMITTED or AUTO_SUBMITTED.
         */
        private int submissionGraceSeconds = 5;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        /** Maximum failed authentication-sensitive requests per client key inside the window. */
        private int maxAttempts = 10;
        private int windowSeconds = 300;
    }
}
