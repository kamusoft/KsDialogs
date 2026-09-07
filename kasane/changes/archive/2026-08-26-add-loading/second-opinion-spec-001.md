# セカンドオピニオン: add-loading (spec-001)
**相方**: codex / **label**: so-spec-add-loading / **日付**: 2026-08-25 / **対象**: 提案一式 (kasane/changes/add-loading/ の proposal / design / specs 6本 / tasks / ui)
---
# レビュー結果: add-loading

**日付**: 2026-08-25  
**判定**: **NEEDS_DISCUSSION**

## サマリー

Loading の基本方針は、オーナー合意済みの core/ADR-0022〜0024・agenda と概ね整合しています。しかし、Android の表示階層に成立しない実装案が1件あり、合流状態の世代管理、失敗時の action 実行、完了タイミング、複数入口の状態共有にも未決契約があります。このまま実装へ進むと実装者判断で公開挙動が固定されるため、提案段階での修正が必要です。

指摘件数: Critical 1 / Major 7 / Minor 3 / Suggestion 0

## 指摘事項

### [🔴 Critical] Android の decorView 直貼りでは既存 Dialog より手前に表示できない

**該当箇所**: `design.md:44`, `design.md:47`, `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:40`, `kasane/decisions/core/0022-loading-separate-container-shared-parts.md:15`

**問題点**: Android の既存 `DialogContainer` は `android.app.Dialog`、すなわち Activity とは別の Window に表示されます。Activity の `decorView` に追加した子 View はその Dialog Window より背面にあるため、「ダイアログより手前に出る」「背後の操作を遮断する」を満たせません。Decision 4 の代替案Aを却下する根拠である「直貼りで要件を満たせる」が、現行コード構造と矛盾しています。

**推奨修正**: Android について、既存 Dialog Window より上に配置でき、かつ Dialog の多段表示意味論には参加しない Loading 専用の Window 戦略を再設計してください。実装前に「Dialog 表示中に Loading を開始し、Loading が視覚・入力の双方で最前面になる」instrumented Scenario を追加し、技術的成立性を確認してください。

### [🟠 Major] hide 後の旧スコープが新しい表示を壊す世代管理が未定義

**該当箇所**: `specs/dialog-contract/spec.md:11`, `specs/dialog-contract/spec.md:40`

**問題点**: `hide()` 後も action は継続しますが、その後に新しい Loading が開始された場合の扱いがありません。例えば A を start → hide → B を start → A が完了、という順序では、単純な合流カウント実装だと A の終了が B のカウントを減らして閉じてしまいます。A から遅れて届く進捗やメッセージが B を更新する危険もあります。LD-CO-06 は「再出現しない」しか検証していません。

**推奨修正**: 表示世代を契約化してください。`hide()` は現世代を無効化し、旧世代の完了・進捗・メッセージは新世代のカウントや表示へ影響しない、と明記したうえで、次の Scenario を追加してください。

- hide 後に新規 start し、旧 action が先に完了しても新表示が閉じない
- 旧 action の遅延進捗が新表示へ届かない
- 旧 action 自体の戻り値・失敗は元の呼び出し元へ通常どおり返る

### [🟠 Major] show / hide / start の完了時点と演出中の再入が決まっていない

**該当箇所**: `specs/dialog-contract/spec.md:9`, `specs/dialog-contract/spec.md:20`, `specs/dialog-contract/spec.md:171`, `specs/maui-binding/spec.md:9`

**問題点**: `hide は即閉じ` が「退出開始」なのか「View 撤去完了まで待つ」のか不明です。start も action 完了後、退出演出と覆いの消滅を待ってから戻るのか決まっていません。show が出現演出完了を待つか、退出中に再度 show された場合に旧退出を打ち切るか待つかも未定義です。特に MAUI は `HideAsync` という名前なので、実装ごとの差が公開挙動になります。

