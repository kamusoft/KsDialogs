package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropLoadingBridge

/**
 * iOS の既定エントリ。Native ライブラリの ObjC 互換面の共有インスタンスへ委譲する。
 *
 * 表示状態と中身の View の作り方は Native ライブラリ側にあり、共有コードからの表示と
 * 純 Swift 利用者の表示が同じ1つの表示へ合流する。
 * 共有コードから登録する ViewModel factory の表はこの入口が持ち、Native 側の登録とは別勘定になる。
 */
public actual object Loading {
    @OptIn(ExperimentalForeignApi::class)
    public actual val instance: KsLoading =
        GatewayKsLoading(IosLoadingGateway(KSDInteropLoadingBridge.sharedBridge()))
}
