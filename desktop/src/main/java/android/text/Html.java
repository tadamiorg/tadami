package android.text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.Set;

/**
 * Android's HTML to text, without the spans: the text as a browser lays it out, whitespace collapsed,
 * a line break for a br and an empty line between blocks.
 */
public final class Html {
    public static final int FROM_HTML_MODE_LEGACY = 0;
    public static final int FROM_HTML_MODE_COMPACT = 63;

    private static final Set<String> BLOCKS = Set.of(
            "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "blockquote", "pre", "table", "tr"
    );

    private Html() {
    }

    @Deprecated
    public static Spanned fromHtml(String source) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY);
    }

    public static Spanned fromHtml(String source, int flags) {
        StringBuilder out = new StringBuilder();
        append(Jsoup.parse(source).body(), out, (flags & FROM_HTML_MODE_COMPACT) != 0 ? "\n" : "\n\n");
        return new PlainSpanned(out.toString().replaceAll("\\n+$", ""));
    }

    private static void append(Node node, StringBuilder out, String blockBreak) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode) {
                String text = ((TextNode) child).getWholeText().replaceAll("\\s+", " ");
                if (text.startsWith(" ") && (out.length() == 0 || out.charAt(out.length() - 1) == ' '
                        || out.charAt(out.length() - 1) == '\n')) {
                    text = text.substring(1);
                }
                out.append(text);
            } else if (child instanceof Element) {
                String tag = ((Element) child).normalName();
                if (tag.equals("br")) {
                    out.append('\n');
                } else if (BLOCKS.contains(tag)) {
                    breakBlock(out, blockBreak);
                    append(child, out, blockBreak);
                    breakBlock(out, blockBreak);
                } else if (!tag.equals("script") && !tag.equals("style")) {
                    append(child, out, blockBreak);
                }
            }
        }
    }

    private static void breakBlock(StringBuilder out, String blockBreak) {
        if (out.length() == 0 || out.toString().endsWith(blockBreak)) return;
        while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') out.setLength(out.length() - 1);
        out.append(blockBreak);
    }

    private static final class PlainSpanned implements Spanned {
        private final String text;

        PlainSpanned(String text) {
            this.text = text;
        }

        @Override public int length() { return text.length(); }
        @Override public char charAt(int index) { return text.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) { return text.subSequence(start, end); }
        @Override public String toString() { return text; }
    }
}
