package ceb.engine;

public class EvaluatedMove {
    private final Move m;
    private final float eval;
    private String fen = "";

    public EvaluatedMove(Move m, float eval) {
        this.m = m;
        this.eval = eval;
    }

    public EvaluatedMove(Move m, float eval, String fen) {
        this.m = m;
        this.eval = eval;
        this.fen = fen;
    }

    public float getEval() {
        return eval;
    }

    public Move getMove() {
        return m;
    }

    public String toString() {
        return m.toString() + ":" + eval + " " + fen;
    }

}
