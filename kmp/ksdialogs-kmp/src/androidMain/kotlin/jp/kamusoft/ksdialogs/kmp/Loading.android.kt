package jp.kamusoft.ksdialogs.kmp

/**
 * Android の既定エントリ。Native ライブラリの既定エントリへ委譲する。
 *
 * 表示状態と中身の View の作り方は Native ライブラリ側にあり、共有コードからの表示と
 * 純 Kotlin 利用者の表示が同じ1つの表示へ合流する。
 * 共有コードから登録する ViewModel factory の表はこの入口が持ち、Native 側の登録とは別勘定になる。
 */
public actual object Loading {
    public actual val instance: KsLoading =
        GatewayKsLoading(AndroidLoadingGateway(jp.kamusoft.ksdialogs.Loading.instance))
}
