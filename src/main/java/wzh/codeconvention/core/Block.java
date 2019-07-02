package wzh.codeconvention.core;

import java.io.Serializable;

public class Block implements Serializable {
    ContentType type;
    // Plain text
    String text = null; // only plain text can be indexed and navigated
    // Code block or table
    StringBuilder lines = new StringBuilder();

    public Block(ContentType type) {
        this.type = type;
    }

    public ContentType getType() { return type; }

    @Override
    public String toString() {
        return type == ContentType.PLAIN_TEXT ? text : lines.toString();
    }
}