# セカンドオピニオン: add-layout-spec (spec-002)
**相方**: codex / **日付**: 2026-08-18 / **対象**: 改訂提案一式 (proposal / design / specs / layout-cases / tasks / ui/brief + ADR-0014/0015 参照)
---
# スペックレビュー結果: add-layout-spec（改訂版）

**判定**: **NEEDS_DISCUSSION**

Critical 0 / Major 5 / Minor 3 / Suggestion 0

## サマリー

VM 経路を撤去し、`DialogOptions` / `DialogPlacement` とコンテンツ添付へ置換する中心方針は、core/ADR-0014・0015 と整合しています。初版資産を温存するタスク分解も明確です。

ただし、数値境界、placement の合成単位、添付値のスナップショット時点、公開 API の型形状に未決事項があります。このまま実装すると、各プラットフォームが異なる妥当な解釈を選べてしまうため、実装再開前の仕様確定が必要です。

## 指摘事項

### [🟠 Major] 数値入力の正規化規則が矛盾・不足している

**該当箇所**: [design.md:71](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:71)、[design.md:72](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:72)、[design.md:120](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:120)、[dialog-contract/spec.md:9](kasane/changes/archive/2026-08-19-add-layout-spec/specs/dialog-contract/spec.md:9)

**問題点**:

- 比率の有効域を `0〜1` としながら、`0` の意味は未決定です。
- design は「proportional = 0 は proposal の Non-Goals」としていますが、proposal の Non-Goals にその記載はありません。
- 比率の NaN/Infinity、Offset の Infinity、Margin の負値・NaN・Infinity が未規定です。
- 左右または上下 Margin の合計が基準領域を超えると、有効領域 `A` が負の長さになり、現在の手順では負サイズや NaN rectを生成できます。
- 無効値の正規化は Requirement なのに、対応する Scenario やケース表のケースがありません。

**推奨修正**: 全 `Double` フィールドについて有限性・許容範囲・正規化先を表に明記し、`proportional = 0` と `A` が空になる場合を確定してください。比率の負値/0/1超/NaN/±Infinity、Offset の非有限値、Margin の負値・過大値について、ケース表または個別 Scenario を追加してください。

### [🟠 Major] visibleArea が水平軸にも効くことをケース表で検証できない

**該当箇所**: [dialog-contract/spec.md:54](kasane/changes/archive/2026-08-19-add-layout-spec/specs/dialog-contract/spec.md:54)、[layout-cases.json:30](kasane/changes/archive/2026-08-19-add-layout-spec/specs/dialog-contract/layout-cases.json:30)

**問題点**: Scenario は「visibleArea 基準は両軸に効く」と要求していますが、C11を含むすべての visibleArea ケースで `left/right` inset が0です。Android/iOSの実装が水平 inset を完全に無視しても全16ケースを通過できます。これは、原典の垂直軸だけに効く不備を解消するという core/ADR-0008 の狙いを検証できません。

**推奨修正**: 左右に非ゼロかつ非対称な inset を持つケースを追加し、水平 Start/End/Center の少なくとも1つで期待 `x` が window 基準と異なることを固定してください。

### [🟠 Major] show placement の優先順位がオブジェクト単位かフィールド単位か不明

**該当箇所**: [design.md:88](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:88)、[dialog-contract/spec.md:18](kasane/changes/archive/2026-08-19-add-layout-spec/specs/dialog-contract/spec.md:18)、[dialog-contract/spec.md:25](kasane/changes/archive/2026-08-19-add-layout-spec/specs/dialog-contract/spec.md:25)

**問題点**: 添付が `End/End + Offset`、show 引数が `horizontalAlignment = Start` だけの場合、次のどちらか判定できません。

- show の `DialogPlacement` 全体で置換し、垂直配置とOffsetもそのオブジェクトの既定値へ戻す
- 指定フィールドだけ上書きし、添付の垂直配置とOffsetを維持する

全フィールドが既定値付きの非optional値なら、フィールド単位合成では「省略」と「明示的に既定値を指定」を区別できません。

**推奨修正**: 全体置換かフィールド単位合成かを明記してください。全体置換なら、添付の他フィールドも失われるScenarioを追加します。フィールド単位合成なら、未指定状態を表現できる型設計へ変更する必要があります。

### [🟠 Major] 添付値の読み取り時点が一意でなく、凍結後の不変性も検証されない

