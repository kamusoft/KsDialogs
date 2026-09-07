package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogBridge

/**
 * iOS の既定エントリ。Native ライブラリの ObjC 互換面の共有インスタンスへ委譲する。
 *
 * 中身の View の作り方は Native ライブラリ側の 1 個の紐付けだけなので、共有コードからの show と
 * 純 Swift 利用者の View factory 登録が同じ紐付けを見る。
 * 共有コードから登録する ViewModel factory の表はこの入口が持ち、Native 側の登録とは別勘定になる。
 */
public actual object Dialog {
    @OptIn(ExperimentalForeignApi::class)
    public actual val instance: KsDialog =
        GatewayKsDialog(IosDialogGateway(KSDInteropDialogBridge.sharedBridge()))
}
