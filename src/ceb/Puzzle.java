package ceb;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import ceb.engine.Board;
import ceb.engine.Chess;
import ceb.engine.Move;

public class Puzzle implements Externalizable {
    private String hash;
    private String fen;
    private String moves;
    private int level;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static List<Puzzle> all = new ArrayList<>();
    private static HashMap<String, Puzzle> allMap = new HashMap<>();
    private static int levelCount;

    public Puzzle() {

    }

    public Puzzle(final String fen, final String moves, final int level) {
        this.fen = fen;
        this.moves = moves;
        this.level = level;
    }

    public List<Move> getMoves() {
        final List<String> parts = fastSplit(this.moves, ' ');
        final List<Move> result = new ArrayList<>(parts.size());
        for (final String p : parts) {
            final Move m = new Move(p.substring(0, 2), p.substring(2, 4));
            if (p.length() == 4) {
                result.add(m);
            } else if (p.length() == 5) {
                final char c = p.charAt(4);
                switch (c) {
                case 'q':
                    m.setPromotionPiece(Chess.QUEEN);
                    break;
                case 'n':
                    m.setPromotionPiece(Chess.KNIGHT);
                    break;
                case 'b':
                    m.setPromotionPiece(Chess.BISHOP);
                    break;
                case 'r':
                    m.setPromotionPiece(Chess.ROOK);
                    break;
                default:
                    throw new IllegalArgumentException(p + " invalid promotion");

                }

                result.add(m);

            } else {
                throw new IllegalArgumentException(p + " invalid");
            }
        }
        return result;
    }

    public String getHash() {
        return this.hash;
    }

    public String getFen() {
        return this.fen;
    }

    public String getAllMoves() {
        return this.moves;
    }

    public int getLevel() {
        return this.level;
    }

    @Override
    public String toString() {
        return this.fen + " : " + this.moves + " : " + this.level;
    }

    public static List<Puzzle> getAll() {
        loadIfNeeded();
        return all;
    }

    public static Puzzle getPuzzle(final String hash) {
        loadIfNeeded();
        return allMap.get(hash);
    }

    private static void loadIfNeeded() {
        if (all.isEmpty()) {
            try {
                load();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public boolean isGain() {
        final Board b = new Board(getFen());
        final boolean computerIsBlack = b.getTurn();
        int startBlack = b.getBlackPiecesOnBoard().size();
        int startWhite = b.getWhitePiecesOnBoard().size();
        final List<Move> moves = getMoves();
        for (int i = 0; i < moves.size(); i++) {
            final Move m = moves.get(i);
            b.apply(m);
            if (i == 0) {
                startBlack = b.getBlackPiecesOnBoard().size();
                startWhite = b.getWhitePiecesOnBoard().size();
            }

        }
        if (b.isCheckmate()) {
            return false;
        }
        final int endBlack = b.getBlackPiecesOnBoard().size();
        final int endWhite = b.getWhitePiecesOnBoard().size();
        final int deltaBlack = startBlack - endBlack;
        final int deltaWhite = startWhite - endWhite;
        System.out.println("Puzzle.isGain() deltaBlack:" + deltaBlack + " deltaWhite:" + deltaWhite);
        if (deltaBlack == deltaWhite) {
            return false;
        }

        if (computerIsBlack) {
            return deltaBlack > deltaWhite;

        } else {
            return deltaBlack < deltaWhite;
        }

    }

    public boolean endAtCheckMate() {
        final Board b = new Board(getFen());
        for (final Move m : getMoves()) {
            if (!b.isLegal(m)) {
                throw new IllegalStateException("illegal:" + m + " " + getAllMoves());
            }
            b.apply(m);

        }
        return b.isCheckmate();

    }

    public static int getLevelCount() {
        loadIfNeeded();
        return levelCount;
    }

    private static void load() throws Exception {
        final long t1 = System.currentTimeMillis();
        final InputStream in = Puzzle.class.getResourceAsStream("data.bin");
        final InflaterInputStream infIn = new InflaterInputStream(in, new Inflater(true), 2048);
        final ObjectInputStream oIn = new ObjectInputStream(infIn);
        levelCount = oIn.readInt();
        final int PUZZLES_PER_LEVEL = oIn.readInt();
        for (int i = 0; i < levelCount; i++) {
            for (int j = 0; j < PUZZLES_PER_LEVEL; j++) {
                final Puzzle p = new Puzzle();
                p.readExternal(oIn);
                all.add(p);
                allMap.put(p.getHash(), p);
            }
        }
        Collections.shuffle(all);

        oIn.close();
        infIn.close();
        in.close();
        final long t2 = System.currentTimeMillis();
        System.out.println("Puzzle.load() " + (t2 - t1) + "ms");
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] encodedhash = digest.digest(this.fen.getBytes());
            out.writeUTF(bytesToHex(encodedhash));
            out.writeUTF(this.fen);
            out.writeUTF(this.moves);
            out.writeInt(this.level);
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.hash = in.readUTF();
        this.fen = in.readUTF();
        this.moves = in.readUTF();
        this.level = in.readInt();
    }

    public static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static final List<String> fastSplit(final String string, final char sep) {
        final List<String> l = new ArrayList<>();
        final int length = string.length();
        final char[] cars = string.toCharArray();
        int rfirst = 0;

        for (int i = 0; i < length; i++) {
            if (cars[i] == sep) {
                l.add(new String(cars, rfirst, i - rfirst));
                rfirst = i + 1;
            }
        }

        if (rfirst < length) {
            l.add(new String(cars, rfirst, length - rfirst));
        }
        return l;
    }

    public static Puzzle getPuzzleAtLevel(final float level) {
        loadIfNeeded();
        Collections.shuffle(all);
        int index = Math.round(level);
        if (index < 0) {
            index = 0;
        } else if (index >= levelCount) {
            index = levelCount - 1;
        }
        System.out.println("Puzzle.getPuzzleAtLevel(" + index + ")");
        for (final Puzzle p : all) {
            if (p.level == index) {
                return p;
            }
        }
        System.out.println("Puzzle.getPuzzleAtLevel(" + index + ") not found");
        return all.get(0);
    }

}
