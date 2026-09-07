# セカンドオピニオン: expand-api-surface (spec-002)
**相方**: codex / **日付**: 2026-08-19 / **対象**: 2026-08-19 改訂後の提案一式 (add-layout-spec 完了分の反映)
---
# レビュー結果: expand-api-surface（改訂ラウンド）

**日付**: 2026-08-19  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 5 / Minor 2 / Suggestion 0

## サマリー

ADR-0014・0015 の基本方針、宣言的 UI 添付 DSL の名称、Compose のモジュール境界、KMP の options 非輸送は正しく反映されています。一方、改訂で追加された供給契約・SwiftUI の到達待ち・読み上げ対応には、実装者だけでは決められない挙動と検証上の穴があります。

特に、新しいインライン show 宣言が placement 引数を持たず、改訂で追加した「show 引数 > 添付 DSL」の契約を表現できない点は、実装開始前の解決が必要です。

## 指摘事項

### [🟠 Major] 新しい show 宣言が placement 上書きを表現できない

**該当箇所**:

- [design.md:67](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:67)
- [design.md:89](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:89)
- [design.md:94](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:94)
- [design.md:109](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:109)
- [design.md:131](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:131)
- [dialog-contract/spec.md:47](kasane/changes/archive/2026-08-19-expand-api-surface/specs/dialog-contract/spec.md:47)

**問題点**: 改訂後の共通契約は「show 引数の placement が添付 DSL より優先」と要求しています。しかし Decision 6 の iOS・Android View/Compose・MAUI のインライン show、および KMP Swift 直接 show の宣言には placement 引数がありません。

このままでは、未登録 VM のインライン表示で ADR-0015 の正規供給点から動的 placement を渡せません。factory が外部値を capture して添付する回避策は可能ですが、「show 引数 > コンテンツ添付」という公開契約とは別の API になります。

**推奨修正**: 各インライン／Swift 直接 show 宣言に nullable・省略可能な placement 引数を追加し、引数順序とラベルを確定してください。登録済み show とインライン show の双方について、添付 A に対して show 引数 B がオブジェクト単位で勝つ compile/runtime Scenario を置いてください。

### [🟠 Major] SwiftUI の Preference 未到達時の状態遷移が定義されていない

**該当箇所**:

- [design.md:164](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:164)
- [design.md:165](kasane/changes/archive/2026-08-19-expand-api-surface/design.md:165)
- [ios-native/spec.md:40](kasane/changes/archive/2026-08-19-expand-api-surface/specs/ios-native/spec.md:40)
- [layout-semantics.md:86](kasane/concepts/core/api/layout-semantics.md:86)

**問題点**: Decision 10 は「preference が未着なら到達を待つ」としますが、次が決まっていません。

- DSL を付けていない正常なコンテンツと、DSL 値の到達が遅れているコンテンツをどう区別するか
- preference が永久に来ない場合の終了条件、既定値採用、throw、キャンセルのいずれにするか
- 一度 on-window のレイアウトパスが完了したあと再レイアウトで値を採用する場合、それを「初回ネイティブレイアウトパス完了時点」とどう整合させるか

添付なし Scenario を満たすため即座に既定値を採れば「遅延到達を待つ」が崩れ、常に待てば添付なしの表示が停止し得ます。

**推奨修正**: preference 解決の状態機械を設計として確定してください。少なくとも「添付なし」「初回パス中の遅延到達」「到達しない」の3 Scenarioを追加し、待機上限と終了結果を定める必要があります。実際のダイアログコンテナへ載せる前に staging host で収束させるなら、その境界も明記してください。

### [🟠 Major] DialogOptions の DSL 輸送が Scenario では検証できない

**該当箇所**:

- [dialog-contract/spec.md:42](kasane/changes/archive/2026-08-19-expand-api-surface/specs/dialog-contract/spec.md:42)
- [ios-native/spec.md:35](kasane/changes/archive/2026-08-19-expand-api-surface/specs/ios-native/spec.md:35)
- [android-native/spec.md:40](kasane/changes/archive/2026-08-19-expand-api-surface/specs/android-native/spec.md:40)
- [layout-semantics.md:39](kasane/concepts/core/api/layout-semantics.md:39)
- [tasks.md:17](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:17)
- [tasks.md:24](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:24)

**問題点**: 追加された Scenario と tasks は「配置結果」「ケース表適合」を中心にしています。しかし `DialogOptions` には rect だけでは判定できない `overlayColor` と `isCanceledOnTouchOutside` があります。

