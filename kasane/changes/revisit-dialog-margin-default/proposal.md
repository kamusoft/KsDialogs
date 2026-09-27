# Proposal: revisit-dialog-margin-default

## Why

ダイアログの余白 (`dialogMargin`) の契約既定値は全辺 24 で、原典 (0) からの意図的乖離として core/ADR-0008 が定めている。add-page-layout-area の Sample 撮影証跡で、カードが基準領域の端から 24 離れて出るため、配置と基準領域の効き方をぱっと見で確かめにくいとオーナーが指摘した (同 change は Layout Dialog パネルにだけ全辺 0 を明示して回避)。

数値属性の既定値のうち余白だけが器の側から値を足している。24 を選んだ理由 (契約導入前の実装の見た目を保つ) も、beta を公開した今は守る相手が変わっている。探索 (exploration.md) で、余白の契約既定値を 0 に戻すと決めた。利用者が余白を渡せない既定 Toast のピルだけは、従来の 24 を自分で持つ (core/ADR-0039 proposed、0008 を amends)。

## What Changes

- **dialog-contract (core)**: 余白の契約既定値を全辺 0 にする。Dialog・Loading (既定ローディングを含む)・カスタム Toast に効く。非有限値の一般規則 (その値の既定値) はそのままで、NaN / ±Infinity の辺は 0 になる。既定 Toast (メッセージだけで出すデフォルト View) は、ライブラリがデフォルト View に余白 全辺 24 を添付する。show の配置引数・ToastStyle のアプリ既定配置・契約既定配置のどれで置いても効く。共通ケース表 `core/layout-spec/cases.json` の既定値の説明と期待値・note を直す
- **ios-native / android-native**: 契約既定値 (`DialogOptions`) を差し替える。Android で NaN のときに戻る余白を契約とは別に定義している箇所 (`DialogLayout.kt` の `DEFAULT_MARGIN`) を、iOS と同じく契約既定値から引く形にそろえる。デフォルト View を作る箇所で余白 24 を添付する
- **maui-binding**: C# の `DialogOptions.DialogMargin` の既定値と、Android bridge (`MauiDialogAttributes.kt`) に直書きされた 24 を 0 にする。iOS bridge と添付プロパティは契約既定値から引くので追従する。実機検証ホスト (PlacementHost) の余白定数も 0 にする。デフォルト Toast の中身は Native が持つので、MAUI 側の Toast は変えない
- **KMP**: 変更なし (commonMain に余白が無く、既定 Toast は Native へ委譲している)
- **テスト**: 既定値 24 を前提にした期待値を直す (iOS 7・Android unit 1 と instrumented 3・MAUI 1・PlacementHost 2 のファイル)。既定 Toast の余白の Scenario テストを両 Native に足す
- **samples (UI 変更)**: 4 ルートの Layout Dialog パネル (属性調整パネル) に `Margin` の行を足し、全辺そろえの余白を選んで出せるようにする。初期値は契約の既定値 (0) で、パネルの状態を ViewModel に載せて登録した View factory が中身に添付する (基準領域と同じ経路)。これで各ルートの登録にある全辺 0 の明示は、パネルの値の添付に置き換わる。余白はプリセットのセグメント `0` / `24` / `48` で選ぶ (`ui/mock/mock-a.html` を 2026-09-27 に承認)

## 実現経路と確認先 (lessons spec-review L-001 / L-002)

