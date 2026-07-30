package jp.co.osstech.jeidreader.ocr;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ICAO 9303 Part 4 の TD3(旅券)MRZ を、汎用OCRの誤認識を補正しながらパースします。
 *
 * <p>設計上のポイント:
 * <ul>
 *   <li><b>2行目だけで成立させる</b>。BAC/PACEの鍵導出に必要な旅券番号・生年月日・
 *       有効期限はすべて2行目にあるため、1行目が読めなくても成功とする。</li>
 *   <li><b>字種別の補正を積極的に行う</b>。数値フィールドの {@code O→0} のような
 *       誤認識を補正した上でチェックデジットを検証し、通ったものだけを採用する。
 *       1文字の誤りは必ずチェックデジット不一致になるため、補正を強くかけても
 *       誤った値を採用してしまうことはない。</li>
 * </ul>
 *
 * <p>Androidに依存しない純粋なロジックのみで構成し、単体テストの対象とする。
 */
public final class MrzTd3Parser {
    /** TD3の1行の文字数。 */
    public static final int LINE_LENGTH = 44;

    // MRZ2行目のフィールド位置(0起点)
    private static final int DOC_NUMBER_BEGIN = 0;
    private static final int DOC_NUMBER_END = 9;
    private static final int DOC_NUMBER_CD = 9;
    private static final int NATIONALITY_BEGIN = 10;
    private static final int NATIONALITY_END = 13;
    private static final int BIRTH_DATE_BEGIN = 13;
    private static final int BIRTH_DATE_END = 19;
    private static final int BIRTH_DATE_CD = 19;
    private static final int SEX = 20;
    private static final int EXPIRATION_BEGIN = 21;
    private static final int EXPIRATION_END = 27;
    private static final int EXPIRATION_CD = 27;
    private static final int OPTIONAL_BEGIN = 28;
    private static final int OPTIONAL_END = 42;
    private static final int OPTIONAL_CD = 42;
    private static final int COMPOSITE_CD = 43;

    // 1行目のフィールド位置(0起点)
    private static final int NAME_BEGIN = 5;

    /** チェックデジットの重み。 */
    private static final int[] WEIGHTS = {7, 3, 1};

    /** 誤認識行が長大な場合の探索打ち切り長。 */
    private static final int MAX_SEARCH_LENGTH = 256;

    private MrzTd3Parser() {
    }

    /**
     * OCRが返した行群からMRZを探し、チェックデジット検証を通ったものを返します。
     *
     * @param rawLines OCRが返した行(正規化前でよい)
     * @return 検証を通った結果。見つからない場合は {@code null}
     */
    public static MrzScanResult parse(List<String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>(rawLines.size());
        for (String raw : rawLines) {
            String line = MrzNormalizer.normalize(raw);
            if (line.length() >= LINE_LENGTH && line.length() <= MAX_SEARCH_LENGTH) {
                normalized.add(line);
            }
        }
        // 2行目を探す。OCRが2行を1行に連結して返すことや、余分な文字を
        // 挿入することがあるため、長さ44の窓をずらしながら総当たりする。
        MrzScanResult result = null;
        for (String line : normalized) {
            for (int offset = 0; offset + LINE_LENGTH <= line.length(); offset++) {
                result = parseLine2(line.substring(offset, offset + LINE_LENGTH));
                if (result != null) {
                    break;
                }
            }
            if (result != null) {
                break;
            }
        }
        if (result == null) {
            return null;
        }
        String[] names = findNames(normalized);
        if (names != null) {
            result = result.withName(names[0], names[1]);
        }
        return result;
    }

