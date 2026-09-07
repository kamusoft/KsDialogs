# Tasks: add-loading-toast-typed-show

先に読む: handbook/cross の comment-policy (常時)・test-execution (テスト実行と scenario-id-coverage)・sample-parity (samples を触るとき)。lessons/impl.md も読む。

## 1. iOS Native (ios/)
- [x] 1.1 `LoadingViewRegistry` / `ToastViewRegistry` を View factory + VM factory の 2 スロットのエントリ構成にし、`register(_:viewModel:)` と `entry(forKey:)` を追加する (→ Requirement: Loading / Toast レジストリの VM factory スロット)
- [x] 1.2 `KsLoading` に型指定 show / start (非同期 configure) と省略形 extension を追加し、`Loading` 実装で「VM 生成 → configure → 進捗受け口紐付け → View → 提示」の順序と失敗伝播を実装する (→ Requirement: Loading の型指定 show / 型指定 start)
- [x] 1.3 `KsToast` に型指定 show (同期 configure) と省略形 extension を追加し、`Toast` 実装で呼び出し時点の同期失敗を実装する (→ Requirement: Toast の型指定 show)
- [x] 1.4 テスト: `LoadingTypedShowTests.swift` (LD-TY-01〜12・14・15。LD-TY-13 は Swift 対象外)・`ToastTypedShowTests.swift` (TS-TY-01〜07・09。TS-TY-08 は Swift 対象外)・compile 検査 (LD-YI-01 / TS-YI-01)・SwiftUI 登録との組み合わせ (LD-YI-02 / TS-YI-02) (→ 各 Requirement)

## 2. Android Native (android/)
- [x] 2.1 `LoadingViewRegistry` / `ToastViewRegistry` を 2 スロットのエントリ構成にし、`registerViewModel` と `entry()` を追加する (value class 拒否を含む) (→ Requirement: Loading / Toast レジストリの VM factory スロット)
- [x] 2.2 `KsLoading` に型指定 show / start (suspend configure) を追加し、`Loading` 実装で順序保証と失敗伝播を実装する (→ Requirement: Loading の型指定 show / 型指定 start)
- [x] 2.3 `KsToast` に型指定 show (同期 configure) を追加し、`Toast` 実装で呼び出し時点の同期失敗を実装する (→ Requirement: Toast の型指定 show)
- [x] 2.4 テスト: `LoadingTypedShowTests.kt` (LD-TY-01〜15)・`ToastTypedShowTests.kt` (TS-TY-01〜09)・compile 検査 (LD-YA-01 / TS-YA-01)・Compose 登録との組み合わせ (LD-YA-02 / TS-YA-03)・value class 拒否 (TS-YA-02 / LD-YA-03) (→ 各 Requirement)

## 3. MAUI (maui/)
- [x] 3.1 `LoadingViewRegistry` / `ToastViewRegistry` を 2 スロットのエントリ構成にし、`RegisterViewModel<TViewModel>` と internal の `StoreViewModelFactory` を追加する (→ Requirement: Loading / Toast レジストリの VM factory スロット)
- [x] 3.2 `IKsLoading` に型指定 ShowAsync / StartAsync (同期・非同期 configure、値なし / 値あり) を、`IKsToast` に型指定 Show を追加し、facade / gateway で VM 生成 → configure → 既存のインスタンス渡し経路への合流を実装する (→ Requirement: C# 公開面の Loading / Toast 型指定 show)
- [x] 3.3 maui-binding spec の署名表どおりに公開面を切り、オーバーロード束縛の compile 検査を**最初に**書く。表の署名で曖昧になる形が見つかれば作業を止めて報告する (署名の変更は spec の凍結に反するため deviation ではなくユーザー判断) (→ Scenario: LD-YM-01 / TS-YM-01)
- [x] 3.4 `RegisterForLoading` / `RegisterForToast` に VM factory の自動配線を追加する (→ Requirement: 1 行登録の VM factory 自動配線)
- [x] 3.5 テスト: `LoadingTypedShowTests.cs` (LD-TY-03〜13 のうち facade 責務分: 03〜07・08 (進捗受け口の同一性)・09〜13)・`ToastTypedShowTests.cs` (TS-TY-02〜08 のうち 02〜08。06 は duration / placement のパススルー)・DI テストへの追加 (LD-YM-02 / TS-YM-02) (→ 各 Requirement)

## 4. samples/ (sample-parity 準拠)
- [x] 4.1 ios / android / maui の `Custom Loading` / `Custom Toast` の登録経路の呼び出しを型指定 show に差し替え、ios / android に VM factory 登録を追加する (→ Requirement: Custom Loading / Custom Toast デモの登録経路を型指定 show にする)
- [x] 4.2 ios / android / maui の 3 ルートで手動通しを行い verification/ に証跡を残し、kmp ルートは archive の既存証跡を基準として 4 ルートの見た目が同一であることを記録する (LD-YS-01 / TS-YS-01)。scenario-id-coverage の除外 ID に 2 件を理由つきで登録する

## 5. 仕上げ
- [x] 5.1 test-execution の規約どおり完了ゲートを全部通す: ios / android (JVM + instrumented) / kmp (`allTests`) / maui (`dotnet test` + Android / iOS 互換面テスト)。scenario-id-coverage を `--require-mirror` 付きで回し「未網羅なし」まで確認する
- [x] 5.2 KMP 互換面 (ios/Sources/KsDialogs/Kmp/ の `KmpLoadingViewModel` / `KmpToastViewModel` がレジストリを引く経路、kmp/ の actual) が Loading / Toast レジストリの内部表現変更後も View factory を解決できることを既存の KMP テスト (`KsLoadingKmpTests` / `KsToastKmpTests`・kmp `allTests`) で確認する
