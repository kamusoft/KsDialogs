package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.Loading

/**
 * カスタム Loading の ViewModel 型と、View factory・ViewModel factory の紐付け。
 *
 * Loading のレジストリは Dialog のものとは独立しているため、登録もこちらへ行う。
 */
internal object SampleLoadingRegistration {
    /** この Sample が使うカスタム Loading を登録する。 */
    fun register() {
        // 中身は ViewModel だけを受け取るので、(Context, VM) を受けるコンストラクタの参照をそのまま渡せる
        Loading.instance.registry.register(CustomLoadingViewModel::class, ::CustomLoadingCardView)
        // 型を渡す表示 (start(CustomLoadingViewModel::class)) が実体を作るための factory
        Loading.instance.registry.registerViewModel(CustomLoadingViewModel::class, ::CustomLoadingViewModel)
    }
}
