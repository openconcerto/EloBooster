package ceb.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Row {
    protected Map<Column, Object> values;
    Table table;

    public Row(Table table, Map<Column, Object> values) {
        this.table = table;
        this.values = values;
    }

    public Table getTable() {
        return table;
    }

    public Object getObject(Column column) {
        return values.get(column);
    }

    public Column getColumn(String column) {
        for (Column col : this.values.keySet()) {
            if (col.getName().equals(column)) {
                return col;
            }
        }
        return null;
    }

    public Object getObject(String column) {
        for (Column col : this.values.keySet()) {
            if (col.getName().equals(column)) {
                return getObject(col);
            }
        }
        return null;
    }

    public String getString(String column) {
        return getString(table.getColumn(column));
    }

    public String getString(Column column) {
        final Object object = values.get(column);
        if (object == null)
            return null;
        return object.toString();
    }

    public Integer getInt(String column) {
        return getInt(table.getColumn(column));
    }

    public Long getLong(String column) {
        return (Long) values.get(table.getColumn(column));
    }

    public float getFloat(String column) {
        return ((Number) values.get(table.getColumn(column))).floatValue();
    }

    public Integer getInt(Column column) {
        return (Integer) values.get(column);
    }

    List<Column> getColumns() {
        List<Column> cols = new ArrayList<>();
        cols.addAll(values.keySet());
        Collections.sort(cols, new Comparator<Column>() {

            @Override
            public int compare(Column o1, Column o2) {
                String v1 = o1.getTable().getName() + "...." + o1.getName();
                String v2 = o2.getTable().getName() + "...." + o2.getName();
                return v1.compareTo(v2);
            }
        });
        return cols;
    }

    public int getId() {
        return getInt(table.getPrimaryKey());
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Column c : getColumns()) {
            b.append(c.getName());
            b.append(":");
            b.append(this.getString(c));
            b.append(";");
        }
        return b.toString();

    }

    public MutableRow createMutableRow() {
        Map<Column, Object> m = new HashMap<>();
        for (Column col : this.values.keySet()) {
            m.put(col, this.getObject(col));
        }
        return new MutableRow(this.getTable(), m);
    }

}
