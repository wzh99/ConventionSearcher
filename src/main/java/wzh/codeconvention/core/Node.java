package wzh.codeconvention.core;

import java.io.Serializable;
import java.util.ArrayList;

public class Node implements Serializable {
    // Contents of current node
    String headline = null;
    int level = 0;
    ArrayList<Block> contents = new ArrayList<>();
    // Parent and children nodes
    Node parent = null;
    ArrayList<Node> children = new ArrayList<>();

    public String getHeadline() { return headline; }
    public int getLevel() { return level; }
    public ArrayList<Block> getContents() { return contents; }

    public Node getParent() { return parent; }
    public ArrayList<Node> getChildren() { return children; }

    @Override
    public String toString() {
        var output = new StringBuilder();
        contents.forEach((var block) -> output.append(block.toString().concat("\n")));
        return String.format("%s\n%s", headline, output);
    }
}