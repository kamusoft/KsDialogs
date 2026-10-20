# Deviation: guard-kmp-swift-int-result-type

- 蒸留時に反映: concepts `kasane/concepts/kmp/api/ios-host-integration.md` (「型付き入口の署名 (Dialog)」の「どちらでも観察できる挙動は同じ」) — 安全領域では成り立たないことを書き添える (中央の軸と窓全体以外の基準領域では、UIView の中身には器と安全領域の重なりが見え、SwiftUI の中身は 0)。phase-1 (ios-safe-area-and-kmp-int-result/phase-1-ios-swiftui-content-safe-area) の蒸留で分かったことで、同じ文書を書き直すこの change の蒸留で直す (自律判断 (根拠: conductor.md「ブランチとコミット」6. — メインからの申し送り) により、2026-10-20)
- proposal.md の Impact 1 項目め: 提案のコミット (f98ed41) では「それ以外の結果型の利用者には影響しない」→ 自律判断 (根拠: kasane/lessons/process.md L-002 — 互換の主張は実証した範囲に限る) により、実装を委ねる前に「影響は無い見込みで、実証は ios/ の既存テストのビルドと全件実行・IR-KM-07 に限る。実証しない面: 関数参照としての取り出し・ABI」に書き換えた。理由: 主張を実証範囲に絞るためで、Requirement・Scenario・Non-Goals は変えていない (2026-10-20)