**該当箇所**: [design.md:37](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:37)、[dialog-contract/spec.md:18](kasane/changes/archive/2026-08-19-add-layout-spec/specs/dialog-contract/spec.md:18)、[tasks.md:13](kasane/changes/archive/2026-08-19-add-layout-spec/tasks.md:13)

**問題点**: 「factory 直後から初回レイアウト完了までに読む」は時間範囲であり、どの瞬間の値を採用するかを決めていません。区間内で値が変化した場合、factory直後に1回読む実装と、初回レイアウト直前に読む実装の双方が仕様を満たすように見えます。また「初回レイアウト後の変更は反映しない」という SHALL に対応するScenario・テストタスクがありません。

**推奨修正**: スナップショット時点または読み取りプロトコルを一意に定義してください。そのうえで、スナップショット前の値、完了直後の変更、表示中の変更を検証するScenarioを追加してください。

### [🟠 Major] 公開 API の型形状が実装者判断に残されている

**該当箇所**: [design.md:66](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:66)、[design.md:84](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:84)、[design.md:89](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:89)、[maui-binding/spec.md:9](kasane/changes/archive/2026-08-19-add-layout-spec/specs/maui-binding/spec.md:9)、[kmp-facade/spec.md:9](kasane/changes/archive/2026-08-19-add-layout-spec/specs/kmp-facade/spec.md:9)

**問題点**:

- MAUI の `ksd:Dialog.*` が、2つのオブジェクト値添付プロパティなのか、10個のスカラー添付プロパティなのか決まっていません。
- MAUI Binding、null、デフォルト値、個別フィールドの合成方法も未定義です。
- KMP commonMain の具象 `DialogOptions` に対し、`overlayColor` は「各形態のNative色型」、`dialogMargin` は「Thickness相当」としかなく、commonMainで使用する具体型が決まっていません。
- `DialogOptions` はKMPで公開される一方、供給経路がないため、実行時テストだけではAPI形状の誤りを検出できません。

**推奨修正**: 各形態の公開シグネチャ表を追加してください。少なくとも型名、フィールド型、constructor/default、show省略時の表現、MAUI添付プロパティ名、KMPのARGB・Insets表現を確定し、公開APIのコンパイル検査をタスクへ追加してください。

### [🟡 Minor] Sampleパリティの長命SSoTを更新するタスクがない

**該当箇所**: [tasks.md:5](kasane/changes/archive/2026-08-19-add-layout-spec/tasks.md:5)、[tasks.md:33](kasane/changes/archive/2026-08-19-add-layout-spec/tasks.md:33)、[sample-parity.md:40](kasane/concepts/cross/conventions/sample-parity.md:40)

**問題点**: `sample-parity.md` は表示文言の全体を正と宣言し、現在はBasic Dialog 1件だけを列挙しています。本提案はLayout Dialogと新しい文言を追加しますが、tasksには同概念の更新がありません。完了後にbrief・実装と長命規約が食い違います。

**推奨修正**: `sample-parity.md` のデモ項目・文言表を更新するタスクを追加してください。

### [🟡 Minor] 「既定値時の挙動維持」と外側タップ既定trueが矛盾している

**該当箇所**: [proposal.md:28](kasane/changes/archive/2026-08-19-add-layout-spec/proposal.md:28)、[design.md:100](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:100)、[design.md:116](kasane/changes/archive/2026-08-19-add-layout-spec/design.md:116)

**問題点**: proposalは既定値時の挙動が現状維持としていますが、designは外側タップ機構が現行実装になく、既定trueで新規導入されると明記しています。ソース互換・視覚互換と操作互換が混同されています。

**推奨修正**: 「既存呼び出しはソース互換、初期rectと見た目は維持。ただし外側タップは新たにcancelledになる」とImpactを限定してください。

### [🟡 Minor] UI brief が禁止されたローカル絶対パスを恒久参照にしている

**該当箇所**: [ui/brief.md:15](kasane/changes/archive/2026-08-19-add-layout-spec/ui/brief.md:15)、[aiforms-origin-reference.md:45](kasane/concepts/cross/conventions/aiforms-origin-reference.md:45)

**問題点**: briefは原典リポジトリの `/Volumes/...` を「恒久リファレンス」としていますが、プロジェクト規約はローカルパスの直書きを禁止し、参照対応表への集約を要求しています。別環境では参照不能です。

