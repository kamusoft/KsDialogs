package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropToastBridge

/**
 * iOS の既定エントリ。Native ライブラリの ObjC 互換面の共有インスタンスへ委譲する。
 *
 * 中身の View の作り方の紐付けと表示中のリストは Native ライブラリ側にあり、共有コードからの表示と
 * 純 Swift 利用者の表示が同じ重なりの管理に載る。
 * 共有コードから登録する ViewModel factory の表はこの入口が持ち、Native 側の登録とは別勘定になる。
 */
public actual object Toast {
    @OptIn(ExperimentalForeignApi::class)
    public actual val instance: KsToast =
        GatewayKsToast(IosToastGateway(KSDInteropToastBridge.sharedBridge()))
}