    /**
     * MRZ2行目1本をパースします。
     *
     * @param rawLine MRZ2行目(正規化前でよい)
     * @return 検証を通った結果。検証に失敗した場合は {@code null}
     */
    public static MrzScanResult parseLine2(String rawLine) {
        String line = MrzNormalizer.normalize(rawLine);
        if (line.length() != LINE_LENGTH) {
            return null;
        }
        char[] chars = line.toCharArray();
        // 字種が確定しているフィールドを補正する
        coerceToDigit(chars, BIRTH_DATE_BEGIN, BIRTH_DATE_END);
        coerceToDigit(chars, BIRTH_DATE_CD, BIRTH_DATE_CD + 1);
        coerceToDigit(chars, EXPIRATION_BEGIN, EXPIRATION_END);
        coerceToDigit(chars, EXPIRATION_CD, EXPIRATION_CD + 1);
        coerceToDigit(chars, DOC_NUMBER_CD, DOC_NUMBER_CD + 1);
        coerceToDigit(chars, OPTIONAL_CD, OPTIONAL_CD + 1);
        coerceToDigit(chars, COMPOSITE_CD, COMPOSITE_CD + 1);
        coerceToAlpha(chars, NATIONALITY_BEGIN, NATIONALITY_END);
        coerceSex(chars, SEX);

        String birthDate = new String(chars, BIRTH_DATE_BEGIN, BIRTH_DATE_END - BIRTH_DATE_BEGIN);
        if (!isValidDate(birthDate)
                || calculateCheckDigit(birthDate) != toDigit(chars[BIRTH_DATE_CD])) {
            return null;
        }
        String expirationDate =
                new String(chars, EXPIRATION_BEGIN, EXPIRATION_END - EXPIRATION_BEGIN);
        if (!isValidDate(expirationDate)
                || calculateCheckDigit(expirationDate) != toDigit(chars[EXPIRATION_CD])) {
            return null;
        }

        // 旅券番号は英字と数字の双方を取り得るため字種を確定できない。
        // 候補を順に試し、チェックデジットが一致したものを採用する。
        String rawDocNumber = new String(chars, DOC_NUMBER_BEGIN, DOC_NUMBER_END - DOC_NUMBER_BEGIN);
        String docNumber = null;
        for (String candidate : documentNumberCandidates(rawDocNumber)) {
            if (calculateCheckDigit(candidate) == toDigit(chars[DOC_NUMBER_CD])) {
                docNumber = candidate;
                break;
            }
        }
        if (docNumber == null) {
            return null;
        }
        System.arraycopy(docNumber.toCharArray(), 0, chars, DOC_NUMBER_BEGIN, docNumber.length());

        String line2 = new String(chars);
        boolean compositeValid = calculateCheckDigit(compositeSource(line2))
                == toDigit(chars[COMPOSITE_CD]);
        return new MrzScanResult(line2,
                unpad(docNumber),
                new String(chars, NATIONALITY_BEGIN, NATIONALITY_END - NATIONALITY_BEGIN),
                birthDate,
                expirationDate,
                chars[SEX],
                compositeValid,
                "",
                "");
    }

    /**
     * MRZのチェックデジットを計算します。
     *
     * @param value 対象文字列(正規化済みであること)
     * @return チェックデジット(0-9)
     */
    static int calculateCheckDigit(String value) {
        int sum = 0;
        for (int i = 0; i < value.length(); i++) {
            sum += weightOf(value.charAt(i)) * WEIGHTS[i % 3];
        }
        return sum % 10;
    }

    /**
     * 複合チェックデジットの計算対象を返します。
     * 対象は1行目からではなく2行目の 0-9、13-19、21-42 の各文字。
     *
     * @param line2 MRZ2行目(44文字)
     * @return 複合チェックデジットの計算対象文字列
     */
    static String compositeSource(String line2) {
        return line2.substring(DOC_NUMBER_BEGIN, DOC_NUMBER_CD + 1)
                + line2.substring(BIRTH_DATE_BEGIN, BIRTH_DATE_CD + 1)
                + line2.substring(EXPIRATION_BEGIN, OPTIONAL_CD + 1);
    }

    private static int weightOf(char c) {
        if (c == '<') {
            return 0;
        }
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("not a MRZ character: " + c);
    }

    private static int toDigit(char c) {
        return (c >= '0' && c <= '9') ? c - '0' : -1;
    }

