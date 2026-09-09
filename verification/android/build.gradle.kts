// KsDialogs Android 消費者検証 — ルートビルドファイル
//
// 使用するプラグインはルートで宣言だけしておき、適用は各モジュールが行う。
// 版は共有バージョンカタログ (agp / kotlin) に従うため、消費者側では版を書かない。
plugins {
    // カタログの plugins には application 用の alias が無いため、id は書いて版だけカタログから取る。
    id("com.android.application") version libs.versions.agp.get() apply false
    // 宣言的 UI (Jetpack Compose) で中身を書くモジュールに要る
    alias(libs.plugins.composeCompiler) apply false
}
