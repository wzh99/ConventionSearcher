package wzh.codeconvention.gui;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.options.DataHolder;

class MarkdownRenderer {
    private static final DataHolder options =
            PegdownOptionsAdapter.flexmarkOptions(Extensions.ALL);
    private static final Parser parser = Parser.builder(options).build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

    public static String render(String src) {
        return renderer.render(parser.parse(src));
    }
}
