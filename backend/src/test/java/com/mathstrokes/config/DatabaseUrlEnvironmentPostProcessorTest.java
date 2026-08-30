package com.mathstrokes.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deployment fails in ways that are tedious to debug, so the URL translation is tested rather
 * than trusted. A password containing a reserved character is the case that actually bites.
 */
class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor =
            new DatabaseUrlEnvironmentPostProcessor();

    private MockEnvironment process(String databaseUrl) {
        MockEnvironment environment = new MockEnvironment();
        if (databaseUrl != null) {
            environment.setProperty("DATABASE_URL", databaseUrl);
        }
        processor.postProcessEnvironment(environment, null);
        return environment;
    }

    @Test
    @DisplayName("a platform postgresql:// URL becomes a JDBC URL with credentials split out")
    void convertsPlatformUrl() {
        MockEnvironment environment =
                process("postgresql://iota_user:s3cret@dpg-abc.singapore-postgres.render.com:5432/iota_db");

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-abc.singapore-postgres.render.com:5432/iota_db");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("iota_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("s3cret");
    }

    @Test
    @DisplayName("query parameters survive, so sslmode=require is not silently dropped")
    void preservesQueryParameters() {
        MockEnvironment environment =
                process("postgresql://u:p@ep-cool.aws.neon.tech/neondb?sslmode=require");

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-cool.aws.neon.tech/neondb?sslmode=require");
    }

    @Test
    @DisplayName("the postgres:// spelling is accepted too")
    void acceptsShortScheme() {
        MockEnvironment environment = process("postgres://u:p@host:5432/db");

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/db");
    }

    @Test
    @DisplayName("a percent-encoded password is decoded, so a '@' in it does not break the URL")
    void decodesEncodedPassword() {
        MockEnvironment environment = process("postgresql://user:p%40ss%3Aword@host:5432/db");

        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("p@ss:word");
    }

    @Test
    @DisplayName("an existing jdbc: URL is left completely alone")
    void passesThroughJdbcUrl() {
        MockEnvironment environment =
                process("jdbc:postgresql://localhost:5432/mathstrokes");

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    @DisplayName("no DATABASE_URL at all is not an error")
    void toleratesMissingUrl() {
        MockEnvironment environment = process(null);

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    @DisplayName("a URL without credentials leaves username and password to other configuration")
    void doesNotBlankCredentialsWhenUrlHasNone() {
        MockEnvironment environment = process("postgresql://host:5432/db");

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/db");
        assertThat(environment.getProperty("spring.datasource.username")).isNull();
    }
}
