package ceb;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * Un composant très simple qui affiche une image.
 *
 * @author ILM Informatique 4 juin 2004
 */
public class JImage extends JComponent {

    private Image image;
    private ImageIcon icon;

    private boolean centered;
    private URI hyperlink;
    private MouseListener hyperlinkL;

    public JImage(final String fileName) {
        this(new ImageIcon(fileName));
    }

    public JImage(final URL url) {
        this(new ImageIcon(url));
    }

    public JImage(final ImageIcon img) {
        this(img.getImage());
        this.icon = img;
    }

    public JImage(final Image img) {
        this.image = img;
        this.icon = null;
        this.setOpaque(true);
    }

    public void check() {
        if (this.image == null || this.image.getHeight(null) <= 0) {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        final int dx;
        final int dy;
        if (!this.centered) {
            dx = dy = 0;
        } else {
            dx = (this.getWidth() - this.image.getWidth(null)) / 2;
            dy = (this.getHeight() - this.image.getHeight(null)) / 2;
        }
        g.drawImage(this.image, dx, dy, null);
    }

    public ImageIcon getImageIcon() {
        if (this.icon == null) {
            this.icon = new ImageIcon(this.image);
        }
        return this.icon;
    }

    @Override
    public Dimension getPreferredSize() {
        return this.getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(this.image.getWidth(null), this.image.getHeight(null));
    }

    public void setCenterImage(final boolean t) {
        this.centered = true;
    }

    public void setImage(final Image image) {
        this.image = image;
    }

    public final URI getHyperlink() {
        return this.hyperlink;
    }

    public final void setHyperLink(final URI uri) {

        if (this.hyperlink == null) {
            this.hyperlinkL = new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(getHyperlink());
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                    }
                    e.consume();
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            };
            this.addMouseListener(this.hyperlinkL);
        }
        this.hyperlink = uri;
        if (this.hyperlink == null) {
            this.removeMouseListener(this.hyperlinkL);
            this.hyperlinkL = null;
        }

    }
}
