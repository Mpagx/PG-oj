package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Upgrade old checkouts without assuming the queue columns already exist. */
public class V2__queue_and_account_constraints extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        column(connection, "judgeAttempt", "int not null default 0");
        column(connection, "judgeToken", "varchar(64) null");
        column(connection, "judgeDeadline", "datetime null");
        column(connection, "nextRetryTime", "datetime null");
        column(connection, "lastError", "varchar(1024) null");
        index(connection, "question_submit", "idx_judge_queue", "(status, nextRetryTime)", false);
        index(connection, "question_submit", "idx_judge_recovery", "(status, judgeDeadline)", false);
        try (Statement statement = connection.createStatement();
             ResultSet duplicates = statement.executeQuery("SELECT userAccount FROM user WHERE userAccount IS NOT NULL GROUP BY userAccount HAVING COUNT(*) > 1 LIMIT 1")) {
            if (duplicates.next()) throw new IllegalStateException("存在重复账号，请先人工合并账号后执行迁移；迁移不会删除用户");
        }
        index(connection, "user", "uk_userAccount", "(userAccount)", true);
    }
    private void column(Connection connection, String name, String definition) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "question_submit", name)) {
            if (columns.next()) return;
        }
        try (Statement statement = connection.createStatement()) { statement.execute("ALTER TABLE question_submit ADD COLUMN " + name + " " + definition); }
    }
    private void index(Connection connection, String table, String name, String columns, boolean unique) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (indexes.next()) if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return;
        }
        try (Statement statement = connection.createStatement()) { statement.execute("CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + name + " ON " + table + " " + columns); }
    }
}
