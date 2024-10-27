package ceb;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ceb.engine.Board;
import ceb.engine.Chess;
import ceb.engine.Move;

public class BoardComponent extends JComponent {
    public static final Color BOARD_BACKGROUND = new Color(240, 240, 240);
    boolean isFlipped = false;
    private Board board;
    Color black = new Color(85, 125, 165);
    Color white = new Color(235, 230, 210);
    Color black_darker = Color.decode("#ffff8b");
    Color white_darker = Color.decode("#ffffa8");

    private static BufferedImage sprites;
    private int cellSize;
    private int offsetX;
    private int offsetY;
    protected MouseEvent lastMouseMoved;
    protected int mouseOverPosX = -1;
    protected int mouseOverPosY = -1;
    protected int mousePressedPosX = -1;
    protected int mousePressedPosY = -1;
    protected String from = "";
    protected int draggedPiece = -1;
    protected int deltaDraggedPieceX;
    protected int deltaDraggedPieceY;
    private boolean locked;
    private Image arrow;
    private boolean showArrow;
    private int redIndex = -1;

    BoardComponent(final Board board, final PieceGlassPane glassPane, final UserListener listener) throws IOException {
        this.board = board;
        this.isFlipped = board.getTurn();
        this.arrow = new ImageIcon(getClass().getResource("arrow.png")).getImage();
        this.showArrow = true;
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(final MouseEvent e) {

                if (BoardComponent.this.locked) {
                    return;
                }
                final int posX = getPosX(e.getX());
                final int posY = getPosY(e.getY());
                final String to = getHumanPos(posX, posY);

                System.out.println("Move : " + BoardComponent.this.from + " ->" + to);
                if (BoardComponent.this.from != null && to != null && !BoardComponent.this.from.isEmpty() && !BoardComponent.this.from.equals(to)) {
                    final Move m = new Move(BoardComponent.this.from, to);
                    try {
                        if (board.isPseudoALegal(m)) {
                            final Move goodMove = board.getPuzzle().getMoves().get(board.getMoveIndex());
                            if (m.equals(goodMove)) {
                                board.applyNow(m);
                                listener.pieceMovedAsExpected(board);
                            } else {
                                if (board.isLegal(m)) {
                                    listener.unexpectedMove(board, m);
                                }
                            }
                        }
                    } catch (final Exception ex) {
                        // empty move
                    }
                }
                BoardComponent.this.mousePressedPosX = -1;
                BoardComponent.this.mousePressedPosY = -1;

                glassPane.clear();
                repaint();
            }

            @Override
            public void mousePressed(final MouseEvent e) {

                if (BoardComponent.this.showArrow) {
                    return;
                }
                BoardComponent.this.mousePressedPosX = getPosX(e.getX());
                BoardComponent.this.mousePressedPosY = getPosY(e.getY());
                try {
                    final int piece = board.getPieceAt(BoardComponent.this.mousePressedPosX, 7 - BoardComponent.this.mousePressedPosY);
                    final boolean blackPiece = piece > 8;
                    if (board.getTurn() == blackPiece) {
                        BoardComponent.this.from = getHumanPos(BoardComponent.this.mousePressedPosX, BoardComponent.this.mousePressedPosY);
                        final int xx = e.getX();
                        final int yy = e.getY();
                        BoardComponent.this.draggedPiece = piece;
                        glassPane.setVisible(true);
                        BoardComponent.this.deltaDraggedPieceX = BoardComponent.this.cellSize / 2;
                        BoardComponent.this.deltaDraggedPieceY = BoardComponent.this.cellSize / 2;

                        final JFrame root = (JFrame) SwingUtilities.getRoot(BoardComponent.this);
                        final Point p = SwingUtilities.convertPoint(BoardComponent.this, new Point(xx, yy), root.getContentPane());
                        glassPane.setPiece(BoardComponent.this.draggedPiece, p.x - BoardComponent.this.deltaDraggedPieceX, p.y - BoardComponent.this.deltaDraggedPieceY, BoardComponent.this.cellSize);

                    } else {
                        BoardComponent.this.mousePressedPosX = -1;
                        BoardComponent.this.mousePressedPosY = -1;
                    }
                } catch (final Exception ex) {
                    // TODO: handle exception
                }
                repaint();
            }

        });
        final SwingThrottle tMove = new SwingThrottle(20, new Runnable() {

            @Override
            public void run() {
                final MouseEvent e = BoardComponent.this.lastMouseMoved;
                final int posX = getPosX(e.getX());
                final int posY = getPosY(e.getY());

                if (BoardComponent.this.mouseOverPosX != posX || BoardComponent.this.mouseOverPosY != posY) {
                    BoardComponent.this.mouseOverPosX = posX;
                    BoardComponent.this.mouseOverPosY = posY;
                    repaint();
                }
            }

        });
        this.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(final MouseEvent e) {
                if (BoardComponent.this.locked) {
                    return;
                }
                BoardComponent.this.lastMouseMoved = e;
                tMove.execute();
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (BoardComponent.this.locked) {
                    return;
                }
                BoardComponent.this.lastMouseMoved = e;
                tMove.execute();
                final int xx = e.getX();
                final int yy = e.getY();
                final JFrame root = (JFrame) SwingUtilities.getRoot(BoardComponent.this);
                final Point p = SwingUtilities.convertPoint(BoardComponent.this, new Point(xx, yy), root.getContentPane());

                glassPane.setPiece(BoardComponent.this.draggedPiece, p.x - BoardComponent.this.deltaDraggedPieceX, p.y - BoardComponent.this.deltaDraggedPieceY, BoardComponent.this.cellSize);
            }
        });
    }

    public int getCellSize() {
        return this.cellSize;
    }

    // 0 : a , 7: h
    // -1 : hors du plateau
    protected int getPosX(final int x) {
        if (x < this.offsetX || x > this.offsetX + 8 * this.cellSize) {
            return -1;
        }
        if (this.isFlipped) {
            return 7 - (x - this.offsetX) / this.cellSize;
        } else {
            return (x - this.offsetX) / this.cellSize;
        }
    }

    // 0 : 1 , 7: 8
    // -1 : hors du plateau
    protected int getPosY(final int y) {
        if (y < this.offsetY || y > this.offsetY + 8 * this.cellSize) {
            return -1;
        }
        if (this.isFlipped) {
            return 7 - (y - this.offsetY) / this.cellSize;
        } else {
            return (y - this.offsetY) / this.cellSize;
        }
    }

    String getHumanPos(final int posX, final int posY) {
        final char[] s = new char[2];
        s[0] = (char) ('a' + posX);
        s[1] = (char) ('1' + 7 - posY);
        return new String(s);
    }

    public void setFlipped(final boolean isFlipped) {
        this.isFlipped = isFlipped;
        repaint();
    }

    @Override
    public void paint(final Graphics g) {
        g.setColor(BOARD_BACKGROUND);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        if (this.showArrow) {
            g.setColor(new Color(248, 248, 248));
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.drawImage(this.arrow, 16, 0, null);
        } else {

            this.cellSize = (Math.min(this.getWidth(), this.getHeight())) / 8;
            this.offsetX = (this.getWidth() - this.cellSize * 8) / 2;
            this.offsetY = (this.getHeight() - this.cellSize * 8) / 2;

            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int ratio = 1;
            if (this.getHeight() < 800) {
                ratio = 2;
            }
            final int w = this.getWidth() * ratio;
            final int h = this.getHeight() * ratio;
            final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            paintOffscreen(img.getGraphics(), ratio, w, h);
            g.drawImage(img, this.offsetX, this.offsetY, this.getWidth() + this.offsetX, this.getHeight() + this.offsetY, 0, 0, w, h, null);

        }

    }

    public void paintOffscreen(final Graphics g, final int ratio, final int w, final int h) {
        g.setColor(BOARD_BACKGROUND);
        g.fillRect(0, 0, w, h);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        final int cSize = (Math.min(w, h)) / 8;
        g.setFont(g.getFont().deriveFont(ratio * 15f).deriveFont(Font.BOLD));

        g.setColor(this.white);
        g.fillRect(0, 0, cSize * 8, cSize * 8);
        g.setColor(this.black);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                g.fillRect(cSize + x * cSize * 2, 2 * y * cSize, cSize, cSize);
                g.fillRect(2 * x * cSize, 2 * y * cSize + cSize, cSize, cSize);
            }
        }

        //
        final int mOverPosX = this.mouseOverPosX;
        final int mOverPosY = this.mouseOverPosY;
        if (!this.locked && mOverPosX >= 0 && mOverPosY >= 0 && mOverPosX < 8 && mOverPosY < 8) {
            g.setColor(Color.DARK_GRAY);
            if (this.isFlipped) {
                g.drawRect((7 - this.mouseOverPosX) * cSize, (7 - this.mouseOverPosY) * cSize, cSize - 1, cSize - 1);
                g.drawRect((7 - this.mouseOverPosX) * cSize + 1, (7 - this.mouseOverPosY) * cSize + 1, cSize - 3, cSize - 3);
            } else {
                g.drawRect(this.mouseOverPosX * cSize, this.mouseOverPosY * cSize, cSize - 1, cSize - 1);
                g.drawRect(this.mouseOverPosX * cSize + 1, this.mouseOverPosY * cSize + 1, cSize - 3, cSize - 3);
            }
        }
        if (this.mousePressedPosX >= 0 && this.mousePressedPosY >= 0 && mOverPosX < 8 && mOverPosY < 8) {
            if ((this.mousePressedPosX + this.mousePressedPosY) % 2 == 0) {
                g.setColor(this.white_darker);
            } else {
                g.setColor(this.black_darker);
            }
            if (this.isFlipped) {
                g.fillRect((7 - this.mousePressedPosX) * cSize, (7 - this.mousePressedPosY) * cSize, cSize, cSize);
            } else {
                g.fillRect(this.mousePressedPosX * cSize, this.mousePressedPosY * cSize, cSize, cSize);
            }
        }

        //
        if (!this.isFlipped) {
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0) {
                    g.setColor(this.white);
                } else {
                    g.setColor(this.black);
                }
                g.drawString(String.valueOf((char) ('1' + i)), 2, (7 - i) * cSize + 13 * 2);
            }
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0) {
                    g.setColor(this.white);
                } else {
                    g.setColor(this.black);
                }
                g.drawString(String.valueOf((char) ('A' + i)), i * cSize + cSize - 12 * ratio, 8 * cSize - 2);
            }
        } else {
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0) {
                    g.setColor(this.white);
                } else {
                    g.setColor(this.black);
                }
                g.drawString(String.valueOf((char) ('1' + (7 - i))), 2, (7 - i) * cSize + 13 * 2);
            }
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0) {
                    g.setColor(this.white);
                } else {
                    g.setColor(this.black);
                }
                g.drawString(String.valueOf((char) ('A' + (7 - i))), i * cSize + cSize - 12 * ratio, 8 * cSize - 2);
            }

        }
        if (this.redIndex >= 0) {
            final Point p = Board.internalToXY(this.redIndex);
            g.setColor(Color.RED);
            if (this.isFlipped) {
                System.out.println("BoardComponent.paint() flipped");
                g.fillRect((7 - p.x) * cSize, (p.y) * cSize, cSize, cSize);
            } else {
                System.out.println("BoardComponent.paint() non flipped");
                g.fillRect(p.x * cSize, (7 - p.y) * cSize, cSize, cSize);
            }

        }
        if (!this.showArrow) {
            // Pièces
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int xOnBoard = x;
                    int yOnBoard = 7 - y;
                    // Ligne par ligne

                    if (this.isFlipped) {
                        xOnBoard = 7 - x;
                        yOnBoard = y;
                    }
                    final int piece = this.board.getPieceAt(xOnBoard, yOnBoard);

                    final int xx = x * cSize;
                    final int yy = y * cSize;

                    if (xOnBoard != this.mousePressedPosX || 7 - yOnBoard != this.mousePressedPosY) {
                        drawPiece(g, piece, xx, yy, cSize);
                    }

                }
            }
        }

    }

    public static void drawPiece(final Graphics g, final int piece, final int xx, final int yy, final int cellSize) {
        if (sprites == null) {
            try {
                sprites = ImageIO.read(BoardComponent.class.getResource("sprites3.png"));
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // position on bitmap
        int x = 0;
        int y = 0;
        switch (piece) {
        case Chess.WHITE_KING: {
            break;
        }
        case Chess.WHITE_QUEEN: {
            x = 1;
            break;
        }
        case Chess.WHITE_ROOK: {
            x = 4;
            break;
        }
        case Chess.WHITE_BISHOP: {
            x = 2;
            y = 0;
            break;
        }
        case Chess.WHITE_KNIGHT: {
            x = 3;
            y = 0;
            break;
        }
        case Chess.WHITE_PAWN: {
            x = 5;
            y = 0;
            break;
        }

        case Chess.BLACK_KING: {
            y = 1;
            break;
        }
        case Chess.BLACK_QUEEN: {
            x = 1;
            y = 1;
            break;
        }
        case Chess.BLACK_ROOK: {
            x = 4;
            y = 1;
            break;
        }
        case Chess.BLACK_BISHOP: {
            x = 2;
            y = 1;
            break;
        }
        case Chess.BLACK_KNIGHT: {
            x = 3;
            y = 1;
            break;
        }
        case Chess.BLACK_PAWN: {
            x = 5;
            y = 1;
            break;
        }
        default:
            return;
        }
        x = x * 200;
        y = y * 200;
        g.drawImage(sprites, xx, yy, xx + cellSize, yy + cellSize, x, y, x + 212, y + 212, null);
    }

    public void setLocked(final boolean b) {
        this.locked = b;

    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public void setShowArrow(final boolean showArrow) {
        this.showArrow = showArrow;
    }

    public void setRedBackground(final int moveTargetSquare) {
        this.redIndex = moveTargetSquare;
        repaint();
    }
}
