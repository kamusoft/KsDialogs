# 日本語版 Skill 初見レビュー 001

## レビュー条件

- 対象は `skills/ja/ksdialogs-ios/**`、`skills/ja/ksdialogs-android/**`、`skills/ja/ksdialogs-maui/**`、`skills/ja/ksdialogs-kmp/**`、`skills/ja/ksdialogs-aiforms-migration/**` の `SKILL.md` と `references/**` に限定した。
- concepts、実装コード、tests、samples、英語版 Skill、change 資料、既存 review、manifest、README は参照していない。
- 技術仕様の外部検証は行わず、各文書群だけを初めて読む利用者が、導入から目的達成まで進めるかを評価した。
- Markdown リンクは、5 Skill ともリンク先が同じ Skill 内に存在し、リンク元の目的に合っていることを確認した。

## 1. `ksdialogs-ios`

**判定: APPROVED**

`SKILL.md` の能力マップと「レシピを選ぶ」から、Dialog、ViewModel、レイアウト、トランジション、Loading、Toast の各 reference へ迷わず移動できる。Swift Package の追加条件、最小例、登録済み・インラインの各経路、型付き結果、Loading の命令形・スコープ形、Toast の寿命と失敗時の挙動まで、この文書群だけで目的別の実装手順を理解できる。リンク切れ、宙に浮いた参照、目的達成を妨げる未説明事項は見つからなかった。

**指摘なし。**

## 2. `ksdialogs-android`

**判定: CHANGES_REQUESTED**

### A-1. 基本 artifact の `<version>` を決める手掛かりがない

- 該当: `skills/ja/ksdialogs-android/SKILL.md`（セットアップ、特に依存宣言）
- 問題: 基本 artifact と Compose artifact がどちらも `<version>` のままで、利用可能な版の調べ方、固定すべき版、または version catalog へ置き換えるための情報がない。
- 利用者が詰まる理由: 初見利用者はコードをそのまま Gradle に追加できず、セットアップの最初の段階で有効な依存宣言を作れない。
- 最小限の改善案: 現在利用できる版を明記するか、版を取得する正式な場所へのリンクと、`<version>` をその版へ置換する旨を1文追加する。

### A-2. View factory 内の `this` が何を指すか説明されていない

- 該当: `skills/ja/ksdialogs-android/references/dialogs.md`、`skills/ja/ksdialogs-android/references/view-models.md`、`skills/ja/ksdialogs-android/references/layout.md`、`skills/ja/ksdialogs-android/references/transitions.md`、`skills/ja/ksdialogs-android/references/toast.md`
- 問題: 複数の View レシピが `Button(this)` または `TextView(this)` を使うが、factory lambda の receiver が Android `Context` であり、そこでの `this` がその `Context` を表すことを文書内で説明していない。
- 利用者が詰まる理由: top-level 関数や `object` 内のコードとして読んだ場合、通常の `this` と見分けられず、どの `Context` を渡せばよいか、別の場所へレシピを移す際にどう書き換えるか判断できない。
- 最小限の改善案: 最初の View factory レシピの直前に「factory は `Context` receiver を持ち、`this` はその `Context`」と明記し、以後の View レシピから参照できるようにする。

## 3. `ksdialogs-maui`

**判定: CHANGES_REQUESTED**

### M-1. 非同期 configure の例が文書単体では自己完結していない

- 該当: `skills/ja/ksdialogs-maui/references/view-models.md` の「表示前に非同期で configure する」
- 問題: コードブロックは `using KsDialogs;` しか示していない一方、`Button` と `Command` を使用する。これらの型の namespace は同じ文書内の直前の例では `Microsoft.Maui.Controls` として示されているが、この完結したコードブロックには含まれない。
- 利用者が詰まる理由: 暗黙またはプロジェクト固有の global using を前提にしてよいか文書から判断できず、単独で転記した例が型解決で止まり得る。
- 最小限の改善案: 当該コードブロックへ `using Microsoft.Maui.Controls;` を追加する。

### M-2. custom Loading の進捗レシピが UI 更新まで到達しない

- 該当: `skills/ja/ksdialogs-maui/references/loading.md` の「custom Loading content を登録する」
- 問題: `SyncLoadingViewModel.OnProgress` は `Progress` を更新するが変更通知を行わず、登録する `Label` も `Progress` を binding または表示していない。レシピを実行しても、報告された進捗が custom content に見える形で反映されない。
- 利用者が詰まる理由: `ILoadingProgressReceiver` を実装した後、MAUI View をどう更新すればよいかがこの文書群だけでは分からず、レシピの主目的である custom content での進捗表示を達成できない。
- 最小限の改善案: `Progress` の変更通知を実装し、`ProgressBar.Progress` などを binding する最小例に置き換える。単純化する場合でも、UI thread 上で ViewModel の変更を画面へ通知する必要があることを明記する。

## 4. `ksdialogs-kmp`

**判定: CHANGES_REQUESTED**

### K-1. 未配布 artifact しか示されず、現在実行できる Setup 経路がない

- 該当: `skills/ja/ksdialogs-kmp/SKILL.md` の「Setup」と「共有 module」、`skills/ja/ksdialogs-kmp/references/android-host.md` の Compose artifact 追加
- 問題: 文書自身が Maven Central での配布は未開始と明記する一方、導入手順は予定 coordinate と `<version>` を追加する方法だけで、現在取得可能な代替経路を示していない。
- 利用者が詰まる理由: 共有 module が依存を解決できないため、commonMain の最小コードにも Android/iOS host 登録にも進めない。iOS の `integrateLinkagePackage` も、その依存を導入できない状態では完動手順にならない。
- 最小限の改善案: 配布開始までは Skill を「利用不可」と明確に位置付けるか、利用者が再現できるローカル Maven／composite build 等の正式な暫定導入手順と具体的な version を追加する。配布後は予定表記と `<version>` を実在する版へ更新する。

## 5. `ksdialogs-aiforms-migration`

**判定: APPROVED**

導入と最小移行から API 対応表へ直接移動でき、Dialog、ViewModel、再利用 Dialog/Loading、LoadingConfig、ExtraView、DialogView、LoadingView、Toast を旧メンバー単位で追える。「直接の対応先なし」にも代替方針があり、特に共有 Loading を `HideAsync` へ機械置換してはいけない理由と `StartAsync` を選ぶ次手が明記されている。新 API の実装詳細が必要な箇所は、同じ配布物にある Skill 名 `ksdialogs-maui` を一貫して指定しており、参照先を識別できる。リンク切れや目的達成を妨げる未説明事項は見つからなかった。

**指摘なし。**

## 全体判定

**CHANGES_REQUESTED**

- APPROVED: 2 Skill（`ksdialogs-ios`、`ksdialogs-aiforms-migration`）
- CHANGES_REQUESTED: 3 Skill（`ksdialogs-android`、`ksdialogs-maui`、`ksdialogs-kmp`）
- 指摘件数: 5件

Android は依存 version と View factory receiver、MAUI は自己完結したコード例と進捗 UI 更新、KMP は取得可能な artifact 導入経路を補えば、初見利用者が文書群だけで導入から目的達成へ進める。
