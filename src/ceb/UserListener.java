package ceb;

import ceb.engine.Board;
import ceb.engine.Move;

public interface UserListener {

    void pieceMovedAsExpected(Board board);

    void unexpectedMove(Board board, Move m);
}