    /**
     * 旅券番号フィールドの補正候補を、可能性の高い順に返します。
     */
    private static Set<String> documentNumberCandidates(String field) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(field);
        // 日本の旅券番号は英字2桁+数字7桁
        char[] jp = field.toCharArray();
        coerceToAlpha(jp, 0, Math.min(2, jp.length));
        coerceToDigit(jp, Math.min(2, jp.length), jp.length);
        candidates.add(new String(jp));
        char[] digits = field.toCharArray();
        coerceToDigit(digits, 0, digits.length);
        candidates.add(new String(digits));
        char[] alphas = field.toCharArray();
        coerceToAlpha(alphas, 0, alphas.length);
        candidates.add(new String(alphas));
        return candidates;
    }

    /**
     * 数字であるべき範囲を数字へ寄せます。フィラーはそのまま残します。
     */
    private static void coerceToDigit(char[] chars, int begin, int end) {
        for (int i = begin; i < end; i++) {
            switch (chars[i]) {
                case 'O':
                case 'Q':
                case 'D':
                    chars[i] = '0';
                    break;
                case 'I':
                case 'L':
                    chars[i] = '1';
                    break;
                case 'Z':
                    chars[i] = '2';
                    break;
                case 'A':
                    chars[i] = '4';
                    break;
                case 'S':
                    chars[i] = '5';
                    break;
                case 'G':
                    chars[i] = '6';
                    break;
                case 'T':
                    chars[i] = '7';
                    break;
                case 'B':
                    chars[i] = '8';
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 英字であるべき範囲を英字へ寄せます。フィラーはそのまま残します。
     */
    private static void coerceToAlpha(char[] chars, int begin, int end) {
        for (int i = begin; i < end; i++) {
            switch (chars[i]) {
                case '0':
                    chars[i] = 'O';
                    break;
                case '1':
                    chars[i] = 'I';
                    break;
                case '2':
                    chars[i] = 'Z';
                    break;
                case '4':
                    chars[i] = 'A';
                    break;
                case '5':
                    chars[i] = 'S';
                    break;
                case '6':
                    chars[i] = 'G';
                    break;
                case '7':
                    chars[i] = 'T';
                    break;
                case '8':
                    chars[i] = 'B';
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 性別を {@code M}、{@code F}、{@code X}、{@code <} のいずれかへ寄せます。
     * このフィールドは複合チェックデジットの対象外のため、判別できない場合は
     * フィラーとして扱っても他の検証に影響しない。
     */
    private static void coerceSex(char[] chars, int index) {
        switch (chars[index]) {
            case 'M':
            case 'F':
            case 'X':
                break;
            case 'H':
            case 'N':
                chars[index] = 'M';
                break;
            case 'E':
            case 'P':
                chars[index] = 'F';
                break;
            default:
                chars[index] = '<';
                break;
        }
    }

    private static boolean isValidDate(String yymmdd) {
        for (int i = 0; i < yymmdd.length(); i++) {
            if (toDigit(yymmdd.charAt(i)) < 0) {
                return false;
            }
        }
        int month = Integer.parseInt(yymmdd.substring(2, 4));
        int day = Integer.parseInt(yymmdd.substring(4, 6));
        return month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    private static String unpad(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '<') {
            end--;
        }
        return value.substring(0, end);
    }

    /**
     * 1行目を探し、姓と名を返します。
     *
     * @return {@code {姓, 名}}。1行目が見つからない場合は {@code null}
     */
    private static String[] findNames(List<String> normalizedLines) {
        for (String line : normalizedLines) {
            for (int offset = 0; offset + LINE_LENGTH <= line.length(); offset++) {
                String candidate = line.substring(offset, offset + LINE_LENGTH);
                if (candidate.charAt(0) != 'P' || !isAlphaOrFiller(candidate)) {
                    continue;
                }
                return extractNames(candidate.substring(NAME_BEGIN));
            }
        }
        return null;
    }

    private static boolean isAlphaOrFiller(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '<' && (c < 'A' || c > 'Z')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 氏名フィールドを姓と名へ分解します。区切りが見つからない場合は空文字列を返します。
     */
    private static String[] extractNames(String nameField) {
        String name = unpad(nameField);
        int separator = name.indexOf("<<");
        if (separator <= 0) {
            return new String[] {"", ""};
        }
        String surname = name.substring(0, separator);
        String givenName = name.substring(separator + 2);
        if (surname.contains("<") || givenName.contains("<<")) {
            return new String[] {"", ""};
        }
        return new String[] {surname, givenName.replace('<', ' ').trim()};
    }
}
