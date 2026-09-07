package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.Foundation.numberWithInt
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropToastBridge

/**
 * iOS Native ライブラリの ObjC 互換面へ委譲する Toast の委譲面。
 *
 * View factory の紐付け・一括設定・表示中のリストと各表示の期限はすべて Native ライブラリ側が持ち、
 * この面は共有コードの呼び出しをそこへ渡すだけである (kmp/ADR-0002)。
 * 表示は fire-and-forget なので完了の通知は無く、返るのは解決の失敗だけである (core/ADR-0031)。
 *
 * ViewModel は包み直さずに渡すため、Swift 側の解決キーは共有コードで定義したクラスの ObjC クラスになる。
 *
 * この面は共有コードの内部にあり Swift / ObjC へは公開されないため、`@Throws` は書かない。
 * カスタム Toast の解決に失敗したときの [DialogException] は、公開契約 [KsToast] を実装する
 * [GatewayKsToast] を通って呼び出し元へ伝わる。
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosToastGateway(
    private val bridge: KSDInteropToastBridge,
) : ToastGateway {

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) {
        bridge.showMessage(
            message,
            duration = durationMs?.toNSNumber(),
            placement = placement?.toInterop(),
        )
    }

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        val failure = bridge.showViewModel(
            viewModel,
            duration = durationMs?.toNSNumber(),
            placement = placement?.toInterop(),
        )
        if (failure != null) {
            throw DialogException(failure.localizedDescription)
        }
    }
}

/** 省略を表現できる形で duration を互換面へ渡すための写し。 */
private fun Int.toNSNumber(): NSNumber = NSNumber.numberWithInt(this)