したがって、DSL が placement やサイズ関連属性だけを運び、overlayColor／外側タップ設定を落とす実装でも、現在の明示的な受け入れ条件を通過できます。提示後変更の非反映も、配置だけ確認すれば同様に空振りします。

**推奨修正**: iOS・Android 両方に、少なくとも次の観察可能な Scenario を追加してください。

- DSL で非既定 overlayColor と `isCanceledOnTouchOutside=false` を与え、覆いと外側タップ結果の双方を確認する
- スナップショット後に同じ2値を変更しても、覆い・外側タップ挙動が変わらない
- KMP SwiftUI 経路でも、commonMain の show placement が SwiftUI DSL の placement に勝つ

### [🟠 Major] 読み上げ契約の期待値と検査方法が一意でない

**該当箇所**:

- [samples/spec.md:27](kasane/changes/archive/2026-08-19-expand-api-surface/specs/samples/spec.md:27)
- [samples/spec.md:31](kasane/changes/archive/2026-08-19-expand-api-surface/specs/samples/spec.md:31)
- [tasks.md:44](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:44)
- [tasks.md:45](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:45)
- [tasks.md:50](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:50)

**問題点**:

- 「読み上げ名は画面文言と同一」と「同名の Start 等を所属行で区別」の両立方法が未確定です。子の名前を `Horizontal Start` にすれば前者を外れ、親グループの文脈に頼る場合は OS ごとの読み上げ順に依存します。
- 選択肢の現在の選択状態、トグルの ON/OFF、入力欄の現在値が受け入れ条件にありません。名前と役割だけ付けても、どの配置が選択中か分からない実装が合格します。
- Scenario の「accessibility 検査」の方法が未定義です。視覚照合は明示的に対象外で、Sample 通しにも VoiceOver／TalkBack や accessibility tree の確認が含まれていません。

**推奨修正**: 操作部ごとに「名前・文脈・役割・状態・値」の期待表を設けてください。重複名は、複合名にするか親グループの読み上げを契約にするかを決定する必要があります。さらに各実行形態について accessibility tree の自動検査、または VoiceOver／TalkBack の実機確認と証跡記録を tasks へ追加してください。

### [🟠 Major] 新しい MAUI 負コンパイル検査が完了検証から漏れる

**該当箇所**:

- [maui-binding/spec.md:16](kasane/changes/archive/2026-08-19-expand-api-surface/specs/maui-binding/spec.md:16)
- [tasks.md:28](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:28)
- [tasks.md:49](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:49)
- [test-execution.md:109](kasane/concepts/cross/conventions/test-execution.md:109)
- [test-execution.md:132](kasane/concepts/cross/conventions/test-execution.md:132)

**問題点**: 参照先は現行方式へ直りましたが、規約に列挙された負検査は現在16本で、新しい「bool 既定オーバーロードへカスタム結果 VM を渡す」禁止形状は含まれていません。負検査はフラグなしの全件テストでは1本も実行されないため、tasks 7.1 の「全件テスト通過」ではこの Scenario を検証できません。

現状の tasks は検査を「書く」としか要求せず、個別フラグで期待どおり失敗することや、規約表を17本へ更新する作業がありません。

**推奨修正**: 新フラグ名と期待診断を確定し、`test-execution.md` の件数・表・実行方法を更新するタスクを追加してください。検証タスクでも、そのフラグを単独実行して期待した診断1件で失敗することを必須にしてください。

### [🟡 Minor] MAUI スナップショット実機確認タスクが解決済み証跡と重複している

**該当箇所**:

- [tasks.md:51](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:51)
- [maui-snapshot-wiring/notes.md:1](kasane/changes/archive/2026-08-19-add-layout-spec/verification/maui-snapshot-wiring/notes.md:1)
- [review-004.md:44](kasane/changes/archive/2026-08-19-add-layout-spec/review-004.md:44)

**問題点**: tasks 7.3 は add-layout-spec review-003 の未解決申し送りとして実機確認を要求していますが、アーカイブ済み change では review-004 前に iOS Simulator・Android 実機の双方で確認済みです。証跡も残り、review-004 で解消判定されています。

**推奨修正**: tasks 7.3 を削除し既存証跡を参照するか、本変更による回帰確認が必要なら、その理由・対象プラットフォーム・期待観察を新しい回帰タスクとして書き直してください。

### [🟡 Minor] layout-semantics の「未提供」が実装後も残る可能性がある

**該当箇所**:

- [layout-semantics.md:75](kasane/concepts/core/api/layout-semantics.md:75)
- [tasks.md:10](kasane/changes/archive/2026-08-19-expand-api-surface/tasks.md:10)

