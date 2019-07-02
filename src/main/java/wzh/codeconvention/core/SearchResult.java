package wzh.codeconvention.core;

public class SearchResult {
    private ContentTag tag;
    private float score;

    SearchResult(ContentTag tag, float score) {
        this.tag = tag;
        this.score = score;
    }

    public ContentTag getTag() { return tag; }
    public float getScore() { return score; }

    @Override
    public String toString() {
        return String.format("{%s, %.2f}", tag, score);
    }
}