# レビュー結果: guard-kmp-swift-int-result-type (001 回目)

**日付**: 2026-10-20
**判定**: CHANGES_REQUESTED

## サマリー

実装 (12 個の deprecated のオーバーロード、名前の違う private の委ね先 `store` / `boundNotifier` / `presentTyped`、`kotlinIntegerMismatch` による型の同一性での判定) はデルタスペックの 2 つの Requirement と 7 つの Scenario を満たしている。テストは各 Scenario の GIVEN/WHEN/THEN を表しており、全件実行も通った。コードに直すべき点は無い。指摘は 1 件だけで、実装期間中に足場の `proposal.md` (Impact) が書き換えられ、deviation.md に記録が無いこと。足場の凍結の規律 (ksn-core `references/change-scope.md`) に当たるので、記録するか元に戻すまで CHANGES_REQUESTED とする。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加した doc コメント・行コメントすべて
- `kasane/handbook/cross/test-execution.md` — テストの実行・結果の報告・完了判定 (全件実行の表を行ごとに突き合わせた、lessons L-002)
- `kasane/handbook/cross/diagnostic-message-language.md` — `ios/Sources/**` の `@available` の message と `resultTypeMismatch` の `expected` / `actual` の文言 (「検査」の静的 grep を再実行)
- 決定: kmp/ADR-0004 (型付きジェネリックな糖衣・型不一致は型付きエラー・公開エラー型の case 集合を広げない)
- lessons: `kasane/lessons/code-review.md` (L-001: 証跡が命題の真偽で分かれるか / L-002: 全件実行の表との行ごとの突き合わせ)

ロードしたスキル: swift-ui-impl-skill / kotlin-impl-skill (パッケージで指定。kotlin-impl-skill は kmp ドメインの code-review 用。kmp/ のコードはこの diff に無いので、観点は当てたが該当なし)

## 確認した観点

