# セカンドオピニオン: add-presentation-behavior (code-001)
**相方**: codex (codex-cli 0.145.0、器 ksn-reviewer) / **日付**: 2026-08-21 / **対象**: HEAD 66ea298 に対する作業ツリーの未コミット変更全体 (189 ファイル)。入力はホスト側 ksn-reviewer と同一 (アーティファクト・diff 範囲・deviation.md・テスト結果の要約)
---
# レビュー結果: add-presentation-behavior

**判定: CHANGES_REQUESTED**

## サマリー

ホスト実施の全テスト成功と 58/61 Scenario 網羅は確認しましたが、Major 2件、Minor 1件があります。特に iOS の公開 API 表面と、UIKit 上での「器の撤去後に結果配送」という順序保証は修正が必要です。

## 指摘事項

### 🟠 Major: 凍結 API にない `defaultDuration` が公開されている

**該当箇所**: [ios/Sources/KsDialogs/Contract/DialogTransition.swift:52](ios/Sources/KsDialogs/Contract/DialogTransition.swift:52)

**問題点**: `public extension DialogTransition` 内の `static let defaultDuration` は公開メンバーになります。Decision 3 の完全シグネチャには存在せず、意図しない公開 API の追加です。一度リリースすると後から非公開化することが破壊的変更になります。現在の API 検査は必要な API の存在と禁止引数を検査しますが、余分な公開メンバーを検出しません。

**推奨修正**: 公開 factory の既定引数は `0.25` を直接記述し、実装用定数を `internal` にしてください。コンパイラ上必要なら `@usableFromInline internal` も選択肢です。利用者側から `DialogTransition.defaultDuration` を参照できないことの負のコンパイル検査も追加してください。

### 🟠 Major: iOS は UIKit の撤去完了を待たずに結果を配送できる

**該当箇所**: [ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:472](ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:472)、[ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:30](ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:30)

**問題点**: `finishRemoval()` は `onDismissRequest` を呼んだ直後に結果を配送します。一方、実 surface は `dismiss(animated: false)` の完了コールバックを受け取っておらず、提示関係と View の撤去が完了したことを確認できません。これは PB-TR-10 / PB-TR-13 と Decision 5 の「器の撤去後に配送する」保証を構造上満たせず、結果を受けて直ちに次のダイアログを出す場合の競合要因になります。

テスト surface は `dismiss` 内で配列と View を同期的に除去するため、この差が隠れています。

**推奨修正**: `DialogPresentationSurface.dismiss` を async または completion 付きにし、UIKit の dismissal completion 後に `releaseContentHost()` と結果配送へ進んでください。host-lost 経路だけは既に撤去済みとして即配送できます。テストにも「閉鎖要求」と「実際の撤去完了」を別々に進められる surface を追加してください。

### 🟡 Minor: Scenario 網羅検査がテスト名以外の ID でも通る

**該当箇所**: [scripts/scenario-id-coverage.py:219](scripts/scenario-id-coverage.py:219)

**問題点**: `collect_test_ids` はテストソース全文から ID を抽出します。そのため、コメント、説明用文字列、証跡ファイル名だけに ID があっても「テストあり」と判定され、`--require-mirror` も誤って通過できます。Decision 7 と tasks 1.5 が求めるのは「テスト名からの抽出」です。現変更では各 ID が実際のテスト名にも含まれているため、直ちに 58/61 という結果が虚偽というわけではありません。

**推奨修正**: 対応言語のテスト宣言・属性に限定して抽出するか、ID がテスト宣言の近傍にあることを検証してください。「コメントだけに ID がある場合は未網羅になる」自己テストも追加すべきです。

## 暫定 deviation への所見

- factory の `@MainActor` 化は Swift 6 と UIKit 型の隔離を踏まえると妥当です。
- alpha 復帰の暫定差分は、実行時不変条件より既存テストの `hitTest` を優先している点がやや脆弱です。違反とは扱いませんが、将来的にはテスト側の観察方法を変え、フック開始との同一フレーム性を守る案が望ましいです。

## アクションプラン

1. iOS の余分な公開 `defaultDuration` を非公開化し、負の API 検査を追加する。
2. UIKit の実撤去完了後に配送する completion-aware な経路へ変更する。
3. Scenario ID 検査をテスト宣言に限定する。
4. 既存テスト一式と公開 API の負の検査を再実施する。

ビルド・テストは制約に従って再実行しておらず、提示されたホスト側結果をレビュー根拠として扱いました。ファイルへの書き込みも行っていません。


## 突き合わせ結果 (ホスト review-001.md との照合、2026-08-21)

| # | 指摘 | ホスト (review-001) | 相方 (codex) | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | iOS `DialogTransition.defaultDuration` が public 露出 (Decision 3 の完全シグネチャ外) | 🟠 Major | 🟠 Major | **確定** | 双方一致。lessons/inbox「公開 API 表面の見逃し」と同型だが今回は双方が検出 |
| 2 | iOS `finishRemoval()` が UIKit `dismiss(animated: false)` の完了を待たずに配送 (PB-TR-10 / PB-TR-13 の構造的な穴。テスト surface は同期撤去で差が隠れる) | — | 🟠 Major | **採用** (Major) | 相方のみだが該当箇所 (`DialogContainerViewController.swift:472` / `UIKitDialogPresentationSurface.swift:30`)・実害シナリオ (結果を受けて直ちに次のダイアログを出す競合)・テストが検出できない理由まで具体的。ホスト側の見逃しとして扱う |
| 3 | `scripts/scenario-id-coverage.py` がテスト宣言ではなくソース全文から ID を抽出 (コメントだけの ID や `--require-mirror` の偽陽性) | — | 🟡 Minor | **採用** (Minor) | 該当行と偽陽性の経路が具体的。Decision 7 / tasks 1.5 の「テスト名から抽出」との食い違いで、修正も軽い |
| 4 | handoff-distill.md の maui 実測件数 61 → 62 | 🟡 Minor | — | 確定 (ホストのみ) | オーケストレーターが直接修正 |
| 5 | 新設パネル操作部の accessibility tree 証跡が 4 ルート分ない (sample-parity.md の証跡規定) | 🟡 Minor | — | 確定 (ホストのみ) | 証跡取得を委譲 |
| 6 | フック未完了警告の有効条件が iOS (`#if DEBUG` = ライブラリのビルド構成) と Android (提示先の `FLAG_DEBUGGABLE`) で非対称 | 🔵 Suggestion | — | 申し送り | 現状維持 + handoff-distill.md へ記録 |
| 7 | Android だけ「ミリ秒に落とすと 0 になる duration」が成立しない値に含まれる (文書側の追記) | 🔵 Suggestion | — | 申し送り | transition-semantics.md の蒸留時追記として handoff-distill.md へ |

- 相方の暫定 deviation への所見: factory の `@MainActor` 化は妥当 / alpha 復帰の暫定差分は「既存テストの hitTest を優先している点がやや脆弱。将来的にはテスト側の観察方法を変えて同一フレーム性を守る案が望ましい」— オーナー判断の材料として完了報告に含める
- 降格: 0 件 / 未解決 (矛盾): 0 件 / 採用 (相方のみ): 2 件 (#2 Major, #3 Minor)
