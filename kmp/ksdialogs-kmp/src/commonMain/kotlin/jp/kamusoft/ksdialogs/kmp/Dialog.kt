package jp.kamusoft.ksdialogs.kmp

/**
 * ダイアログ表示の既定エントリ。
 *
 * [instance] を直接呼び出すほか、これを [KsDialog] として DI 注入しても同じレジストリを共有する
 * (core/ADR-0002・0004)。OS ごとの実体との結び付けはこのエントリの取得だけが担い、
 * 契約そのものは共有コードで完結する (kmp/ADR-0002)。
 */
public expect object Dialog {
    /** 既定の singleton エントリ。 */
    public val instance: KsDialog
}
