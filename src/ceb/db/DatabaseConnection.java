package ceb.db;

import java.io.File;
import java.sql.SQLException;

public class DatabaseConnection {
    String login = "SA";
    String password = "";
    private File file;

    public DatabaseConnection(File f) {
        this.file = f;
    }

    public Database getDatabase() throws SQLException {
        Database d = new Database();
        d.setDBConnection(this);
        d.refresh();
        return d;
    }

    public String getURL() throws SQLException {
        try {
            String p = this.file.getCanonicalPath();
            p = p.replace('\\', '/');
            return "jdbc:hsqldb:file:" + p + ";shutdown=true";
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
