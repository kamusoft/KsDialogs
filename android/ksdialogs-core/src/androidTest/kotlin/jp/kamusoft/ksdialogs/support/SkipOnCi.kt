package jp.kamusoft.ksdialogs.support

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * CI 上では実行しないテストの印。
 *
 * 手元では通るのに CI の実行機でだけ環境のタイミングで揺らぎ、かつ公開契約の保証そのものを
 * 固定しているわけではないテストに付ける。[reason] には切り分けの根拠 — 手元の反復で通ること・
 * CI での落ち方・重要な保証でないと判断した理由 — を、このファイルだけで意味が通る形で書く。
 *
 * 印を付けても手元では通常どおり実行される。CI 側だけが [SkipOnCiRule] の判定材料を渡す。
 *
 * 印はオーナーの承認 (リポジトリ設定の許可リスト `lint.ci-skip.allow`) があってはじめて置ける。
 * 許可リストに対応する項目が無い印と、根拠を欠いた [reason] は `scripts/ci-skip-lint.py` が違反として落とす。
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SkipOnCi(val reason: String)

/**
 * [SkipOnCi] を付けたテストを CI 上だけ skip する規則。テストクラスに 1 行足して使う。
 *
 * skip は前提の不成立 ([AssumptionViolatedException]) として起こすので、結果 XML では
 * skipped に数えられる。実行数を「tests から skipped を引いた数」で見る件数検査は、
 * この skip があっても実行数の不足を検出できる。
 *
 * CI かどうかは、実行機が持つ汎用の環境変数ではなく **CI 側が明示的に渡す実行引数**で判定する。
 * 汎用の変数に頼ると、同じ名前を持つ手元の環境で意図せず skip される。
 */
class SkipOnCiRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val skip = description.getAnnotation(SkipOnCi::class.java)
                    ?: description.testClass?.getAnnotation(SkipOnCi::class.java)
                if (skip != null && isRunningOnCi()) {
                    throw AssumptionViolatedException("CI 上では実行しない: ${skip.reason}")
                }
                base.evaluate()
            }
        }

    private fun isRunningOnCi(): Boolean =
        InstrumentationRegistry.getArguments().getString(CI_ARGUMENT_NAME) == "true"

    private companion object {
        /** CI の実行が渡す実行引数の名前。 */
        const val CI_ARGUMENT_NAME = "ksdialogsCi"
    }
}
