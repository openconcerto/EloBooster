/*
 * JerryFX - A Chess Graphical User Interface Copyright (C) 2020 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package ceb.engine;

public class Chess {

    private Chess() {
    }

    public static final int EMPTY = 0;
    public static final int PAWN = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK = 4;
    public static final int QUEEN = 5;
    public static final int KING = 6;

    public static final int FRINGE = 0xFF;
    public static final int ANY_PIECE = 0x08;
    public static final int ANY_SQUARE = 0;

    public static final int WHITE_KING = 0x06;
    public static final int WHITE_QUEEN = 0x05;
    public static final int WHITE_ROOK = 0x04;
    public static final int WHITE_BISHOP = 0x03;
    public static final int WHITE_KNIGHT = 0x02;
    public static final int WHITE_PAWN = 0x01;
    public static final int WHITE_ANY_PIECE = 0x07;

    public static final int BLACK_KING = 0x86;
    public static final int BLACK_QUEEN = 0x85;
    public static final int BLACK_ROOK = 0x84;
    public static final int BLACK_BISHOP = 0x83;
    public static final int BLACK_KNIGHT = 0x82;
    public static final int BLACK_PAWN = 0x81;
    public static final int BLACK_ANY_PIECE = 0x87;

    public static String getPieceName(int p) {
        switch (p) {
        case WHITE_KING:
            return "roi blanc";
        case WHITE_QUEEN:
            return "reine blanche";
        case WHITE_ROOK:
            return "tour blanche";
        }
        return "?";
    }

    public static final int[] EMPTY_POS = { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF };

    // initial board position
    public static final int[] INIT_POS = { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x04, 0x02, 0x03, 0x05, 0x06,
            0x03, 0x02, 0x04, 0xFF, 0xFF, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x81, 0x81, 0x81, 0x81, 0x81, 0x81, 0x81,
            0x81, 0xFF, 0xFF, 0x84, 0x82, 0x83, 0x85, 0x86, 0x83, 0x82, 0x84, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF };

    // board positions
    public static final int A1 = 21;
    public static final int A2 = 31;
    public static final int A3 = 41;
    public static final int A4 = 51;
    public static final int A5 = 61;
    public static final int A6 = 71;
    public static final int A7 = 81;
    public static final int A8 = 91;

    public static final int B1 = 22;
    public static final int B2 = 32;
    public static final int B3 = 42;
    public static final int B4 = 52;
    public static final int B5 = 62;
    public static final int B6 = 72;
    public static final int B7 = 82;
    public static final int B8 = 92;

    public static final int C1 = 23;
    public static final int C2 = 33;
    public static final int C3 = 43;
    public static final int C4 = 53;
    public static final int C5 = 63;
    public static final int C6 = 73;
    public static final int C7 = 83;
    public static final int C8 = 93;

    public static final int D1 = 24;
    public static final int D2 = 34;
    public static final int D3 = 44;
    public static final int D4 = 54;
    public static final int D5 = 64;
    public static final int D6 = 74;
    public static final int D7 = 84;
    public static final int D8 = 94;

    public static final int E1 = 25;
    public static final int E2 = 35;
    public static final int E3 = 45;
    public static final int E4 = 55;
    public static final int E5 = 65;
    public static final int E6 = 75;
    public static final int E7 = 85;
    public static final int E8 = 95;

    public static final int F1 = 26;
    public static final int F2 = 36;
    public static final int F3 = 46;
    public static final int F4 = 56;
    public static final int F5 = 66;
    public static final int F6 = 76;
    public static final int F7 = 86;
    public static final int F8 = 96;

    public static final int G1 = 27;
    public static final int G2 = 37;
    public static final int G3 = 47;
    public static final int G4 = 57;
    public static final int G5 = 67;
    public static final int G6 = 77;
    public static final int G7 = 87;
    public static final int G8 = 97;

    public static final int H1 = 28;
    public static final int H2 = 38;
    public static final int H3 = 48;
    public static final int H4 = 58;
    public static final int H5 = 68;
    public static final int H6 = 78;
    public static final int H7 = 88;
    public static final int H8 = 98;

    // attack table
    // the index of this array corresponds to the distance
    // between two squares of the board (note the board is
    // encoded as a one dim array of size 120, where A1 = 21, H1 = 28
    // A8 = 91, A8 = 98.
    // the value denotes whether an enemy rook, bishop, knight, queen, king
    // on one square can attack the other square. The following encoding
    // is used:
    // Bitposition Piece
    // 0 Knight
    // 1 Bishop
    // 2 Rook
    // 3 Queen
    // 4 King
    // e.g. distance one, i.e. index 1 (=left, up, down, right square) has
    // value 0x1C = MSB 00011100 LSB, i.e. king, queen, rook can
    // potentially attack
    public static final int[] ATTACK_TABLE = { 0x00, 0x1C, 0x0C, 0x0C, 0x0C, 0x0C, 0x0C, 0x0C, 0x01, 0x1a, 0x1C, 0x1A, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x01, 0x0C, 0x01, 0x0A, 0x00, 0x00,
            0x00, 0x00, 0x0a, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x0A, 0x0a, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x0a, 0x0A,
            0x00, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A };

    // first dim is for different piece types
    // [piece_type[0] is DCOUNT (as in Byte Magazine paper)
    // [piece_type[1] ... [piece_type][4] resp.
    // [piece_type[1] ... [piece_type][8] contain
    // DPOINT table
    public static final int[][] DIR_TABLE = { { 4, -10, -20, -11, -9, 0, 0, 0, 0 }, // max 4 black
                                                                                    // pawn
                                                                                    // directions,
                                                                                    // rest 0's
            { 4, +10, +20, +11, +9, 0, 0, 0, 0 }, // max 4 white pawn directions, rest 0's
            { 8, -21, -12, +8, +19, +21, +12, -8, -19 }, // 8 knight directions
            { 4, +9, +11, -11, -9, 0, 0, 0, 0 }, // 4 bishop directions
            { 4, +10, -10, +1, -1, 0, 0, 0, 0 }, // 4 rook directions
            { 8, +9, +11, -11, -9, +10, -10, +1, -1 }, // 8 queen directions
            { 8, +9, +11, -11, -9, +10, -10, +1, -1 } // 8 king directions (= queen dir's)
    };

    // indices into the direction table
    public static final int IDX_BPAWN = 0;
    public static final int IDX_WPAWN = 1;
    public static final int IDX_KNIGHT = 2;
    public static final int IDX_BISHOP = 3;
    public static final int IDX_ROOK = 4;
    public static final int IDX_QUEEN = 5;
    public static final int IDX_KING = 6;

    // players
    public static final boolean WHITE = false;
    public static final int IWHITE = 0;
    public static final boolean BLACK = true;
    public static final int IBLACK = 1;

    public static final int RANDOM_CASTLE = 768;
    public static final int RANDOM_EN_PASSENT = 772;
    public static final int RANDOM_TURN = 780;

    public static final int RES_UNDEF = 0;
    public static final int RES_WHITE_WINS = 1;
    public static final int RES_BLACK_WINS = 2;
    public static final int RES_DRAW = 3;
    public static final int RES_ANY = 4;

}
