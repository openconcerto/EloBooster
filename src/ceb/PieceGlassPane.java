package ceb;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

public class PieceGlassPane extends JComponent {
    private int x;
    private int y;
    private int size;
    private int piece;

    @Override
    protected void paintComponent(final Graphics g) {
        if (this.piece >= 0) {
            final Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            BoardComponent.drawPiece(g, this.piece, this.x, this.y, this.size);
        }

    }

    public void setPiece(final int piece, final int x, final int y, final int size) {
        this.piece = piece;
        this.x = x;
        this.y = y;
        this.size = size;
        repaint();

    }

    public void clear() {
        this.piece = -1;
        setVisible(false);
    }
}
