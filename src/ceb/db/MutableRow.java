package ceb.db;

import java.util.Map;

public class MutableRow extends Row {

    public MutableRow(Table table, Map<Column, Object> values) {
        super(table, values);
    }

    public void set(String fielddName, Object value) {
        final Column col = this.getTable().getColumn(fielddName);
        if (col == null) {
            throw new IllegalArgumentException("field name " + fielddName + " not found in table " + this.getTable().getName() + " (schema : " + this.getTable().getSchema().getName());
        }
        this.values.put(col, value);
    }

}
