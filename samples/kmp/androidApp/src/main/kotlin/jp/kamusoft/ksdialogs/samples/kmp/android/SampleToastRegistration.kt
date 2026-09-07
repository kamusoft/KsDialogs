package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.samples.kmp.CustomToastViewModel

/**
 * 共有コードのカスタム Toast の ViewModel 型と Android の View factory の紐付け。
 *
 * Android では共有コードの ViewModel 契約が Native ライブラリのものと同一の型になるため、
 * 登録は Native の登録面へそのまま行える (kmp/ADR-0002)。
 * Toast のレジストリは Dialog / Loading のものとは独立している。
 */
internal object SampleToastRegistration {
    /** この Sample が使うカスタム Toast を登録する。 */
    fun register() {
        // 中身は ViewModel だけを受け取るので、(Context, VM) を受けるコンストラクタの参照をそのまま渡せる
        Toast.instance.registry.register(CustomToastViewModel::class, ::CustomToastCardView)
    }
}
