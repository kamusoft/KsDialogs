# phase-5-2-api-surface 議論履歴

## 2026-08-17: phase-5-dialog-completion から分割 (論点13超の膨張のため3テーマに分割)

## 2026-08-17: コンテンツ View 技術両対応の登録 API の見せ方

phase-5-1 の core/ADR-0010 (両対応必須) を受けた最初の設計論点。ksn-scout で現状 (4形態とも factory 戻り値は従来 View 系1本) と KsSettingsView の両対応設計 (モジュール分割 + 装飾領域は sealed ラッパ KsAnyView + 行は宣言 UI 専用 builder) を実測 (artifacts/scout-registration-api-and-kssettingsview.md)。

選択肢は A. 技術別オーバーロード (公開面) + 内部で単一の型消去表現に収束 / B. sealed 公開ラッパ (KsAnyView 型) / C. 従来 View のみ + 公式 interop 丸投げ。判断軸は書き味 (ceremony の有無)・実装コスト・ADR-0010 への適合・将来拡張。C は宣言 UI が一級市民にならず ADR-0010 と矛盾するため除外。**A を採用** — KsSettingsView で sealed ラッパが必要だったのは View がモデル値として差分検出パイプラインを旅するためで、ダイアログは登録時に技術が確定し show 時に提示器が直接消費するだけなのでその動機が無い。show 毎回生成 (core/ADR-0005) のおかげでホスティングのライフサイクルも「閉じたら破棄」で済む。B の再考条件 (View を値として運ぶ必要が出た場合) は「View 直接渡しの show 系統」の議論に申し送り。ADR: core/0011 を proposed で起票。

## 2026-08-17: MAUI `Register` の書き味

phase-4 申し送りの「型引数2つ明示」の改善を議論。scratchpad の C# 実験で言語制約を実証: 部分的型引数推論は存在せず、`TResult` は制約にしか現れないため型なしラムダでは推論不可。ただし明示型付きラムダ (`Register((BasicDialogViewModel vm, DialogNotifier<bool> notifier) => ...)`) なら型引数を省略できることを確認。

当初アシスタントは「現行維持 + 両スタイル文書化 + DI は phase-6」で提示したが、オーナーから原典の実例 (`.RegisterForDialog<OKDialog, OKDialogViewModel>()` の DI チェーン1行登録) が示され「かなり後退している」と指摘。後退の理由を整理 — (1) DI 糖衣は最初から phase-6 の担当で未実装なだけ (2) 型付き結果 (core/ADR-0003) が原典に無かった新要素で型宣言がどこかに必要 (3) factory ラムダも MAUI 固有機構 (BindingContext 自動ひも付け) を core 契約に持ち込まない方針 (core/ADR-0001) の帰結。iOS/Swift・Android/Kotlin は VM 型1回のみで結果型不要 (associatedtype / 型境界推論) であり MAUI が最冗長という非対称も共有した。

**採用**: 素の `Register` は低水準 API として現行維持し、**原典水準の1行登録 (View 型 + VM 型のみ・factory なし・結果型なし・DI チェーン可能) を phase-6 の DI 糖衣の必須要件として申し送る** (phase-6 agenda の DI 差し込み論点に追記済み)。ADR なし — 設計の実体は phase-6 で決まるため、そちらの ADR に委ねる。

## 2026-08-17: 結果通知の型設計のクローズ (phase-4 解消済み)

分割前 agenda から引き継いだ「通知役を結果型に束縛する型設計が残課題」は、scout 実測で phase-4 解消済みと確認 — iOS (associatedtype 束縛)・Android (型引数束縛)・MAUI (型引数束縛) の3形態とも DialogNotifier は結果型付き。唯一の型なし面は KMP iOS 機械面 (KsDialogsInteropBridge: VM の Any 型消去 + KsDialogsInteropResultType の自己申告) のみで、これは「Swift 向け KMP 面の一括設計」の対象そのものであるため同論点へ統合してクローズ。新規決定なし・ADR なし。

## 2026-08-17: Swift 向け KMP 面の一括設計

前提: kmp/ADR-0003 (登録 API は Swift パッケージ側)・kmp/ADR-0002 (レジストリは Native 側・結果型は自己申告)・core/ADR-0011 (SwiftUI 受け口)・phase-4 deviation (Swift 直接 await 未実証、実測済みの粗 = ObjC 境界のジェネリクス消失・sealed 網羅性喪失、Sample の機械面直接利用)。

**3点セットを採用**: ①登録はジェネリック糖衣 (`register(SharedConfirmViewModel.self, result: Bool.self) { vm, notifier in ... }` の形。自己申告を型引数から自動導出し手動 enum 廃止、申告ミスをコンパイル時検出範囲に縮小。SwiftUI オーバーロード含む) ②Swift からの直接 show は Swift パッケージに型付き入口 (代替の KMP framework 直接 await は実測の粗により却下) ③型不一致は型付きエラー throw (内部印の漏れを廃止)。完了条件に KMP iOS Sample の新 API 差し替え (verify-001 ❌3 解消)。ADR: kmp/0004 を proposed で起票。

