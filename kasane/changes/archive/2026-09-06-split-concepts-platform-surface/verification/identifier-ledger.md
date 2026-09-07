# 着地台帳 (core/api の公開名 × 期待する移動先)

`kasane/concepts/core/api/*.md` のバッククォート識別子を、baseline (verification/baseline.md) の
時点ですべて抽出したもの。抽出規則は API 名網羅検査と同じで、STOP 語だけを除く
(小文字の API 名も落とさない)。表は `identifier-landing.py collect` が生成し、
「期待する移動先」の初期値 (表の形態列・節見出し・トークン直前の形態名からの機械推定) を
実装者が 1 行ずつ確認して確定させ、「備考」に確認の根拠を書いている。

- **期待する移動先**: `core` (共通概念名として core に残す) / `ios` / `android` / `maui` / `kmp` /
  `architecture` (検証機構の記述として `core/architecture/` へ移す) / `-` (どこにも移さない)。複数可
- **判定**: 再構成後に `identifier-landing.py check` で埋める。「着地 (移動先)」か
  「意図して落とした (理由)」の二択で、未説明が 0 件になるまで残す。
  `architecture` と `-` は利用者向け Skill の源泉から外れるため、着地とは数えず理由つきの説明が要る
- 再構成の過程で移動先の見立てが変わったら、移動先の列を書き換えて備考に理由を足す
  (`collect` を回し直すと確認結果ごと上書きされるので、初回の生成後は手で直す)

