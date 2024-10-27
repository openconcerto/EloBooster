package ceb.db;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Database {

    List<Schema> schemas = new ArrayList<>();
    private DatabaseConnection databaseConnection;
    private Connection currentConnection;

    public Database() {
        //
    }

    public Connection getCurrentConnection() throws SQLException {
        if (currentConnection == null || currentConnection.isClosed()) {
            try {
                Class.forName("org.hsqldb.jdbc.JDBCDriver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(e);
            }
            String url = databaseConnection.getURL();
            String user = databaseConnection.getLogin();
            String password = databaseConnection.getPassword();
            currentConnection = DriverManager.getConnection(url, user, password);
        }
        return currentConnection;
    }

    public void refresh() throws SQLException {
        this.schemas.clear();
        Connection connection = getCurrentConnection();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet resultSet = databaseMetaData.getSchemas();

        while (resultSet.next()) {
            // Print
            String o = resultSet.getString(1);
            if (!o.equals("pg_catalog") && !o.equals("information_schema")) {
                Schema schema = new Schema(this, o);
                this.schemas.add(schema);
                schema.refresh();
            }

        }
        Collections.sort(this.schemas, new Comparator<Schema>() {

            @Override
            public int compare(Schema o1, Schema o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    public void setDBConnection(DatabaseConnection databaseConnection) throws SQLException {
        this.databaseConnection = databaseConnection;
        refresh();
    }

    public void dump(PrintStream out) throws SQLException {
        out.println("Connection " + this.databaseConnection.getURL());
        out.println("Database :" + this.schemas.size() + " schemas");
        for (Schema schema : this.schemas) {
            schema.dump(out);
        }

    }

    public Schema getSchema(String schemaName) {
        for (Schema schema : this.schemas) {
            if (schema.getName().equals(schemaName)) {
                return schema;
            }
        }
        return null;
    }

    public List<Schema> getSchemas() {
        return schemas;
    }

    public List<Column> getReferents(Column f) {
        List<Column> columns = new ArrayList<>();
        for (Schema schema : this.schemas) {
            columns.addAll(schema.getReferents(f));
        }
        return columns;
    }

    public void execute(String sql) throws SQLException {
        Connection connection = getCurrentConnection();
        try (Statement s = connection.createStatement()) {
            s.execute(sql);
        }
    }

    public void close() throws SQLException {
        execute("SHUTDOWN;");
        Connection connection = getCurrentConnection();

        connection.commit();
        connection.close();
    }
}
