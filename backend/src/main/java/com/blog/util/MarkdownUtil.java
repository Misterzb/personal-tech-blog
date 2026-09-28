package com.blog.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownUtil {
    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;
    private static final Pattern ANCHOR = Pattern.compile(
            "<a\\s+([^>]*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern HREF_HTTP = Pattern.compile(
            "href\\s*=\\s*\"(https?://[^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    static {
        MutableDataSet options = new MutableDataSet();
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    private MarkdownUtil() {
    }

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return enhanceExternalLinks(RENDERER.render(PARSER.parse(markdown)));
    }

    /** 外链默认新标签打开，站内相对链接保持当前页。 */
    static String enhanceExternalLinks(String html) {
        Matcher m = ANCHOR.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String attrs = m.group(1);
            if (!HREF_HTTP.matcher(attrs).find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            String lower = attrs.toLowerCase();
            if (!lower.contains("target=")) {
                attrs = attrs + " target=\"_blank\"";
            }
            if (!lower.contains("rel=")) {
                attrs = attrs + " rel=\"noopener noreferrer\"";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement("<a " + attrs + ">"));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
