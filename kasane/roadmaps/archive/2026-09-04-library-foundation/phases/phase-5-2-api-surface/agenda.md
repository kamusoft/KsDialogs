# API 表面の完成

結果通知の型設計と Swift 向け KMP 公開面を含む、3形態の利用者向け API 表面を完成させる change フェーズ (phase-5-dialog-completion から分割)。

> phase-1 決定の反映 (2026-08-13): reusable 機構 (Create* / IReusableDialog / OnceInitializeAction) は core/ADR-0005 で契約から削除決定済みのため、論点から除外した。show は毎回生成の使い捨てモデル。

## 前提 (phase-10 完了の反映、2026-08-17 追記)

- 配布モデルが確定し、「Swift 向け KMP 面の一括設計」への入力が揃った: 登録 API の置き場所は **Swift パッケージ側** (kmp/ADR-0003)。`KsDialogsInteropBridge` は cinterop 委譲専用の機械面として残し、利用者向け公開登録 API を Swift パッケージ内に設計する。KMP iOS Sample の機械面直接利用 (verify-001 ❌3) はこの新 API へ差し替える
- 配布単位・バージョンの前提: cross/ADR-0008 (標準3チャネル・アプリ側 SwiftPM 1点)・cross/ADR-0009 (lockstep 単一バージョン) を API 設計の前提として扱う

## 申し送り (phase-5-1 再設計の反映、2026-08-18 追記)

- **SwiftUI / Compose の添付 DSL の実装**: phase-5-1 (add-layout-spec) の再設計で、メタ属性の供給は「コンテンツ定義への添付」に確定した (core/ADR-0015)。宣言的 UI 系の添付 DSL — SwiftUI は body ルートの `.ksDialogOptions(...)` / `.ksDialogPlacement(...)` modifier、Compose は composable 冒頭の `KsDialogAttributes(options, placement)` — は、登録経路 (core/ADR-0011) が本フェーズのスコープであるため実装をこちらへ申し送る (契約の意味論は add-layout-spec 側で確定済み)
- 実現可能性プローブ実施済み (2026-08-18、いずれも判定 YES):
  - SwiftUI: PreferenceKey 方式。表示中 window 接続 + `layoutIfNeeded()` 中に同期到達を実測。同期発火は公式保証のない実装挙動のため、未着の場合は初回提示前に到達を待って再レイアウトし、スナップショット契約 (添付値の採用 = 初回レイアウトパス完了時点) に収束させること (提示後の再適用は契約違反)。`onPreferenceChange` の値型は `Equatable & Sendable` 必須 (Swift 6)
  - Compose: `staticCompositionLocalOf` の collector + `KsDialogAttributes` は `SideEffect` で書き込み、ホストは `doOnPreDraw` で読む。attach または初回 measure で composition が同期実行される構造保証を実機2台で確認。Lazy スコープ内に書かれると初回に実行されない点を DSL の契約に明記すること
- 既存リスク (1) の「SwiftUI / Compose ホスティングの内容サイズ測定がレイアウト規則と整合」に加え、**ホスティング経由でも添付 DSL の値がケース表適合すること**を受け入れ条件に含める

## 申し送り (add-layout-spec 完了の反映、2026-08-19 追記)

- **expand-api-surface spec の検査方式参照の読み替え**: `changes/expand-api-surface/specs/maui-binding/spec.md:16` の「既存の NegativeCompileChecks 方式」は add-layout-spec で撤去済み。実装時は新方式 (`KsDialogs.Maui.ApiSurfaceCheck` + 禁止形状別の `KsDialogsNegativeCheck*` フラグ、回し方の規約は `handbook/cross/test-execution.md`) に読み替えること (出典: add-layout-spec review-004 Suggestion)
- **属性調整パネル内の操作部の読み上げ対応 (残課題)**: add-layout-spec では戻る導線 `‹` にのみ読み上げ名「戻る」と役割を付与した。パネル内のトグル (Use visible area)・配置セグメント (Start/Center/End)・移動量欄の読み上げ対応は未着手のため、samples を拡張する本フェーズで4ルート一斉の対応を検討する → **決定事項へ昇格済み (2026-08-19: 本 change のスコープに含める)**
- **MAUI スナップショット配線の実機確認**: ~~MAUI 経路の「レイアウトパス中の添付変更が採用される」配線は静的レビュー + 単体テストのみで、実機での動的確認は未実施~~ → **2026-08-19 訂正 (相方スペックレビュー spec-002 で判明)**: 実機確認は add-layout-spec 内で実施済み — iOS Simulator / Android 実機の証跡が `changes/archive/2026-08-19-add-layout-spec/verification/maui-snapshot-wiring/` にあり、review-004 #4 で解消判定済み。本フェーズでの再確認は不要

