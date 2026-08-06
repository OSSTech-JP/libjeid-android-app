package jp.co.osstech.jeidreader.ocr;

/**
 * MRZ(TD3)から読み取った情報。
 *
 * <p>BAC/PACEの鍵導出に必要なのは旅券番号・生年月日・有効期限の3つで、
 * これらはすべてMRZ2行目に含まれ、チェックデジットで検証済みである。
 * 氏名は1行目が読めた場合のみ設定される任意情報。
 */
public final class MrzScanResult {
    private final String line2;
    private final String documentNumber;
    private final String nationality;
    private final String birthDate;
    private final String expirationDate;
    private final char sex;
    private final boolean compositeCheckDigitValid;
    private final String surname;
    private final String givenName;

    MrzScanResult(String line2,
                  String documentNumber,
                  String nationality,
                  String birthDate,
                  String expirationDate,
                  char sex,
                  boolean compositeCheckDigitValid,
                  String surname,
                  String givenName) {
        this.line2 = line2;
        this.documentNumber = documentNumber;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.expirationDate = expirationDate;
        this.sex = sex;
        this.compositeCheckDigitValid = compositeCheckDigitValid;
        this.surname = surname;
        this.givenName = givenName;
    }

    /**
     * 氏名を追加した新しいインスタンスを返します。
     *
     * @param surname 姓
     * @param givenName 名
     * @return 氏名を設定した新しいインスタンス
     */
    MrzScanResult withName(String surname, String givenName) {
        return new MrzScanResult(line2, documentNumber, nationality, birthDate,
                expirationDate, sex, compositeCheckDigitValid, surname, givenName);
    }

    /**
     * 検証を通過したMRZ2行目(44文字、誤認識補正後)を返します。
     * @return MRZ2行目
     */
    public String getLine2() {
        return line2;
    }

    /**
     * 旅券番号を返します。フィラー({@code <})は除去済み。
     * @return 旅券番号
     */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /**
     * 国籍コードを返します。
     * @return 国籍コード
     */
    public String getNationality() {
        return nationality;
    }

    /**
     * 生年月日(YYMMDDの6桁)を返します。
     * @return 生年月日
     */
    public String getBirthDate() {
        return birthDate;
    }

    /**
     * 有効期限(YYMMDDの6桁)を返します。
     * @return 有効期限
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * 性別({@code M}、{@code F}、{@code X}、{@code <})を返します。
     * @return 性別
     */
    public char getSex() {
        return sex;
    }

    /**
     * 複合チェックデジットが一致したかどうかを返します。
     *
     * <p>任意データの誤認識で不一致になることがあるため、この値が {@code false} でも
     * 鍵導出に必要な3フィールドの妥当性には影響しない。ログ出力用の参考情報。
     *
     * @return 一致した場合 {@code true}
     */
    public boolean isCompositeCheckDigitValid() {
        return compositeCheckDigitValid;
    }

    /**
     * 姓を返します。1行目が読めなかった場合は空文字列。
     * @return 姓
     */
    public String getSurname() {
        return surname;
    }

    /**
     * 名を返します。1行目が読めなかった場合は空文字列。
     * @return 名
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * 鍵導出に使う3フィールドが一致するかどうかを返します。
     * 複数フレームの一致判定に使用する。
     *
     * @param other 比較対象
     * @return 旅券番号・生年月日・有効期限がすべて一致する場合 {@code true}
     */
    public boolean hasSameKeyFields(MrzScanResult other) {
        return other != null
                && documentNumber.equals(other.documentNumber)
                && birthDate.equals(other.birthDate)
                && expirationDate.equals(other.expirationDate);
    }

    @Override
    public String toString() {
        return "MrzScanResult[documentNumber=" + documentNumber
                + ", birthDate=" + birthDate
                + ", expirationDate=" + expirationDate
                + ", nationality=" + nationality
                + ", sex=" + sex
                + ", compositeCheckDigitValid=" + compositeCheckDigitValid
                + ", surname=" + surname
                + ", givenName=" + givenName + "]";
    }
}
