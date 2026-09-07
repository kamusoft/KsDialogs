# core ADR 一覧

全 platform が共有するダイアログ契約・共通 architecture の決定記録。

| ID | タイトル | status | date |
|---|---|---|---|
| [0001](0001-native-independent-thin-wrappers.md) | プラットフォーム構成は Native 2実装を土台とし MAUI / KMP は薄いラッパーとする | accepted | 2026-08-13 |
| [0002](0002-public-api-shape.md) | 公開 API は契約 interface + 既定 singleton エントリの両対応とし、命名は原典踏襲とする | accepted | 2026-08-13 |
| [0003](0003-result-notification-async-typed.md) | 結果通知は async 単発 + 型付き結果とし、completed / cancelled を型で区別する | accepted | 2026-08-13 |
| [0004](0004-di-view-factory-registry.md) | DI 差し込みは VM 型キー → View factory の明示レジストリとする | accepted | 2026-08-13 |
| [0005](0005-no-view-reuse-mechanism.md) | View 再利用機構は契約に持ち込まず、show は毎回生成の使い捨てモデルとする | accepted | 2026-08-13 |
| [0006](0006-multi-display-os-delegation.md) | 多段表示は OS の提示機構への委譲とし、契約は観察可能な意味論のみを規定する | accepted | 2026-08-13 |
| [0007](0007-layout-spec-not-shared-code.md) | レイアウト計算は観察可能な規則を core 仕様として1本化し、実装は各 OS のレイアウト機構に委ねる | accepted | 2026-08-13 |
| [0008](0008-layout-attributes-deliberate-deviations.md) | レイアウト属性は原典踏襲を基本とし、実装都合の歪みは仕様の一貫性で意図的に乖離する | accepted | 2026-08-18 |
| [0009](0009-layout-spec-test-case-table.md) | レイアウト共通仕様テストは共通ケース表を単一の正とし、全量検証は Native 2実装・ラッパーはパススルー検証とする | accepted | 2026-08-18 |
| [0010](0010-dual-content-view-technology.md) | ダイアログコンテンツは従来 View 系 (Android.View / UIView) と宣言的 UI 系 (Compose / SwiftUI) の両対応を必須とする | accepted | 2026-08-17 |
| [0011](0011-dual-content-registration-overloads.md) | 両対応の登録 API は技術別オーバーロードを公開面とし、内部は単一の型消去表現に収束する | accepted | 2026-08-17 |
| [0012](0012-default-result-type-bool.md) | 結果型は Bool を既定とし、カスタム結果型は宣言した VM だけが持つ | accepted | 2026-08-17 |
| [0013](0013-inline-factory-show.md) | 登録不要の単発表示はインライン factory 型の show で提供する | accepted | 2026-08-17 |
| [0014](0014-contract-keeps-only-container-meta-attributes.md) | ダイアログ契約は器にしか実現できないメタ属性のみとし、View で表現可能な属性は契約から外す | accepted | 2026-08-18 |
| [0015](0015-attribute-supply-content-attachment.md) | メタ属性は静動2分割の値オブジェクトとし、コンテンツ定義への添付 + show 引数 (placement のみ) で供給する | accepted | 2026-08-18 |
| [0016](0016-behavior-spec-scenario-tests.md) | 挙動系の共通仕様は安定 ID つきの同名 Scenario テストで検証し、OS 間差は concepts の差分表を正として記録する | accepted | 2026-08-22 |
| [0017](0017-animation-hooks-transition-attachment.md) | アニメーションフックは完了通知つき両フックを別添付スロットで供給し、フックは統一形 (ホスト View)・既定はライブラリのクロスフェード・結果は最初の報告でラッチし配送は撤去後とする | accepted | 2026-08-22 |
| [0018](0018-notifier-vm-injection-side-table.md) | notifier は show 時に VM へサイドテーブルで紐付け、VM 契約は参照型限定とする | accepted | 2026-08-24 |
| [0019](0019-no-lifecycle-hooks-configure-closure.md) | VM 契約にライフサイクルフックを持ち込まず、型指定呼び出しの初期化は configure クロージャで行う | accepted | 2026-08-24 |
| [0020](0020-show-verb-unification.md) | 表示 API の動詞は show 1本とし、呼び出し経路は引数の形で表現する | accepted | 2026-08-24 |
| [0021](0021-vm-factory-registry-resolution.md) | 型指定呼び出しの VM 解決はレジストリ登録の VM factory で行い、暗黙の既定コンストラクタ生成は採らない | accepted | 2026-08-24 |
| [0022](0022-loading-separate-container-shared-parts.md) | Loading は器を OS 提示スタックから分離し、コンテンツ載せ・レイアウト・演出の部品を Dialog と共有する | accepted | 2026-08-25 |
| [0023](0023-default-loading-builtin-content.md) | 既定ローディングは共有経路上の内蔵コンテンツとして実現し、styling は一括設定のスタイル値オブジェクトで受ける | accepted | 2026-08-25 |
| [0024](0024-loading-concurrent-use-coalescing.md) | Loading の多重利用は単一表示への合流とし、黙った無視・未実行を契約から排除する | accepted | 2026-08-25 |
| [0025](0025-loading-dedicated-registry-vm-progress-receiver.md) | カスタム Loading は専用レジストリで登録し、進捗は VM の進捗受け口 interface へ転送する | accepted | 2026-08-25 |
| [0026](0026-loading-container-implementation-form.md) | Loading の器は iOS = key window 直貼り / Android = 専用の全画面透過 Window とし、レイアウト・演出の部品は Dialog の器から切り出して共有する | accepted | 2026-08-25 |
| [0027](0027-loading-process-coordinator-single-source.md) | プロセス内 Loading coordinator を状態の唯一の正とし、全入口が委譲する | accepted | 2026-08-25 |
| [0028](0028-toast-default-and-custom-view.md) | Toast は既定 View とカスタム View の両対応とし、メッセージだけで出せる入口を設ける | accepted | 2026-08-27 |
| [0029](0029-toast-registry-and-inline-factory.md) | カスタム Toast は型指定レジストリとインライン factory の両対応とし、レジストリは共有層からの呼び出し経路を担う | accepted | 2026-08-27 |
| [0030](0030-toast-container-implementation-form.md) | Toast の器は Loading の器の非モーダル派生とし、1 Toast 1器・重なりは追加順・Loading が常に前面とする | accepted | 2026-08-27 |
| [0031](0031-toast-non-interactive-fire-and-forget.md) | Toast は完全非対話とし、時間経過でのみ消える fire-and-forget の表示とする | accepted | 2026-08-27 |
| [0032](0032-toast-default-view-and-placement.md) | Toast の既定 View は OS 慣習寄せのピルとし、既定配置は下部中央 + ボトムバー回避オフセット、styling とアプリ既定配置は ToastStyle で受ける | accepted | 2026-08-27 |
| [0033](0033-user-factory-failure-boundary.md) | 利用者 View factory の失敗は言語境界の内側で捕捉し、各機能の既存失敗契約へ合流させる | accepted | 2026-08-28 |
| [0034](0034-contract-type-name-singular-feature.md) | 契約の型名は「Ks + 機能名 (単数)」で揃え、Dialog の契約は KsDialogs から KsDialog へ改名する | accepted | 2026-09-06 |
| [0035](0035-loading-toast-typed-show-vm-factory.md) | Loading / Toast のレジストリに VM factory スロットを追加し、型指定 show を Dialog と同型で提供する | accepted | 2026-09-06 |