**仕様充足**
- [x] Requirement「Swift の Int / UInt を結果型に指定したときの案内」: 6 入口 × `Int` / `UInt` = 12 個の `@available(*, deprecated, message:)` オーバーロード (宣言は `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の 78・95・133・150・188・206・242・260・322・338・418・436 行)。署名は対応するジェネリックな入口と同じ (`@MainActor`・`async throws`・`@ViewBuilder`・`placement` の既定値)。中身は private の委ね先へ渡していて、自分自身を呼び直す形になっていない
- [x] Requirement「Int / UInt の結果型での show と報告口の取り出しは型付きの失敗になる」: `presentTyped` は指定した型を先に判定し、`??` で登録された型の判定へ進む (`KsDialogsKmp.swift:453-459`。判定の本体は `KsDialogsKmp.swift:503-511`)。登録の参照は show を呼んだ時点のレジストリで行う (spec のとおり、そのあとの再登録は既存の照合に任せる)。`boundNotifier` は表示中かどうかの判定より先に判定する (`KsDialogsKmp.swift:351-353`)。ジェネリックな入口も同じ委ね先を通るので、ラッパー越し (IR-KM-05) にも同じ判定が掛かる
- [x] 結果型を省略した `show(_:placement:)` は `show(viewModel, result: Bool.self, ...)` → ジェネリックな入口 → `presentTyped` を通り、登録された型の判定が掛かる (`KsDialogsKmp.swift:383-388`)
- [x] `register` はこの判定で失敗しない (`store` は `registry.register` だけ)
- [x] 共有コードからの show (互換面 `KsDialogsInteropBridge`) は変えていない (proposal の Non-Goals)
- [x] tasks.md の全 15 タスクのチェックは実装・テスト・証跡と一致した (虚偽チェックなし)
- [ ] 足場アーティファクトが実装中に書き換えられていないか → **書き換えあり** (指摘 1)
- [x] deviation.md: 記録は蒸留送りの 1 行だけ (phase-1 由来の申し送り)。付随修正の行は無い。diff には、Scenario に対応しない変更として tasks 2.1 の `[MB-KM-04]` の `Int.self` → `Int64.self` の置き換えがあるが、これは tasks に書かれた作業で乖離ではない

**テスト**
- [x] 全件実行 (下の表)。IR-KM-01〜07 と書き換えた `[MB-KM-04]` 2 本が結果に出ている
- [x] Scenario とテストの対応: IR-KM-01〜07 がそれぞれ 1 本のテストに対応している (`ios/Tests/KsDialogsTests/KsDialogsKmpKotlinIntegerResultTests.swift`)。`scripts/scenario-id-coverage.py` は `IR-KM 7/7`・未網羅なし
- [x] 証跡が命題の真偽で分かれるか (L-001):
  - IR-KM-01・02 の登録の確認は、直前に `String` で登録し直してから `declaredResultType == Int` / `UInt` を見る (`KsDialogsKmpKotlinIntegerResultTests.swift:26-31,44`)。登録が何もしないと `String` のままになって落ちる。`show(result: Int.self)` を登録の確認に使っていない (spec-001 の指摘 3 への対応)
  - 結果型を省略した show の失敗は、登録の結果型からしか生まれない (`Bool` の指定は判定に当たらない)。登録された型の判定が無いと show は表示へ進むので、`#expect(throws:)` は通らない
  - IR-KM-05 は、判定が deprecated のオーバーロードの中にしか無い実装だと throw せずに表示へ進むので、分かれる
  - deprecation 警告の証跡 (`evidence/deprecation-warnings.md`) は、オーバーロードが無ければ警告が出ない。さらに IR-KM-05 のラッパー内と IR-KM-07 の呼び出しに警告が出ていないことも確かめており、解決先の取り違えも分かれる
- [x] 手抜き: 言い訳コメントでのスキップは無い。境界の組 (`Int` 登録 × `UInt` 指定、`UInt` 登録 × `Int` 指定) と、表示中・未表示の両方の VM (IR-KM-06) を押さえている。View factory が呼ばれないことは呼び出し回数と提示されたコンテナの数の両方で見ている

**設計品質**
- [x] kmp/ADR-0004: 公開エラー型 `KsDialogsKmpError` に case を足していない (既存の `resultTypeMismatch` を使う)。型付きジェネリックな面のまま
- [x] オーバーエンジニアリング: 判定は private な static 関数 1 つ (`KsDialogsKmp.swift:503-511`)、委ね先は 3 つの private 関数で、再帰を避けるのに要る最小の構成
- [x] コメント規約: 新しい公開 doc コメントに ADR ID・change 名・作業文書の参照は無く、利用者向けの機能と契約だけを書いている。private 側のコメント (`store` の「委ね先の名前を分ける理由」) は単独で読める。`scripts/comment-policy-lint.py` は対象 4 ファイルで禁止 0 件
- [x] 診断文言: `@available` の message 12 個と `expected` / `actual` の値はすべて英語で、末尾にピリオドがある。handbook の静的 grep は出力なし
- [x] 判定は名前ではなく `DialogDeclaredResultType` (`ObjectIdentifier` を持つ) の同一性で行う。型の名前が偶然 `Int` になる別の型を誤判定しない
- [x] 並行性: `presentTyped` は `@MainActor`、`boundNotifier` は既存と同じくどの actor からも呼ばれうるが、レジストリの読み出しは既存コードと同じ経路
- [x] 入力検証・認証認可・機密情報: 該当なし
- [x] 既存の呼び出しの解決が変わらないこと: 既存テスト (ほかに `Int.self` を使う箇所なし) が警告なしでビルドされ、`samples/kmp/iosApp` と `verification/kmp/iosApp` が使う形 (結果型を省略した `register`・`String` の `register`・`notifier(for:)`・結果型を省略した `show`) は既存テストに同じ形がある

### 全件実行の表との突き合わせ (L-002)

`kasane/handbook/cross/test-execution.md` の全件実行の表 7 行と、表の外の Scenario ID の網羅検査:

| ビルドルート | 実行 | 件数 / 結果 | 備考 |
|---|---|---|---|
| ios/ | 実装側 + レビューで再実行 | Swift Testing 379 tests / 62 suites passed、XCTest 0 tests、`** TEST SUCCEEDED **` | 再実行も同じ件数 (`-collect-test-diagnostics never`、レーンの Simulator) |
| android/ | 未実行 | — | この diff は `android/` が読むファイルに触れていない |
| android/ (instrumented) | 未実行 | — | 同上 |
| kmp/ | 実装側 | 169 tests / 0 failures (`iosSimulatorArm64Test` 86・`testAndroidHostTest` 83) | `evidence/test-run-summary.md`。kmp/ のコードは diff に無い |
| maui/ | 未実行 | — | .NET 側は `ios/Sources/` を直接コンパイルしない |
| maui/android/native/ | 未実行 | — | `ios/` に依存しない |
| maui/macios/native/ | レビューで実行 | Swift Testing 17 tests / 7 suites passed、XCTest 0 tests、`** TEST SUCCEEDED **` | `ios/` を local package として取り込むので、変更した Swift パッケージのビルドの確認として回した |
| (網羅検査) `scripts/scenario-id-coverage.py` | 実装側 + レビューで再実行 | `IR-KM 7/7`・`MB-KM 4/4`・未網羅なし | — |

未実行の 4 行はこの diff のファイルをコンパイルしないルートで、完了判定の妨げにはしない。ただし完了報告では「全ルートで成功」とは書けない (指摘 2)。

## 指摘事項

### 🟡 Minor (優先度高) 実装期間中に proposal.md の Impact が書き換えられ、deviation.md に記録が無い
**該当箇所**: `proposal.md:34` (作業ツリーの未コミット差分。HEAD `f98ed41` の提案のコミットからの変更)
**問題点**: Impact の 1 項目目の「それ以外の結果型の利用者には影響しない」が、「影響は無い見込みで、実証は ios/ の既存テスト … のビルドと全件実行、IR-KM-07 に限る。実証しない面: … 関数参照として取り出す書き方 …、ABI …」に書き換わっている。中身は主張を正確に狭めたもので、Requirement・Scenario・Non-Goals は変わっておらず、挙動の契約には影響しない。それでも足場 (proposal / design / specs) は実装中は凍結で、実装の都合で書き換えないのが規律 (ksn-core `references/change-scope.md`「足場の凍結と deviation」)。deviation.md に記録が無いので、このままでは「記録のない足場の書き換え」になり、verify の逆流検査でも ❌ になる (verify-001.md)。
**推奨修正**: 記録の主体 (ksn-orchestrator / フェーズ担当) が次のどちらかを選ぶ。
1. `proposal.md` を HEAD の文面に戻し、足した内容 (実証の範囲と実証しない面) は `evidence/test-run-summary.md` か deviation.md の乖離の行へ移す
2. 書き換えを合意済みの差分として deviation.md に乖離の行で記録する (例: `- proposal.md Impact: 提案時は「それ以外の結果型の利用者には影響しない」→ 実装後に実証の範囲 (既存テストのビルドと全件実行・IR-KM-07) と実証しない面 (関数参照・ABI) を書き添えた。理由: 実証していない主張を残さないため (2026-10-20)`)

コードの修正は要らない。どちらかを済ませれば APPROVED にできる。

### 🔵 Suggestion 完了報告の実行範囲を、実行したルートに限って書く
**該当箇所**: `evidence/test-run-summary.md:1`
**問題点**: 証跡は ios/・kmp/・網羅検査を正しく報告しており、虚偽は無い。ただし `kasane/handbook/cross/test-execution.md` の表のうち android/・android/ (instrumented)・maui/・maui/android/native/ は未実行、maui/macios/native/ はレビューでだけ実行した。この change はそれらのルートが読むファイルに触れていないので実行は要らないが、PR やフェーズの完了報告で「全件成功」と書くと表と食い違う。
**推奨修正**: 完了報告には「ios/・kmp/・maui/macios/native/ (レビューで実行) で成功、ほかのルートは対象のファイルに触れないため未実行」と、ルート名を挙げて書く。必要なら maui/macios/native/ の結果 (17 tests passed) を証跡に足す。

## アクションプラン

1. (Minor・優先度高) `proposal.md` の Impact の書き換えを元に戻すか、deviation.md に乖離として記録する — 記録の主体の判断
2. (Suggestion) 完了報告に実行したルートを挙げて書く
