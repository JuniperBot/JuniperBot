package ru.caramel.juniperbot.core.modules.wiki.utils;

import de.fau.cs.osr.ptk.common.AstVisitor;
import de.fau.cs.osr.utils.StringTools;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriUtils;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.config.Namespace;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngPage;
import org.sweble.wikitext.parser.nodes.*;
import org.sweble.wikitext.parser.parser.LinkTargetException;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;

public class TextConverter
        extends
        AstVisitor<WtNode> {
    private static final Pattern ws = Pattern.compile("[ \\r\\t\\f]+");

    private final WikiConfig config;

    private final EmbedBuilder builder;

    private StringBuilder textBuilder;

    private int extLinkNum;

    /**
     * Becomes true if we are no long at the Beginning Of the whole Document.
     */
    private boolean beginning;

    private int needNewlines;

    private boolean needSpace;

    private LinkedList<Integer> sections;

    private boolean endReached;

    private boolean limitCheck;

    private boolean sectionStarted;

    private boolean hasImage;

    // =========================================================================

    public TextConverter(WikiConfig config, EmbedBuilder builder) {
        this.config = config;
        this.builder = builder;
    }

    @Override
    protected WtNode before(WtNode node) {
        textBuilder = new StringBuilder();
        extLinkNum = 1;
        beginning = true;
        needNewlines = 0;
        needSpace = false;
        endReached = false;
        limitCheck = true;
        sectionStarted = false;
        hasImage = false;
        sections = new LinkedList<Integer>();
        return super.before(node);
    }

    @Override
    protected Object after(WtNode node, Object result) {
        builder.appendDescription(textBuilder.toString());
        return builder.build();
    }

    @Override
    protected Object dispatch(WtNode node) {
        return super.dispatch(node);
    }

    // =========================================================================

    public void visit(WtNodeList n) {
        iterate(n);
    }

    public void visit(WtUnorderedList e) {
        iterate(e);
    }

    public void visit(WtOrderedList e) {
        iterate(e);
    }

    public void visit(WtListItem item) {
        newline(1);
        iterate(item);
    }

    public void visit(EngPage p) {
        iterate(p);
    }

    public void visit(WtText text) {
        write(text.getContent());
    }

    public void visit(WtWhitespace w) {
        write(" ");
    }

    public void visit(WtBold b) {
        String content = getContent(b);
        append("**" + content + "**");
    }

    public void visit(WtItalics i) {
        String content = getContent(i);
        append("_" + content + "_");
    }

    public void visit(WtXmlCharRef cr) {
        write(Character.toChars(cr.getCodePoint()));
    }

    public void visit(WtXmlEntityRef er) {
        String ch = er.getResolved();
        if (ch == null) {
            write('&');
            write(er.getName());
            write(';');
        } else {
            write(ch);
        }
    }

    public void visit(WtUrl wtUrl) {
        if (!wtUrl.getProtocol().isEmpty()) {
            write(wtUrl.getProtocol());
            write(':');
        }
        write(wtUrl.getPath());
    }

    public void visit(WtExternalLink link) {
        if (link.hasTitle()) {
            iterate(link.getTitle());
        }
    }

    public void visit(WtInternalLink link) throws UnsupportedEncodingException {
        try {
            if (link.getTarget().isResolved()) {
                PageTitle page = PageTitle.make(config, link.getTarget().getAsString());
                if (page.getNamespace().equals(config.getNamespace("Category")))
                    return;
            }
        } catch (LinkTargetException e) {
            // fall down
        }

        /*if (!link.hasTitle()) {
            iterate(link.getTarget());
        } else {
            String title = getContent(link.getTitle());
            String target = UriUtils.encode(getContent(link.getTarget()), "UTF-8");
            String url = config.getWikiUrl() + "/wiki/" + target;
            write(CommonUtils.mdLink(title, url));
        }*/

        iterate(link.hasTitle() ? link.getTitle() : link.getTarget());
    }

    public void visit(WtSection s) {
        sectionStarted = true;
        String title = getContent(s.getHeading());

        if (s.getLevel() >= 1) {
            while (sections.size() > s.getLevel())
                sections.removeLast();
            while (sections.size() < s.getLevel())
                sections.add(1);

            StringBuilder sb2 = new StringBuilder();
            for (int i = 0; i < sections.size(); ++i) {
                if (i < 1) {
                    continue;
                }

                sb2.append(sections.get(i));
                sb2.append('.');
            }

            if (sb2.length() > 0) {
                sb2.append(' ');
            }
            sb2.append(title);
            title = sb2.toString();
        }

        newline(1);
        write("__" + title + "__");
        newline(1);

        iterate(s.getBody());

        while (sections.size() > s.getLevel())
            sections.removeLast();
        sections.add(sections.removeLast() + 1);
    }

    public void visit(WtParagraph p) {
        iterate(p);
        newline(1);
    }

    public void visit(WtHorizontalRule hr) {
        newline(1);
        write(StringTools.strrep('-', 10));
        newline(1);
    }

    public void visit(WtXmlElement e) {
        if (e.getName().equalsIgnoreCase("br")) {
            newline(1);
        } else {
            iterate(e.getBody());
        }
    }

    // =========================================================================
    // Stuff we want to hide

    public void visit(WtImageLink n) throws UnsupportedEncodingException {
        if (sectionStarted || hasImage) {
            return;
        }

        if (n.getTarget() != null) {
            WtPageName target = n.getTarget();
            if (!target.isEmpty() && target.get(0) instanceof WtText) {
                String content = ((WtText) target.get(0)).getContent();
                if (StringUtils.isNotEmpty(content)) {
                    content = getNamespaceValue(6, content);
                    if (StringUtils.isNotEmpty(content)) {
                        builder.setImage(config.getWikiUrl() + "/wiki/ru:Special:Filepath/" + UriUtils.encode(content, "UTF-8"));
                        hasImage = true;
                    }
                }
            }
        }
    }

    private String getNamespaceValue(int id, String value) {

        Namespace ns = config.getNamespace(6);

        Set<String> names = new HashSet<>(ns.getAliases());
        names.add(ns.getName());

        for (String name : names) {
            if (value.startsWith(name + ":")) {
                return value.substring(name.length() + 1);
            }
        }

        return null;
    }

    public void visit(WtIllegalCodePoint n) {
    }

    public void visit(WtXmlComment n) {
    }

    public void visit(WtTemplate n) {
    }

    public void visit(WtTemplateArgument n) {
    }

    public void visit(WtTemplateParameter n) {
    }

    public void visit(WtTagExtension n) {
    }

    public void visit(WtPageSwitch n) {
    }

    public void visit(WtNode n) {
    }

    // =========================================================================

    private void newline(int num) {
        if (!beginning && num > needNewlines) {
            needNewlines = num;
        }
    }

    private void wantSpace() {
        if (!beginning) {
            needSpace = true;
        }
    }

    private String getContent(WtNode node) {
        limitCheck = false;
        StringBuilder mainBuilder = textBuilder;
        textBuilder = new StringBuilder();
        iterate(node);
        String value = textBuilder.toString();
        textBuilder = mainBuilder;
        limitCheck = true;
        return value;
    }

    private void append(CharSequence sequence) {
        if (limitCheck) {
            if (endReached) {
                return;
            }
            if (textBuilder.length() + sequence.length() > MessageEmbed.TEXT_MAX_LENGTH) {
                endReached = true;
                if (textBuilder.length() + 3 <= MessageEmbed.TEXT_MAX_LENGTH) {
                    textBuilder.append("...");
                }
                return;
            }
        }
        textBuilder.append(sequence);
    }

    private void writeNewlines(int num) {
        append(StringTools.strrep('\n', num));
        needNewlines = 0;
        needSpace = false;
    }

    private void writeWord(String s) {
        if (needSpace && needNewlines <= 0) {
            append(" ");
        }
        if (needNewlines > 0) {
            writeNewlines(needNewlines);
        }
        needSpace = false;
        beginning = false;

        while (s.contains("\n\n")) {
            s = s.replace("\n\n", "\n");
        }
        if (s.startsWith("\n") && textBuilder.lastIndexOf("\n") == textBuilder.length() - 1) {
            s = s.substring(1);
        }
        append(s);
    }

    private void write(String s) {
        if (s.isEmpty() || (limitCheck && endReached)) {
            return;
        }

        if (Character.isSpaceChar(s.charAt(0))) {
            wantSpace();
        }

        String[] words = ws.split(s);
        for (int i = 0; i < words.length; ) {
            writeWord(words[i]);
            if (++i < words.length)
                wantSpace();
        }

        if (Character.isSpaceChar(s.charAt(s.length() - 1)))
            wantSpace();
    }

    private void write(char[] cs) {
        write(String.valueOf(cs));
    }

    private void write(char ch) {
        writeWord(String.valueOf(ch));
    }

    private void write(int num) {
        writeWord(String.valueOf(num));
    }
}