| 識別子 | 出典 (concept / 節) | 期待する移動先 | 判定 | 備考 |
|---|---|---|---|---|
| `A.min` | layout-semantics.md / 最終 rect の決め方 (軸ごとの手順) | - | 意図して落とした (数式を fenced code block 化 — design Decision 3) | rect 決定手順の数式断片。数式は fenced code block へ移し、識別子として拾われない形にする |
| `AA` | transition-semantics.md / トランジションのルール (出入りの演出と結果が返る時点) | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (Android 添付面)。API 名ではないため共通概念名の一覧には載らない — 識別子表記を外すなら「意図して落とした」に振り替える |
| `AbstractComposeView` | transition-semantics.md / フックの形は全形態で1つ (統一形) | android | 着地 (android) | Compose のホスト View 型 |
| `android.view.View` | registration-show-semantics.md / 中身は従来 View 系でも宣言的 UI 系でも書ける<br>registration-show-semantics.md / 用語<br>transition-semantics.md / 用語 | android | 着地 (android) | Android の従来 View 系の型 |
| `AnyObject` | model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定 | ios | 着地 (ios) | Swift の class 制約 |
| `approvedBy` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json の統制フィールド名 |
| `approvedDiff` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json の統制フィールド名 |
| `attributes` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json のケース入力のフィールド名 |
| `Bool` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | ios kmp | 着地 (ios kmp) | Swift の真偽値型。kmp は Swift 向け公開面 (ios-host-integration.md) の result ラベル省略時の型として |
| `bool` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | maui | 着地 (maui) | C# の真偽値型 |
| `Boolean` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | android | 着地 (android) | Kotlin の真偽値型 |
| `bottom` | transition-semantics.md / プリセット | ios | 着地 (ios) | Swift のプリセット辺名。Android・MAUI は大文字綴りで各 surface に載る |
| `C` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | 共通ケース表のケース ID 接頭辞 |
| `C05` | layout-semantics.md / レイアウトのルール (ダイアログのサイズと位置の決まり方) | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | 共通ケース表のケース ID |
| `C19` | layout-semantics.md / レイアウトのルール (ダイアログのサイズと位置の決まり方) | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | 共通ケース表のケース ID |
| `CancellationException` | result-notification-semantics.md / 呼び出し元をキャンセルしたときの見え方<br>transition-semantics.md / 呼び出し元のキャンセルはどう観察されるか | android kmp | 着地 (android kmp) | コルーチン規約のキャンセル例外。core には識別子なしの散文で残す |
| `cancelled` | result-notification-semantics.md / 呼び出し元をキャンセルしたときの見え方 | ios | 着地 (ios) | Swift の結果 case 綴り。Kotlin・C# は `Cancelled` で別綴り。core には識別子なしの散文で残す |
| `Dialog.Instance` | registration-show-semantics.md / 基本形: 登録してから show する | maui | 着地 (maui) | MAUI の既定エントリ |
| `Dialog.instance` | registration-show-semantics.md / 基本形: 登録してから show する | android kmp | 着地 (android kmp) | Kotlin と KMP 共有コードの既定エントリ |
| `Dialog.shared` | registration-show-semantics.md / 基本形: 登録してから show する | ios | 着地 (ios) | Swift の既定エントリ |
| `DialogException.ValueClassViewModel` | model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定 | android | 着地 (android) | Kotlin の詳細例外型 |
| `DialogException.ValueTypeViewModel` | model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定 | maui | 着地 (maui) | C# の詳細例外型 |
| `DialogException.ViewModelFactoryNotRegistered` | model-binding-semantics.md / VM factory による解決 | android maui | 着地 (android maui) | Kotlin と C# の詳細例外型 (同名) |
| `DialogNotifier` | model-binding-semantics.md / notifier の VM 供給 (`vm.notifier`) | core | 着地 (core) | 4 形態すべての公開面に同綴りで存在することを実装コードで確認 (共通概念名の候補) |
| `DialogOptions` | layout-semantics.md / 器が持つメタ属性<br>layout-semantics.md / 属性の渡し方と優先順位<br>loading-semantics.md / 既定ローディング (内蔵コンテンツ) と styling<br>toast-semantics.md / 配置と ToastStyle<br>transition-semantics.md / 演出は中身に添付する (第3の添付スロット)<br>transition-semantics.md / してはいけないこと<br>transition-semantics.md / 用語 | ios android maui | 着地 (ios android maui) | commonMain は公開しないため共通概念名にならない |
| `DialogPlacement` | layout-semantics.md / 器が持つメタ属性<br>layout-semantics.md / 属性の渡し方と優先順位<br>registration-show-semantics.md / 基本形: 登録してから show する<br>toast-semantics.md / 配置と ToastStyle<br>transition-semantics.md / 演出は中身に添付する (第3の添付スロット) | core | 着地 (core) | 共通概念名の候補 |
| `DialogTransition` | loading-semantics.md / 器の性質 (ダイアログとの重なり)<br>loading-semantics.md / 持たない機能<br>registration-show-semantics.md / 宣言的 UI での属性の添付<br>toast-semantics.md / デフォルト View とカスタム View・演出<br>transition-semantics.md / 演出は中身に添付する (第3の添付スロット)<br>transition-semantics.md / プリセット<br>transition-semantics.md / 用語 | core | 着地 (core) | 共通概念名の候補 |
| `DialogViewModel` | model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定<br>model-binding-semantics.md / KMP での見え方<br>registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | core | 着地 (core) | 共通概念名の候補 |
| `DialogViewRegistry` | registration-show-semantics.md / 基本形: 登録してから show する | core | 着地 (core) | 4 形態すべての公開面に同綴りで存在することを実装コードで確認 (共通概念名の候補) |
| `Dismissal` | transition-semantics.md / プリセット | maui | 着地 (maui) | C# のフック型名 |
| `dismissal` | transition-semantics.md / プリセット | ios android | 着地 (ios android) | Swift・Kotlin のフックのプロパティ名。MAUI は PascalCase の別綴り。commonMain は演出型を公開しない |
| `Easing` | transition-semantics.md / プリセット | maui | 着地 (maui) | MAUI の easing 表現 |
| `END` | transition-semantics.md / プリセット | android | 着地 (android) | Android のプリセット辺名。訂正: MAUI の実装は `Start` / `End` の PascalCase (maui/KsDialogs.Maui/Contract/DialogTransitionEdge.cs) で別綴りのため、期待する移動先から maui を外した |
| `expected` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json の期待値フィールド名 |
| `hide` | toast-semantics.md / 公開面 | ios android kmp | 着地 (ios android kmp) | Toast が持たない操作として Loading の hide に言及する箇所。MAUI は `HideAsync()` (訂正: 実装は `HideAsync` — maui/KsDialogs.Maui/Presentation/IKsLoading.cs) |
| `hide()` | loading-semantics.md / 公開面<br>loading-semantics.md / 合流のルール (多重利用) | ios android kmp | 着地 (ios android kmp) | Loading の公開面。MAUI は `HideAsync()` (訂正: 実装は `HideAsync` — maui/KsDialogs.Maui/Presentation/IKsLoading.cs) |
| `IA` | transition-semantics.md / トランジションのルール (出入りの演出と結果が返る時点) | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (iOS 添付面)。AA と同じ扱い |
| `IDialogViewModel` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | maui | 着地 (maui) | MAUI の ViewModel interface |
| `IKsDialogs` | registration-show-semantics.md / 基本形: 登録してから show する | maui | 着地 (maui) | MAUI の契約 interface |
| `IKsLoading` | loading-semantics.md / 公開面 | maui | 着地 (maui) | MAUI の契約 interface |
| `IKsToast` | toast-semantics.md / 公開面 | maui | 着地 (maui) | MAUI の契約 interface |
| `Interpolator` | transition-semantics.md / プリセット | android | 着地 (android) | Android の easing 表現 |
| `isCanceledOnTouchOutside` | loading-semantics.md / 器の性質 (ダイアログとの重なり) | ios android | 着地 (ios android) | 小文字綴りの器メタ属性名。MAUI は `IsCanceledOnTouchOutside` |
| `KC` | transition-semantics.md / トランジションのルール (出入りの演出と結果が返る時点) | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (KMP 経由)。AA と同じ扱い |
| `kotlin.time.Duration` | transition-semantics.md / プリセット | android | 着地 (android) | Android の duration 表現 |
| `KsDialogAttributes` | layout-semantics.md / 用語 | android | 着地 (android) | Android の添付属性型 |
| `ksDialogOptions` | layout-semantics.md / 属性の渡し方と優先順位 | ios android | 着地 (ios android) | iOS の添付名。訂正: Android も同綴りの拡張プロパティを公開する (android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ViewDialogAttributes.kt) ため、移動先に android を加えた |
| `ksDialogPlacement` | layout-semantics.md / 属性の渡し方と優先順位 | ios android | 着地 (ios android) | iOS の添付名。訂正: Android も同綴りの拡張プロパティを公開する (android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ViewDialogAttributes.kt) ため、移動先に android を加えた |
| `KsDialogs` | registration-show-semantics.md / 基本形: 登録してから show する | core | 着地 (core) | 共通概念名の候補 (MAUI は `IKsDialogs`。接頭辞 I は同綴り扱い) |
| `ksdialogs` | registration-show-semantics.md / 形態ごとの呼び分け | android | 着地 (android) | Android の Gradle モジュール名 |
| `KsDialogs.show` | result-notification-semantics.md / ルール5: 構成ミスは結果ではなく失敗で返す | kmp | 着地 (kmp) | Swift 向け公開面で @Throws を宣言するメンバ (ios-host-integration.md) |
| `ksDialogTransition` | transition-semantics.md / 演出は中身に添付する (第3の添付スロット) | ios android | 着地 (ios android) | iOS・Android の添付名 (同綴り) |
| `KsLoading` | loading-semantics.md / 公開面 | core | 着地 (core) | 共通概念名の候補 (MAUI は `IKsLoading`) |
| `KsToast` | toast-semantics.md / 公開面 | core | 着地 (core) | 共通概念名の候補 (MAUI は `IKsToast`) |
| `LazyColumn` | registration-show-semantics.md / 宣言的 UI での属性の添付<br>transition-semantics.md / 演出は中身に添付する (第3の添付スロット) | android | 着地 (android) | Compose の遅延評価スコープの例 |
| `LD` | loading-semantics.md / 関連 | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (Loading)。AA と同じ扱い |
| `leading` | transition-semantics.md / プリセット | ios | 着地 (ios) | Swift のプリセット辺名。Android・MAUI は `START` |
| `Loading.Instance` | loading-semantics.md / 公開面 | maui | 着地 (maui) | MAUI の既定エントリ |
| `Loading.instance` | loading-semantics.md / 公開面 | android kmp | 着地 (android kmp) | Kotlin と KMP 共有コードの既定エントリ |
| `Loading.shared` | loading-semantics.md / 公開面 | ios | 着地 (ios) | Swift の既定エントリ |
| `Loading.shared.options` | loading-semantics.md / 既定ローディング (内蔵コンテンツ) と styling | ios | 着地 (ios) | Swift の既定エントリ経由の器メタ属性設定 |
| `Loading.shared.style` | loading-semantics.md / 既定ローディング (内蔵コンテンツ) と styling | ios | 着地 (ios) | Swift の既定エントリ経由の styling 設定 |
| `LoadingCoordinator` | loading-semantics.md / 合流のルール (多重利用) | ios android | 着地 (ios android) | 内部層の coordinator 名で公開面ではない。訂正: 「状態の正はプロセス内に 1 つ」を説明する注記として ios / android の loading-surface.md に残す判断 (オーケストレーターの裁定) のため、移動先を ios android にした |
| `LoadingProgressReceiver` | loading-semantics.md / カスタム View 版 (登録と進捗配送) | core | 着地 (core) | 4 形態すべての公開面に同綴りで存在することを実装コードで確認 (共通概念名の候補) |
| `LoadingStyle` | loading-semantics.md / 公開面<br>loading-semantics.md / 既定ローディング (内蔵コンテンツ) と styling | ios android maui | 着地 (ios android maui) | commonMain は公開しないため共通概念名にならない |
| `MA` | transition-semantics.md / トランジションのルール (出入りの演出と結果が返る時点) | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (MAUI 添付面)。AA と同じ扱い |
| `none` | transition-semantics.md / してはいけないこと | ios android | 着地 (ios android) | Swift・Kotlin のプリセット名。MAUI は PascalCase の別綴り |
| `overlayDuration` | transition-semantics.md / 既定の演出と、オーバーレイの扱い<br>transition-semantics.md / プリセット | ios android | 着地 (ios android) | 小文字綴りの器メタ属性名。MAUI は `OverlayDuration` |
| `Presentation` | transition-semantics.md / プリセット | maui | 着地 (maui) | C# のフック型名 |
| `presentation` | transition-semantics.md / プリセット | ios android | 着地 (ios android) | Swift・Kotlin のフックのプロパティ名。MAUI は PascalCase の別綴り。commonMain は演出型を公開しない |
| `ProportionalWidth` | layout-semantics.md / DialogPlacement (動的メタ) | maui | 着地 (maui) | MAUI の属性綴りの例 |
| `R` | result-notification-semantics.md / ルール1: 結果の型は ViewModel が宣言する | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | 結果型の型引数の見本で API 名ではない。AA と同じ扱い |
| `reason` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json の統制フィールド名 |
| `Register` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | maui | 着地 (maui) | MAUI の登録メソッド |
| `RegisterForDialog` | model-binding-semantics.md / 関連 | maui | 着地 (maui) | 既存の maui/api/di-registration.md にある登録糖衣 |
| `Result` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | ios | 着地 (ios) | Swift の associatedtype 名 |
| `screen` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json のケース入力のフィールド名 |
| `ShowAsync` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む<br>result-notification-semantics.md / 各形態での形 | maui | 着地 (maui) | MAUI の表示メソッド |
| `showCompose` | registration-show-semantics.md / 登録せずにその場で表示する (インライン show) | android | 着地 (android) | Android のインライン show の別名 |
| `ShowResultAsync` | result-notification-semantics.md / 各形態での形 | maui | 着地 (maui) | 移植元の API 名で現行公開面には無い。MAUI 側の注記に残す想定 |
| `SimpleDialogViewModel` | registration-show-semantics.md / 真偽値の結果は結果型を書かずに済む | android | 着地 (android) | Kotlin の真偽値用の別名 |
| `START` | transition-semantics.md / プリセット | android | 着地 (android) | Android のプリセット辺名。訂正: MAUI の実装は `Start` / `End` の PascalCase (maui/KsDialogs.Maui/Contract/DialogTransitionEdge.cs) で別綴りのため、期待する移動先から maui を外した |
| `suspend` | result-notification-semantics.md / 各形態での形 | android kmp | 着地 (android kmp) | Kotlin の関数修飾子 |
| `TimeInterval` | transition-semantics.md / プリセット | ios | 着地 (ios) | iOS の duration 表現 |
| `TimeSpan` | transition-semantics.md / プリセット | maui | 着地 (maui) | MAUI の duration 表現 |
| `TimeSpan.MaxValue` | transition-semantics.md / プリセット | maui | 着地 (maui) | MAUI の duration 上限値 |
| `Toast.Instance` | toast-semantics.md / 公開面 | maui | 着地 (maui) | MAUI の既定エントリ |
| `Toast.instance` | toast-semantics.md / 公開面 | android kmp | 着地 (android kmp) | Kotlin と KMP 共有コードの既定エントリ |
| `Toast.shared` | toast-semantics.md / 公開面 | ios | 着地 (ios) | Swift の既定エントリ |
| `ToastStyle` | toast-semantics.md / 配置と ToastStyle | ios android maui | 着地 (ios android maui) | LoadingStyle と同型で commonMain は公開しない |
| `tolerance` | layout-semantics.md / 共通ケース表と OS 差の統制 | architecture | 意図して落とした (`core/architecture/layout-case-table.md` へ移動。excluded 予定のため着地と数えない。出現は grep で確認) | cases.json の許容誤差フィールド名 |
| `top` | transition-semantics.md / プリセット | ios | 着地 (ios) | Swift のプリセット辺名。Android・MAUI は大文字綴りで各 surface に載る |
| `TR` | transition-semantics.md / トランジションのルール (出入りの演出と結果が返る時点) | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (全形態共通の演出契約)。AA と同じ扱い |
| `trailing` | transition-semantics.md / プリセット | ios | 着地 (ios) | Swift のプリセット辺名。Android・MAUI は `END` |
| `TS` | toast-semantics.md / 関連 | core | 意図して落とした (非 API のラベル、バッククォートを外して平文化) | Scenario ID 接頭辞 (Toast)。AA と同じ扱い |
| `UIHostingController` | registration-show-semantics.md / 保証すること<br>transition-semantics.md / フックの形は全形態で1つ (統一形) | ios | 着地 (ios) | SwiftUI の中身を包む内部の入れ物 |
| `uint` | transition-semantics.md / プリセット | maui | 着地 (maui) | MAUI の総ミリ秒の上限を説明する C# の型 |
| `UITimingCurveProvider` | transition-semantics.md / プリセット | ios | 着地 (ios) | iOS の easing 表現 |
| `UIView` | layout-semantics.md / 属性の渡し方と優先順位<br>registration-show-semantics.md / 中身は従来 View 系でも宣言的 UI 系でも書ける<br>registration-show-semantics.md / 形態ごとの呼び分け<br>registration-show-semantics.md / 用語<br>transition-semantics.md / 演出は中身に添付する (第3の添付スロット)<br>transition-semantics.md / フックの形は全形態で1つ (統一形)<br>transition-semantics.md / 用語 | ios kmp | 着地 (ios kmp) | iOS の従来 View 系の型。kmp は Swift 向け公開面 (ios-host-integration.md) の factory 署名として |
| `UseCurrentPageLocation` | layout-semantics.md / 原典と既定値が違う3つ<br>layout-semantics.md / 基準領域 (LayoutArea) | maui | 着地 (maui) | 移植元の API 名で現行公開面には無い。MAUI 側の注記に残す想定 |
| `ValueClassViewModel` | model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定 | android | 着地 (android) | Kotlin の詳細例外名 |
| `ValueTypeViewModel` | model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定 | maui | 着地 (maui) | C# の詳細例外名 |
| `View` | layout-semantics.md / 属性の渡し方と優先順位<br>registration-show-semantics.md / 中身は従来 View 系でも宣言的 UI 系でも書ける<br>registration-show-semantics.md / 形態ごとの呼び分け<br>registration-show-semantics.md / 用語<br>transition-semantics.md / 演出は中身に添付する (第3の添付スロット)<br>transition-semantics.md / フックの形は全形態で1つ (統一形) | android maui | 着地 (android maui) | Android の拡張プロパティのレシーバ型と MAUI の中身の型 |
| `ViewModelAlreadyShowing` | model-binding-semantics.md / notifier の VM 供給 (`vm.notifier`) | android maui | 着地 (android maui) | Kotlin と C# の詳細例外名 (同名) |
| `viewModelAlreadyShowing` | model-binding-semantics.md / notifier の VM 供給 (`vm.notifier`) | ios | 着地 (ios) | Swift の enum case 名 |
| `viewModelFactoryNotRegistered` | model-binding-semantics.md / VM factory による解決 | ios | 着地 (ios) | Swift の enum case 名 |
| `vm.Notifier` | model-binding-semantics.md / notifier の VM 供給 (`vm.notifier`)<br>model-binding-semantics.md / 用語 | maui | 着地 (maui) | C# の綴り |
| `vm.notifier` | model-binding-semantics.md / notifier の VM 供給 (`vm.notifier`)<br>model-binding-semantics.md / ViewModel 契約は参照型 (class) 限定<br>model-binding-semantics.md / 1引数 factory<br>model-binding-semantics.md / KMP での見え方<br>model-binding-semantics.md / してはいけないこと<br>model-binding-semantics.md / 用語<br>registration-show-semantics.md / 関連 | ios android kmp | 着地 (ios android kmp) | Swift・Kotlin・KMP 共有コードの綴り。core には識別子なしの散文で残す |