**推奨修正**: 各 API の完了条件を明記してください。少なくとも以下を Scenario 化する必要があります。

- 最後の start は action 完了後、器の撤去まで待って戻るか
- 合流中の非最終 start は自身の action 完了時点で戻るか
- hide の戻り時点で操作ブロックが解除済みか
- 出現中の hide、退出中の show/start の状態遷移

### [🟠 Major] 提示失敗と「action は必ず実行」の優先関係が未定義

**該当箇所**: `specs/dialog-contract/spec.md:11`, `specs/dialog-contract/spec.md:156`, `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:50`, `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ActivityDialogPresentationSurface.kt:18`

**問題点**: 現行 Dialog は提示先不在で失敗します。カスタム Loading には未登録 VM という構成失敗もあります。一方、Loading の action は表示状態によらず必ず実行すると規定されています。提示やレジストリ解決を action より先に行うとこの保証に反し、action を先に実行すると表示失敗をどのチャネルで返すか、action も失敗した場合の優先順位が決まりません。

**推奨修正**: start の失敗モデルを明文化してください。提示先不在、未登録、factory 失敗のそれぞれについて、action の実行有無、action と表示の開始順、戻り値／例外の優先順位を Scenario で固定してください。オーナー合意済みの「action は必ず実行」を維持するなら、表示失敗が action を抑止しない設計が必要です。

### [🟠 Major] singleton・DI・MAUI・KMP 間で合流状態を共有する保証がない

**該当箇所**: `design.md:19`, `design.md:44`, `kasane/decisions/core/0002-public-api-shape.md:17`, `kasane/decisions/core/0002-public-api-shape.md:33`

**問題点**: core/ADR-0002 により具体的な契約実装は DI 用に構築可能である必要がありますが、design は「シングルトン側が合流状態を保持する」としか定めていません。複数の `Loading` インスタンスや、Native・MAUI bridge・KMP facade が別々の状態を持つと、同一 OS 上に複数の Loading が表示され、ADR-0024 の単一表示契約が破れます。

**推奨修正**: 1 OS プロセス内で共有する Native coordinator を状態の唯一の正とし、既定 singleton、構築した契約実装、MAUI bridge、KMP actual のすべてがそこへ委譲することを design/spec に明記してください。異なる2入口から重ねた利用が1表示へ合流する Scenario も必要です。

### [🟠 Major] KMP のカスタム Loading 公開経路と iOS 登録面が欠落している

**該当箇所**: `specs/kmp-facade/spec.md:9`, `specs/samples/spec.md:18`, `tasks.md:42`, `kasane/decisions/kmp/0003-swift-facing-registration-in-swift-package.md:21`, `kasane/decisions/kmp/0004-swift-facing-typed-generic-facade.md:20`

**問題点**: KMP spec は「共有 VM 型をキーに OS 側で登録して共有コードから表示できる」としますが、commonMain の VM 契約、カスタム用 show/start overload、進捗受け口の共通型が定義されていません。さらに iOS KMP の利用者向け登録面は既存 ADR により Swift パッケージ側に置く必要がありますが、Loading 用 Swift facade・interop・API 検査のタスクがありません。現状の tasks 5.1 は `KsLoading` と gateway だけです。

**推奨修正**: 次を設計・spec・tasks に追加してください。

- commonMain のカスタム Loading VM 契約と進捗受け口
- VM を受ける show/start の正確なシグネチャ
- Android actual の Native 型同一性
- iOS Swift パッケージ側の型付き登録・表示面と内部 interop
- KMP iOS Sample が機械面を直接使用していないことの API／Sample 検査

### [🟠 Major] 既定ローディングへ DialogOptions を供給する経路が閉じていない

**該当箇所**: `specs/dialog-contract/spec.md:62`, `design.md:60`, `kasane/decisions/core/0023-default-loading-builtin-content.md:21`

