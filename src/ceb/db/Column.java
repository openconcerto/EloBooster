package ceb.db;

public class Column {

    private String name;
    private int datatype;
    private String foreignShema;
    private String foreignTable;
    private String foreignColumn;
    private boolean isPrimaryKey;
    private Table table;

    public Column(Table table, String name, int datatype) {
        this.table = table;
        this.name = name;
        this.datatype = datatype;
    }

    public String getName() {
        return name;
    }

    public void setForeign(String toSchema, String toTable, String toColumn) {
        this.foreignShema = toSchema;
        this.foreignTable = toTable;
        this.foreignColumn = toColumn;
    }

    boolean isForeignKey() {
        return this.foreignColumn != null;
    }

    boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean isPrimaryKey) {
        this.isPrimaryKey = isPrimaryKey;
    }

    public String getForeignShemaName() {
        return foreignShema;
    }

    public String getForeignTableName() {
        return foreignTable;
    }

    public String getForeignColumnName() {
        return foreignColumn;
    }

    public int getDatatype() {
        return datatype;
    }

    public Table getTable() {
        return table;
    }

    @Override
    public String toString() {
        return this.getTable().getSchema().getName() + " " + this.getTable().getName() + "." + this.getName() + " type:" + this.datatype;
    }

    public String toQuotedSQL() {
        return "\"" + this.getTable().getSchema().getName() + "\".\"" + this.getTable().getName() + "\".\"" + this.getName() + "\"";
    }

}