## 2026-08-17: 既定結果型 Bool と notifier 省略の行き先

「View 直接渡しの show」議論中にオーナーが遡って提起: 「戻り値は true/false がほとんどで、一部カスタム型のニーズ。利用頻度が低い形に縛られたくない」。

**決定1 — 既定結果型 Bool を契約に導入** (core/ADR-0012): iOS はデフォルト associatedtype (`Result = Bool`)、Android は typealias の bool 既定顔 (Kotlin は同名の型引数違い宣言不可のため。名前は spec 化)、MAUI は非ジェネリック `IDialogViewModel : IDialogViewModel<bool>` + VM 単型引数オーバーロード (scratchpad 実験でカスタム型2型引数版との共存・曖昧なしを確認)、KMP Swift 面は `result:` 省略オーバーロード。既存決定2件を上方修正 (MAUI Register 書き味・kmp/ADR-0004)。

**決定2 — notifier 引数の省略は phase-6 へ**: オーナー案「View のコンストラクタで `new DialogNotifier<bool>()`」は、notifier が show の結果配線 (ライブラリが show ごとに生成・結線) と繋がらず不成立と説明。選択肢は A. 後付け接続 (実行時型検査への後退で却下方向) / B. VM 注入 (原典 BindingContext 方式の先例あり、factory が `(vm) => View` に縮む) / C. 現状維持。**B を phase-6「ViewModel ライフサイクル」論点に織り込み** — VM 契約の変更はライフサイクル契約と一体で設計するのが適切。phase-5 帯は (vm, notifier) を基本形として維持し、B 実現後に非破壊で追加。

phase-6 最終形の API スケッチ (C#/Swift/Kotlin/KMP) を phase-6 artifacts (api-sketch-final-form.md) に保存。

## 2026-08-17: View 直接渡しの show 系統

原典の登録不要系統 (`ShowAsync<TView>(vm)` / `ShowAsync(view, vm)`) への回答。選択肢は A. インライン factory 型の show (`show(vm) { vm, notifier in View }` — 登録 factory と同形をその場で使う) / B. 原典同型の View インスタンス渡し / C. 追加しない。判断軸は notifier の型束縛・原典ニーズのカバー・ADR-0011 との整合。B は原典が MAUI 固有機構 (DialogView 自前生成 notifier + BindingContext) 前提であり、注入型の KsDialogs では notifier を型安全に届ける場所がなく壊れるため却下。C は1回きりダイアログに登録の儀式が残るため却下。**A を採用** (core/ADR-0013)。対象は Native 2実装 + MAUI (KMP 共有コードは View を供給できないため対象外)。原典の View 型指定 (框架生成) は phase-6 の DI 糖衣とセットで扱う。オーナー指摘「インラインは notifier 省略できないのか」→ phase-6 の VM 注入が入れば登録 factory と同形のためインラインも `(vm) => View` に縮むことを確認し申し送りに含めた。

## 2026-08-19: add-layout-spec 完了の change 反映監査と、パネル内操作部の読み上げ対応の扱い

add-layout-spec の実装完了・蒸留を受けて、expand-api-surface (2026-08-17 凍結・相方スペックレビュー済み) への反映漏れを監査。change 一式の grep で添付 DSL (DialogOptions / KsDialogAttributes) の言及ゼロを確認し、反映すべき4点を特定 — (1) 添付 DSL (core/ADR-0015 の申し送り) がスコープ外のまま: specs (ios-native / android-native)・design (実現可能性プローブ結果)・tasks に追加が必要、受け入れ条件「ホスティング経由でも添付 DSL の値がケース表適合」も未記載 (2) maui-binding spec:16 が撤去済みの「NegativeCompileChecks 方式」を参照 — 新方式 (KsDialogs.Maui.ApiSurfaceCheck + KsDialogsNegativeCheck* フラグ) へ書き換え (3) MAUI スナップショット配線の実機確認タスクが未記載 (4) ui/references/layout-spec-approved.png の鮮度が 5-1 最終 UI と未照合。(1)〜(3) は申し送りで決定済みのため議論なしで改訂へ。

唯一の未決論点だった読み上げ対応 (5-1 残課題: 属性調整パネルのトグル・配置セグメント・移動量欄) は、選択肢 A. 本 change に含める / B. 別 S 級に切り出す / C. phase-9 帯へ先送り。判断軸は実装コスト・レビューのまとまり・change の膨張・風化リスク。**A を採用** — 本 change は samples 4ルートへ新デモ3種を追加する変更で、同じ画面・同じレビューサイクルで検査が回るため追加コストが最小。一般公開予定ライブラリとして sample のアクセシビリティ品質を早期に揃える。ADR なし (覆すコスト低のスコープ配分判断)。上記4点 + 読み上げ Requirement の反映は ksn-propose (改訂) へハンドオフ。
