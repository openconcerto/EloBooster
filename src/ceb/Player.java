package ceb;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ceb.db.Database;
import ceb.db.MutableRow;
import ceb.db.Row;
import ceb.db.Table;
import ceb.db.Where;

public class Player {
    private final int id;
    private float level = 36f;
    private String name;
    private long exp = 0;

    public Player(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public int getPublicLevel() {
        return Math.round(this.level) + 1;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String playerName) {
        this.name = playerName;
    }

    public void setLevel(final float level) {
        this.level = level;
    }

    public void setExp(final long exp) {
        this.exp = exp;
    }

    // 0 - MAXLEVEL
    public int getLevelIndex() {
        int l = Math.round(this.level);
        if (l < 1) {
            l = 0;
        }
        if (l >= Puzzle.getLevelCount() - 1) {
            l = Puzzle.getLevelCount() - 1;
        }
        return l;
    }

    public long getExp() {
        return this.exp;
    }

    public void addXP(final int deltaXP) {
        this.exp += deltaXP;
    }

    public void commit(final Database database, final Round round) throws SQLException {
        commitPlayerInfo(database);

        final int size = round.getRecordCount();
        for (int i = 0; i < size; i++) {
            final PuzzleRecord r = round.getRecord(i);
            if (!r.isSuccess()) {
                // On l'enregistre toujours
                r.commit(database);
            } else {
                // Le puzzle est reussi
                // On regarde l'historique de ce puzzle
                // si les 3 derniers sont reussis, on efface tout ce qui parle de ce puzzle
                if (!needRetry(database, r.getPuzzleHash())) {
                    deleteAll(database, r.getPuzzleHash());
                } else {
                    // sinon on stock ce record
                    r.commit(database);
                }
            }

        }

    }

    public void commitPlayerInfo(final Database database) throws SQLException {
        final Table tPlayer = database.getSchema("PUBLIC").getTable("PLAYER");
        final List<Row> r = tPlayer.fectchRows(new Where(tPlayer.getColumn("ID"), Arrays.asList(Integer.valueOf(this.id))));
        final MutableRow m = r.get(0).createMutableRow();
        m.set("EXP", this.exp);
        m.set("LEVEL", this.level);
        m.set("HASH", getCheckHash());
        System.out.println("Player.commit()" + m);
        tPlayer.updateRow(m);
    }

    public void deleteAll(final Database database, final String puzzleHash) throws SQLException {
        final String sql = "DELETE FROM RECORD WHERE PLAYER=" + this.id + " AND HASH='" + puzzleHash + "'";
        database.execute(sql);
    }

    public Row getOldestRecord(final Database database, final Set<String> hashes) throws SQLException {
        final Table tPlayer = database.getSchema("PUBLIC").getTable("RECORD");
        final long time = System.currentTimeMillis() - 3 * 24 * 3600 * 1000;
        final String sql = tPlayer.getColumn("PLAYER").toQuotedSQL() + "=" + getId() + " AND " + tPlayer.getColumn("DATE").toQuotedSQL() + "<" + time + " ORDER BY "
                + tPlayer.getColumn("DATE").toQuotedSQL();
        final List<Row> rows = tPlayer.fectchRows(new Where(sql));
        for (final Row r : rows) {
            if (!hashes.contains(r.getString("HASH"))) {
                return r;
            }
        }
        return null;
    }

    public Set<String> getRecentPlayedHash(final Database database) throws SQLException {
        final HashSet<String> result = new HashSet<>();
        final Table tPlayer = database.getSchema("PUBLIC").getTable("RECORD");
        final long time = System.currentTimeMillis() - 3 * 24 * 3600 * 1000;
        final String sql = tPlayer.getColumn("PLAYER").toQuotedSQL() + "=" + getId() + " AND " + tPlayer.getColumn("DATE").toQuotedSQL() + ">" + time;
        final List<Row> rows = tPlayer.fectchRows(new Where(sql));
        for (final Row r : rows) {
            result.add(r.getString("HASH"));
        }
        return result;
    }

    public List<Row> getHistory(final Database database) throws SQLException {
        final Table tPlayer = database.getSchema("PUBLIC").getTable("RECORD");
        final String sql = tPlayer.getColumn("PLAYER").toQuotedSQL() + "=" + getId() + " ORDER BY " + tPlayer.getColumn("DATE").toQuotedSQL();
        return tPlayer.fectchRows(new Where(sql));
    }

    public List<Row> getHistory(final Database database, final String puzzleHash) throws SQLException {
        final Table tPlayer = database.getSchema("PUBLIC").getTable("RECORD");
        final String sql = tPlayer.getColumn("PLAYER").toQuotedSQL() + "=" + getId() + " AND " + tPlayer.getColumn("HASH").toQuotedSQL() + "='" + puzzleHash + "'" + " ORDER BY "
                + tPlayer.getColumn("DATE").toQuotedSQL();
        return tPlayer.fectchRows(new Where(sql));
    }

    public boolean needRetry(final Database database, final String puzzleHash) throws SQLException {
        final List<Row> rows = getHistory(database, puzzleHash);
        final int size = rows.size();
        int success = 0;
        for (int i = size - 1; i >= 0; i--) {
            final Row r = rows.get(i);
            if (r.getInt("SUCCESS") == 1) {
                success++;
            } else {
                break;
            }

        }
        return success >= 3;
    }

    public String getCheckHash() {
        final String s = this.id + " " + (int) this.level + " " + this.exp;
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] encodedhash = digest.digest(s.getBytes());
            return Puzzle.bytesToHex(encodedhash);
        } catch (final NoSuchAlgorithmException e) {
            return "" + s.hashCode();
        }

    }

}
