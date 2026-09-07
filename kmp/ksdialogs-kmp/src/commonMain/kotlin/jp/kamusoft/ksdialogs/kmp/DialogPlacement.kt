package jp.kamusoft.ksdialogs.kmp

/**
 * ダイアログの器に渡す動的メタ属性 (置き場所)。
 *
 * 共有コードから供給できるメタ属性はこの置き場所だけで、[KsDialog.show] の引数で渡す (core/ADR-0015)。
 * 渡した値は中身の View に添付された置き場所を**オブジェクトまるごと置換**し、フィールド単位では合成しない。
 * 静的メタ属性 (基準領域・余白・比率・覆いの色・外側タップの扱い) は各 OS の View 定義側で完結するため、
 * 共有コードからは供給しない。
 *
 * 全フィールドに既定値があるため、何も設定しなければ契約の既定値 (中央配置・移動なし) で動く。
 * 数値はすべて論理単位 (iOS は pt / Android は dp) で、非有限値は各 OS の Native ライブラリが 0 に丸める。
 *
 * @property horizontalAlignment 水平方向の配置。既定は中央
 * @property verticalAlignment 垂直方向の配置。既定は中央
 * @property offsetX 配置を決めた後に加える水平方向の移動量。正の値で右へ動く。既定は 0
 * @property offsetY 配置を決めた後に加える垂直方向の移動量。正の値で下へ動く。既定は 0
 */
public data class DialogPlacement(
    public val horizontalAlignment: DialogAlignment = DialogAlignment.CENTER,
    public val verticalAlignment: DialogAlignment = DialogAlignment.CENTER,
    public val offsetX: Double = 0.0,
    public val offsetY: Double = 0.0,
)
