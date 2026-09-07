# Deviation: add-kmp-typed-show

- 共有コードの型指定 show の失敗文言 (Requirement「共有コードの型指定 show」の「VM factory 未登録」「型不一致」): spec は文言の言語を定めていない → 実装後に main で accepted になった cross/ADR-0015 (ライブラリが外へ出す診断文言は英語固定) に合わせ、オーナー指示で main 取り込み時に英語化 (テストの文言 assertion も追随)。理由: ADR-0015 違反を main に持ち込まない (2026-09-07)
- [付随修正] `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt` の PB-KT-09: Native iOS の提示先なし文言を日本語の部分一致で見ていたため、main 側 (localize-dialog-error-messages) の英語化と合流して落ちた → main と同じ `No screen is available to present the Dialog.` に追随。理由: 合流後の全件成功を保つ (2026-09-07)
