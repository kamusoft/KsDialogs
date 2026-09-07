# セカンドオピニオン: add-sample-capture-automation (spec-001)
**相方**: codex / **label**: so-spec-add-sample-capture-automation / **日付**: 2026-08-27 / **対象**: 提案一式 (proposal.md / specs/samples / specs/capture-tooling / tasks.md)
---

# レビュー結果: add-sample-capture-automation

**日付**: 2026-08-27
**判定**: **NEEDS_DISCUSSION**

## サマリー

方針自体は妥当ですが、起動引数の契約、再起動時のライフサイクル、デモ別ディスパッチ、撮影成功条件が未確定です。このまま実装すると6アプリで異なる解釈が入り、スクリプトが部分成功を成功扱いする恐れがあります。

**指摘件数**: Critical 0 / Major 8 / Minor 2 / Suggestion 0
指定に従いビルド・テストは実行せず、ファイルも作成していません。

## 指摘事項

### [🟠 Major] 変更級 M が Kasane の分類基準と一致しない

**該当箇所**: `proposal.md:11`、`proposal.md:12`、`proposal.md:29`
**問題点**: 変更自身が `samples` と新設 `capture-tooling` の2能力を明記し、6アプリと `adb` / `simctl` 連携を横断します。Kasane の基準では「複数能力横断」「外部連携」は L 級です。後述する起動ライフサイクル、CLI、保存規約などを固定する `design.md` がないことも、仕様の穴に直結しています。
**推奨修正**: L 級へ再分類し、`design.md` に引数表現、プロセス再起動、ディスパッチ責務、撮影状態、失敗・保存契約を記述してください。

### [🟠 Major] KMP のディスパッチ責務が現行コードと矛盾する

**該当箇所**: `proposal.md:11`、`tasks.md:8`、`specs/samples/spec.md:18`
**問題点**: 「KMP では Inline Dialog のみ OS 側でディスパッチ」とありますが、現行コードでは Layout / Transition も OS 側の画面状態で開始します。

- `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:18` と `:34` はローカル状態を変更してパネルを開く
- `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:28` と `:32` は OS 側の `open*Panel` を呼ぶ
- 共有 `SamplePresenter` の Layout / Transition メソッドは、パネルで確定した属性を受けてダイアログを表示する後段処理

したがって共有 Presenter だけでは「メニュー項目をタップしたのと同じ」初期状態を作れません。iOS Native についても `SampleMenuModel` にはパネルを開く責務がありません。
**推奨修正**: 9 ID ごとに「起動直後の期待状態」と「担当層」を表で確定してください。少なくとも Layout / Transition / Inline は OS UI 層のディスパッチが必要です。

### [🟠 Major] iOS launch argument の外部表現が定義されていない

**該当箇所**: `specs/samples/spec.md:9`、`specs/capture-tooling/spec.md:9`
**問題点**: キー名だけでは、iOS の文字列配列をどう key/value に変換するか決まりません。例えば `--demo basic-dialog`、`-demo basic-dialog`、`demo=basic-dialog` はすべて別契約です。値欠落、重複キー、空文字の場合も未定義です。6実装が異なる形式を採用しても、現 Scenario では違反と判定できません。
**推奨修正**: 外部 CLI 表現を完全なコマンド例で契約化し、iOS のトークン列と Android intent extra への写像、欠落・重複・空値の扱いを定めてください。

### [🟠 Major] 連続撮影時の再起動と「一度だけ」の意味論がない

**該当箇所**: `specs/samples/spec.md:18`、`specs/capture-tooling/spec.md:9`
**問題点**: 現行 MAUI Android は `SingleTop` (`samples/maui/KsDialogs.Sample.Maui/Platforms/Android/MainActivity.cs:7`) です。既に起動中なら新しい extra は `OnCreate` ではなく新しい Intent 経路へ届き得ます。iOS でも、起動中アプリへの `simctl launch` が新規プロセス起動と同じ意味にはなりません。一方、Activity/scene 再生成時に同じ設定を再読込すると、ダイアログを二重に自動再生する危険があります。
**推奨修正**: スクリプトが毎回 `force-stop` / `terminate` してコールド起動する契約にするか、再 activation を正式対応するかを決めてください。また自動再生設定を「プロセス起動につき一度だけ消費」などと明記し、連続して異なるデモを撮る Scenario を追加してください。

