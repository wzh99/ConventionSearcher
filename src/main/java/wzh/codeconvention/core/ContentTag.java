package wzh.codeconvention.core;

import java.io.Serializable;

public class ContentTag implements Serializable, Comparable<ContentTag> {
    private Node node;
    private Block block;

    ContentTag(Node node, Block block) {
        this.node = node;
        this.block = block;
    }

    public Node getNode() { return node; }
    public Block getBlock() { return block; }

    @Override
    public int compareTo(ContentTag o) {
        if (node != o.node) { // belongs to possibly node
            var diff = node.headline.compareTo(o.node.headline);
            if (diff != 0) return diff;
        }
        if (block.type != o.block.type)
            return block.type.compareTo(o.block.type);
        if (block.type == ContentType.PLAIN_TEXT)
            return block.text.compareTo(o.block.text);
        else
            return block.lines.compareTo(o.block.lines);
    }

    @Override
    public String toString() {
        return String.format("%s\n%s", node.headline, block);
    }
}