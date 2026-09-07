# モノレポの器づくり

platform 別ビルドルートを持つモノレポの器を作る change フェーズ。器づくりと API 設計を混ぜない (API 設計は phase-1 の決定と phase-4 以降の実装で)。

## 論点

(すべて決定済み)

## 決定事項

- **ビルドルート構成は4形態分離**: ios/ (Package.swift) / android/ (settings.gradle.kts) / kmp/ (settings.gradle.kts) / maui/ (.slnx) の4つを独立ビルドルートとし、リポジトリルートに共通ビルドファイルを置かない (KsSettingsView cross/0001 の翻案)。KMP→Android Native の依存は composite build (includeBuild) で張り、Kotlin / AGP のバージョン整合はバージョンカタログの共有で緩和する (2026-08-14)
- **公開識別子の写像表** (KsSettingsView cross/0002 の翻案。ドメイン根拠 `kamusoft.jp`): iOS = Swift モジュール / SwiftPM パッケージとも `KsDialogs`。Android = Kotlin パッケージ `jp.kamusoft.ksdialogs` / Maven `jp.kamusoft:ksdialogs` (素の artifactId は Native 主方針により Android Native へ)。KMP = Kotlin パッケージ `jp.kamusoft.ksdialogs.kmp` / Maven `jp.kamusoft:ksdialogs-kmp`。MAUI = .NET namespace は素の `KsDialogs` (.NET で使う形態は MAUI だけで自明のため) / NuGet ID は `KsDialogs.Maui` (文脈のない依存リスト・検索で MAUI 前提を自己説明させるため) (2026-08-14)
- **最小疎通の構成** (バージョンは cross/ADR-0002 に従う): ios/ = Package.swift + library ターゲット `KsDialogs` (空ソース1枚) / android/ = `:ksdialogs` (com.android.library、minSdk 24) / kmp/ = `:ksdialogs-kmp` (androidTarget + iosArm64 / iosSimulatorArm64 / iosX64、includeBuild("../android") で `jp.kamusoft:ksdialogs` への依存を実際に張る) / maui/ = KsDialogs.slnx + `KsDialogs.Maui` クラスライブラリ (TFM `net10.0;net10.0-ios;net10.0-android` — 素の net10.0 はテスト基盤決定で追加)。疎通の定義は各ルートのビルドが通ること。composite 接続 (cross/0004 の最大リスク) は scaffold で疎通させ、KMP iOS→Swift の cinterop は Swift 実物が要るため phase-4 送り。maccatalyst / windows は Native 実装が存在しないため対象外 (core/ADR-0001 の帰結、原典も iOS + Android のみ) (2026-08-14)
- **テスト基盤の最小構成** (スモークテスト = 再構成の検証装置。CI・カバレッジはスコープ外): ios/ = **Swift Testing** (先例 KsSettingsView の XCTest から乗り換え。グリーンフィールドで Apple 現行標準を採る。UI 自動化が要る場面のみ XCTest 併用に戻る) / android/ = **JUnit 5 (Jupiter)** (先例踏襲。Robolectric + JUnit 4 は UI テスト到来時に導入) / kmp/ = **kotlin.test** の commonTest を androidTarget + iosSimulatorArm64 の両方で実行 (composite + multiplatform のテスト配線が検証対象) / maui/ = **NUnit 4** (先例踏襲。`KsDialogs.Maui.Tests` は net10.0 + UseMaui)。MAUI ライブラリ TFM に素の net10.0 を追加 (platform TFM だけでは unit test プロジェクトから参照不能。KsSettingsView と同じ理由・同じ形) (2026-08-14)
- **Sample の器は phase-4 まで作らない**: Sample 構成とパリティ規約は最初の実物と同時に確定する (roadmap 前提) ため、空の器での先取りをしない。ビルドルート4分離 (cross/0004) 確定済みで、Sample は後からどこに置いても器の再構成なしに接続できる (2026-08-14)
- **初期整備は README / .gitignore / LICENSE の3点に限定**: README はルートに英語主で最小限 (名前・コンセプト1段落・開発中ステータス・4ビルドルートの地図。README-ja 併設は phase-9 docs で検討) / .gitignore はルート1本に4形態分をまとめる (先例踏襲) / LICENSE は MIT (c) kamusoft (原典・先例踏襲)。.editorconfig・lint 等の規約系は実コードが生まれるフェーズ (phase-3 or phase-4) に送る (2026-08-14)

## TODO

- [x] 論点の解消
- [ ] scaffold 実装時に swift-tools-version を実物確定し、結果を cross/ADR-0002 に追記して残課題を閉じる (議論の自由度がないため論点から付け替え。add-monorepo-scaffold task 2.3 に転記済み、2026-08-14)
- [x] ksn-propose で変更提案を起こす → [add-monorepo-scaffold](../../../../changes/archive/2026-08-14-add-monorepo-scaffold/proposal.md) (2026-08-14)

## 実装結果 (2026-08-14 反映)

- deviation なし — 全 7 Requirements / 8 Scenarios 一致 (verify-001)。レビュー2周 (review-001/002 とも APPROVED) + codex セカンドオピニオン (spec / code) 実施。4ビルドルートすべてビルド + スモークテスト成功、composite 置換は dependencyInsight で実証
- swift-tools-version は **6.3** で実物確定 (実測 Xcode 26.5)。cross/ADR-0002 へ追記し accepted に昇格
- 申し送り:
  - BuildProbe (4ルートの疎通マーカー、internal) は実 API 実装時に削除する → phase-4 agenda の TODO に追記済み
  - KMP の iOS deployment target 17 指定が非保証フラグ (-Xoverride-konan-properties) 依存 → phase-4 agenda の TODO に追記済み
  - 見送り: MAUI テスト namespace の global:: 修飾 / gradle.properties の android・kmp 重複 / kmp モジュール自身の version リテラル — いずれも実害なし・好みの域と判断 (review-002 に記録あり)
