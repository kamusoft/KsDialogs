package jp.kamusoft.ksdialogs

import android.view.View

/**
 * この View をダイアログとして表示するときの静的メタ属性 (core/ADR-0015)。
 *
 * View の初期化時などに設定しておくと、その View をダイアログとして表示する器が
 * 初回のネイティブレイアウトパス完了時点の値を実効値として採用する。
 * 未設定 (null) は契約の既定値を意味する。
 */
public var View.ksDialogOptions: DialogOptions?
    get() = getTag(R.id.ksdialogs_dialog_options) as? DialogOptions
    set(value) {
        setTag(R.id.ksdialogs_dialog_options, value)
    }

/**
 * この View をダイアログとして表示するときの動的メタ属性 (core/ADR-0015)。
 *
 * 採用される時点は [ksDialogOptions] と同じ。
 * show の引数で placement を渡した場合は、そちらがこの値を置換する。
 */
public var View.ksDialogPlacement: DialogPlacement?
    get() = getTag(R.id.ksdialogs_dialog_placement) as? DialogPlacement
    set(value) {
        setTag(R.id.ksdialogs_dialog_placement, value)
    }

/**
 * この View をダイアログとして表示するときの出入りの演出 (core/ADR-0017)。
 *
 * 採用される時点は [ksDialogOptions] と同じで、初回のネイティブレイアウトパス完了時点の値が
 * そのダイアログの演出として固定される。未設定 (null) なら器の既定のクロスフェードが使われる。
 */
public var View.ksDialogTransition: DialogTransition?
    get() = getTag(R.id.ksdialogs_dialog_transition) as? DialogTransition
    set(value) {
        setTag(R.id.ksdialogs_dialog_transition, value)
    }