## 論点

(すべて決定事項へ昇格済み)

## 素材

- [scout-registration-api-and-kssettingsview.md](artifacts/scout-registration-api-and-kssettingsview.md) — 登録 API の現状 (4形態のシグネチャ・KMP iOS 機械面・MAUI 型引数2つの原因) と KsSettingsView の両対応設計 (2026-08-17)

## 決定事項

- **コンテンツ View 技術両対応の登録 API (2026-08-17)**: 技術別オーバーロード (公開面) + 内部で単一の型消去表現に収束する方式を採用 (core/ADR-0011)。iOS = 現行 `-> UIView` に加え SwiftUI 用 `-> some View` (内部 `UIHostingController`)、Android = 現行 `-> View` に加え Compose 用 (内部 `ComposeView`。命名は Kotlin のオーバーロード解決の都合を spec 化で確定)、MAUI = 変更なし (`Maui.View` 一本、従来 View 系経路が MAUI 連携の前提)、KMP = Android は Native API で自動的に両対応・iOS は「Swift 向け KMP 面の一括設計」の登録 API に SwiftUI 受け口を含める (本決定が入力)。sealed 公開ラッパ (KsAnyView 型) は「View を値としてパイプラインに運ぶ動機」がダイアログに無いため不採用 — View 直接渡しの show 系統の議論で値運搬が必要になったら再考。Sample での両系統デモを完了条件に含める
- **MAUI `Register` の書き味 (2026-08-17)**: 素の `Register` (factory 形・型引数2つ) は低水準 API として現行維持。C# は部分的型引数推論を持たないため型引数の削減は API 再設計でしか実現できず、notifier の型束縛 (core/ADR-0003 方向) と形態間対称性を壊す対価に見合わない (明示型付きラムダで型引数を省略できることは実験で確認済み — 両スタイルを Sample・ドキュメントで示す)。**原典水準の1行登録 (`RegisterForDialog<TView, TViewModel>()` 相当) は phase-6 の DI 糖衣の必須要件として申し送り済み** (phase-6 agenda に追記)。ADR は起こさず (設計の実体は phase-6 で決まるため、そちらの ADR に委ねる)
- **結果通知の型設計 (2026-08-17)**: phase-4 で解消済みと確認してクローズ — iOS `DialogNotifier<ViewModel.Result>` / Android `DialogNotifier<R>` / MAUI `DialogNotifier<TResult>` と3形態とも通知役は結果型に束縛済み (根拠: artifacts/scout-registration-api-and-kssettingsview.md)。唯一の型なし面 (KMP iOS 機械面 KsDialogsInteropBridge) は「Swift 向け KMP 面の一括設計」に統合。新規決定なしのため ADR なし
- **Swift 向け KMP 面の一括設計 (2026-08-17)**: 3点セットで確定 (kmp/ADR-0004 proposed)。①登録はジェネリック糖衣 — 結果型の自己申告 (kmp/ADR-0002 の制約) を型引数 `result:` から自動導出し、手動 enum 申告を廃止。SwiftUI 用オーバーロード (core/ADR-0011) を含む ②Swift からの直接 show は Swift パッケージに型付き入口 (`kmpShow` 相当) を置く — KMP framework 直接 await は ObjC 境界のジェネリクス消失・sealed 網羅性喪失が実測済みのため不採用 ③登録申告型と実結果の型不一致は型付きエラーを throw — 内部印が結果表示に漏れる現状を廃止。完了条件: KMP iOS Sample の機械面直接利用を新 API へ差し替え (verify-001 ❌3 解消)。API の名前・シグネチャ細部は spec 化で確定 (※既定結果型 Bool の決定により `result:` 省略 = Bool のオーバーロードを追加 — 後述)
- **既定結果型 Bool (2026-08-17)**: VM が結果型を宣言しなければ bool 扱いとし、カスタム結果型は宣言した VM だけが持つ (core/ADR-0012 proposed)。形態別表現 — iOS: `associatedtype Result: Sendable = Bool` (デフォルト associatedtype) / Android: `typealias SimpleDialogViewModel = DialogViewModel<Boolean>` 相当 (名前は spec 化で確定。register は従来から型境界推論で儀式なし) / MAUI: 非ジェネリック `IDialogViewModel : IDialogViewModel<bool>` + VM 単型引数 `Register<TViewModel>` オーバーロード (2型引数版と共存・曖昧なしを実験で確認) / KMP Swift 面: `result:` 省略 = Bool のオーバーロード。**上方修正**: 「MAUI Register の書き味」決定に bool 用の型引数1つオーバーロードを追加、kmp/ADR-0004 に result 省略オーバーロードを追加
- **notifier 引数の省略は phase-6 へ織り込み (2026-08-17)**: View 自前 new の notifier は show の結果配線と繋がらないため不成立。省略を実現するのは「notifier を VM のスロットにライブラリが注入する」設計 (原典の BindingContext 方式が先例) で、VM 契約の変更は phase-6「ViewModel ライフサイクル」と一体で設計すべきと判断し織り込み済み。実現すれば `(vm) => View` の factory 形を非破壊追加できる。phase-5 帯の factory 基本形は (vm, notifier) を維持
- **パネル内操作部の読み上げ対応を本 change に含める (2026-08-19)**: add-layout-spec 残課題 (属性調整パネルのトグル・配置セグメント・移動量欄の読み上げ名未付与) は expand-api-surface のスコープに含め、samples 4ルート一斉で対応する。本 change はもともと samples 4ルートへ新デモ3種を追加する変更で、同じ画面・同じレビューサイクルで検査が回るため追加コストが最小。一般公開予定ライブラリとして sample のアクセシビリティ品質を早期に揃える。却下案 — 別 S 級への切り出し (同作業に change 1個分のオーバーヘッド・検査の二度手間) / phase-9 帯への先送り (残課題の風化)。ADR なし (覆すコスト低のスコープ配分判断のため)
- **View 直接渡しの show 系統 (2026-08-17)**: 登録不要の単発表示は**インライン factory 型の show** で提供する (core/ADR-0013 proposed) — `show(vm) { vm, notifier in View }` の形で、登録経路と同じ factory 形 (型付き notifier・ADR-0011 の両技術オーバーロード・ADR-0012 の bool 既定) をその場で1回だけ使う。対象は Native 2実装 + MAUI、KMP 共有コードは対象外 (View を供給できないため)。原典の「View インスタンス直接渡し」同型は notifier の型安全な受け渡しが壊れるため不採用。原典の「View 型指定 (框架が生成)」は phase-6 の DI 糖衣とセットで扱う。**phase-6 の VM 注入が入ればインラインも `(vm) => View` に縮む** (登録 factory と同形のため自動的に恩恵を受ける)

