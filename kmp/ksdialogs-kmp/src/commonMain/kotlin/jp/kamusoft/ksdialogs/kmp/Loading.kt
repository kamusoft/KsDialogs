package jp.kamusoft.ksdialogs.kmp

/**
 * ローディング表示の既定エントリ。
 *
 * [instance] を直接呼び出すほか、これを [KsLoading] として DI 注入しても同じ表示状態を共有する
 * (core/ADR-0002・0024)。OS ごとの実体との結び付けはこのエントリの取得だけが担い、
 * 契約そのものは共有コードで完結する (kmp/ADR-0002)。
 */
public expect object Loading {
    /** 既定の singleton エントリ。 */
    public val instance: KsLoading
}
