package com.blog.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {
    /** 保留英文、数字、连字符与中日韩统一表意文字，避免中文标签被清空成 post-时间戳。 */
    private static final Pattern NON_SLUG = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fff-]+");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "item-" + System.currentTimeMillis();
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFKC);
        String slug = NON_SLUG.matcher(normalized).replaceAll("-");
        slug = slug.toLowerCase(Locale.ROOT).replaceAll("-{2,}", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        if (slug.isBlank() || slug.equals("-")) {
            return "post-" + System.currentTimeMillis();
        }
        if (slug.length() > 120) {
            slug = slug.substring(0, 120).replaceAll("-+$", "");
        }
        return slug;
    }
}
