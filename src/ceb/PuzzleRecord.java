package ceb;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import ceb.db.Column;
import ceb.db.Database;
import ceb.db.MutableRow;
import ceb.db.Table;

public class PuzzleRecord {
    private int playerId;
    private String puzzleHash;
    private long date;
    private int durationInSeconds;
    private boolean success = true;
    private int hintsCount;

    public int getPlayerId() {
        return this.playerId;
    }

    public void setPlayerId(final int playerId) {
        this.playerId = playerId;
    }

    public String getPuzzleHash() {
        return this.puzzleHash;
    }

    public void setPuzzleHash(final String puzzleHash) {
        this.puzzleHash = puzzleHash;
    }

    public long getDate() {
        return this.date;
    }

    public void setDate(final long date) {
        this.date = date;
    }

    public int getDurationInSeconds() {
        return this.durationInSeconds;
    }

    public void setDurationInSeconds(final int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public int getHintsCount() {
        return this.hintsCount;
    }

    public void setHintsCount(final int hintsCount) {
        this.hintsCount = hintsCount;
    }

    @Override
    public String toString() {
        return this.puzzleHash + " level:" + Puzzle.getPuzzle(this.puzzleHash).getLevel() + " : success:" + this.success + " hints:" + this.hintsCount;
    }

    public void commit(final Database database) throws SQLException {
        final Table table = database.getSchema("PUBLIC").getTable("RECORD");

        final Map<Column, Object> values = new HashMap<>();
        values.put(table.getColumn("PLAYER"), getPlayerId());
        values.put(table.getColumn("HASH"), getPuzzleHash());
        values.put(table.getColumn("DATE"), System.currentTimeMillis());
        values.put(table.getColumn("DURATION"), getDurationInSeconds());
        values.put(table.getColumn("SUCCESS"), isSuccess() ? 1 : 0);
        values.put(table.getColumn("HINTS"), getHintsCount());

        final MutableRow row = new MutableRow(table, values);
        table.insertNewRow(row);
    }

}
