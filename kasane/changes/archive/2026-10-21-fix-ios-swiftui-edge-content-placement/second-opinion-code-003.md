# セカンドオピニオン: fix-ios-swiftui-edge-content-placement (code-003)
**相方**: codex / **label**: so-code-fix-ios-swiftui-edge-content-placement-003 / **日付**: 2026-10-21 / **対象**: HEAD (29a94a6) に対する作業ツリーの差分 (ios/Sources/KsDialogs/・ios/Tests/KsDialogsTests/・change 配下の evidence/)
---
# レビュー結果: fix-ios-swiftui-edge-content-placement (003 回目)

**日付**: 2026-10-21  
**判定**: CHANGES_REQUESTED  
**出力先**: `review-003.md`（保存せず、本文を返却）

## サマリー

前回の Major は、最小高さを外した際に中身の実サイズが変わるケースでは修正されています。ただし、そろえ適用中の実サイズが変わらないケースでは再測定が起動せず、同じ問題が残ります。証跡に残っていた Simulator の UDID 実値は置換済みです。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`：変更された本体・テストのコメント。
- `kasane/handbook/cross/test-execution.md`：全件実行の報告と実行ルート。
- `kasane/handbook/cross/ci-flaky-test-policy.md`：状態変化の待機、継続観測、MainActor 上の読み取り。
- `kasane/handbook/cross/runtime-behavior-verification.md`：修正前後の検出力と動作証跡。
- `kasane/handbook/cross/aiforms-origin-reference.md`：移植前の位置を基準とする合意スコープ。
- ksn-core の change-scope・delta-spec・paths・evidence・ui-artifacts：合意済み差分、足場の凍結、証跡の扱い。

ロードしたスキル: `ksn-review`、`swift-ui-impl-skill`。

関連する accepted の `ios/ADR-0002` とレイアウト公開面を確認した。`ios/ADR-0003` は proposed として扱い、要件の根拠は本 change の仕様と決定事項に置いた。

## 確認した観点

- HEAD に対する対象ディレクトリの差分と、未追跡の新規実装・テスト。
- 仕様と `deviation.md` の合意済み差分、tasks の実装状況。
- start / end と fill の区別、物理方向と RTL の変換。
- 包みの構造的同一性、再測定条件、自己更新による通知の抑制。
- 幅の上限拡大通知と、同一上限で再測定を繰り返さない条件。
- 描画位置と geometry の独立した観測、切り替え後の安定性。
- MainActor、弱参照、公開 API の変更有無、不要なスキップの有無。
- 証跡画像の下端への接地と、証跡テキストの UDID・ローカル絶対パス。

ビルド・テストは依頼に従って実行せず、ホスト提供結果と `evidence/implementation-verification.md` を照合した。

| ルート | 提供された結果・対象外理由 |
|---|---|
| ios/ | 421 tests / 69 suites passed |
| android/ | 対象外：iOS 実装を取り込まない |
| android/ (instrumented) | 同上 |
| kmp/ | iOS 86 tests / 0 failures、Android host 83 tests / 0 failures |
| maui/ | 206 passed / 0 failed / 0 skipped |
| maui/android/native/ | 対象外：iOS 実装を取り込まない |
| maui/macios/native/ | 17 tests / 7 suites passed |

Scenario ID 検査はホスト報告で未網羅なし（PB-IS 35/35）。以下の指摘は静的解析によるもので、再現実行はしていない。

## 指摘事項

### [🟠 Major] そろえ適用中の実サイズが変わらないと、最小高さを外しても再測定されない

**該当箇所**: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentLayoutReader.swift:32`  
関連: `ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIContentView.swift:205`、`ios/Tests/KsDialogsTests/DialogSwiftUIInnerEdgeAlignmentTests.swift:380`

**問題点**: 再測定の追加経路は、目印の `bounds.size` が変わった場合だけ通知します。しかし、縮められる範囲が変わっても、そろえ適用中の実サイズが変わるとは限りません。

例えば、既存の逆向き回帰テストと同じ窓・理想高さ 1000・下端配置で、最小高さを **800** にしてから外すケースです。

1. 通常の安全領域内は高さ 759 なので、最小高さ 800 は収まらず、そろえが適用される。
2. 適用後は上の安全領域を無視するため、中身は高さ 818 に収まる。
3. 最小高さを外しても、適用中の提案に対する実サイズは 818 のままになる。
4. Reader は同じサイズとして通知せず、器・理想サイズ・ホストの安全領域・付いた辺も変わらないため、`updateOverflowAlignment()` が再測定を省略する。

結果として、中身は本来の y 59・高さ 759 に戻らず、上のシステム領域へ入った状態が残ります。`deviation.md` の逆向きの決定を満たしません。

追加されたテストは最小高さ 1000 を使うため、解除時に 1000 → 818 のサイズ変化が発生し、この取りこぼしを検出できません。

**推奨修正**: そろえ適用中の実サイズの変化だけに依存せず、中身のレイアウト条件の更新を契機として、包みを外した状態で再判定できるようにしてください。包み自身の更新による循環は引き続き抑制する必要があります。

回帰テストには、既存の 1000 に加えて、通常の内側 759 より大きく、適用中の領域には収まる最小高さ（例：780）を追加してください。最小高さの解除後に、上下端とも y 59・高さ 759、安全領域が上 59・下 34 へ戻り、安定することを確認します。

## 前回指摘の確認

- **Major：縮められる範囲が戻った際のそろえ解除** — 部分解消。既存の最小高さ 1000 のケースは対応されているが、上記の条件が残る。
- **Minor：証跡中の Simulator UDID 実値** — 解消。`evidence/implementation-verification.md` は `<uuid>` に置換され、対象 evidence テキストに UUID 実値は見つからなかった。

## アクションプラン

1. 実サイズが変わらない更新でも、そろえの要否を再判定する。
2. 部分的に縮む最小高さを使った逆向き回帰テストを追加する。
3. 修正後、対象テストと必要な全件検証をホスト側で再実行する。

指摘件数: Critical 0 / Major 1 / Minor 0 / Suggestion 0。



## 突き合わせ結果

ホスト側: review-003.md (APPROVED、Suggestion 1)、verify-003.md (VALID)。前回の確定指摘のうち UDID は双方とも解消を確認。逆向きのケース (最小の高さ 1000 を外す) はホスト側が検出力まで確認して解消とした。

- 相方のみ (1): そろえが効いている間の中身の大きさが変わらない形で縮められる範囲が戻ると (理想 1000 で最小の高さ 800 を外すなど。効いている間は 818 のまま)、測り直しのきっかけが来ず、そろえが外れない (Major)。箇所と仕組みの説明は具体的だが、静的な読みで再現はしていない。ホスト側はこの条件を試しておらず、矛盾ではない。同じ系統の指摘 (表示中に縮められる範囲が変わる) が 3 周続き、`orchestrator.max-review-cycles` (3) にも達したため、修正サイクルを回さずにオーナーへ諮った → オーナー決定 (2026-10-21) で既知の制約として受け入れ、修正しない (deviation.md 5〜6 行目)
- ホスト側のみ (Suggestion): そろえが効いたまま中身が置き直されると、中身の geometry に描かれない途中の位置が 1 回届く (最後の値は描かれた位置と一致)。実装の変更は求めず、蒸留送り (deviation.md 4 行目)