**問題点**: 合意済み ADR は overlayColor を既存 `DialogOptions` で扱うとしていますが、既定ローディングには利用者が属性を添付する View がなく、表示 API から渡せるのも placement だけです。そのため既定ローディングでは overlayColor を設定する経路がありません。layoutArea、margin、比率についても同様に契約既定値へ固定されるのか明記されていません。

**推奨修正**: 新しい重複属性を作らない判断は維持したまま、既定コンテンツに既存 `DialogOptions` を供給する公開経路を決めてください。例えば Loading 単位の設定スナップショットとして既存型を再利用する案が考えられます。固定値とする項目があるなら明記し、overlayColor の変更を観察する Scenario を追加してください。

### [🟠 Major] 「後勝ち」の直列化規則と呼び出しスレッド契約が曖昧

**該当箇所**: `specs/dialog-contract/spec.md:11`, `specs/dialog-contract/spec.md:86`, `specs/ios-native/spec.md:9`, `ios/Sources/KsDialogs/Presentation/KsDialogs.swift:13`

**問題点**: 並行 action から進捗やメッセージが報告された場合、「最新」を何の順序で決めるかがありません。また iOS spec の「API は MainActor 保証を Dialog 面と揃える」は、任意スレッドから呼び出せる現行 Dialog 契約と矛盾または少なくとも曖昧です。進捗受け口が UI executor 上で呼ばれる保証もなく、VM が直接 UI 状態を更新する設計では事故になります。

**推奨修正**: 全操作をどの executor へ直列化するか、後勝ちを coordinator が受理した順とするか、VM 受け口と formatter をどのスレッドで呼ぶかを規定してください。iOS は「呼び出し側を `@MainActor` に制限する」のか「内部で MainActor へ移す」のかを明確にし、Android／MAUI／KMP も同じ意味へ写してください。

### [🟡 Minor] LD-CV-05 の「削除」は操作されず、既存レジストリにも削除 API がない

**該当箇所**: `specs/dialog-contract/spec.md:161`

**問題点**: Scenario の THEN は「片方の再登録・削除が他方に影響しない」としますが、WHEN では再登録も削除も行っていません。また現行 Dialog レジストリには公開削除 API がありません。このままでは削除についてテストで判定できません。

**推奨修正**: 削除 API を追加しないなら「削除」を Scenario から除き、再登録を実際に WHEN で行う独立 Scenario にしてください。削除を契約化するなら公開 API・エラー意味論・tasks を追加してください。

### [🟡 Minor] LD 系だけをミラー必須にする指示を現行検査構造では表現できない

**該当箇所**: `design.md:52`, `tasks.md:53`, `scripts/scenario-id-coverage.py:74`

**問題点**: 現行 `MIRROR_AREAS` は ID の第2要素だけを判定し、prefix を見ません。「LD 系領域を追加」すると、CO/AT/PR/ST/CV など同じ領域名を持つ将来の別 prefix にもミラー制約がかかります。

**推奨修正**: ミラー条件を `(prefix, area)` で持てるよう検査基盤の変更を task に明記するか、全 prefix へ適用することを意図した変更として対象領域を明示してください。

### [🟡 Minor] UI brief に生カラー値が残っている

**該当箇所**: `ui/brief.md:7`

**問題点**: `黒40%` は ui/brief の禁止事項である生カラー値です。既存 `DialogOptions.overlayColor` の既定値の再掲でも、brief では長命層のトークン／契約へ参照する規律です。

**推奨修正**: 値を削除し、`kasane/concepts/core/api/layout-semantics.md` の overlayColor 既定値への参照に置き換えてください。

## アクションプラン

1. Android の表示階層を成立性調査し、Decision 4 の Android 案を修正する。
2. Loading coordinator の状態機械を定義する。特に世代、hide、退出中の再入、旧進捗の無効化、API 完了時点を固定する。
3. 提示失敗時にも action 必須実行を守れる失敗モデルと、全入口で共有する coordinator の境界を決める。
4. KMP iOS の公開登録経路、既定 Loading の `DialogOptions` 供給経路、スレッド規則を補完する。
5. 追加した決定ごとに Scenario と tasks を対応させ、Minor 3件を修正してから再レビューする。

