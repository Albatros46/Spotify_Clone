package com.muzikApp.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;

/**
 * SC-06: a database created by the original code (ddl-auto) must be repaired by the migrations.
 * The test builds exactly that old schema, shows the bug, migrates, and shows the fix.
 */
class LegacyDatabaseMigrationTest {

    private static final String INSERT_SONG_WITHOUT_COVER = """
            insert into song (title, artist, album, song_url, image_url, created_at, appuser_id)
            values ('No cover', 'Artist', 'Album', '/uploads/songs/x.wav', null, current_timestamp, 1)""";

    @Test
    void old_database_is_baselined_and_gets_only_the_fix() throws Exception {
        String url = "jdbc:h2:mem:legacy-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=POSITION,KEY,VALUE;DB_CLOSE_DELAY=-1";
        try (Connection db = DriverManager.getConnection(url, "sa", "")) {
            run(db, new String(getClass().getResourceAsStream("/legacy/schema-before-flyway.sql").readAllBytes(),
                    StandardCharsets.UTF_8));

            // the bug, reproduced: no cover -> NOT NULL violation (the API answered 500)
            assertThatThrownBy(() -> run(db, INSERT_SONG_WITHOUT_COVER)).isInstanceOf(SQLException.class)
                    .hasMessageContaining("image_url");

            Flyway flyway = Flyway.configure().dataSource(url, "sa", "")
                    .baselineOnMigrate(true).baselineVersion("1").load();
            flyway.migrate();

            // V1 must NOT run on the old database (its tables already exist), only V2
            assertThat(Arrays.stream(flyway.info().applied()).map(MigrationInfo::getVersion).map(Object::toString))
                    .containsExactly("1", "2");
            assertThat(flyway.info().applied()[0].getDescription()).contains("Baseline");

            run(db, INSERT_SONG_WITHOUT_COVER); // the fix: works now
        }
    }

    @Test
    void new_database_gets_the_full_schema_and_v2_is_harmless() {
        String url = "jdbc:h2:mem:fresh-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=POSITION,KEY,VALUE;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(true).baselineVersion("1").load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
    }

    /** Runs a script: comment lines removed first, then one statement per ";". */
    private static void run(Connection db, String sql) throws SQLException {
        String withoutComments = sql.lines().filter(line -> !line.strip().startsWith("--"))
                .reduce("", (a, b) -> a + "\n" + b);
        try (Statement statement = db.createStatement()) {
            for (String part : withoutComments.split(";")) {
                if (!part.isBlank()) {
                    statement.execute(part);
                }
            }
        }
    }
}
