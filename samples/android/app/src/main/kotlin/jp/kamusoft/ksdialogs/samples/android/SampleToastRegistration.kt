package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.Toast

/**
 * カスタム Toast の ViewModel 型と、View factory・ViewModel factory の紐付け。
 *
 * Toast のレジストリは Dialog / Loading のものとは独立しているため、登録もこちらへ行う。
 */
internal object SampleToastRegistration {
    /** この Sample が使うカスタム Toast を登録する。 */
    fun register() {
        // 中身は ViewModel だけを受け取るので、(Context, VM) を受けるコンストラクタの参照をそのまま渡せる
        Toast.instance.registry.register(CustomToastViewModel::class, ::CustomToastCardView)
        // 型を渡す表示 (show(CustomToastViewModel::class)) が実体を作るための factory
        Toast.instance.registry.registerViewModel(CustomToastViewModel::class, ::CustomToastViewModel)
    }
}
