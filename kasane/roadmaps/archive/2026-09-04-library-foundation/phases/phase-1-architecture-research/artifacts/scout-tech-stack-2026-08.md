# scout 調査: 技術セット裏取り + 参考 ADR 内容 (2026-08-13)

ksn-scout による調査要約。論点「技術セット」「リブランド方針」「既存資産知識の取り込み方針」の議論素材。

## 1. 参考リポジトリのローカルパス

対応表: `kasane/concepts/cross/conventions/reference-repositories.md`

| リポジトリ | パス | 役割 |
|---|---|---|
| AiForms.Maui.Dialogs | `../AiForms.Maui.Dialogs` | 移植元・仕様の正 (README が API リファレンス) |
| KsSettingsView | `../KsSettingsView` | Native+MAUI リビルドの先例 |
| KsAppKMP | `../KsAppKMP` | KMP 基盤。Swift interop・モノレポ構成の出典 |

規約: ADR / concepts にはパスを直書きせず「リポジトリ名 + ADR 番号 / 文書タイトル」で参照する。

## 2. KsSettingsView cross/ADR-0017 (リブランド方針の翻案元)

`KsSettingsView kasane/decisions/cross/0017-port-aiforms-to-native.md` (accepted, 2026-07-31)

- Context: MAUI の将来性不安 (終息すれば共倒れ) + 純ネイティブから使えない + 設計の仕切り直し需要
- Decision: KsSettingsView として再構築。Native (iOS/Android) を主、MAUI を副 (Native への binding)。互換 shim は提供しない (独立ブランド、仕様と実装パターンのみ継承)。移植元参照ルールは concepts 側に分離 (aiforms-origin-reference.md)
- Alternatives: MAUI 版継続のみ → 却下 (ネイティブから使えない / MAUI 終息リスク)
- Consequences: 正 = MAUI 非依存・設計仕切り直し / 負 = 移植完了まで機能劣後と二重知識の参照コスト、3系統の保守コスト増

## 3. KsAppKMP の技術セット判断型と実採用版

ADR: `KsAppKMP kasane/decisions/cross/0001-kmp-tech-stack-room3-gradle.md` (accepted, 2026-08-03)

- 判断軸: 「グリーンフィールドの今が移行コストゼロで新世代に乗れる唯一のタイミング」— 最新安定版セットで開始し、以後は追従型の小差分に収める
- Decision: Kotlin 2.4.0 / CMP 1.11.0 / Koin 4.2.x / Room 3.0 / Gradle 継続。Amper は却下 (experimental・アーカイブ形跡)
- 実採用版 (libs.versions.toml 実測): Kotlin 2.4.10 / AGP 9.3.0 / CMP 1.11.1 / KSP 2.3.11 / Koin 4.2.2 / coroutines 1.11.0 / Gradle wrapper 9.5.0 / compileSdk 36, **minSdk 24**, targetSdk 36
- iOS 側実測: IPHONEOS_DEPLOYMENT_TARGET 17.0 / SWIFT_VERSION 5.0。**最低 OS を決めた ADR は存在しない** (実測値であり意思決定の記録ではない)
- Swift interop の先例: `KsAppKMP core/ADR-0003` — 公式 Swift Export は Kotlin 2.4.0 で Alpha 昇格・production 非推奨と評価し、**KMP-NativeCoroutines (1.0.5) を採用**。SKIE は Kotlin 追随の遅さで却下。将来 Swift Export へ剥がせる API 設計を意識と明記

## 4. Web 調査 (2026-08 時点の相場)

- **Kotlin**: 最新安定 2.4.0 (2026-08-11)。2.4.20-RC 直後 (kotlinlang.org/docs/releases.html)
- **Swift Export**: Alpha・production 非推奨。2.4.0 で Flow→AsyncSequence 出力と Swift package 依存宣言に対応。2026 年中の安定化目標 (kotlinlang.org/docs/native-swift-export.html)
- **Kotlin → Swift 呼び出し**: Swift を直接呼ぶ選択肢は現時点で**存在しない**。Kotlin/Native が話せるのは ObjC ランタイムのみ — Swift 側に `@objc` 互換面を出して cinterop でバインドが唯一の実用解。`@objc(ExplicitName)` 明示が定石。SwiftUI の値型・ジェネリクス・async は ObjC 面に出せないため、KMP から呼ぶ Swift ファサードの API 形状を制約する (kotlinlang.org/docs/native-objc-interop.html)
- **Xcode / Swift**: 安定版 Xcode 26.6 (2026-06、Swift 6.3 同梱)。2026-04-28 以降 App Store 提出は Xcode 26 / iOS 26 SDK 必須。swift-tools-version 推奨値は一次情報で裏が取れず (6.2 相当が妥当という推測どまり)
- **Gradle**: 最新安定 9.7.0 (2026-08-07)。KsAppKMP は 9.5.0
- **.NET / MAUI**: 現行 LTS は .NET 10 (MAUI 10)。.NET 11 は STS で GA 2026-11-10。ライブラリ新規なら net10.0 (LTS) 土台 + .NET 11 GA 後に TFM 追加が順当。MAUI 11 で CoreCLR が全プラットフォーム既定になり Mono パス削除 (バインディング挙動に影響しうる)
- **最低対象 OS の相場**: iOS 16 (2026 年中頃) → 17 (2027 年頃) / Android minSdk 31 (2026 年) → 33 (2027 年) が推奨相場。MAUI 10 自体の下限 (iOS 12.2 / API 21) は非常に低く、バインディング側の下限は Native 実装側に引きずられる構図

## 未確認事項

- KsAppKMP に最低 OS の ADR はない → KsDialogs で決めるなら新規判断
- swift-tools-version の 2026-08 推奨値は要裏取り
- Gradle 最新は release notes (current) ページ由来で 9.7.0