## TODO

- [x] 論点の解消
- [x] ksn-propose で変更提案を起こす (2026-08-17 作成済み: expand-api-surface)
- [x] ksn-propose (改訂) で add-layout-spec 完了分を change に反映する (2026-08-19 完了) — ①添付 DSL のスコープ追加 ②maui-binding spec の検査方式書き換え ③~~MAUI 実機確認タスク追加~~ (相方レビューで実施済みと判明し撤回 — 上の申し送り訂正参照) ④ui/references の鮮度照合 (PASS) ⑤パネル内操作部の読み上げ対応 Requirement 追加。相方スペックレビュー spec-002 (Major 5 / Minor 2) の採用反映込み
- [x] 完了条件: パリティ準拠の Sample 通し (コンテンツ両技術デモ・KMP iOS Sample の新 API 差し替えを含む) — 2026-08-19 実施 (証跡: changes/archive/2026-08-19-expand-api-surface/verification/sample-walkthrough/)

## 実装結果 (2026-08-19 反映)

- 実装完了・アーカイブ済み: [expand-api-surface](../../../../changes/archive/2026-08-19-expand-api-surface/proposal.md) — レビュー2周 (ホスト+相方 codex) APPROVED・verify-001 VALID (53 Scenario 全一致)・deviation なし・UI 照合はオーナー最終承認 (2026-08-19) 取得済み
- ADR: core/0010〜0013・kmp/0003〜0004 を実装確定形へ精緻化して accepted 昇格。新規 accepted 2本 — android/0001 (ksdialogs-compose 分離)・kmp/0005 (show ハンドル経由のキャンセル閉鎖)。cross/0008・0009 の配布物一覧へ ksdialogs-compose 追記 (proposed 維持)
- 申し送りのルーティング (蒸留 2026-08-19):
  - **Kotlin `IosDialogGateway` のキャンセル追随 + doc コメント訂正** → [phase-5-3 agenda](../phase-5-3-presentation-behavior/agenda.md) の論点へ追記済み
  - **原典水準の1行登録・DI 糖衣・notifier の VM 注入** → [phase-6 agenda](../phase-6-model-binding-di/agenda.md) に記載済み (2026-08-17 申し送り)
  - **利用者向けドキュメント整備** → phase-9 のスコープ (proposal Non-Goals どおり。個別追記は不要と判断)