ユーザー指定どおり、ビルド・テスト実行およびレビュー結果ファイルの作成は行っていません。



## 突き合わせ結果 (2026-08-25)

ホスト側自己レビュー (2周、指摘なしで通過) との突き合わせ。全11件が「相方のみ」の指摘で、いずれも該当箇所の特定と実害シナリオを伴う根拠強と判定した。

| # | 指摘 | 採否 | 扱い |
|---|---|---|---|
| C1 | Android decorView 直貼りは既存 Dialog Window より背面 | **採用** | design Decision 4 の Android 案を専用 Window 方式に修正。成立性 Scenario 追加 (反映方針はオーナー確認対象) |
| M1 | hide 後の旧スコープの世代管理が未定義 | **採用** | 表示世代を契約化し Scenario 追加 |
| M2 | show / hide / start の完了時点・演出中の再入が未定義 | **採用** | 完了条件を契約化し Scenario 追加 (反映方針はオーナー確認対象) |
| M3 | 提示失敗と「action は必ず実行」の優先関係が未定義 | **採用** | 失敗モデルを契約化 (反映方針はオーナー確認対象) |
| M4 | 入口間 (singleton / DI / bridge / KMP) の合流状態共有の保証がない | **採用** | Native coordinator を唯一の正として design / spec に明記、合流 Scenario 追加 |
| M5 | KMP カスタム Loading の公開経路・iOS Swift facade 登録面の欠落 | **採用** | kmp-facade spec に Requirement 追加、tasks 5.x 拡充 |
| M6 | 既定ローディングへ DialogOptions を供給する経路がない | **採用** | Loading の設定プロパティとして既存 DialogOptions を再利用 (反映方針はオーナー確認対象)。Scenario 追加 |
| M7 | 後勝ちの直列化規則・スレッド契約が曖昧 | **採用** | coordinator の UI executor 直列化・受理順・受け口の実行スレッドを契約化 |
| m1 | LD-CV-05 の「削除」が検証不能 | **採用** | Scenario を再登録ベースに修正 |
| m2 | MIRROR_AREAS が prefix を見ない | **採用** | tasks 7.1 に (prefix, area) 対応の検査基盤変更を明記 |
| m3 | ui/brief.md に生カラー値 | **採用** | layout-semantics の overlayColor 既定値への参照に置換 |

降格・未解決: なし。契約判断を伴う4件 (C1 / M2 / M3 / M6) は反映方針をオーナーに提示して確定し、確定後に反映する (結果はこの下に追記)。

### 確定と反映 (2026-08-25 追記)

契約判断4件はオーナー確認で推奨案どおり確定: C1 = Android は専用の全画面透過 Window (原典同方式) / M2 = hide と最終 start は撤去完了まで待ち、show は操作ブロック有効時点で戻る / M3 = 構成ミスは fail-fast (action 未実行)・提示環境の不在では action 実行 / M6 = Loading の設定プロパティに既存 DialogOptions を再利用。

全11件を反映済み: design.md (Decision 4 改稿・Decision 6 拡張・Decision 8 新設)、specs/dialog-contract (世代・完了時点・失敗モデル・状態共有と直列化の契約化、Scenario LD-CO-10〜15 / LD-AT-04〜05 追加、LD-CV-04/05 修正)、specs/ios-native・android-native・maui-binding (スレッド規則・設定プロパティ・委譲)、specs/kmp-facade (共有 VM カスタム Loading の Requirement + LD-KM-03〜04)、tasks.md (2.1/2.2/3.1/3.2/4.1/5.3/5.4/7.1)、ui/brief.md (生カラー値の除去)、core/ADR-0022・0024 (proposed への明確化追記)。
