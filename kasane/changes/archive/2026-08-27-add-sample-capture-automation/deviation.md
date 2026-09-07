# Deviation: add-sample-capture-automation

- Requirement「撮影支援設定の起動引数受け口」異常系 (design Decision 1「同じキーが複数回現れたら最初の1組を採用」): spec/design では両 OS 共通 → 実際は **iOS のみ成立、Android は後勝ち**。理由: Android の Intent extra は `--es demo a --es demo b` が adb / Bundle 段階で 1 値に畳まれ、アプリから重複を観測できない (プラットフォーム制約)。オーナー確定 (2026-08-27)
- Scenario CA-SA-07 (画面再生成で再発火しない) の検証方法: spec は6アプリの挙動契約 → 実地の再生成検証は android / kmp-android (回転) / maui-android (フォント倍率) の3アプリ。iOS 3アプリは CLI から画面再生成を誘発する手段が無いため、オーナー手動の回転 (Cmd+←/→) による実地証跡 (回転しても再発火しない) + 構造的保証 (自動再生入口の先頭でプロセス単位の消費フラグを検査・消費) の併記で成立とした (evidence/four-route-walkthrough/notes.md)。検証方式はオーナー確定 (2026-08-27)
- ADR cross/0010 (方式確定・status: proposed): ADR は「撮影スクリプト (単一入口 + 宣言的操作列)」と「config.yaml `ui.screenshot` のポインタ化」を宣言 → 2026-08-27 改訂により撮影スクリプトは取り下げ、`ui.screenshot` は逆にデモ駆動モード前提の手順として書き直し。理由: iOS タップの CLI 手段不在とオーナー判断 (proposal 改訂記録参照)。**蒸留時に ADR のスクリプト部分の改訂が必要** (2026-08-27)
- [付随修正] samples/maui `SampleCaptureOptions.cs`: 数値解析が `NumberStyles.None` で先頭符号付き (`+2000`) のみ他3ルートと挙動が分かれていたため `AllowLeadingSign` に変更し4ルートの受理文字列を統一。理由: CA-SA-06 (同じ値 → 同じ挙動) の厳密一致 (2026-08-27)
