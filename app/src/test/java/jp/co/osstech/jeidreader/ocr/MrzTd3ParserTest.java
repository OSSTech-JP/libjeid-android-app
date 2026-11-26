package jp.co.osstech.jeidreader.ocr;

import java.util.Arrays;
import java.util.Collections;
import jp.co.osstech.libjeid.EPMRZ;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MrzTd3ParserTest {
    /** 日本旅券のMRZ1行目(実在しない架空のテストデータ)。 */
    private static final String JP_LINE1 =
            "P<JPNYAMADA<<TARO<<<<<<<<<<<<<<<<<<<<<<<<<<<";
    /** 日本旅券のMRZ2行目。 */
    private static final String JP_LINE2 =
            "MK12345672JPN9001011M3001019<<<<<<<<<<<<<<02";
    /** ICAO 9303の例。任意データに英数字が入るケース。 */
    private static final String ICAO_LINE2 =
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10";

    @Test
    public void testParseJapanesePassport() {
        MrzScanResult result = MrzTd3Parser.parseLine2(JP_LINE2);
        assertNotNull(result);
        assertEquals("MK1234567", result.getDocumentNumber());
        assertEquals("JPN", result.getNationality());
        assertEquals("900101", result.getBirthDate());
        assertEquals("300101", result.getExpirationDate());
        assertEquals('M', result.getSex());
        assertTrue(result.isCompositeCheckDigitValid());
        assertEquals(JP_LINE2, result.getLine2());
    }

    @Test
    public void testParseIcaoExampleWithOptionalData() {
        MrzScanResult result = MrzTd3Parser.parseLine2(ICAO_LINE2);
        assertNotNull(result);
        assertEquals("L898902C3", result.getDocumentNumber());
        assertEquals("UTO", result.getNationality());
        assertEquals("740812", result.getBirthDate());
        assertEquals("120415", result.getExpirationDate());
        assertEquals('F', result.getSex());
        assertTrue(result.isCompositeCheckDigitValid());
    }

    @Test
    public void testRepairsMisreadCharactersInDates() {
        // 生年月日と有効期限の 0 を O として誤認識したケース
        String misread = "MK12345672JPN9OO1O11M3OO1O19<<<<<<<<<<<<<<02";
        MrzScanResult result = MrzTd3Parser.parseLine2(misread);
        assertNotNull(result);
        assertEquals("900101", result.getBirthDate());
        assertEquals("300101", result.getExpirationDate());
        assertEquals(JP_LINE2, result.getLine2());
    }

    @Test
    public void testRepairsMisreadCharactersInDocumentNumber() {
        // 旅券番号の 1 を I、7 を T として誤認識したケース
        String misread = "MKI23456T2JPN9001011M3001019<<<<<<<<<<<<<<02";
        MrzScanResult result = MrzTd3Parser.parseLine2(misread);
        assertNotNull(result);
        assertEquals("MK1234567", result.getDocumentNumber());
        assertEquals(JP_LINE2, result.getLine2());
    }

    @Test
    public void testRejectsCheckDigitMismatch() {
        // 生年月日のチェックデジットを 1 から 2 へ改変
        assertNull(MrzTd3Parser.parseLine2(
                "MK12345672JPN9001012M3001019<<<<<<<<<<<<<<02"));
        // 旅券番号のチェックデジットを 2 から 3 へ改変
        assertNull(MrzTd3Parser.parseLine2(
                "MK12345673JPN9001011M3001019<<<<<<<<<<<<<<02"));
        // 有効期限の1文字を数字のまま誤認識したケースは補正できず棄却される
        assertNull(MrzTd3Parser.parseLine2(
                "MK12345672JPN9001011M3001119<<<<<<<<<<<<<<02"));
    }

    @Test
    public void testRejectsInvalidDate() {
        // 13月。チェックデジットは整合しているが日付として不正
        assertNull(MrzTd3Parser.parseLine2(
                "MK12345672JPN9013016M3001019<<<<<<<<<<<<<<00"));
    }

    @Test
    public void testRejectsWrongLength() {
        assertNull(MrzTd3Parser.parseLine2(JP_LINE2.substring(0, 43)));
        assertNull(MrzTd3Parser.parseLine2(JP_LINE2 + "0"));
        assertNull(MrzTd3Parser.parseLine2(""));
    }

    @Test
    public void testNormalizesSpacesAndGuillemets() {
        // OCRが空白を挿入し、フィラーを « として返したケース
        String noisy = "MK1234567 2 JPN 9001011M 3001019 «««««««««««««« 02";
        MrzScanResult result = MrzTd3Parser.parseLine2(noisy);
        assertNotNull(result);
        assertEquals(JP_LINE2, result.getLine2());
    }

    @Test
    public void testParseFromSeparateLines() {
        MrzScanResult result = MrzTd3Parser.parse(Arrays.asList(
                "JAPAN PASSPORT", JP_LINE1, JP_LINE2));
        assertNotNull(result);
        assertEquals("MK1234567", result.getDocumentNumber());
        assertEquals("YAMADA", result.getSurname());
        assertEquals("TARO", result.getGivenName());
    }

    @Test
    public void testParseFromMergedLine() {
        // OCRが2行を1行に連結して返したケース
        MrzScanResult result =
                MrzTd3Parser.parse(Collections.singletonList(JP_LINE1 + JP_LINE2));
        assertNotNull(result);
        assertEquals("MK1234567", result.getDocumentNumber());
        assertEquals("900101", result.getBirthDate());
        assertEquals("YAMADA", result.getSurname());
    }

    @Test
    public void testParseSucceedsWithoutLine1() {
        // 2行目だけでも鍵導出に必要な情報は揃う
        MrzScanResult result = MrzTd3Parser.parse(Collections.singletonList(JP_LINE2));
        assertNotNull(result);
        assertEquals("MK1234567", result.getDocumentNumber());
        assertEquals("", result.getSurname());
        assertEquals("", result.getGivenName());
    }

    @Test
    public void testParseReturnsNullForNonMrzText() {
        assertNull(MrzTd3Parser.parse(Arrays.asList(
                "日本国 JAPAN", "PASSPORT", "旅券番号/Passport No.")));
        assertNull(MrzTd3Parser.parse(Collections.<String>emptyList()));
        assertNull(MrzTd3Parser.parse(null));
    }

    @Test
    public void testCalculateCheckDigit() {
        assertEquals(2, MrzTd3Parser.calculateCheckDigit("MK1234567"));
        assertEquals(1, MrzTd3Parser.calculateCheckDigit("900101"));
        assertEquals(9, MrzTd3Parser.calculateCheckDigit("300101"));
        assertEquals(0, MrzTd3Parser.calculateCheckDigit("<<<<<<<<<<<<<<"));
        assertEquals(6, MrzTd3Parser.calculateCheckDigit("L898902C3"));
    }

    @Test
    public void testCompositeSource() {
        // 複合チェックデジットの対象は2行目の 0-9、13-19、21-42
        assertEquals("MK1234567290010113001019<<<<<<<<<<<<<<0",
                MrzTd3Parser.compositeSource(JP_LINE2));
        assertEquals(2, MrzTd3Parser.calculateCheckDigit(
                MrzTd3Parser.compositeSource(JP_LINE2)));
    }

    @Test
    public void testCompositeCheckDigitMismatchIsNotFatal() {
        // 任意データを誤認識し複合チェックデジットが不一致でも、
        // 鍵導出に必要な3フィールドが検証できていれば採用する
        String misread = "MK12345672JPN9001011M3001019<<<<<<<<<<<<<<03";
        MrzScanResult result = MrzTd3Parser.parseLine2(misread);
        assertNotNull(result);
        assertEquals("MK1234567", result.getDocumentNumber());
        assertTrue(!result.isCompositeCheckDigitValid());
    }

    @Test
    public void testHasSameKeyFields() {
        MrzScanResult a = MrzTd3Parser.parseLine2(JP_LINE2);
        MrzScanResult b = MrzTd3Parser.parseLine2(
                "MKI23456T2JPN9OO1O11M3OO1O19<<<<<<<<<<<<<<02");
        MrzScanResult c = MrzTd3Parser.parseLine2(ICAO_LINE2);
        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        assertTrue(a.hasSameKeyFields(b));
        assertTrue(!a.hasSameKeyFields(c));
        assertTrue(!a.hasSameKeyFields(null));
    }

    /**
     * 読み取り結果をlibjeidのEPMRZへ渡し、鍵導出まで到達できることを確認する。
     */
    @Test
    public void testResultIsAcceptedByEPMRZ() {
        MrzScanResult result = MrzTd3Parser.parseLine2(JP_LINE2);
        assertNotNull(result);
        EPMRZ mrz = new EPMRZ(result.getDocumentNumber(),
                result.getBirthDate(), result.getExpirationDate());
        assertEquals("MK1234567", mrz.getPassportNumber());
        assertEquals("900101", mrz.getBirthDate());
        assertEquals("300101", mrz.getExpirationDate());
        // PACE用Kseedは SHA-1 の全長20バイト
        assertEquals(20, mrz.getPACEKseed().length);

        // MRZ全体から生成したEPMRZと鍵導出の入力が一致すること
        EPMRZ fromFullMrz = new EPMRZ(JP_LINE1 + result.getLine2());
        assertEquals(mrz.getPassportNumber(), fromFullMrz.getPassportNumber());
        assertEquals(mrz.getBirthDate(), fromFullMrz.getBirthDate());
        assertEquals(mrz.getExpirationDate(), fromFullMrz.getExpirationDate());
        assertTrue(Arrays.equals(mrz.getPACEKseed(), fromFullMrz.getPACEKseed()));
    }
}
