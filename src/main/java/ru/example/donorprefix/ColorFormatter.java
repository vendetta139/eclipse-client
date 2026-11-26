package ru.example.donorprefix;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

/**
 * Утилита для разбора и форматирования строк префиксов с кодами цветов &amp; и &#RRGGBB
 * в нативный MutableText Minecraft для красивого живого предпросмотра в GUI.
 */
public class ColorFormatter {

    public static MutableText format(String raw, boolean replaceUnderscores) {
        if (raw == null || raw.isEmpty()) {
            return Text.empty();
        }

        String text = raw;
        if (replaceUnderscores) {
            text = text.replace('_', ' ');
        }

        MutableText root = Text.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder currentSegment = new StringBuilder();

        int length = text.length();
        int i = 0;

        while (i < length) {
            char c = text.charAt(i);

            // 1. Проверка на BungeeCord / Spigot hex-код: &x&r&r&g&g&b&b (14 символов)
            if ((c == '&' || c == '§') && i + 13 < length) {
                char x = text.charAt(i + 1);
                if ((x == 'x' || x == 'X')
                        && isDelimiter(text.charAt(i + 2)) && isHexChar(text.charAt(i + 3))
                        && isDelimiter(text.charAt(i + 4)) && isHexChar(text.charAt(i + 5))
                        && isDelimiter(text.charAt(i + 6)) && isHexChar(text.charAt(i + 7))
                        && isDelimiter(text.charAt(i + 8)) && isHexChar(text.charAt(i + 9))
                        && isDelimiter(text.charAt(i + 10)) && isHexChar(text.charAt(i + 11))
                        && isDelimiter(text.charAt(i + 12)) && isHexChar(text.charAt(i + 13))) {

                    if (currentSegment.length() > 0) {
                        root.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
                        currentSegment.setLength(0);
                    }
                    int rgb = (hexVal(text.charAt(i + 3)) << 20)
                            | (hexVal(text.charAt(i + 5)) << 16)
                            | (hexVal(text.charAt(i + 7)) << 12)
                            | (hexVal(text.charAt(i + 9)) << 8)
                            | (hexVal(text.charAt(i + 11)) << 4)
                            | hexVal(text.charAt(i + 13));
                    currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    i += 14;
                    continue;
                }
            }

            // 2. Проверка на hex-код &#RRGGBB или §#RRGGBB (8 символов)
            if ((c == '&' || c == '§') && i + 7 < length && text.charAt(i + 1) == '#') {
                int rgb = parseHex6(text, i + 2);
                if (rgb >= 0) {
                    if (currentSegment.length() > 0) {
                        root.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
                        currentSegment.setLength(0);
                    }
                    currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    i += 8;
                    continue;
                }
            }

            // 3. Проверка на компактный hex &xRRGGBB или §xRRGGBB (8 символов)
            if ((c == '&' || c == '§') && i + 7 < length && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
                int rgb = parseHex6(text, i + 2);
                if (rgb >= 0) {
                    if (currentSegment.length() > 0) {
                        root.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
                        currentSegment.setLength(0);
                    }
                    currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    i += 8;
                    continue;
                }
            }

            // 4. Проверка на MiniMessage hex-код <#RRGGBB> (9 символов)
            if (c == '<' && i + 8 < length && text.charAt(i + 1) == '#' && text.charAt(i + 8) == '>') {
                int rgb = parseHex6(text, i + 2);
                if (rgb >= 0) {
                    if (currentSegment.length() > 0) {
                        root.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
                        currentSegment.setLength(0);
                    }
                    currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    i += 9;
                    continue;
                }
            }

            // 5. Проверка на обычные коды &0-9, &a-f, &k-o, &r
            if ((c == '&' || c == '§') && i + 1 < length) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                Formatting formatting = Formatting.byCode(code);

                if (formatting != null) {
                    if (currentSegment.length() > 0) {
                        root.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
                        currentSegment.setLength(0);
                    }

                    if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else if (formatting == Formatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting == Formatting.BOLD) {
                        currentStyle = currentStyle.withBold(true);
                    } else if (formatting == Formatting.ITALIC) {
                        currentStyle = currentStyle.withItalic(true);
                    } else if (formatting == Formatting.UNDERLINE) {
                        currentStyle = currentStyle.withUnderline(true);
                    } else if (formatting == Formatting.STRIKETHROUGH) {
                        currentStyle = currentStyle.withStrikethrough(true);
                    } else if (formatting == Formatting.OBFUSCATED) {
                        currentStyle = currentStyle.withObfuscated(true);
                    }

                    i += 2;
                    continue;
                }
            }

            currentSegment.append(c);
            i++;
        }

        if (currentSegment.length() > 0) {
            root.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
        }

        return root;
    }

    public static MutableText format(String raw) {
        return format(raw, true);
    }

    private static boolean isDelimiter(char c) {
        return c == '&' || c == '§';
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static int hexVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        return 10 + (c - 'A');
    }

    private static int parseHex6(String s, int start) {
        int val = 0;
        for (int j = 0; j < 6; j++) {
            char ch = s.charAt(start + j);
            if (!isHexChar(ch)) return -1;
            val = (val << 4) | hexVal(ch);
        }
        return val;
    }
}
