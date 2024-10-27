package ceb.db;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Table {
    public static final Logger LOGGER = Logger.getLogger(Table.class.getName());
    private Schema schema;
    private String name;
    private boolean loaded;
    private List<Column> columns = new ArrayList<>();

    public Table(Schema schema, String tableName) {
        this.schema = schema;
        this.name = tableName;
    }

    public Schema getSchema() {
        return schema;
    }

    public void addColumn(Column column) {
        this.columns.add(column);

    }

    public void dump(PrintStream out) {
        out.println("Table " + this.name + " (" + this.columns.size() + " columns)");
        for (Column c : this.columns) {
            if (c.isPrimaryKey()) {
                out.println("* " + c.getName());
            } else if (c.isForeignKey()) {
                if (c.getForeignShemaName().equals(schema.getName())) {
                    out.println("+ " + c.getName() + " -> " + c.getForeignTableName() + "." + c.getForeignColumnName());
                } else {
                    out.println("+ " + c.getName() + " -> " + c.getForeignShemaName() + "." + c.getForeignTableName() + "." + c.getForeignColumnName());
                }
            } else {
                out.println("- " + c.getName() + " " + c.getDatatype());
            }
        }

    }

    public String getName() {
        return this.name;
    }

    public void addPrimaryKey(String name) {
        this.getColumn(name).setPrimaryKey(true);
    }

    public Column getColumn(String name) {
        for (Column c : this.columns) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public void sort() {
        Collections.sort(this.columns, new Comparator<Column>() {

            @Override
            public int compare(Column o1, Column o2) {
                boolean k1 = o1.isPrimaryKey();
                boolean k2 = o2.isPrimaryKey();
                //
                if (k1 && !k2) {
                    return -1;
                }
                if (!k1 && k2) {
                    return 1;
                }
                k1 = o1.isForeignKey();
                k2 = o2.isForeignKey();
                if (k1 && !k2) {
                    return -1;
                }
                if (!k1 && k2) {
                    return 1;
                }

                //
                return o1.getName().compareTo(o2.getName());
            }
        });

    }

    public void setLoaded(boolean b) {
        this.loaded = b;

    }

    public void addForeignKey(String column, String toSchema, String toTable, String toColumn) {
        this.getColumn(column).setForeign(toSchema, toTable, toColumn);
    }

    public List<Column> getReferentFields(String field) {
        Column f = getColumn(field);
        if (f == null) {
            throw new IllegalArgumentException(field + " is not a column of table (" + this.schema.getName() + ") " + this.getName());
        }
        return schema.getDataBase().getReferents(f);
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> fectchRows() throws SQLException {
        return fectchRows(null);
    }

    public List<Row> fectchRows(Where where) throws SQLException {
        Connection c = getSchema().getCurrentConnection();
        Statement s = c.createStatement();
        StringBuilder b = new StringBuilder();
        b.append("SELECT ");
        int columnsSize = this.columns.size();
        List<Table> tables = new ArrayList<>();
        for (int i = 0; i < columnsSize; i++) {
            b.append(this.columns.get(i).toQuotedSQL());
            if (i < columnsSize - 1) {
                b.append(',');
            }
            Table t = this.columns.get(i).getTable();
            if (!tables.contains(t)) {
                tables.add(t);
            }
        }
        b.append(" FROM ");
        int tablesSize = tables.size();
        for (int i = 0; i < tablesSize; i++) {
            b.append(tables.get(i).toQuotedSQL());
            if (i < tablesSize - 1) {
                b.append(',');
            }
        }
        if (where != null) {
            b.append(" WHERE ");
            b.append(where.getSQL());
        }
        LOGGER.log(Level.FINE, "{0}", b.toString());
        ResultSet rs = s.executeQuery(b.toString());
        List<Row> rows = new ArrayList<>();
        while (rs.next()) {
            Map<Column, Object> val = new HashMap<>();
            for (int i = 0; i < columnsSize; i++) {
                Column col = this.columns.get(i);
                Object o = rs.getObject(i + 1);
                val.put(col, o);
            }
            rows.add(new Row(this, val));

        }
        return rows;
    }

    private String toQuotedSQL() {
        return "\"" + getSchema().getName() + "\".\"" + getName() + "\"";
    }

    public List<Column> getPrimaryKeys() {
        List<Column> cols = new ArrayList<>();
        for (Column c : this.columns) {
            if (c.isPrimaryKey()) {
                cols.add(c);
            }
        }
        return cols;
    }

    public Column getPrimaryKey() {
        List<Column> cols = getPrimaryKeys();
        if (cols.isEmpty()) {
            return null;
        }
        if (cols.size() == 1) {
            return cols.get(0);
        }
        throw new IllegalStateException("multiple primary key on table " + this.getSchema().getName() + " " + this.getName());
    }

    public List<Integer> fetchAllPrimaryKeys() throws SQLException {
        final Connection c = getSchema().getCurrentConnection();
        final List<Integer> rows = new ArrayList<>();
        try (Statement s = c.createStatement()) {
            StringBuilder b = new StringBuilder();
            b.append("SELECT ");
            b.append(this.getPrimaryKey().toQuotedSQL());
            b.append(" FROM ");
            b.append(toQuotedSQL());

            try (ResultSet rs = s.executeQuery(b.toString())) {
                while (rs.next()) {
                    rows.add(rs.getInt(1));
                }
            }
        }
        return rows;
    }

    public List<List<Integer>> fetchAllMultiplePrimaryKeys() throws SQLException {
        Connection c = getSchema().getCurrentConnection();
        List<Column> pkey = this.getPrimaryKeys();

        Statement s = c.createStatement();
        StringBuilder b = new StringBuilder();
        b.append("SELECT ");
        int columnsSize = pkey.size();
        for (int i = 0; i < columnsSize; i++) {
            b.append(pkey.get(i).toQuotedSQL());
            if (i < columnsSize - 1) {
                b.append(',');
            }

        }

        b.append(" FROM ");
        b.append(toQuotedSQL());
        ResultSet rs = s.executeQuery(b.toString());
        List<List<Integer>> rows = new ArrayList<>();

        while (rs.next()) {
            List<Integer> l = new ArrayList<>(columnsSize);
            for (int i = 0; i < columnsSize; i++) {
                l.add(rs.getInt(i + 1));
            }
            rows.add(l);
        }

        return rows;
    }

    public void insertNewRow(MutableRow row) throws SQLException {

        if (row.getColumns().isEmpty()) {
            throw new IllegalStateException("empty row");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ");
        builder.append(toQuotedSQL());
        builder.append(" (");
        //
        final List<Column> cols = this.getColumns();
        int stop = cols.size();
        List<String> fielsNames = new ArrayList<>();
        for (int i = 0; i < stop; i++) {
            final Column field = cols.get(i);
            if (!field.isPrimaryKey() && !field.getName().equals("ID_USER_COMMON_CREATE") && !field.getName().equals("ID_USER_COMMON_MODIFY") && !field.getName().equals("ORDRE")) {
                final String fieldName = cols.get(i).getName();
                if (row.getColumn(fieldName) != null) {
                    fielsNames.add("\"" + field.getName() + "\"");
                }
            }
        }
        builder.append(String.join(",", fielsNames));

        builder.append(") VALUES (");
        List<String> values = new ArrayList<>();
        for (int i = 0; i < stop; i++) {

            final Column field = cols.get(i);
            if (!field.isPrimaryKey() && !field.getName().equals("ID_USER_COMMON_CREATE") && !field.getName().equals("ID_USER_COMMON_MODIFY") && !field.getName().equals("ORDRE")) {
                final String fieldName = cols.get(i).getName();
                if (row.getColumn(fieldName) != null) {

                    final Object value = row.getObject(fieldName);
                    if (value != null) {
                        if (value instanceof Number) {
                            values.add(String.valueOf(((Number) value).longValue()));
                        } else if (value instanceof String) {
                            values.add('\'' + value.toString().replace("\'", "''") + '\'');
                        } else if (value instanceof Timestamp) {
                            values.add('\'' + value.toString().replace("\'", "''") + '\'');
                        } else if (value instanceof Boolean) {
                            if ((boolean) value) {
                                values.add("true");
                            } else {
                                values.add("false");
                            }
                        } else if (value instanceof UUID) {
                            values.add('\'' + value.toString().replace("\'", "''") + '\'');
                        } else {
                            throw new IllegalArgumentException(value.getClass().getName() + "not yet supported");
                        }
                    } else {
                        values.add("NULL");
                    }

                }
            }
        }
        builder.append(String.join(",", values));
        // System.out.print(values);

        builder.append(");");
        final String sql = builder.toString();
        System.out.print(sql);
        LOGGER.log(Level.FINE, "{0}", sql);

        // Execute
        Connection c = getSchema().getCurrentConnection();
        c.createStatement().executeQuery(sql);

    }

    public int updateRow(MutableRow row) throws SQLException {
        if (row.getColumns().isEmpty()) {
            throw new IllegalStateException("empty row");
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("UPDATE ");
        builder.append(this.toQuotedSQL());
        builder.append(" SET ");
        //
        final List<Column> cols = this.getColumns();
        int stop = cols.size();

        for (int i = 0; i < stop; i++) {
            final Column field = cols.get(i);
            final String fieldName = field.getName();

            if (!field.isPrimaryKey() && !fieldName.equals("GID")) {
                builder.append("\"" + field.getName() + "\"");
                builder.append("=");

                Object value = row.getObject(fieldName);

                if (value != null) {
                    if (value instanceof Integer || value instanceof Long) {
                        builder.append(String.valueOf(((Number) value).longValue()));
                    } else if (value instanceof Float || value instanceof Double) {
                        builder.append(String.format(Locale.US, "%.6f", ((Number) value).floatValue()));
                    } else if (value instanceof String) {
                        builder.append('\'' + value.toString().replace("\'", "''") + '\'');
                    } else if (value instanceof Timestamp) {
                        builder.append('\'' + value.toString().replace("\'", "''") + '\'');
                    } else if (value instanceof Boolean) {
                        if ((boolean) value) {
                            builder.append("true");
                        } else {
                            builder.append("false");
                        }
                    } else if (value instanceof UUID) {
                        builder.append('\'' + value.toString().replace("\'", "''") + '\'');
                    } else {
                        throw new IllegalArgumentException(value.getClass().getName() + "not yet supported");
                    }

                } else {
                    builder.append("null");
                }
                if (i < stop - 1) {
                    builder.append(',');
                }
            }
        }
        builder.append(" WHERE ");

        String primKeyName = "\"" + row.getTable().getPrimaryKey().getName() + "\"";
        Integer primKeyValue = row.getInt(row.getTable().getPrimaryKey().getName());

        builder.append(primKeyName + "=" + primKeyValue);

        int resultCode = -1;

        LOGGER.log(Level.FINE, "{0}", builder.toString());
        try (Statement st = getSchema().getCurrentConnection().createStatement()) {
            LOGGER.log(Level.FINE, builder.toString());
            resultCode = st.executeUpdate(builder.toString());
        }

        return resultCode;
    }

    public int[] deleteRowsByID(List<String> ids, boolean simulate) throws SQLException {
        int[] resultCode = null;
        Connection c = getSchema().getCurrentConnection();
        c.setAutoCommit(false);
        Statement st = c.createStatement();

        try {
            for (String id : ids) {
                if (simulate) {
                    System.out.println("DELETE FROM " + this.toQuotedSQL() + " WHERE \"ID\"=" + id);
                } else {
                    st.addBatch("DELETE FROM " + this.toQuotedSQL() + " WHERE \"ID\"=" + id);
                }
            }
            resultCode = st.executeBatch();
            getSchema().getCurrentConnection().commit();
        } finally {
            st.close();
            getSchema().getCurrentConnection().close();
        }

        return resultCode;
    }
}
