package jp.kamusoft.ksdialogs.kmp

/**
 * Android の既定エントリ。Native ライブラリの既定エントリへ委譲する。
 *
 * 中身の View の作り方は Native ライブラリ側の 1 個の紐付けだけなので、共有コードからの show と
 * 純 Kotlin 利用者の View factory 登録が同じ紐付けを見る。
 * 共有コードから登録する ViewModel factory の表はこの入口が持ち、Native 側の登録とは別勘定になる。
 */
public actual object Dialog {
    public actual val instance: KsDialog =
        GatewayKsDialog(AndroidDialogGateway(jp.kamusoft.ksdialogs.Dialog.instance))
}
