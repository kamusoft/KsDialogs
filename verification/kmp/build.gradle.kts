// KsDialogs KMP 消費者検証 — ルートビルドファイル
//
// 使用するプラグインはルートで宣言だけしておき、適用は各モジュールが行う。
// 版は共有バージョンカタログ (agp / kotlin) に従うため、モジュール側では版を書かない。
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
}
