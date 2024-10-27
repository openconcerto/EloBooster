package ceb;

import java.util.ArrayList;
import java.util.List;

public class ChessNode {
    String label;
    ChessNode parent;
    List<ChessNode> nodes;
    int depth = 0;

    public ChessNode(final String label) {
        this.label = label;
    }

    public void addChild(final ChessNode node) {
        if (this.nodes == null) {
            this.nodes = new ArrayList<>();
        }
        node.depth = this.depth + 1;
        node.parent = this;
        this.nodes.add(node);
    }

    public String getLabel() {
        return this.label;
    }

    public ChessNode getParent() {
        return this.parent;
    }

    public List<ChessNode> getNodes() {
        return this.nodes;
    }

    public int getDepth() {
        return this.depth;
    }

    @Override
    public String toString() {
        return this.label + "[" + this.depth + "]";
    }

    public ChessNode getChild(final String label2) {
        if (this.nodes == null)
            return null;
        for (final ChessNode n : this.nodes) {
            if (n.getLabel().equals(label2)) {
                return n;
            }
        }
        return null;
    }
}
