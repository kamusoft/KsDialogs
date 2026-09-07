@testable import KsDialogsMauiBridge
import Foundation
import Testing

extension MauiBridgeSuite {
    /// 互換面が外へ出す診断文言が英語で固定されていることを確かめる (cross/ADR-0015)。
    ///
    /// 読み手はライブラリを組み込む開発者であり、失敗の説明は英語で書く。
    /// 文言そのものは互換契約ではない (契約は失敗の case と起きる条件) が、
    /// ここでは置き換えが意図どおりであることを完全一致で固定する。
    @Suite("互換面の診断文言")
    struct DiagnosticMessageTests {
        @Test("[DM-MA-03] 互換面の失敗が英語の説明文を返す")
        func DM_MA_03_bridgeErrorDescriptionsAreEnglish() {
            #expect(
                MauiDialogBridgeError.unsupportedResult.errorDescription
                    == "Could not determine the Dialog result."
            )
            #expect(
                MauiDialogBridgeError.contentUnavailable.errorDescription
                    == "The MAUI side could not create the presentation content."
            )
        }
    }
}
