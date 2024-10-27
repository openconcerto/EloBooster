package ceb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import ceb.db.Column;
import ceb.db.Database;
import ceb.db.DatabaseConnection;
import ceb.db.MutableRow;
import ceb.db.Row;
import ceb.db.Table;
import ceb.engine.Board;
import ceb.engine.Chess;
import ceb.engine.Move;

public class ChessFrame extends JFrame {
    private static Database database = new Database();

    private static final Color GREEN = Color.decode("#005b4f");
    private final Board board;
    private final Player player;

    final PieceGlassPane glassPane = new PieceGlassPane();
    final BoardComponent boardComponent;
    final JLabel labelInfo = new JLabel("Info");
    final JButton newRoundButton = new JButton("Nouveau round");
    final JButton nextPuzzleButton = new JButton("Puzzle suivant");
    final JButton tipsButton = new JButton("Indice");
    final JLabel labelPlayerBottom = new JLabel("  Joueur  ");
    final JLabel labelPlayerTop = new JLabel("  Computer  ");

    JLabel labelLevel = new JLabel("Niveau", SwingConstants.RIGHT);
    JLabel labelExp = new JLabel("exp", SwingConstants.RIGHT);

    // Info sur le puzzle actuel
    private int tipsCount;
    private int totalTipsCount;
    private long startPuzzleTime;
    private long stopPuzzleTime;
    private int badMoveCount = 0;

    Round round;