**問題点**: 現在の長命概念文書は「宣言的 UI 向け添付イディオムはまだ提供していない」と明記しています。task 1.1 は共通契約の文書化を要求していますが、この既存記述の更新先を明示していないため、新 DSL 実装後も正とされる概念文書が古いまま残る可能性があります。

**推奨修正**: task 1.1 に `layout-semantics.md` の添付面一覧・未提供記述・KMP 境界説明の更新を明記してください。

## 問題なしと確認した観点

- core/ADR-0014 の属性取捨、および core/ADR-0015 の `DialogOptions`／`DialogPlacement` 分割は改訂内容と整合しています。
- SwiftUI／Compose DSL の公開名と `ksdialogs-compose` への配置は ADR-0015 と一致しています。
- KMP commonMain に `DialogOptions` を公開せず、SwiftUI options を iOS View 定義側で完結させる方針は問題ありません。
- UI リファレンス2点は、アーカイブ済み add-layout-spec の最終承認画像と SHA-256 が一致しており、鮮度注記は正確です。
- 先行ラウンドのホスト lifecycle、KMP キャンセル、ABI 公開・利用者非公開、インライン非干渉、MAUI 4呼び出し形式の修正は維持されています。
- proposal の前提「add-layout-spec 完了・アーカイブ済み」は現状と一致しています。
- 静的レビュー制約に従い、ビルド・テストは実行していません。

## アクションプラン

1. 新しい全 show 宣言へ placement をどう載せるか確定する。
2. SwiftUI preference の「未添付／遅延／未到達」の状態遷移を決定する。
3. DialogOptions の非レイアウト属性と KMP override 経路を Scenario 化する。
4. accessibility の期待表と実機・自動検査方式を確定する。
5. MAUI 負検査を規約・実行タスクへ組み込み、解決済みのスナップショットタスクを整理する。

**総合判定: NEEDS_DISCUSSION**

## 突き合わせ結果 (2026-08-19、ホスト判定)

ホスト側自己レビュー (2周、指摘3件は反映済み) との重複なし。全7件が相方のみの指摘で、実物で裏取りのうえ判定した。

| # | 指摘 | 採否 | 裏取り / 反映 |
|---|---|---|---|
| Major 1 | インライン/Swift show 宣言に placement 引数がない | **採用** | 登録済み show は3形態とも placement 引数を実装済みと実測 (ios/KsDialogs.swift:17 ほか)。Decision 6 の全インライン / KMP Swift show 宣言へ省略可能な placement を追加、dialog-contract にオブジェクト置換 + インライン成立の Scenario 追加 |
| Major 2 | SwiftUI preference 未到達時の状態遷移が未定義 | **採用** | Decision 10 を3終了状態 (同期解決 / 追加パス1回上限の遅延到達 / 上限到達で既定値 + 警告) に確定。ios spec に「添付なしは遅延なし」「上限到達で既定値 + 警告」Scenario 追加 |
| Major 3 | DialogOptions の非レイアウト属性が Scenario で検証不能 | **採用** | ios/android に overlayColor + isCanceledOnTouchOutside の Scenario、提示後変更 Scenario の THEN を覆い・外側タップまで拡張、kmp に commonMain placement 優先 Scenario 追加 |
| Major 4 | 読み上げ契約の期待値と検査方法が一意でない | **採用** | 複合名規則 (行文言 + 選択肢文言) に確定、状態・現在値の読み上げを要件化、検査方法 (accessibility tree / 実機スクリーンリーダー + 証跡4ルート) を spec と tasks 6.4 に明記 |
| Major 5 | MAUI 負検査が完了検証から漏れる | **採用** | tasks 4.1 に「新フラグ単独実行で期待診断1件の失敗確認 + test-execution.md の表・件数更新」を明記 |
| Minor 1 | MAUI 実機確認タスクは解決済み証跡と重複 | **採用** | archive の verification/maui-snapshot-wiring/notes.md と review-004 #4 で解消済みを確認 — agenda 申し送りが古かった。tasks 7.3 を撤回し agenda を訂正 |
| Minor 2 | layout-semantics の「未提供」記述が残置リスク | **採用** | tasks 1.1 に layout-semantics.md の更新 (添付面の表 + 未提供記述の削除) を明記 |

採用 7 / 降格 0 / 未解決 0。NEEDS_DISCUSSION の主因 (Major 1・2・4 の設計判断) はホスト側で確定して反映済み — 判断3点 (placement 引数追加・preference 上限1回で既定値 + 警告・読み上げ複合名) はオーナーへ提示して確認を得る。