| 対象 | 経路と確認先 |
|---|---|
| 既定 Toast の中身 (Native) | Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:196-197` (`ToastContentRequest.Builtin` → `ToastDefaultContentView`)、iOS `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:265-270` (`.builtin`) |
| Toast の実効値の読み方 | 中身の添付 (`ksDialogOptions`) を読み、無ければ `DialogOptions()` を使う (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastLayoutSnapshot.kt:26` / `ios/Sources/KsDialogs/Presentation/ToastContainerViewController.swift:235`)。配置の供給 (show 引数 > 添付 > ToastStyle > 契約既定) とは別に読まれるので、デフォルト View への添付はどの配置でも効く |
| MAUI の既定 Toast | 中身は Native が持つ (`maui/KsDialogs.Maui/Internals/ToastGateway.cs:70`) |
| KMP の既定 Toast | iOS `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosToastGateway.kt:26` (`bridge.showMessage`)、Android `kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidToastGateway.kt:15` (`native.show(message, …)`) |
| 契約既定値 | iOS `ios/Sources/KsDialogs/Contract/DialogOptions.swift:37`、Android `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt:28`、MAUI `maui/KsDialogs.Maui/Contract/DialogOptions.cs:30`、MAUI Android bridge `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogAttributes.kt:51-60` |
| 既定ローディング | 一括設定の既定が `DialogOptions()` (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingSettings.kt:13` / `ios/Sources/KsDialogs/Presentation/LoadingSettings.swift:11`)。公開口は `Loading` の `options` なので、契約既定値の差し替えに追従する |

## Non-Goals

- **concepts の追随** (`core/api/layout-semantics.md`・`core/architecture/layout-case-table.md`・`core/api/toast-semantics.md`): 長命層の更新は蒸留 (ksn-distill) で行う
- **skills と README の追随**: `skills/` の更新は docs-refresh 経由のみ (CLAUDE.md)。移行スキルの「変わった既定値」は 3 件から 2 件になる
- **アプリ全体の余白の既定を戻す切り替え口**: 作らない (探索の決定。24 が欲しい利用者は中身ごとに添付する)
- **既定 Toast に余白を渡す口** (ToastStyle の項目など): 足さない。足すときは core/ADR-0039 の見直しのきっかけになる
- **Custom Toast メニューのオフセットの見直し** (Sample): 2 枚とも 24 下がるが間隔は変わらないので直さない
- **fix-layout-contract-gaps の非有限値の適用順**: 別 change。ただし余白の既定が 0 になると -Infinity の辺は「既定へ戻す」「下限 0」のどちらで読んでも 0 になるので、同 change の論点のうち余白の分はこの change で解消する (比率の +Infinity は残る)

## Impact

- **見た目の破壊的変更**: 公開済みの beta (0.1.0-beta.2) で余白を指定していない Dialog・Loading・カスタム Toast は、先頭寄せ・末尾寄せの位置とクランプの結果が変わる (中央配置で内容が収まっていれば変わらない)。既定 Toast は変わらない。コンパイル互換は保つ。リリース pull request の `## Changes` に書く
- **accepted ADR**: core/ADR-0008 の余白の既定値を core/ADR-0039 (proposed) で amends する。蒸留時に 0039 を accepted にし、0008 に `amended-by`、index に「一部改訂: 0039」を書く
- **handbook の規約変更**: `kasane/handbook/cross/sample-parity.md` の「Layout Dialog の属性調整パネル」節を書き換える。文言表に `Margin` の行と選択肢 (または初期値) を足し、画面の構成の表に行の置き方を足す。`:114` の「dialogMargin を全辺 0 にして添付する」は「パネルの余白の値を添付する (初期値は契約の既定値 0)」に改める。「パネル操作部の読み上げ」節にも余白の行を足す。handbook の更新は蒸留時に行い、この change の実装は承認モックと ui/brief.md の文言表に合わせる
- **UI**: Sample の画面が変わるので `ui/` (brief・モック 2 案・承認済み `approved.png`) を持つ。承認モックが実装の見た目の正
- **リスク**: Android の Toast / Loading の instrumented テストが、数値 24 を書かずに既定値へ暗黙に依存している可能性がある (未精査)。実装の最初に全テストを走らせて洗い出す

## 級: M

3 形態の公開 API の既定値の変更と accepted ADR の改訂を伴う。能力はレイアウト契約 1 つで、新しい設計判断は既定 Toast の余白の持たせ方に限られる。

domain: core