### [🟠 Major] 9デモの ID 対応と Custom Loading が検証されない

**該当箇所**: `specs/samples/spec.md:20`、`specs/samples/spec.md:34`、`specs/samples/spec.md:44`、`tasks.md:28`
**問題点**: 自動再生 Scenario は `basic-dialog` だけ、刻み間隔 Scenario は Default Loading だけです。ID の取り違え、Layout / Transition でパネルではなくダイアログを直接出す実装、Custom Loading だけ固定400msのまま、といった不具合が仕様上合格できます。
**推奨修正**: 9 ID すべてについて期待する初期状態を表にし、6アプリとの対応を検証してください。刻み間隔は Default / Custom 両方を対象にします。引数なし回帰も、少なくとも9デモすべてのディスパッチ経路を対象化してください。

### [🟠 Major] Scenario 網羅検査が確実に未網羅を報告する構成になっている

**該当箇所**: `specs/capture-tooling/spec.md:11`、`specs/capture-tooling/spec.md:20`、`specs/capture-tooling/spec.md:29`、`tasks.md:26`、`tasks.md:27`
**問題点**: `tasks.md` が allow-missing に登録するのは CA-SA-01〜06 だけです。CA-CT-01〜03 に対応するテストタスクはなく、`--selftest` の単発実行は Scenario ID を持つテスト宣言ではありません。現行 `scenario-id-coverage.py` は進行中 spec も走査するため、CA-CT-01〜03 が未網羅になります。
**推奨修正**: Python テストを Scenario 網羅検査の対象にできるよう整備して CA-CT テストを追加するか、実機限定 Scenario と自動化可能 Scenario を分離し、理由付きの除外方針を明記してください。CA-CT-03 は自動テスト可能なので、単純な除外は避けるべきです。

### [🟠 Major] 撮影成功・失敗と生成物の契約が不足している

**該当箇所**: `specs/capture-tooling/spec.md:9`、`specs/capture-tooling/spec.md:11`、`proposal.md:12`
**問題点**: 「スクリーンショット一式」がどの状態・何枚・どの名前なのか決まっていません。また、複数デバイス、表示待ちタイムアウト、起動失敗、タップ失敗、空画像、保存済みファイルとの衝突、途中まで保存された場合の終了コードも未定義です。固定 sleep だけで実装しても Scenario を満たした扱いになり得ます。

保存先についても、Kasane の媒体ホワイトリストは `evidence/` や `ui/verification/` などに限定されますが、spec は単に「指定した保存先」としています。
**推奨修正**: デモごとの成果物マニフェスト、命名、準備完了の待機条件、タイムアウト、デバイス選択、非0終了条件、部分成果物・上書き方針を定義してください。保存先は許可ディレクトリへ制限するか、一時出力と正式証跡保存を明確に分離してください。

### [🟠 Major] selftest の検査単位と失敗経路が不足している

**該当箇所**: `specs/capture-tooling/spec.md:27`、`tasks.md:14`、`tasks.md:16`、`tasks.md:26`
**問題点**: 座標・起動コマンドは6アプリごとに異なり得るのに、要件は「全デモ ID」に操作列があることしか求めません。1アプリ分の定義欠落を見逃せます。また正常な定義で `--selftest` を一度実行するだけでは、「不整合なら非0」を検証できません。`tasks.md` にある不明ルート/ID の拒否も Requirement にはありません。
**推奨修正**: selftest の整合条件を6アプリ×9デモの全組み合わせ、起動設定、必要スクリーンショット状態まで拡張してください。欠落・重複・不明 ID を注入した負のテストで、非0終了を固定してください。

