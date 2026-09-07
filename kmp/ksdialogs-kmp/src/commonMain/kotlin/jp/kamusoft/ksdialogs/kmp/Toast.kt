package jp.kamusoft.ksdialogs.kmp

/**
 * Toast 表示の既定エントリ。
 *
 * [instance] を直接呼び出すほか、これを [KsToast] として DI 注入しても同じレジストリと
 * 同じ重なりの管理を共有する (core/ADR-0002)。OS ごとの実体との結び付けはこのエントリの取得だけが担い、
 * 契約そのものは共有コードで完結する (kmp/ADR-0002)。
 */
public expect object Toast {
    /** 既定の singleton エントリ。 */
    public val instance: KsToast
}
