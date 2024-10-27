package ceb.db;

import java.sql.SQLType;
import java.sql.Types;
import java.util.List;

public class Where {
    private String sql;

    public Where(String sql) {
        this.sql = sql;
    }

    public Where(Column column, List<? extends Object> objects) {
        StringBuilder b = new StringBuilder();
        b.append(column.toQuotedSQL());
        final int size = objects.size();
        if (size == 0) {
            throw new IllegalArgumentException("empty values");
        }
        if (size > 1) {
            b.append(" IN (");

            for (int i = 0; i < size; i++) {
                if (column.getDatatype() == Types.VARCHAR) {
                    b.append("'");
                    b.append(objects.get(i));
                    b.append("'");
                } else {
                    b.append(objects.get(i));
                }
                if (i < size - 1) {
                    b.append(',');
                }
            }
            b.append(")");
        } else

        {
            if (column.getDatatype() == Types.VARCHAR) {
                b.append('=');
                b.append("'");
                b.append(objects.get(0));
                b.append("'");
            } else {
                b.append('=');
                b.append(objects.get(0));
            }
        }
        this.sql = b.toString();
    }

    public String getSQL() {
        return sql;
    }

}
