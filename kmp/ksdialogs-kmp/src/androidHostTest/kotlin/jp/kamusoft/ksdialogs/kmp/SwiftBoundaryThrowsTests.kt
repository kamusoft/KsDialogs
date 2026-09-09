package jp.kamusoft.ksdialogs.kmp

import java.lang.reflect.Method
import java.util.concurrent.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Swift 境界へ失敗を届ける宣言 (`@Throws`) が、失敗しうる経路だけに付いていることの確認。
 *
 * 共有コードの `@Throws` は 1 つの宣言で 2 つの意味を持つ — JVM では throws 節になり、
 * Kotlin/Native では Swift の `throws` になる。宣言が落ちても Kotlin 側のビルドとテストは
 * 緑のままで、利用者の Swift コードが失敗時に abort して初めて分かる。
 * ここでは JVM 側の throws 節を反射で読み、宣言に載る例外型の集合を境界の契約として固定する。
 *
 * Swift / ObjC から見える面のヘッダ検査 (ObjCApiSurfaceTests) はこの線引きを区別できない —
 * suspend 関数の completionHandler は宣言の有無によらず NSError を持つため、
 * 生成ヘッダ上では宣言のあるものと無いものが同じ形になる。
 *
 * 検査対象は Swift から呼ばれて失敗しうる入口が集まる 3 つの公開 interface の宣言メソッドに限る。
 */
class SwiftBoundaryThrowsTests {

    /**
     * 失敗を Swift 境界へ届ける宣言を持つ経路。
     *
     * 同名の overload を取り違えないよう、interface・メソッド名・第 1 引数の型で特定する
     * (型を渡す表示は共有 Kotlin コード専用で Swift から見えないため対象外)。
     */
    private data class ThrowingRoute(
        val owner: Class<*>,
        val methodName: String,
        val firstParameterType: Class<*>,
        val expectedExceptionTypes: Set<Class<*>>,
    )

    /**
     * 中断可能な経路が宣言する 2 つの型。
     *
     * `DialogException` が落ちれば失敗が Swift の `throws` として届かなくなり、
     * `CancellationException` が落ちれば取り消しが Swift 側で「想定外の例外」となって abort する。
     * どちらも Kotlin 側のビルドとテストは緑のままなので、集合として固定する。
     */
    private val suspendRouteExceptions = setOf(
        DialogException::class.java,
        CancellationException::class.java,
    )

    private val throwingRoutes = listOf(
        ThrowingRoute(KsDialog::class.java, "show", DialogViewModel::class.java, suspendRouteExceptions),
        ThrowingRoute(KsLoading::class.java, "show", LoadingViewModel::class.java, suspendRouteExceptions),
        ThrowingRoute(KsLoading::class.java, "start", LoadingViewModel::class.java, suspendRouteExceptions),
        // 中断しない経路は取り消されないため、宣言するのは失敗の型だけ
        ThrowingRoute(
            KsToast::class.java,
            "show",
            ToastViewModel::class.java,
            setOf(DialogException::class.java),
        ),
    )

    private val inspectedInterfaces = listOf(
        KsDialog::class.java,
        KsLoading::class.java,
        KsToast::class.java,
    )

    /**
     * 検査対象の宣言メソッド。
     *
     * コンパイラが足す橋渡し (synthetic / bridge) と、既定引数の受け口である `DefaultImpls` の
     * static メソッドは利用者が呼ぶ面ではないため除く (`DefaultImpls` は別クラスなので
     * interface の宣言メソッドには現れないが、`jvm-default` の設定次第で現れうるため名前でも落とす)。
     */
    private fun declaredSurfaceMethods(type: Class<*>): List<Method> =
        type.declaredMethods
            .filterNot { it.isSynthetic || it.isBridge }
            .filterNot { it.name.contains("\$default") }
            .sortedBy { it.toString() }

    private fun Method.describe(): String =
        "${declaringClass.simpleName}.$name(${parameterTypes.joinToString { it.simpleName }})"

    private fun Method.matches(route: ThrowingRoute): Boolean =
        declaringClass == route.owner &&
            name == route.methodName &&
            parameterTypes.firstOrNull() == route.firstParameterType

    @Test
    fun `失敗しうる 4 経路は Swift 境界へ届ける宣言を過不足なく持つ`() {
        val allMethods = inspectedInterfaces.flatMap { declaredSurfaceMethods(it) }

        throwingRoutes.forEach { route ->
            val matched = allMethods.filter { it.matches(route) }

            // 名前や引数の綴りが変わった経路を「宣言なし」ではなく「見つからない」として落とす。
            // 見つからないまま素通りすると、検査の対象が静かに空になる
            assertEquals(
                1,
                matched.size,
                "${route.owner.simpleName}.${route.methodName}(" +
                    "${route.firstParameterType.simpleName}, ...) が 1 つ見つかりません " +
                    "(見つかった数: ${matched.size})。",
            )

            val method = matched.single()

            // 過不足の両方を落とす — 型が欠ければ Swift 側でその失敗が throws として届かず
            // 実行時に abort し、余分な型が増えれば Swift の呼び出し側の契約が変わる
            assertEquals(
                route.expectedExceptionTypes.map { it.simpleName }.sorted(),
                method.exceptionTypes.map { it.simpleName }.sorted(),
                "${method.describe()} が Swift 境界へ届ける宣言の型が期待と違います " +
                    "(期待: ${route.expectedExceptionTypes.map { it.simpleName }.sorted()}、" +
                    "実際: ${method.exceptionTypes.map { it.simpleName }.sorted()})。",
            )
        }
    }

    @Test
    fun `失敗しない経路は Swift 境界へ届ける宣言を持たない`() {
        inspectedInterfaces.forEach { type ->
            declaredSurfaceMethods(type)
                .filterNot { method -> throwingRoutes.any { method.matches(it) } }
                .forEach { method ->
                    assertEquals(
                        emptyList(),
                        method.exceptionTypes.map { it.simpleName },
                        "${method.describe()} に失敗を届ける宣言が付いています。" +
                            "Swift 側の呼び出しが不要な try を要求する形に変わります。",
                    )
                }
        }
    }
}
