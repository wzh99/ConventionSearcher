package wzh.codeconvention.core;

public class SearchResult implements Comparable<SearchResult> {
    ContentTag tag;
    int nWords, nMatches;

    SearchResult(ContentTag tag, int nWords, int nMatches) {
        this.tag = tag;
        this.nWords = nWords;
        this.nMatches = nMatches;
    }

    public ContentTag getTag() { return tag; }

    @Override
    public String toString() {
        return String.format("{%s, %d, %d}", tag, nWords, nMatches);
    }

    @Override
    public int compareTo(SearchResult o) {
        if (nWords != o.nWords)
            return o.nWords - nWords;
        else if (nMatches != o.nMatches)
            return o.nMatches - nMatches;
        else
            return tag.toString().length() - o.tag.toString().length();
    }
}