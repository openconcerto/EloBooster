package ceb.db;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schema {
    private static final String TABLE_NAME = "TABLE_NAME";
    private Database database;
    private List<Table> tables = new ArrayList<>();
    private String name;
    private boolean loaded;

    Schema(Database database, String name) {
        this.database = database;
        this.name = name;
    }

    void refresh() throws SQLException {
        this.loaded = false;
        this.tables.clear();
        Connection connection = database.getCurrentConnection();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        // Print TABLE_TYPE "TABLE"
        // get columns
        ResultSet columns = databaseMetaData.getColumns(null, this.name, null, null);
        Map<String, Table> tablesMap = new HashMap<>();
        while (columns.next()) {

            String tableName = columns.getString(TABLE_NAME);
            String columnName = columns.getString("COLUMN_NAME");
            int datatype = columns.getInt("DATA_TYPE");
            String columnsize = columns.getString("COLUMN_SIZE");
            String decimaldigits = columns.getString("DECIMAL_DIGITS");
            String isNullable = columns.getString("IS_NULLABLE");
            String isAutoIncrment = columns.getString("IS_AUTOINCREMENT");

            // Printing results
            // System.out.println(tableName + " : " + columnName + "---" + datatype + "---" +
            // columnsize + "---"
            // + decimaldigits + "---" + isNullable + "---" + isAutoIncrment);

            Table t = tablesMap.get(tableName);
            if (t == null) {
                t = new Table(this, tableName);
                tablesMap.put(tableName, t);
            }
            Column column = new Column(t, columnName, datatype);
            t.addColumn(column);
        }

        for (String tName : tablesMap.keySet()) {
            // GetPrimarykeys
            ResultSet pkResultSet = databaseMetaData.getPrimaryKeys(null, this.name, tName);
            while (pkResultSet.next()) {
                String tableName = pkResultSet.getString(TABLE_NAME);
                Table t = tablesMap.get(tableName);
                if (t == null) {
                    t = new Table(this, tableName);
                    tablesMap.put(tableName, t);
                }
                t.addPrimaryKey(pkResultSet.getString("COLUMN_NAME"));
            }
        }
        for (String tName : tablesMap.keySet()) {
            // Get Foreign Keys
            ResultSet FK = databaseMetaData.getImportedKeys(null, this.name, tName);
            while (FK.next()) {
                String fromSchema = FK.getString("FKTABLE_SCHEM");
                final String fromTable = FK.getString("FKTABLE_NAME");
                final String fromColumn = FK.getString("FKCOLUMN_NAME");

                final String toSchema = FK.getString("PKTABLE_SCHEM");
                final String toTable = FK.getString("PKTABLE_NAME");
                final String toColumn = FK.getString("PKCOLUMN_NAME");
                // System.out.println(fromSchema + " " + fromTable + " " + fromColumn + "===>" +
                // toSchema + " " + toTable + " "
                // + toColumn);

                Table t = tablesMap.get(fromTable);
                if (t == null) {
                    throw new IllegalStateException("no table  " + fromTable + " in " + tablesMap.keySet());
                }
                t.addForeignKey(fromColumn, toSchema, toTable, toColumn);

            }
        }
        this.tables.addAll(tablesMap.values());
        Collections.sort(this.tables, new Comparator<Table>() {

            @Override
            public int compare(Table o1, Table o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (Table t : this.tables) {
            t.sort();
            t.setLoaded(true);
        }
        this.loaded = true;
    }

    public String getName() {
        return name;
    }

    public Connection getCurrentConnection() throws SQLException {
        return database.getCurrentConnection();
    }

    public Table getTable(String name) {
        for (Table t : this.tables) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    public void dump(PrintStream out) {
        out.println("Schema : " + this.getName());
        for (Table table : this.tables) {
            table.dump(out);
        }

    }

    public List<Table> getTables() {
        return tables;
    }

    public Database getDataBase() {
        return this.database;
    }

    public List<Column> getReferents(Column f) {
        List<Column> columns = new ArrayList<>();
        for (Table table : this.tables) {
            List<Column> cols = table.getColumns();
            for (Column c : cols) {
                if (c.isForeignKey() && c.getForeignColumnName().equals(f.getName()) && c.getForeignTableName().equals(f.getTable().getName())
                        && c.getForeignShemaName().equals(f.getTable().getSchema().getName())) {
                    columns.add(c);
                }

            }
        }

        return columns;
    }

}
