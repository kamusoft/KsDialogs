package jp.kamusoft.ksdialogs

import kotlin.reflect.KClass

/** 失敗メッセージや例外に載せる ViewModel の型名。取れない型では JVM のクラス名で代用する。 */
internal val KClass<*>.viewModelTypeName: String
    get() = qualifiedName ?: java.name

/**
 * value class の ViewModel を構成ミスとして拒否する (core/ADR-0018)。
 *
 * 結果報告口はインスタンスの同一性で紐付くため、interface 越しに渡すたびに boxing で
 * 別インスタンスになる value class は ViewModel にできない。
 * Kotlin の型システムでは参照型限定を表現できないので、登録の時点と、全 show 経路が通る
 * 提示の入口で弾く。
 */
internal fun KClass<*>.requireReferenceTypeViewModel() {
    if (isValueClass()) {
        throw DialogException.ValueClassViewModel(viewModelTypeName)
    }
}

/**
 * value class かどうかを JVM のクラスの形から判定する。
 *
 * `KClass.isValue` は kotlin-reflect を必要とし、それを持たない利用者のアプリで
 * `KotlinReflectionNotSupportedError` になるため使わない。
 * 代わりに、value class のコンパイル結果に必ず生成される boxing 用の静的メソッドの有無で見る。
 */
private fun KClass<*>.isValueClass(): Boolean =
    java.declaredMethods.any { it.name == VALUE_CLASS_BOXING_METHOD_NAME }

/** value class のコンパイル結果に生成される boxing 用の静的メソッドの名前。 */
private const val VALUE_CLASS_BOXING_METHOD_NAME = "box-impl"