**推奨修正**: `reference-repositories.md` 経由の参照へ置き換え、視覚的に不可欠な画像なら `ui/references/` に由来を記録した参照画像を置いてください。

## アクションプラン

1. 数値境界、placement合成単位、添付スナップショット時点をオーナー判断で確定する。
2. 各形態の公開APIシグネチャ、特にMAUI添付面とKMP commonMain型を明文化する。
3. 水平visibleArea、無効値、部分的placement、添付凍結後変更の検証ケースを追加する。
4. Sampleパリティ概念・proposal Impact・UI参照記述を整合させる。
5. 修正後、Critical/Majorが解消された状態で再レビューする。

指定どおり、ビルド・テストおよびファイル書き込みは行っていません。


## 突き合わせ結果 (2026-08-18 ホスト側判定)

確定 0 / **採用 8** (全件「相方のみ + 根拠強」= ホスト自己レビューの見逃し) / 降格 0 / 未解決 0。

| 指摘 | 採否 | 反映 |
|---|---|---|
| Major1 数値正規化の矛盾・不足 | 採用 | design D5 に A 空クランプ、D6 に正規化一般規則、spec に正規化 Scenario、proposal Non-Goals に proportional=0 を明記 (意図的未規定は維持) |
| Major2 visibleArea 水平軸の検証穴 | 採用 | ケース表に C20/C21 (非対称 left/right inset の Start/End) を追加 → 18ケース |
| Major3 placement 合成単位 | 採用 | オブジェクト単位置換に確定 (design D6 + spec Requirement/Scenario)。オーナー確認事項 |
| Major4 添付読み取り時点・不変性 | 採用 | スナップショット = 初回レイアウトパス完了時点に確定 (design D4 + spec) + 不変 Scenario 追加 |
| Major5 公開 API 型形状 | 採用 | design D6 に公開シグネチャ表 (MAUI = スカラー添付10個 / KMP = DialogPlacement のみ公開に簡素化)。コンパイル検査を tasks へ。オーナー確認事項 |
| Minor1 sample-parity.md 更新漏れ | 採用 | tasks 6.4 追加 |
| Minor2 互換性表現の混同 | 採用 | proposal Impact を「ソース互換・視覚互換維持、外側タップのみ操作面の新規変更」に限定 |
| Minor3 brief のローカル絶対パス | 採用 | reference-repositories.md 経由の参照へ置換 |

## 突き合わせ結果・第2ラウンド (2026-08-18 ホスト側判定)

前ラウンド8件は相方が解消確認。今回 Major 3 / Minor 2:

| 指摘 | 採否 | 反映 / 状態 |
|---|---|---|
| Major1 KMP DialogOptions 除外が ADR-0015 と矛盾 | **未解決 (オーナー判断待ち)** | ホストが既にオーナーへ提示済みの確認事項③と同一論点。承認なら ADR-0015 改訂 + proposal/dialog-contract の文言追随、否認なら KMP へ復元 |
| Major2 proportional=0 の二重定義 | **未解決 (オーナー判断待ち)** | 確認事項②と同一論点。写像表 (有効域 0〜1) と未規定宣言の不整合は相方の言う通りで、どちらかに確定が必要 |
| Major3 空の有効領域 A の位置未定義 | 採用 | design D5 手順2を式定義 (A.origin = R.min + 先頭 Margin / length = max(0,...)) + C22 追加 (19ケース) |
| Minor1 スナップショット後の機能メタ変更 | 採用 | 不反映 Scenario に isCanceledOnTouchOutside と操作挙動を明記 |
| Minor2 SwiftUI フォールバックと契約の衝突 | 採用 | design と phase-5-2 agenda を「初回提示前に到達を待って収束・提示後の再適用は契約違反」に統一 |

## 突き合わせ結果・第3〜4ラウンド (2026-08-18 ホスト側判定)

- 第3ラウンド (オーナー決定②③反映後): CHANGES_REQUESTED — Major 1 (design Open Questions に proportional=0 の旧未規定記述が残存)。採用し即修正
- 第4ラウンド: **APPROVED** (Critical/Major/Minor/Suggestion すべて 0)。「前回までの全指摘が解消され、実装着手を妨げる仕様上の問題はありません」

最終集計: 相方指摘 計14件 — 採用 12 / オーナー判断で確定 2 (proportional=0 の 0 以下未指定化、KMP の DialogPlacement のみ公開 + ADR-0015 改訂) / 降格 0 / 未解決 0。
