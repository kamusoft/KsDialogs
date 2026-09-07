# Tasks: add-model-binding-di

各タスクは対応する Requirement / Scenario を明記する。Scenario ID (`MB-*`) はテスト名に含める (core/ADR-0016)。

## 1. 契約と iOS Native

- [x] 1.1 VM 契約 protocol の AnyObject 制約化と、サイドテーブル (ObjectIdentifier キー + 弱参照ボックス) の実装 (→ Requirement: notifier の VM 供給 / design Decision 1)
- [x] 1.2 `vm.notifier` protocol extension と、show 経路への紐付け・除去の組み込み (注入は View factory 呼び出し前、除去は紐付け後の全終端経路 — 正常配送・factory 例外・提示失敗・キャンセル・器消失) (→ notifier の VM 供給)
- [x] 1.3 1引数 factory 登録オーバーロード (UIKit / SwiftUI) の追加 (→ Swift 公開面の VM 供給と型指定呼び出し)
- [x] 1.4 VM factory 登録 API と型指定 show (async configure) の追加 (→ 型指定呼び出し / design Decision 3・4)
- [x] 1.5 iOS テスト: MB-NI-01〜07・MB-TS-01〜06 の同名テストと MB-IO-03 (→ dialog-contract 全 Scenario)
- [x] 1.6 iOS compile 検査: MB-IO-01 (正)・MB-IO-02 (負: struct 準拠拒否) を api-surface-check 系に追加

## 2. Android Native

- [x] 2.1 同一性キーの弱参照マップ実装と show 経路への紐付け・除去の組み込み (→ notifier の VM 供給 / design Decision 1)
- [x] 2.2 `vm.notifier` 拡張プロパティの追加 (→ Kotlin 公開面の VM 供給と型指定呼び出し)
- [x] 2.3 1引数 factory 登録オーバーロード (View / Compose) の追加 (→ 同上)
- [x] 2.4 VM factory 登録 API と型指定 show (suspend configure・Main dispatcher 実行) の追加、value class の VM の構成ミス拒否 (→ 型指定呼び出し / Kotlin 公開面)
- [x] 2.5 Android テスト: MB-NI-01〜07・MB-TS-01〜06 の同名テストと MB-AN-02 (Compose 経路)・MB-AN-03 (value class 拒否)
- [x] 2.6 Android compile 検査: MB-AN-01 (コンストラクタ参照登録含む) を api-surface-check に追加

## 3. MAUI

- [x] 3.1 ConditionalWeakTable によるサイドテーブルと `vm.Notifier` 拡張メンバー (→ C# 公開面の VM 供給と型指定呼び出し / design Decision 1・2。拡張プロパティ可否は Open Question 1)
- [x] 3.2 1引数 factory 登録・VM factory 登録・型指定 ShowAsync の2型引数形 (bool 省略形 / カスタム結果型・同期 / 非同期 configure・class 制約) の追加 (→ 同上 / design Decision 4)
- [x] 3.3 `AddKsDialogs(options)` と provider ホルダ (初期化サービス) の実装 (→ fallback resolver / design Decision 5)
- [x] 3.4 `RegisterForDialog<TView, TViewModel>` (View factory + VM factory の自動配線、TryAdd・transient の自動サービス登録、TView 生成への現在 VM の明示引数渡し、チェーン可能) (→ 1行登録 / design Decision 5)
- [x] 3.5 fallback resolver (View / VM 分離・スロット独立判定、解決順序 明示 → fallback → 失敗、fallback View への BindingContext 設定) の実装 (→ fallback resolver)
- [x] 3.6 MAUI テスト: MB-NI-01〜07・MB-TS-01〜06 の同名テスト (MAUI 実装に載る分) と MB-MA-03〜10
- [x] 3.7 MAUI compile 検査: MB-MA-01 (オーバーロード解決の成立確認)・MB-MA-02 (負) — 不成立の形が出たら実装を止めて報告 (core/ADR-0020 の supersede 判断)

## 4. KMP

- [x] 4.1 iOS KMP 面の1引数登録と notifier アクセサ (`notifier(for:result:)` 相当、結果型不一致は typed error) の追加 (→ iOS KMP 面の VM 供給 / design Decision 2)
- [x] 4.2 KMP テスト: MB-KM-01・MB-KM-04 (iosTest / interop 検証)・MB-KM-02 (androidHostTest)・MB-KM-03 (commonTest + 両 OS)
- [x] 4.3 KMP iOS 実 framework 越しの経路検証と verification/ 証跡 (→ 共有層からの疎結合呼び出しの実証)

## 5. Samples (4ルート一斉)

- [x] 5.1 SampleText への `Model Dialog` / `ViewModel から表示しています` の追加 (4ルート) (→ Model Dialog デモ項目)
- [x] 5.2 ios / android: VM factory 手動登録 + 型指定 show デモの追加 (→ MB-SM-01・02)
- [x] 5.3 maui: DI チェーン構成への変更 (`AddKsDialogs` + `RegisterForDialog`) と Model Dialog (→ MB-SM-03)
- [x] 5.4 kmp: 共有 VM のインスタンス渡し show + VM 供給報告によるデモの追加 (→ MB-SM-01・02)
- [x] 5.5 4ルートのパリティ通し確認 (スクリーンショット証跡) (→ MB-SM-02)
- [x] 5.6 Model Dialog の見た目が各ルートの既存 Basic Dialog と同一であることの視覚照合 (→ ui/brief.md の再利用判断)

## 6. 横断検証

- [x] 6.1 scenario-id-coverage.py で全 Scenario ID とテスト名の対応を検査 (cross concepts test-execution)
- [x] 6.2 全ビルドルートのテスト実行 (cross concepts test-execution の手順)
- [x] 6.3 本体の Compose 非依存検査・公開 API 形状検査の既存パスが通ることの確認
