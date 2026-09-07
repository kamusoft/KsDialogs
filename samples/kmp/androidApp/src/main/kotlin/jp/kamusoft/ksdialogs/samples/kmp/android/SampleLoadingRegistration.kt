package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.samples.kmp.CustomLoadingViewModel

/**
 * 共有コードのカスタム Loading の ViewModel 型と Android の View factory の紐付け。
 *
 * View の型は OS ごとに異なるため、登録は Android Native API に対して行う。
 * 共有コードで定義した ViewModel のクラスがそのまま登録キーになり、
 * 共有 Presenter からの表示と同じレジストリを引く。
 */
internal object SampleLoadingRegistration {
    /** この Sample が使うカスタム Loading を登録する。 */
    fun register() {
        // 中身は ViewModel だけを受け取るので、(Context, VM) を受けるコンストラクタの参照をそのまま渡せる
        Loading.instance.registry.register(CustomLoadingViewModel::class, ::CustomLoadingCardView)
    }
}
