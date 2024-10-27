package ceb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class SwingThrottle {
    private final Timer timer;
    private final Runnable runnable;
    long last = System.currentTimeMillis();
    private final int delay;

    public SwingThrottle(final int delayInMs, final Runnable runnable) {
        this.delay = delayInMs;
        this.runnable = runnable;
        this.timer = new Timer(delayInMs, new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                SwingThrottle.this.timer.stop();
                SwingUtilities.invokeLater(runnable);

            }
        });
    }

    public synchronized void execute() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalArgumentException("must be called in EDT");
        }
        final long t = System.currentTimeMillis();
        if (t - this.last < this.delay) {
            this.timer.restart();

        } else {
            SwingUtilities.invokeLater(this.runnable);
            this.last = t;
        }

    }

    public synchronized void executeNow() {
        if (this.timer.isRunning()) {
            this.timer.stop();
            this.runnable.run();
        }
    }
}
