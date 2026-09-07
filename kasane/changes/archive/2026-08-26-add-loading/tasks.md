# Tasks: add-loading

Scenario ID は specs/ の各 spec.md を正とする。挙動系 (LD-CO/AT/WN/PR/ST/CV/TR) は両 Native で同名テストを書く (core/ADR-0016)。

## 1. 共有部品の切り出し (先行リファクタリング — design Decision 4)

- [x] 1.1 iOS: トランジション実行 (フック解決・起動・覆いの別レイヤフェード) を `DialogContainerViewController` から共有部品へ切り出す。挙動不変 (→ Requirement: 出入りの演出)
- [x] 1.2 iOS: レイアウト適用 (resolver 呼び出し・実効値合成・制約反映) の Loading 器から使える形への切り出し (→ Requirement: 器メタ属性の適用)
- [x] 1.3 Android: 同様の切り出し (`DialogContainer` のトランジション実行・レイアウト適用) (→ 同上)
- [x] 1.4 切り出し前後で既存テスト全通過を確認 (Scenario PB/MB 全量 + レイアウト共通ケース表 + api-surface 正負)

## 2. iOS Native (core 契約の実装 1本目)

- [x] 2.1 `KsLoading` protocol + `Loading.shared` + coordinator (合流カウント・表示世代・完了時点・失敗モデル・main 直列化 — design Decision 8) (→ Requirement: Loading の公開面と合流 / Scenario LD-CO-01〜15 のテスト)
- [x] 2.2 Loading 専用オーバーレイ器 (key window 直貼り + 切り出した共有部品) と再配置・設定プロパティ (style / options) (→ Requirement: 器メタ属性の適用 / LD-AT-01〜05, LD-WN-01。LD-AT-04 はダイアログ最前面の成立性検証)
- [x] 2.3 レイアウト共通ケース表を Loading 器で全量実測 (isCanceledOnTouchOutside 読み替え含む) (→ 同 Requirement)
- [x] 2.4 内蔵コンテンツ + `LoadingStyle` (各表示開始時に読む) (→ Requirement: 既定ローディングのスタイル / LD-ST-01〜03)。**mock/approved.png (案A) との視覚照合**
- [x] 2.5 進捗報告 (クランプ・非有限無視・後勝ち・VM 受け口転送) (→ Requirement: 進捗通知 / LD-PR-01〜06)
- [x] 2.6 Loading レジストリ + カスタム View 表示 (UIKit / SwiftUI 登録・インライン・使い捨て) (→ Requirement: カスタム View 版の登録と表示 / LD-CV-01〜06)
- [x] 2.7 演出の適用 (添付スロット共有・既定トランジション・覆い別レイヤ) (→ Requirement: 出入りの演出 / LD-TR-01〜03)
- [x] 2.8 Swift 公開面の compile 検査 (→ Requirement: Swift 公開面の Loading / LD-IO-01〜02)

## 3. Android Native (ミラー実装)

- [x] 3.1 `KsLoading` interface + `Loading.instance` + coordinator (LD-CO-01〜15 ミラー)
- [x] 3.2 Loading 専用の全画面透過 Window の器 (design Decision 4 — 既存 Dialog 器より手前。`ResumedActivityTracker` による Activity 再生成時の再取り付け) + 設定プロパティ (LD-AT-01〜05, LD-WN-01 ミラー。**LD-AT-04 の成立性検証を最初に行う**)
- [x] 3.3 レイアウト共通ケース表の全量実測 (instrumented)
- [x] 3.4 内蔵コンテンツ + `LoadingStyle` (LD-ST-01〜03 ミラー)。**mock/approved.png (案A) との視覚照合**
- [x] 3.5 進捗報告 (LD-PR-01〜06 ミラー)
- [x] 3.6 Loading レジストリ + カスタム View (従来 View 系は本体・Compose 系は `ksdialogs-compose`) (LD-CV-01〜06 ミラー)
- [x] 3.7 演出の適用 (LD-TR-01〜03 ミラー)
- [x] 3.8 Kotlin 公開面の compile 検査 + 本体の Compose 非依存検査の通過 (→ Requirement: Kotlin 公開面の Loading / LD-AN-01〜02)

## 4. MAUI binding

- [x] 4.1 `IKsLoading` + `Loading.Instance` + gateway (両 OS の PlatformGateway / Hostless) + bridge (Swift / Kotlin) の Loading 面 — MAUI 層は状態を持たず Native coordinator へ委譲 (→ Requirement: C# 公開面の Loading / LD-MA-01)
- [x] 4.2 Loading 版1行登録糖衣 + DI 配線 (→ 同 Requirement / LD-MA-02)
- [x] 4.3 placement・LoadingStyle・進捗のパススルー検証 (→ 同 Requirement / LD-MA-03)

## 5. KMP facade

- [x] 5.1 commonMain の `KsLoading` + `expect object Loading` + gateway (android / ios、iOS は interop 経由。状態は Native coordinator へ委譲) (→ Requirement: 共有コードからの Loading 呼び出し / LD-KM-01)
- [x] 5.2 commonMain 公開面の検査 (スタイル型・options 型の不在を含む) (→ 同 Requirement / LD-KM-02)
- [x] 5.3 commonMain のカスタム Loading 面 (共有 VM 契約・VM 受け show / start・進捗受け口 interface) と Android actual の typealias (→ Requirement: 共有 VM によるカスタム Loading / LD-KM-03)
- [x] 5.4 iOS Swift パッケージ KMP 面の Loading 版型付き登録・表示 facade + 内部 interop + compile 検査 (→ 同 Requirement / LD-KM-03〜04)

## 6. Samples (4ルート)

- [x] 6.1 SampleText へ文言追加 + `Default Loading` デモ項目を4ルートに追加 (→ Requirement: Default Loading デモ項目 / LD-SA-01)
- [x] 6.2 `Custom Loading` デモ項目 (進捗受け口つき VM + カスタム View) を4ルートに追加。**mock/approved-sample-custom.png との視覚照合** (→ Requirement: Custom Loading デモ項目 / LD-SA-02)
- [x] 6.3 4ルートのパリティ通し (→ Requirement: 4ルートで同一デモが動く / LD-SA-03、verification 証跡)

## 7. 検査基盤

- [x] 7.1 `scripts/scenario-id-coverage.py`: LD 系領域を対象・ミラー必須に追加し、Sample 専用 (LD-SA-*) を allow-missing に登録。ミラー判定は現行の領域名のみの集合 (MIRROR_AREAS) では他 prefix の同名領域を巻き込むため、**(prefix, 領域) の組で持てるよう検査基盤を変更する** (→ design Decision 5、second-opinion spec-001 m2)
- [x] 7.2 api-surface 負の検査の追加 (表示 API にスタイル引数が存在しないこと・commonMain にスタイル型が存在しないこと 等) (→ Requirement: 各形態公開面 / LD-KM-02)
