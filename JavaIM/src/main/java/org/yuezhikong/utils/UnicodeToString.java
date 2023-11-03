package org.yuezhikong.utils;

import org.jetbrains.annotations.NotNull;

public class UnicodeToString {
    public static @NotNull String unicodeToString(@NotNull String unicode) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = unicode.length();
        while (i < len) {
            char c = unicode.charAt(i);
            if (c == '\\') {
                if (i < len - 5) {
                    char c1 = unicode.charAt(i + 1);
                    char c2 = unicode.charAt(i + 2);
                    char c3 = unicode.charAt(i + 3);
                    char c4 = unicode.charAt(i + 4);
                    if (c1 == 'u' && isHexDigit(c2) && isHexDigit(c3) && isHexDigit(c4)) {
                        int code = (Character.digit(c2, 16) << 12)
                                + (Character.digit(c3, 16) << 8)
                                + (Character.digit(c4, 16) << 4)
                                + Character.digit(unicode.charAt(i + 5), 16);
                        sb.append((char) code);
                        i += 6;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9') ||
                (ch >= 'a' && ch <= 'f') ||
                (ch >= 'A' && ch <= 'F');
    }
    private UnicodeToString() {}
}
