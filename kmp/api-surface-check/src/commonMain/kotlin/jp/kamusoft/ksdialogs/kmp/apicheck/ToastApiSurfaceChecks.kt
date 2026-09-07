package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast
import jp.kamusoft.ksdialogs.kmp.ToastViewModel

/** 利用者が書くのと同じ形のカスタム Toast の ViewModel。データの運搬体と型キーを兼ねる。 */
public class ConsumerToastViewModel(
    /** 中身の View が読む文言。 */
    public var message: String = "",
) : ToastViewModel

/**
 * 共有コードの Toast 公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
 * 利用者から見えなくなればビルドが失敗する。
 * 公開してはならないもの (スタイルの型と設定プロパティ・View factory の登録・
 * 閉じる操作・show の戻り値) の不在は負の検査が受け持つ。
 */
public object ToastApiSurfaceChecks {

    /** 既定エントリは契約 interface として受け取れる。 */
    public fun TS_KM_01_acceptsDefaultEntryAsContract(): KsToast = Toast.instance

    /** メッセージだけで表示できる (duration も配置も省略できる)。 */
    public fun TS_KM_01_acceptsShowWithMessageOnly(toast: KsToast) {
        toast.show("保存しました")
    }

    /** duration はミリ秒の整数で指定できる。 */
    public fun TS_KM_01_acceptsShowWithDuration(toast: KsToast) {
        toast.show("保存しました", durationMs = 3000)
    }

    /** duration と placement は表示の引数で供給できる。 */
    public fun TS_KM_01_acceptsShowWithDurationAndPlacement(toast: KsToast) {
        toast.show(
            message = "保存しました",
            durationMs = 3000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START, offsetY = 24.0),
        )
    }

    /** カスタム Toast は型キーになる共有 VM を渡して表示でき、duration・placement も同じ形で渡せる。 */
    public fun TS_KM_01_acceptsCustomToast(toast: KsToast) {
        val viewModel = ConsumerToastViewModel(message = "保存しました")
        toast.show(viewModel)
        toast.show(viewModel, durationMs = 3000)
        toast.show(viewModel, durationMs = 3000, placement = DialogPlacement(offsetX = 4.0))
    }

    /** ViewModel factory はラムダでもコンストラクタ参照でも登録できる。 */
    public fun PB_KT_10_registersViewModelFactory(toast: KsToast) {
        toast.registry.registerViewModel(ConsumerToastViewModel::class) { ConsumerToastViewModel() }
        toast.registry.registerViewModel(ConsumerToastViewModel::class, ::ConsumerToastViewModel)
    }

    /** 型を渡す show は configure も duration も placement も省略できる。 */
    public fun PB_KT_10_acceptsTypedShow(toast: KsToast) {
        toast.show(ConsumerToastViewModel::class)
    }

    /** 型を渡す show には duration・placement・configure を渡せる。 */
    public fun PB_KT_10_acceptsTypedShowWithDurationPlacementAndConfigure(toast: KsToast) {
        toast.show(
            ConsumerToastViewModel::class,
            durationMs = 3000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START, offsetY = 24.0),
        ) { viewModel ->
            viewModel.message = "保存しました"
        }
    }
}
