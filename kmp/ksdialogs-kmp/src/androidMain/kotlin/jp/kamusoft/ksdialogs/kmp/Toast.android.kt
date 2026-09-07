package jp.kamusoft.ksdialogs.kmp

/**
 * Android の既定エントリ。Native ライブラリの既定エントリへ委譲する。
 *
 * 中身の View の作り方の紐付けと表示中のリストは Native ライブラリ側にあり、共有コードからの表示と
 * 純 Kotlin 利用者の表示が同じ重なりの管理に載る。
 * 共有コードから登録する ViewModel factory の表はこの入口が持ち、Native 側の登録とは別勘定になる。
 */
public actual object Toast {
    public actual val instance: KsToast =
        GatewayKsToast(AndroidToastGateway(jp.kamusoft.ksdialogs.Toast.instance))
}