### [🟡 Minor] Loading の数値範囲と時間判定が曖昧

**該当箇所**: `specs/samples/spec.md:46`、`specs/samples/spec.md:48`
**問題点**: 「正の整数」には極端に大きい値も含まれ、overflow、前後空白、符号付き表現などの扱いが決まりません。また「約2000ミリ秒」には許容誤差がなく、スクリーンショットだけでは時間間隔を判定できません。
**推奨修正**: 受理範囲、数値構文、範囲外時の扱いを定めてください。時間検証には許容誤差または注入可能な待機依存を用いたテスト基準を設定してください。

### [🟡 Minor] docstring の受け入れ基準が主観的

**該当箇所**: `specs/capture-tooling/spec.md:20`
**問題点**: 「追加の手順再構築なし」はレビュアーごとに判定が変わります。必要事項の列挙も、保存規約や失敗時対処について十分か判定できません。
**推奨修正**: 必須節をチェックリスト化してください。例として、全引数、6ターゲット、完全なコマンド例、前提デバイス、ゼロ/複数デバイス時の扱い、成果物一覧、終了コード、保存先、再実行・上書き方法を明記します。

## アクションプラン

1. L 級へ再分類し、起動・ディスパッチ・撮影・保存の設計判断を `design.md` に固定する。
2. 9デモの「ID → 初期状態 → 担当層」を定義し、Layout / Transition の責務矛盾を解消する。
3. 引数表現、コールド起動、one-shot、エラー・出力契約を Scenario 化する。
4. 6アプリ×9デモと両 Loading の検証マトリクスを追加する。
5. CA-CT Scenario と `scenario-id-coverage.py` の対応、および selftest の負のテストを整備する。

---

## 突き合わせ結果 (ホスト側 2026-08-27)

ホスト側自己レビュー (2周・指摘なし) との突き合わせ。相方のみの指摘につき根拠で判定:

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | 変更級 M → L 再分類 | **未解決 (オーナー判断へ)** | 級の確定はオーナー権限。探索時に M 確定済みだが、指摘 2〜8 が design 級の判断を要することが新事実として加わったため再提示 |
| 2 | KMP ディスパッチ責務の矛盾 (Layout / Transition はパネルを OS 側で開く) | **採用** | ホスト側で現物確認済み (SampleMenuScreen.swift の状態変更 / MainActivity.kt の open*Panel)。探索時の調査要約の誤りに起因する提案の事実誤認 |
| 3 | 引数の外部表現が未契約 | **採用** | 6実装の解釈割れは CA-SA-06 (パリティ) を空文化する実害シナリオあり |
| 4 | コールド起動 / one-shot 意味論の欠落 | **採用** | MAUI Android の SingleTop は現物確認可能で、二重自動再生の実害シナリオが具体的 |
| 5 | 9 ID 対応と Custom Loading の未検証 | **採用** | ID→初期状態の表と Scenario の一般化で対応 (6アプリ×9デモの全数マトリクスは撮影スクリプト自体の成果物で兼ねる) |
| 6 | CA-CT Scenario が網羅検査で未網羅になる | **採用** | scenario-id-coverage の走査仕様に照らして機械的に正しい |
| 7 | 撮影成果物・失敗契約の不足 | **採用** | 部分成功の成功扱いはワーカー撮影の信頼性を直撃する実害シナリオあり |
| 8 | selftest の検査単位・負の経路不足 | **採用** | 「1アプリ分の欠落を見逃す」は selftest の目的に対する具体的な穴 |
| 9 | 数値範囲・時間判定の曖昧さ | **降格 (Minor)** | 実害は限定的。受理範囲の1行のみ反映し、時間の厳密判定は導入しない (撮影目的には各段階が捉えられれば十分) |
| 10 | docstring 基準の主観性 | **降格 (Minor)** | 好みの域だが、必須節のチェックリスト化は安価なので反映する |

採用 7 / 降格 2 / 未解決 1 (級の再分類 = オーナー判断)。
