package com.poj.poj.judge;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "poj.migration.tests", matches = "true")
class LegacyMigrationTest {
    @Test void upgradesLegacySchemaToOneUniqueUserNameWithoutLosingRows() throws Exception {
        String configured = System.getenv("E2E_DB_URL");
        if (configured == null || !configured.matches("jdbc:mysql://[^/]+/poj_e2e[a-zA-Z0-9_]*(\\?.*)?")) throw new IllegalStateException("Use an isolated E2E_DB_URL");
        String server = configured.substring(0, configured.lastIndexOf('/') + 1);
        String schema = "poj_e2e_migration_" + Long.toUnsignedString(System.nanoTime());
        String user = System.getenv("E2E_DB_USERNAME"), password = System.getenv("E2E_DB_PASSWORD");
        boolean created = false;
        try (Connection serverConnection = DriverManager.getConnection(server, user, password); Statement statement = serverConnection.createStatement()) {
            try {
                statement.execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); created = true;
                String url = server + schema;
                try (Connection connection = DriverManager.getConnection(url, user, password)) {
                    String original = new String(new ClassPathResource("db/migration/V1__initial_schema.sql").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    String fixture = original.lines().filter(line -> !line.matches(".*(judgeAttempt|judgeToken|judgeDeadline|nextRetryTime|lastError|idx_judge_queue|idx_judge_recovery).*"))
                            .collect(java.util.stream.Collectors.joining("\n")).replaceAll(",\\s*\\)", "\n)");
                    ScriptUtils.executeSqlScript(connection, new ByteArrayResource(fixture.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    try (Statement data = connection.createStatement()) { data.execute("INSERT INTO user(userAccount,userPassword) VALUES ('migration_user','legacy-hash')"); }
                }
                Flyway migration = Flyway.configure().dataSource(url, user, password).baselineVersion("1").cleanDisabled(true).load();
                migration.baseline(); migration.migrate(); migration.validate();
                try (Connection connection = DriverManager.getConnection(url, user, password); Statement data = connection.createStatement()) {
                    try (ResultSet rows = data.executeQuery("SELECT COUNT(*) FROM user")) { rows.next(); assertEquals(1, rows.getInt(1)); }
                    try (ResultSet columns = connection.getMetaData().getColumns(schema, null, "question_submit", "judgeDeadline")) { assertTrue(columns.next()); }
                    try (ResultSet oldColumn = connection.getMetaData().getColumns(schema, null, "user", "userAccount")) { assertFalse(oldColumn.next()); }
                    try (ResultSet cooldownColumn = connection.getMetaData().getColumns(schema, null, "user", "userNameUpdateTime")) { assertTrue(cooldownColumn.next()); }
                    try (ResultSet listTable = connection.getMetaData().getTables(schema, null, "question_list", new String[]{"TABLE"})) { assertTrue(listTable.next()); }
                    try (ResultSet itemTable = connection.getMetaData().getTables(schema, null, "question_list_item", new String[]{"TABLE"})) { assertTrue(itemTable.next()); }
                    try (ResultSet externalTable = connection.getMetaData().getTables(schema, null, "external_problem", new String[]{"TABLE"})) { assertFalse(externalTable.next()); }
                    try (ResultSet referenceColumn = connection.getMetaData().getColumns(schema, null, "question", "referenceSolution")) { assertTrue(referenceColumn.next()); }
                    try (ResultSet solutionTable = connection.getMetaData().getTables(schema, null, "question_solution", new String[]{"TABLE"})) { assertTrue(solutionTable.next()); }
                    try (ResultSet revealTable = connection.getMetaData().getTables(schema, null, "question_solution_reveal", new String[]{"TABLE"})) { assertTrue(revealTable.next()); }
                    assertThrows(java.sql.SQLException.class, () -> data.execute("INSERT INTO user(userName,userPassword) VALUES ('migration_user','duplicate')"));
                }
            } finally {
                // Only remove the exact schema created by this test, never a pre-existing database.
                if (created && schema.matches("poj_e2e_migration_[0-9]+")) statement.execute("DROP DATABASE " + schema);
            }
        }
    }
}
