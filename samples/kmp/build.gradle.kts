// composite build に取り込む本体ビルドと同じ classloader でプラグインを解決させるため、
// 使用するプラグインはルートで宣言だけしておく (適用は各モジュール側)。
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
}
