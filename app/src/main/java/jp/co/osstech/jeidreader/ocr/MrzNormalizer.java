package jp.co.osstech.jeidreader.ocr;

import java.util.Locale;

/**
 * OCRが返した行をMRZの文字集合({@code A-Z}、{@code 0-9}、{@code <})へ正規化します。
 *
 * <p>汎用OCRはMRZのフィラー({@code <})を {@code «} や {@code ≪} として返すことがあり、
 * また文字間に空白を挿入することがあるため、比較・パースの前に必ずこの正規化を通す。
 */
public final class MrzNormalizer {
    private MrzNormalizer() {
    }

    /**
     * 1行を正規化します。MRZに現れ得ない文字は削除します。
     *
     * @param line OCRが返した行(nullを許容)
     * @return 正規化後の文字列(nullの場合は空文字列)
     */
    public static String normalize(String line) {
        if (line == null) {
            return "";
        }
        String upper = line.toUpperCase(Locale.US);
        StringBuilder sb = new StringBuilder(upper.length());
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '<') {
                sb.append(c);
                continue;
            }
            switch (c) {
                // 全角/半角のギュメ、山括弧に類する誤認識をフィラーへ寄せる
                case '«': // «
                case '‹': // ‹
                case 'ⱼ': // ⱼ (稀に < の誤認識で出る)
                case '(':
                case '{':
                case '[':
                case '^':
                case '＜': // ＜
                    sb.append('<');
                    break;
                case '≪': // ≪
                case '《': // 《
                    sb.append("<<");
                    break;
                default:
                    // 空白・記号・かな漢字などは削除する
                    break;
            }
        }
        return sb.toString();
    }
}
