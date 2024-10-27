package ceb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class ChessTree {
    ChessNode root;

    public static void main(final String[] args) throws IOException {
        final ChessTree tree = new ChessTree();
        tree.fillTest();
        tree.dumpFlat(System.out);
        System.out.println("-");
        tree.loadFrom(new File("arbre_blancs.txt"));
        tree.dumpFlat(System.out);
        for (final ChessNode n : tree.root.getNodes()) {
            System.out.println(n.toString());
        }
    }

    private void fillTest() {

        final ChessNode t1 = new ChessNode("1");
        t1.addChild(new ChessNode("1.1"));
        t1.addChild(new ChessNode("1.2"));

        final ChessNode t2 = new ChessNode("2");
        t2.addChild(new ChessNode("2.1"));
        t2.addChild(new ChessNode("2.2"));

        this.root = new ChessNode("root");
        this.root.addChild(t1);
        this.root.addChild(t2);

    }

    private void dumpFlat(final PrintStream out) {
        dump(out, this.root);

    }

    private void dump(final PrintStream out, ChessNode n) {
        if (n.getNodes() == null) {
            String s = n.getLabel();
            n = n.getParent();
            while (n != null) {
                s = n.getLabel() + " " + s;
                n = n.getParent();
            }
            out.println(s);

        } else {
            for (final ChessNode node : n.getNodes()) {
                dump(out, node);
            }
        }

    }

    private void loadFrom(final File file) throws IOException {
        this.root = null;

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            ChessNode currentNode = null;
            String line = reader.readLine();
            int lineIndex = 1;
            while (line != null) {
                final String label = line.trim();
                if (!label.isEmpty()) {
                    final int depth = getDepth(line);
                    System.out.println("---- line " + lineIndex + " : " + depth + ":" + label);
                    if (depth == 0) {
                        if (currentNode == null) {
                            // first time root
                            this.root = new ChessNode(label);
                            currentNode = this.root;
                        } else {
                            if (this.root.getLabel().equals(label)) {
                                currentNode = this.root;
                            } else {
                                throw new IllegalStateException("level 0 node " + label + " != root (" + this.root.getLabel() + ") on line " + lineIndex);
                            }
                        }

                    } else {
                        if (depth == currentNode.getDepth()) {
                            throw new IllegalStateException("node " + label + " on same level the current one (" + currentNode.getLabel() + ") on line " + lineIndex);
                        }
                        if (depth < currentNode.getDepth()) {
                            boolean found = false;
                            // rewind
                            final int step = currentNode.getDepth();
                            for (int j = 0; j < step; j++) {
                                currentNode = currentNode.getParent();
                                System.out.println("rewind: " + currentNode);
                                if (currentNode.getDepth() == depth) {
                                    if (!currentNode.getLabel().equals(label)) {
                                        currentNode.getParent().addChild(new ChessNode(label));
                                    }
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                System.out.println("ChessTree.loadFrom() not found");
                            }
                        } else if (depth == currentNode.getDepth() + 1) {
                            // child
                            ChessNode node = currentNode.getChild(label);
                            if (node == null) {
                                node = new ChessNode(label);
                                currentNode.addChild(node);
                            }
                            currentNode = node;
                        } else {
                            throw new IllegalStateException(
                                    "node " + label + " has invalid depth " + depth + " (" + currentNode.getLabel() + "is depth " + currentNode.getDepth() + " on line " + lineIndex);
                        }

                    }

                }
                // next
                line = reader.readLine();
                lineIndex++;

            }
        }
    }

    private int getDepth(final String line) {
        int result = 0;
        final int lenght = line.length();
        for (int i = 0; i < lenght; i++) {
            final char c = line.charAt(i);
            if (c == '\t') {
                result++;
            } else {
                break;
            }

        }
        return result;
    }

}