    public ChessFrame(final Player player, final Board board) throws IOException {
        if (board == null) {
            throw new IllegalArgumentException("null board");
        }
        this.board = board;
        this.player = player;
        this.setGlassPane(this.glassPane);

        final UserListener l = new UserListener() {

            @Override
            public void pieceMovedAsExpected(final Board board) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (board.isCheckmate()) {
                            puzzleEnded("Échec et mat!");
                        } else {
                            if (board.getMoveIndex() >= board.getPuzzle().getMoves().size()) {
                                puzzleEnded("Bien joué!");
                            } else {
                                // Next computer move
                                ChessFrame.this.tipsCount = 0;
                                animate(board.getPuzzle().getMoves().get(board.getMoveIndex()));
                                setInfo("");
                            }
                        }

                    }

                    private void puzzleEnded(final String reason) {
                        ChessFrame.this.boardComponent.setLocked(true);
                        ChessFrame.this.nextPuzzleButton.setEnabled(true);
                        ChessFrame.this.tipsButton.setEnabled(false);
                        ChessFrame.this.stopPuzzleTime = System.currentTimeMillis();
                        String message = "";
                        String titre = "";
                        if (ChessFrame.this.badMoveCount == 0 && ChessFrame.this.totalTipsCount == 0) {
                            // next puzzle !
                            titre = "Bien joué";
                            message = reason;
                            ChessFrame.this.round.getCurrentRecord().setDurationInSeconds((int) ((ChessFrame.this.stopPuzzleTime - ChessFrame.this.startPuzzleTime) / 1000));

                            try {
                                final PuzzleRecord next = ChessFrame.this.round.createNextRecord(database);

                                if (next != null) {
                                    message += "\nPassons au puzzle suivant.";
                                    JOptionPane.showMessageDialog(ChessFrame.this, message, titre, JOptionPane.PLAIN_MESSAGE);
                                    setPuzzle(Puzzle.getPuzzle(next.getPuzzleHash()));
                                } else {
                                    showRoundResult();

                                }
                            } catch (final SQLException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // retry
                            titre = "Attention";
                            if (ChessFrame.this.badMoveCount > 0) {
                                if (ChessFrame.this.badMoveCount == 1) {
                                    message = "Vous avez fait une erreur.";
                                } else {
                                    message = "Vous avez fait " + ChessFrame.this.badMoveCount + " erreurs.";
                                }
                                message += "\nPour passer au puzzle suivant, il faudra réussir celui-ci sans vous tromper.";
                            } else if (ChessFrame.this.totalTipsCount > 0) {
                                message += "Pour passer au puzzle suivant, il faudra réussir celui-ci sans indice!";
                            }
                            ChessFrame.this.round.getCurrentRecord().setHintsCount(ChessFrame.this.round.getCurrentRecord().getHintsCount() + ChessFrame.this.totalTipsCount);
                            JOptionPane.showMessageDialog(ChessFrame.this, message, titre, JOptionPane.PLAIN_MESSAGE);
                            ChessFrame.this.badMoveCount = 0;
                            ChessFrame.this.tipsCount = 0;
                            ChessFrame.this.totalTipsCount = 0;
                            ChessFrame.this.round.getCurrentRecord().setSuccess(false);
                            setPuzzle(Puzzle.getPuzzle(ChessFrame.this.round.getCurrentRecord().getPuzzleHash()));

                        }

                    }

                });

            }

            @Override
            public void unexpectedMove(final Board board, final Move m) {
                System.out.println("Bad move : " + m);
                ChessFrame.this.boardComponent.setRedBackground(m.getMoveTargetSquare());
                ChessFrame.this.badMoveCount++;
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                ChessFrame.this.boardComponent.setRedBackground(-1);

                            }
                        });

                    }
                });
                t.start();
            }

        };

        final JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(BoardComponent.BOARD_BACKGROUND);
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        final JImage im = new JImage(this.getClass().getResource("logo.png"));
        p.add(im, c);
        final JPanel pInfoPlayer = new JPanel();
        pInfoPlayer.setBackground(Color.WHITE);
        pInfoPlayer.setLayout(new GridLayout(2, 0));

        Font f1 = this.labelLevel.getFont().deriveFont(Font.BOLD);
        f1 = f1.deriveFont(f1.getSize2D() * 1.5f);
        this.labelLevel.setFont(f1);
        pInfoPlayer.add(this.labelLevel);

        final Font f2 = this.labelExp.getFont().deriveFont(this.labelExp.getFont().getSize2D() * 1.5f);
        this.labelExp.setFont(f2);
        pInfoPlayer.add(this.labelExp);

        c.gridx++;
        c.weightx = 1;
        p.add(pInfoPlayer, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 1;
        final JPanel toolbar = new JPanel();
        toolbar.setOpaque(true);
        toolbar.setBackground(Color.WHITE);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));

        toolbar.add(this.newRoundButton);

        // toolbar.add(nextPuzzleButton);
        this.tipsButton.setVisible(false);
        toolbar.add(this.tipsButton);
        this.labelInfo.setVisible(false);
        toolbar.add(this.labelInfo);
        p.add(toolbar, c);
        c.gridy++;
        p.add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(4, 0, 4, 0);
        p.add(this.labelPlayerTop, c);

        this.boardComponent = new BoardComponent(board, this.glassPane, l);
        this.boardComponent.setLocked(true);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(this.boardComponent, c);
        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(4, 0, 4, 0);
        p.add(this.labelPlayerBottom, c);

        this.labelPlayerTop.setOpaque(true);
        this.labelPlayerBottom.setOpaque(true);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy++;
        final JLabel lBottom = new JLabel(" © ILM Informatique - www.openconcerto.org");
        lBottom.setOpaque(true);
        lBottom.setBackground(Color.WHITE);
        p.add(lBottom, c);

        setPlayerNamesVisible(false);
        updatePlayerInfo();
        this.setContentPane(p);
        this.nextPuzzleButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setPuzzle(Puzzle.getPuzzle(getNextPuzzle()));

            }
        });
        this.newRoundButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                ChessFrame.this.newRoundButton.setEnabled(false);
                ChessFrame.this.boardComponent.setShowArrow(false);

                ChessFrame.this.totalTipsCount = 0;
                ChessFrame.this.badMoveCount = 0;

                ChessFrame.this.round = new Round(player);
                PuzzleRecord r;
                try {
                    r = ChessFrame.this.round.createNextRecord(database);
                    setPuzzle(Puzzle.getPuzzle(r.getPuzzleHash()));
                } catch (final SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        this.tipsButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                if (board.getMoveIndex() < board.getPuzzle().getMoves().size()) {
                    String tips = "";
                    final Move m = board.getPuzzle().getMoves().get(board.getMoveIndex());
                    if (ChessFrame.this.tipsCount == 0) {
                        tips = "Jouez " + m.getUci().substring(0, 2);
                    } else {
                        tips = "Le coup est " + m.getUci();
                    }
                    setInfo(tips);
                    ChessFrame.this.tipsCount++;
                    ChessFrame.this.totalTipsCount++;
                }
            }
        });
        final MouseAdapter l2 = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final JPanel p = new JPanel();
                p.setLayout(new GridBagLayout());
                final GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;

                p.add(new JLabel("Votre nom/pseudo :"), c);
                c.gridy++;
                final JTextField tf = new JTextField(player.getName());
                p.add(tf, c);
                c.gridy++;
                p.add(new JLabel(""), c);
                c.gridy++;
                tf.requestFocus();
                JOptionPane.showMessageDialog(null, p, "Chess Elo Booster", JOptionPane.PLAIN_MESSAGE);

                String playerName = tf.getText();
                playerName = ChessFrame.correctPlayerName(playerName);
                player.setName(playerName);
                try {
                    player.commitPlayerInfo(database);
                } catch (final SQLException e1) {
                    e1.printStackTrace();
                }
                if (!ChessFrame.this.boardComponent.isFlipped) {
                    setPlayerNames(player.getName(), getComputerName(), false);
                } else {
                    setPlayerNames(getComputerName(), player.getName(), true);
                }
            }
        };
        this.labelPlayerBottom.addMouseListener(l2);
        this.labelPlayerTop.addMouseListener(l2);
    }

    private void setPlayerNamesVisible(final boolean b) {
        this.labelPlayerBottom.setVisible(b);
        this.labelPlayerTop.setVisible(b);
    }

    protected String getNextPuzzle() {
        final Random r = new Random();
        final int nextInt = r.nextInt(Puzzle.getAll().size() - 1);
        return Puzzle.getAll().get(nextInt).getHash();
    }

    public void setPlayerNames(final String white, final String black, final boolean whiteOnTop) {
        if (whiteOnTop) {
            this.labelPlayerTop.setForeground(Color.BLACK);
            this.labelPlayerTop.setBackground(Color.WHITE);
            this.labelPlayerTop.setText("   " + white + "   ");
            this.labelPlayerBottom.setForeground(Color.WHITE);
            this.labelPlayerBottom.setBackground(Color.BLACK);
            this.labelPlayerBottom.setText("   " + black + "   ");

        } else {
            this.labelPlayerBottom.setForeground(Color.BLACK);
            this.labelPlayerBottom.setBackground(Color.WHITE);
            this.labelPlayerBottom.setText("   " + white + "   ");
            this.labelPlayerTop.setForeground(Color.WHITE);
            this.labelPlayerTop.setBackground(Color.BLACK);
            this.labelPlayerTop.setText("   " + black + "   ");
        }
        setPlayerNamesVisible(true);
    }

    public void setInfo(final String txt) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ChessFrame.this.labelInfo.setVisible(true);
                ChessFrame.this.labelInfo.setText(txt);
            }
        });

    }

    public static void main(final String[] args) throws SQLException {
        final JFrame popup = new JFrame("Chess Elo Booster");
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                popup.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                final JImage im = new JImage(this.getClass().getResource("logo-loading.png"));
                popup.setAlwaysOnTop(true);
                popup.setContentPane(im);
                popup.setUndecorated(true);
                popup.pack();
                popup.setLocationRelativeTo(null);
                popup.setVisible(true);
            }
        });
        final long t1 = System.currentTimeMillis();
        final BaseDirs instance = BaseDirs.create("EloBooster");
        System.out.println(instance);
        final File appDataFolder = instance.getAppDataFolder();
        System.out.println("app data : " + appDataFolder);
        appDataFolder.mkdirs();
        final File f = new File(appDataFolder, "chess.db");
        try {
            database.setDBConnection(new DatabaseConnection(f));

        } catch (final Exception e) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    popup.dispose();
                }
            });

            if (e.getMessage().contains("Database lock acquisition failure")) {
                JOptionPane.showMessageDialog(null, "Le logiciel est déjà lancé.\nIl n'est pas possible de l'ouvrir 2 fois en même temps.");
            } else {
                JOptionPane.showMessageDialog(null, "Erreur d'accès à la base de données\n" + f.getAbsolutePath() + "\n" + e.getMessage());
            }
            return;
        }

        if (database.getSchemas().isEmpty() || database.getSchema("PUBLIC").getTable("PLAYER") == null) {
            System.out.println("Creating table PLAYER");
            final String tPlayer = "CREATE TABLE PLAYER (ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, NAME VARCHAR(60), LEVEL DOUBLE PRECISION , EXP BIGINT, HASH VARCHAR(128));";
            database.execute(tPlayer);
            database.refresh();

        }
        final Table tablePlayer = database.getSchema("PUBLIC").getTable("PLAYER");

        if (database.getSchemas().isEmpty() || database.getSchema("PUBLIC").getTable("RECORD") == null) {
            System.out.println("Creating table RECORD");
            final String tRecord = "CREATE TABLE RECORD (ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, PLAYER INTEGER,HASH VARCHAR(128), DATE BIGINT, DURATION INTEGER, SUCCESS INTEGER, HINTS INTEGER );";
            database.execute(tRecord);
            database.refresh();

        }
        // database.getSchema("PUBLIC").dump(System.out);
        final long t2 = System.currentTimeMillis();
        System.out.println("Init db : " + (t2 - t1) + "ms");

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    popup.dispose();
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                        e.printStackTrace();
                    }

                    if (tablePlayer.fectchRows().isEmpty()) {
                        final JPanel p = new JPanel();
                        p.setLayout(new GridBagLayout());
                        final GridBagConstraints c = new GridBagConstraints();
                        c.gridx = 0;
                        c.gridy = 0;
                        c.fill = GridBagConstraints.BOTH;
                        c.weightx = 1;
                        final JLabel comp = new JLabel("Envie de progresser aux échecs à travers quelques puzzles?");
                        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                        p.add(comp, c);
                        p.add(new JLabel(" "), c);
                        c.gridy++;
                        c.gridy++;
                        p.add(new JLabel("Cette application vous propose des rounds de " + Round.ROUND_SIZE + " puzzles (parmi plus de 15000)."), c);
                        c.gridy++;
                        p.add(new JLabel(" "), c);
                        c.gridy++;
                        p.add(new JLabel("Chaque round est analysé et vos erreurs sont enregistrées."), c);
                        c.gridy++;
                        p.add(new JLabel("Ainsi le logiciel s'assurera de vos progrès dans le temps en s'adaptant à votre niveau."), c);
                        c.gridy++;
                        p.add(new JLabel("Les rounds vous feront gagner des points d'expérience (exp)."), c);
                        c.gridy++;
                        p.add(new JLabel(" "), c);
                        c.gridy++;
                        p.add(new JLabel("Avant de commencer, renseignez votre nom/pseudo :"), c);
                        c.gridy++;
                        final JTextField tf = new JTextField("Joueur");
                        p.add(tf, c);
                        c.gridy++;
                        p.add(new JLabel(" "), c);
                        c.gridy++;
                        p.add(new JLabel("Vous allez commencer au niveau 1, les choses sérieuses arriveront après le niveau 100."), c);
                        c.gridy++;
                        p.add(new JLabel(""), c);
                        c.gridy++;
                        tf.requestFocus();
                        JOptionPane.showMessageDialog(null, p, "Chess Elo Booster", JOptionPane.PLAIN_MESSAGE);

                        String playerName = tf.getText();
                        playerName = correctPlayerName(playerName);

                        if (tablePlayer.fectchRows().isEmpty()) {

                            final Player pl = new Player(0, playerName);
                            pl.setLevel(0);
                            pl.setExp(0);
                            final Map<Column, Object> values = new HashMap<>();
                            values.put(tablePlayer.getColumn("ID"), pl.getId());
                            values.put(tablePlayer.getColumn("NAME"), pl.getName());
                            values.put(tablePlayer.getColumn("LEVEL"), pl.getLevelIndex());
                            values.put(tablePlayer.getColumn("EXP"), pl.getExp());
                            values.put(tablePlayer.getColumn("HASH"), pl.getCheckHash());
                            final MutableRow r = new MutableRow(tablePlayer, values);
                            tablePlayer.insertNewRow(r);
                        }
                    }
                    final List<Row> fectchRows = tablePlayer.fectchRows();
                    final Row row = fectchRows.get(0);

                    final int id = row.getInt("ID");
                    final String name = row.getString("NAME");
                    final Player newPlayer = new Player(id, name);
                    newPlayer.setLevel(row.getFloat("LEVEL"));
                    newPlayer.setExp(row.getLong("EXP"));
                    final String loadedHash = row.getString("HASH");
                    if (!newPlayer.getCheckHash().equals(loadedHash)) {
                        newPlayer.setLevel(1);
                        newPlayer.setExp(1);
                        newPlayer.commitPlayerInfo(database);
                        JOptionPane.showMessageDialog(null, "Boo!");
                    }

                    final ChessFrame frame = new ChessFrame(newPlayer, new Board(true));

                    final List<Image> icons = new ArrayList<>();
                    icons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("icone-16.png")));
                    icons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("icone-32.png")));
                    icons.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("icone-256.png")));
                    frame.setIconImages(icons);
                    frame.setTitle("Chess Elo Booster v1.2");
                    System.out.println(Puzzle.getAll().size() + " puzzles loaded");

                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(572, 700);
                    frame.setMinimumSize(new Dimension(420, 620));
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(final WindowEvent e) {
                            try {
                                database.close();
                            } catch (final Exception e1) {
                                e1.printStackTrace();
                            }
                            System.out.println("Closed");
                            e.getWindow().dispose();
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

        });

    }

    private static String correctPlayerName(String playerName) {
        if (playerName.length() == 0) {
            playerName = "Joueur";
        }
        if (playerName.length() > 40) {
            playerName = playerName.substring(0, 40);
        }
        if (playerName.toLowerCase().contains("blitzstream")) {
            playerName += " [NM]";
        }
        if (playerName.toLowerCase().contains("joachim") && playerName.toLowerCase().contains("mouhamad")) {
            playerName += " [FM]";
        }
        return playerName;
    }

    protected void setPuzzle(final Puzzle p) {
        this.tipsCount = 0;
        this.board.setPuzzle(p);

        this.board.initFromFEN(p.getFen());
        this.boardComponent.setFlipped(!this.board.getTurn());

        if (p.endAtCheckMate()) {
            setInfo("Cherchez l'échec et mat");
        } else {
            setInfo("Cherchez un gain matériel");
        }

        repaint();
        if (this.board.getTurn()) {
            setPlayerNames(this.player.getName(), getComputerName(), false);
        } else {
            setPlayerNames(getComputerName(), this.player.getName(), true);
        }
        final Move firstMove = p.getMoves().get(0);
        animate(firstMove);
        this.tipsButton.setEnabled(true);
        this.tipsButton.setVisible(true);
        this.startPuzzleTime = System.currentTimeMillis();
    }

    public String getComputerName() {
        final String[] t = new String[] { "Lucas Sparov", "Mika Sparov", "Rika Sparov", "Foka Sparov", "Miska Sparov" };
        final int i = new Random().nextInt(t.length);
        return t[i];
    }

    private void animate(final Move animate) {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                final int indexStart = animate.getMoveSourceSquare();
                final int indexEnd = animate.getMoveTargetSquare();

                final int piece = ChessFrame.this.board.getPieceAt(indexStart);
                ChessFrame.this.board.setPiece(indexStart, Chess.EMPTY);
                final Point start = Board.internalToXY(indexStart);
                final Point stop = Board.internalToXY(indexEnd);

                final int cellSize = ChessFrame.this.boardComponent.getCellSize();

                final int startX;
                final int stopX;
                final int startY;
                final int stopY;
                if (ChessFrame.this.boardComponent.isFlipped) {
                    startX = (7 - start.x) * cellSize;
                    stopX = (7 - stop.x) * cellSize;

                    startY = start.y * cellSize;
                    stopY = stop.y * cellSize;
                } else {
                    startX = (start.x) * cellSize;
                    stopX = (stop.x) * cellSize;

                    startY = (7 - start.y) * cellSize;
                    stopY = (7 - stop.y) * cellSize;
                }

                final int moveX = stopX - startX;
                final int moveY = stopY - startY;
                final int length = (int) Math.sqrt(moveX * moveX + moveY * moveY);
                final int nbPoint = 2 + length / 4;
                final float deltaX = ((float) moveX) / nbPoint;

                final float deltaY = ((float) moveY) / nbPoint;
                ChessFrame.this.glassPane.setVisible(true);
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        float xx = startX;
                        float yy = startY;
                        for (int i = 0; i < nbPoint; i++) {
                            final int x = Math.round(xx);
                            final int y = Math.round(yy);
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    final Point p = SwingUtilities.convertPoint(ChessFrame.this.boardComponent, new Point(x, y), ChessFrame.this.getContentPane());
                                    ChessFrame.this.glassPane.setPiece(piece, p.x + ChessFrame.this.boardComponent.getOffsetX(), p.y + ChessFrame.this.boardComponent.getOffsetY(), cellSize);

                                }
                            });
                            xx += deltaX;
                            yy += deltaY;
                            try {
                                Thread.sleep(8);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                ChessFrame.this.board.setPiece(indexStart, piece);
                                ChessFrame.this.glassPane.setVisible(false);
                                ChessFrame.this.board.applyNow(animate);
                                ChessFrame.this.repaint();
                                ChessFrame.this.boardComponent.setLocked(false);

                            }
                        });

                    }
                });
                t.start();

            }
        });

    }

    public void updatePlayerInfo() {
        this.labelLevel.setText("Niveau " + this.player.getPublicLevel() + " ");
        this.labelExp.setText(this.player.getExp() + " exp  ");
    }

    private void showRoundResult() {
        final JPanel p = new JPanel();

        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 5);
        for (int i = 0; i < Round.ROUND_SIZE; i++) {
            final JLabel l = new JLabel("Puzzle " + (i + 1), SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            p.add(l, c);
            c.gridx++;
        }
        //
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel l1 = new JLabel("Réussi du 1e coup", SwingConstants.LEFT);
        l1.setFont(l1.getFont().deriveFont(Font.BOLD));
        p.add(l1, c);
        c.fill = GridBagConstraints.NONE;
        for (int i = 0; i < Round.ROUND_SIZE; i++) {
            c.gridx++;
            if (this.round.getRecord(i).isSuccess()) {
                final JLabel label = new JLabel(" oui ", SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(GREEN);
                label.setForeground(Color.WHITE);
                p.add(label, c);
            } else {
                p.add(new JLabel(" non ", SwingConstants.CENTER), c);
            }

        }
        //
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel l2 = new JLabel("Indices", SwingConstants.LEFT);
        l2.setFont(l1.getFont());
        p.add(l2, c);
        c.fill = GridBagConstraints.NONE;
        for (int i = 0; i < Round.ROUND_SIZE; i++) {
            c.gridx++;
            if (this.round.getRecord(i).getHintsCount() == 0) {
                final JLabel label = new JLabel(" aucun ", SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(GREEN);
                label.setForeground(Color.WHITE);
                p.add(label, c);
            } else {
                p.add(new JLabel(" " + this.round.getRecord(i).getHintsCount() + " ", SwingConstants.CENTER), c);
            }

        }
        //
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabel l3 = new JLabel("Temps", SwingConstants.LEFT);
        l3.setFont(l1.getFont());
        p.add(l3, c);
        c.fill = GridBagConstraints.NONE;
        for (int i = 0; i < Round.ROUND_SIZE; i++) {
            c.gridx++;
            final int durationInSeconds = this.round.getRecord(i).getDurationInSeconds();
            if (durationInSeconds < Puzzle.getPuzzle(this.round.getRecord(i).getPuzzleHash()).getMoves().size() * 2) {
                final JLabel label = new JLabel(" " + formatDuration(durationInSeconds) + " ", SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(GREEN);
                label.setForeground(Color.WHITE);
                p.add(label, c);
            } else {
                p.add(new JLabel(" " + formatDuration(durationInSeconds) + " ", SwingConstants.CENTER), c);
            }

        }

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel(" "), c);

        c.gridwidth = Round.ROUND_SIZE + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final int previousLevel = this.player.getPublicLevel();
        final long previousXP = this.player.getExp();

        try {
            this.round.applyScore(database);
        } catch (final Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(ChessFrame.this, "Oups, problème de base de données.\n" + e.getMessage());
        }

        final int newLevel = this.player.getPublicLevel();
        final long newXP = this.player.getExp();
        if (newLevel > previousLevel) {
            c.gridy++;
            final JLabel label = new JLabel(this.player.getName() + ", votre niveau augmente ! Encore un round ?", SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(label.getFont().getSize() + 5f));
            p.add(label, c);
        }
        if (newLevel < previousLevel) {
            c.gridy++;
            final JLabel label = new JLabel(this.player.getName() + ", il va falloir faire quelques rounds de plus pour progresser.", SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(label.getFont().getSize() + 5f));
            p.add(label, c);
        }

        if (newXP > previousXP) {
            c.gridy++;
            final JLabel label = new JLabel("+ " + (newXP - previousXP) + " exp ", SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(label.getFont().getSize() + 5f));
            p.add(label, c);
        }

        JOptionPane.showMessageDialog(ChessFrame.this, p, "Résultats du round", JOptionPane.PLAIN_MESSAGE);

        this.newRoundButton.setEnabled(true);
        setPlayerNamesVisible(false);
        this.board.resetToStartingPosition();
        this.boardComponent.setShowArrow(true);
        repaint();
        updatePlayerInfo();
    }

    private String formatDuration(final int durationInSeconds) {
        if (durationInSeconds < 60) {
            return durationInSeconds + "s";
        }
        final int min = durationInSeconds / 60;
        final int s = durationInSeconds % 60;
        if (s != 0) {
            return min + "min et " + s + "s";
        }
        return min + "min";
    }

}
