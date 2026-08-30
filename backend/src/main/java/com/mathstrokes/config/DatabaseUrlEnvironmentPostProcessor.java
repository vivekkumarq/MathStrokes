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
        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank() || raw.startsWith("jdbc:")) {
            return;
        }
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
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

        // addFirst so this beats the DATABASE_URL placeholder in application.yml.
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, resolved));
    }

    /** Passwords arrive percent-encoded when they contain characters a URL reserves. */
    private String decode(String value) {
        return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
