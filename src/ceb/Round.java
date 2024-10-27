package ceb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ceb.db.Database;
import ceb.db.Row;

public class Round {
    public static final int ROUND_SIZE = 12;
    private final List<PuzzleRecord> records = new ArrayList<>(ROUND_SIZE);
    float startLevel;
    private final Player player;

    Round(final Player player) {
        this.player = player;
        this.startLevel = player.getLevelIndex();
    }

    PuzzleRecord createNextRecord(final Database database) throws SQLException {
        if (this.records.size() >= ROUND_SIZE) {
            return null;
        }
        final PuzzleRecord r = new PuzzleRecord();
        r.setDate(System.currentTimeMillis());
        if (this.records.isEmpty()) {
            r.setPuzzleHash(Puzzle.getPuzzleAtLevel(this.startLevel).getHash());
        } else {
            final PuzzleRecord previous = this.records.get(this.records.size() - 1);
            final Puzzle previousPuzzle = Puzzle.getPuzzle(previous.getPuzzleHash());
            if (previous.isSuccess()) {
                if (this.records.size() % 3 == 1) {
                    // get a failed record
                    // get failed record (cad le plus vieux qui en plus n'a pas été joué
                    // depuis plus de 3 jours)
                    final Set<String> ignore = this.player.getRecentPlayedHash(database);
                    for (final PuzzleRecord record : this.records) {
                        ignore.add(record.getPuzzleHash());
                    }

                    final Row row = this.player.getOldestRecord(database, ignore);
                    if (row != null) {
                        r.setPuzzleHash(row.getString("HASH"));
                    } else {
                        r.setPuzzleHash(Puzzle.getPuzzleAtLevel(previousPuzzle.getLevel() + 1.5f).getHash());
                    }

                } else {
                    r.setPuzzleHash(Puzzle.getPuzzleAtLevel(previousPuzzle.getLevel() + 1.5f).getHash());
                }
            } else {
                r.setPuzzleHash(Puzzle.getPuzzleAtLevel(this.startLevel - 1.1f).getHash());
            }
        }
        this.records.add(r);
        return r;
    }

    public void applyScore(final Database database) throws SQLException {
        int combo = 0;
        float deltaLevel = 0;
        int deltaXP = 0;
        for (final PuzzleRecord p : this.records) {
            final Puzzle puzzle = Puzzle.getPuzzle(p.getPuzzleHash());

            if (p.isSuccess()) {
                if (puzzle.getLevel() > this.startLevel) {
                    deltaXP += 10 + 10 * combo * combo;
                } else if (puzzle.getLevel() < this.startLevel) {
                    deltaXP += 4 + combo;
                } else {
                    deltaXP += 2;
                }
            } else {
                deltaXP++;
            }
            if (p.getHintsCount() == 0) {
                deltaXP += 4;
            }
            if (puzzle.getLevel() > this.startLevel && p.isSuccess()) {
                deltaLevel += 0.7f;
            }
            if (puzzle.getLevel() < this.startLevel && !p.isSuccess()) {
                deltaLevel -= 0.3f;
            }
            if (p.isSuccess()) {
                if (p.getDurationInSeconds() < 5) {
                    deltaXP += 10;
                }
                if (p.getDurationInSeconds() < 10) {
                    deltaXP += 10;
                }
            }
            // Next
            if (p.isSuccess()) {
                combo++;
            } else {
                combo = 0;
            }

        }

        System.out.println("Round.applyScore() deltaLevel:" + deltaLevel);
        System.out.println("Round.applyScore() deltaXP:" + deltaXP);

        this.player.setLevel((deltaLevel + this.startLevel));
        this.player.addXP(deltaXP);
        this.player.commit(database, this);
    }

    public void store() {

    }

    public static void main(final String[] args) throws SQLException {
        final Database d = new Database();
        final Player p = new Player(1, "Test");
        p.setLevel(30);
        final Round r = new Round(p);
        final PuzzleRecord r1 = r.createNextRecord(d);
        r1.setSuccess(false);
        System.out.println(r1);
        final PuzzleRecord r2 = r.createNextRecord(d);

        r2.setSuccess(false);
        System.out.println(r2);
        //
        final PuzzleRecord r3 = r.createNextRecord(d);

        r3.setSuccess(false);
        System.out.println(r3);

        final PuzzleRecord r4 = r.createNextRecord(d);

        r4.setSuccess(false);
        System.out.println(r4);

        final PuzzleRecord r5 = r.createNextRecord(d);

        r5.setSuccess(false);
        System.out.println(r5);

        final PuzzleRecord r6 = r.createNextRecord(d);

        r6.setSuccess(false);
        System.out.println(r6);

        final PuzzleRecord r7 = r.createNextRecord(d);

        r7.setSuccess(false);
        System.out.println(r7);

        final PuzzleRecord r8 = r.createNextRecord(d);

        r8.setSuccess(false);
        System.out.println(r8);

        final PuzzleRecord r9 = r.createNextRecord(d);

        r9.setSuccess(false);
        System.out.println(r9);

        final PuzzleRecord r10 = r.createNextRecord(d);

        r10.setSuccess(false);
        System.out.println(r10);

        final PuzzleRecord r11 = r.createNextRecord(d);

        r11.setSuccess(false);
        System.out.println(r11);

        final PuzzleRecord r12 = r.createNextRecord(d);

        r12.setSuccess(false);
        System.out.println(r12);

        r.applyScore(null);
    }

    public PuzzleRecord getCurrentRecord() {
        return this.records.get(this.records.size() - 1);
    }

    public PuzzleRecord getRecord(final int i) {
        return this.records.get(i);
    }

    public int getRecordCount() {
        return this.records.size();
    }
}
