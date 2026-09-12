package com.mathstrokes.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Accepts a platform-style database URL and turns it into what JDBC needs.
 *
 * Managed hosts (Render, Neon, Heroku, Railway) hand out a single credential-bearing URL:
 *
 *     postgresql://user:password@host:5432/dbname?sslmode=require
 *
 * The JDBC driver cannot parse that - it wants a jdbc: scheme and the credentials supplied
 * separately. Without this, deploying means a human copying four values out of a dashboard and
 * reassembling them by hand, which is exactly the sort of step that fails silently at 2am with a
 * password that had a '@' in it.
 *
 * A URL that already starts with "jdbc:" is passed through untouched, so local development and
 * any existing configuration keep working unchanged.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "platformDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        String original = environment.getProperty("DATABASE_URL");
        String raw = unwrap(original);
        if (raw == null || raw.isBlank()) {
            return;
        }

        // Already a JDBC URL. If unwrapping changed it, the stray characters are still in the
        // value the pool would resolve, so republish the cleaned one rather than returning and
        // letting Hikari fail on quotes nobody can see.
        if (raw.startsWith("jdbc:")) {
            if (!raw.equals(original)) {
                publish(environment, Map.<String, Object>of("spring.datasource.url", raw));
            }
            return;
        }

        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            // Nothing here can translate it, and the failure it causes downstream names Hikari
            // rather than this. Say what arrived - the shape only, never the credentials.
            System.err.println("DATABASE_URL is not a URL this application recognises: "
                    + raw.length() + " characters beginning \""
                    + raw.substring(0, Math.min(12, raw.length())) + "\"");
            return;
        }

        URI uri = URI.create(raw);
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getPath());
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbc.append('?').append(uri.getQuery());
        }

        Map<String, Object> resolved = new HashMap<>();
        resolved.put("spring.datasource.url", jdbc.toString());

        // Credentials in the URL win, but only if the URL actually carries them: a host that
        // supplies them as separate variables must not have them blanked out here.
        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int separator = userInfo.indexOf(':');
            if (separator >= 0) {
                resolved.put("spring.datasource.username", decode(userInfo.substring(0, separator)));
                resolved.put("spring.datasource.password", decode(userInfo.substring(separator + 1)));
            } else {
                resolved.put("spring.datasource.username", decode(userInfo));
            }
        }

        publish(environment, resolved);
    }

    /** addFirst so this beats the DATABASE_URL placeholder in application.yml. */
    private void publish(ConfigurableEnvironment environment, Map<String, Object> resolved) {
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, resolved));
    }

    /**
     * Strips surrounding whitespace and quotes.
     *
     * A host that reads its variable names out of a committed .env file can carry the quotes in
     * that file through to the value it injects, and a URL that arrives as "postgresql://..."
     * with the quotes attached matches none of the scheme checks below. It then reaches the
     * connection pool untouched and fails with 'url' must start with "jdbc", which says nothing
     * about the two stray characters that caused it. Cheaper to tolerate than to diagnose again.
     */
    private String unwrap(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /** Passwords arrive percent-encoded when they contain characters a URL reserves. */
    private String decode(String value) {
        return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
