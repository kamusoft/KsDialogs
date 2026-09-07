# セカンドオピニオン: add-kmp-typed-show (code-001)
**相方**: codex / **label**: so-code-add-kmp-typed-show / **日付**: 2026-09-07 / **対象**: 作業ツリーの未コミット変更すべて (kmp/ksdialogs-kmp・kmp/api-surface-check・samples/kmp/shared・scripts/scenario-id-coverage.py・kasane/handbook/cross/test-execution.md・verification/sample-walkthrough)
---
# レビュー結果: add-kmp-typed-show

**日付**: 2026-09-07  
**判定**: CHANGES_REQUESTED

## サマリー

実装本体は kmp/ADR-0006 の「commonMain で生成・configure 後、既存のインスタンス渡し経路へ流す」という設計に概ね整合しています。提示されたビルド・テスト結果と12枚の Sample 証跡も成立しています。

ただし、明示的な並行性・実行文脈要件を誤実装しても通るテスト、利用者を誤った登録先へ誘導する公開 KDoc、Kasane の媒体配置違反があるため承認できません。

**指摘件数**: Critical 0 / Major 4 / Minor 0 / Suggestion 0

## 照合した規約

- `comment-policy.md` — 常時適用
- `test-execution.md` — テスト結果報告・完了判定
- `sample-parity.md` — `samples/` の変更
- kmp/ADR-0006 — VM factory は commonMain、View factory は Native
- `ksn-core` の証跡・媒体配置規約
- `kotlin-impl-skill` の Coroutines、テスト、KDoc、型安全性の観点
- `kasane/lessons/code-review.md` は存在しません

## 指摘事項

### [🟠 Major] 公開 KDoc が新しいレジストリ境界と矛盾している

**該当箇所**:  
`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt:10`  
`kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/Toast.android.kt:3`  
`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/Toast.ios.kt:6`  
`kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/Dialog.android.kt:3`  
`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/Dialog.ios.kt:6`

**問題点**: `KsToast` は依然として「Native への委譲面にすぎない」、actual object は「レジストリは Native 側の1個だけ」と説明しています。しかし実装後は、VM factory 表と型指定 show の前段を commonMain が持ちます。特に Dialog の記述は、Native 側の VM factory 登録が共有コードから見えるように読め、PB-KT-11 の保証と正反対です。利用者が誤った登録先を選ぶと、実行時に未登録エラーになります。

また、これらは公開 KDoc でありながら `core/ADR-*`、`kmp/ADR-*` などの内部識別子を含み、comment-policy にも違反します。

**推奨修正**: Dialog / Loading / Toast の共通契約と actual entry の公開 KDocを一巡し、「VM factory と型指定前段は commonMain」「View factory・表示状態は Native」の境界へ書き換えてください。公開 KDoc から ADR ID を除き、設計根拠は内部実装コメントへ移してください。

### [🟠 Major] PB-KT-07 が VM factory の別文脈実行を検出できない

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowTests.kt:203`

**問題点**: factory 内で記録した `dispatchCount` と show 前の値を比較していますが、factory を別 dispatcher へ移した場合、呼び出し元 dispatcher のカウントが増えるのは戻ってくる時点です。したがって「factory だけ別 dispatcher、configure は呼び出し元」という禁止実装でも、このテストは通り得ます。ADR が明示的に拒否している dispatcher hop を固定できていません。

**推奨修正**: `CountingDispatcher` が自身の block を実行中か示すマーカーを持ち、factory がその実行区間内で呼ばれたことを直接記録してください。Loading の独立したデコレータについても同じ保証を直接検査してください。

### [🟠 Major] PB-KT-12 が再登録と解決の並行実行を成立させていない

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowTests.kt:243`

**問題点**: 再登録 coroutine と show ループの開始を同期しておらず、再登録が全件終了してから show が始まる、またはその逆でも成功します。最終アサーションも「登録」で始まる文字列だけを確認するため、逐次実行でも常に成立します。非スレッドセーフな通常の mutable map に退行しても検出できない可能性があります。

**推奨修正**: 開始ゲートと進行確認用の atomic/barrier を置き、登録側と解決側が実際に重なる状態を作ってください。少なくとも両処理が進行中であることを観測してから反復を継続し、各生成物が完全な登録単位に対応することを検証してください。

### [🟠 Major] Sample 証跡が媒体配置のホワイトリスト外にある

**該当箇所**:  
`kasane/changes/add-kmp-typed-show/tasks.md:18`  
`kasane/changes/add-kmp-typed-show/verification/sample-walkthrough/notes.md:1`

**問題点**: 動作証跡の静止画は `changes/<id>/evidence/` に置く規約ですが、12枚を任意ディレクトリ `verification/` に保存しています。この場所は媒体配置のホワイトリスト外で、archive 時の `distill.archive-media` 管理から外れるおそれがあります。deviation.md による合意済み乖離もありません。

画像自体は全12枚を確認し、各 Scenario の表示内容と notes の説明は一致しており、個人情報の写り込みも見当たりません。

**推奨修正**: 一式を `kasane/changes/add-kmp-typed-show/evidence/sample-walkthrough/` へ移し、`tasks.md` と参照記述を更新してください。

## アクションプラン

1. 公開 KDoc を新しい commonMain／Native 境界に合わせ、内部識別子を除去する。
2. PB-KT-07 を factory の実行地点そのものを観測するテストへ修正する。
3. PB-KT-12 に同期点を追加し、実並行性を保証する。
4. Sample 証跡一式を `evidence/` 配下へ移す。
5. 修正後に KMP 全件、負の compile 検査、comment-policy lint、scenario-id-coverage を再確認する。

総合判定: CHANGES_REQUESTED


## 突き合わせ結果 (2026-09-07)

ホスト側 review-001.md (CHANGES_REQUESTED: Major 1 / Minor 2 / Suggestion 2) との突き合わせ。採否は ksn-second-opinion の規則による。

| 相方の指摘 | 採否 | 扱い |
|---|---|---|
| 公開 KDoc が新しいレジストリ境界と矛盾 (「レジストリは Native 側の 1 個だけ」が PB-KT-11 と逆) | **採用** (相方のみ・根拠強: 該当行を実物で確認、利用者が登録先を取り違える実害あり) | Major として修正サイクルへ。書き直す KDoc からは ADR ID も除く (comment-policy「公開メンバーの doc コメント」)。触らない既存行の ADR ID は本 change の対象外 |
| PB-KT-07 が VM factory の別 dispatcher 実行を検出できない | **採用** (相方のみ・根拠強: 禁止実装が通る具体シナリオあり) | Major として修正サイクルへ (Dialog / Loading とも factory の実行区間を直接観測する形に) |
| PB-KT-12 が再登録と解決の重なりを成立させていない | **採用** (相方のみ・根拠強: 逐次実行でも通る) | Major として修正サイクルへ (開始ゲートと進行観測を置く) |
| Sample 証跡が `verification/` にあり媒体ホワイトリスト外 | **降格** (根拠弱) | ksn-distill の媒体削除は置き場を問わず change 配下全体が対象で、archive 時の管理から外れない。直近の姉妹 change (archive/2026-09-06-add-loading-toast-typed-show) を含むリポジトリの前例 9 件が `verification/` を使っており、config の lint 除外もこのパスを前提にしている。置き場は前例に合わせて維持する |

ホスト側のみの指摘 (Major: `registerViewModel` の ObjC 露出 / Minor 2 / Suggestion 2) は相方と矛盾せず、そのまま判定処理へ含める。未解決: なし。
