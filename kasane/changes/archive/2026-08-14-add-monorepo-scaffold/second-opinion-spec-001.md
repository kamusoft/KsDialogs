# セカンドオピニオン: add-monorepo-scaffold (spec-001)
**相方**: codex / **日付**: 2026-08-14 / **対象**: 提案一式 (proposal.md / specs/repo-scaffold/spec.md / tasks.md)
---
# レビュー結果: add-monorepo-scaffold

**判定**: `NEEDS_DISCUSSION`  
**指摘件数**: Critical 0 / Major 7 / Minor 0 / Suggestion 0

## サマリー

4つのビルドルートを分離する方向性は ADR と概ね整合していますが、前提 ADR がすべて未承認であり、変更級・実装中の ADR 更新・検証方法にも未解決事項があります。このまま実装すると「ビルドは通ったが、iOS 対応や composite 接続など、この変更で確認したかった事項は証明できていない」という状態になり得ます。

静的レビューのため、ビルド・テストは実行していません。ファイルも変更していません。

## 指摘事項

### [🟠 Major] 未承認の ADR を確定事項として仕様化している

**該当箇所**: `kasane/changes/add-monorepo-scaffold/proposal.md:5`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:3`、`kasane/decisions/core/0001-native-independent-thin-wrappers.md:4`、`kasane/decisions/cross/0002-tech-stack-2026-08.md:4`、`kasane/decisions/cross/0004-monorepo-four-build-roots.md:4`、`kasane/decisions/cross/0005-public-identifier-mapping.md:4`

**問題点**: proposal はアーキテクチャを「確定した」と扱い、spec は各 ADR に従うことを規範化していますが、参照する4件はすべて `status: proposed` です。Kasane の ADR 規約上、`proposed` は人間の承認前のドラフトであり、実装の確定根拠にはできません。

**推奨修正**: オーナーが4件を確認し、必要な修正を済ませて `accepted` に昇格してから本提案を承認してください。未決定のまま進める場合は、少なくとも proposal に承認ゲートを明記し、それまでは実装開始不可としてください。

### [🟠 Major] M 級判定が変更内容と整合しない

**該当箇所**: `kasane/changes/add-monorepo-scaffold/proposal.md:9`、`kasane/changes/add-monorepo-scaffold/proposal.md:27`、`kasane/changes/add-monorepo-scaffold/proposal.md:29`

**問題点**: 本変更は iOS・Android・KMP・MAUI の4ドメインにまたがり、ビルドルート構成、composite build、技術バージョン、公開識別子という高コストなアーキテクチャを実体化します。「repo-scaffold という単一能力」と名付けたことだけでは、Kasane の L 級条件であるアーキテクチャ・複数領域横断を回避できません。特に proposal 自身が composite build を主要リスクとして挙げています。

**推奨修正**: L 級へ再分類し、`design.md` に少なくとも次を定義してください。

- 各ビルドルートの責務と境界
- Gradle composite substitution の設定方法
- バージョンカタログ共有方法
- 最低 OS の設定箇所
- 各ルートの正確な検証コマンド
- 配布基盤を作らず識別子だけ宣言する方法

### [🟠 Major] ADR を変更しないという説明と、実装タスクが矛盾する

**該当箇所**: `kasane/changes/add-monorepo-scaffold/proposal.md:26`、`kasane/changes/add-monorepo-scaffold/tasks.md:13`、`kasane/decisions/cross/0002-tech-stack-2026-08.md:19`

**問題点**: proposal は既存の `kasane/` に触れないとしていますが、task 2.3 は `cross/ADR-0002` への追記を要求しています。また、ADR を実装前に `accepted` にするなら、accepted 後は不変という規約にも抵触します。逆に proposed のまま実装するなら、前項の未承認問題が残ります。

**推奨修正**: `swift-tools-version` は実装開始前の調査事項として確定し、ADR-0002 を修正後に承認してください。task 2.3 は「ADR の確定値と Package.swift が一致することを検証する」タスクに変更し、実装中の ADR 書き換えをなくしてください。

### [🟠 Major] `swift test` では iOS ビルドルートの疎通を検証できない

**該当箇所**: `kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:7`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:11`、`kasane/changes/add-monorepo-scaffold/tasks.md:12`

**問題点**: `swift test` は通常ホストの macOS 向けにパッケージをビルド・実行します。空ソースの段階では成功しても、iOS SDK、iOS 17、UIKit を利用する将来の Native 実装がビルド可能であることは証明できません。「iOS ビルドルートの疎通」という Requirement に対する受け入れ試験として不十分です。

**推奨修正**: macOS 上の高速確認として `swift test` を残す場合でも、iOS Simulator SDKを対象にした `xcodebuild build/test` 等を受け入れ条件へ追加してください。最低でも iOS 17 の deployment target と、iOS Simulator 向けコンパイル成功を検証対象にする必要があります。

### [🟠 Major] 最低対象 OS が各成果物の検証可能な条件になっていない

**該当箇所**: `kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:3`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:25`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:29`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:45`、`kasane/changes/add-monorepo-scaffold/tasks.md:11`、`kasane/changes/add-monorepo-scaffold/tasks.md:24`、`kasane/changes/add-monorepo-scaffold/tasks.md:29`

**問題点**: Android Native の `minSdk 24` だけは明示されていますが、SwiftPM、KMP の Android/iOS、MAUI の Android/iOSについて、最低 OS をどの設定で保証するかが決まっていません。冒頭で ADR を参照するだけでは、Scenario の合否判定や実装箇所を一意にできません。

**推奨修正**: プラットフォーム別の設定値を Requirement と Scenario に明記してください。

- SwiftPM: iOS 17
- Android Native: minSdk 24
- KMP Android: minSdk 24
- KMP iOS: iOS 17
- MAUI Android/iOS: Android 24 / iOS 17

設定ファイルを解析して値を検証する受け入れ条件も追加してください。

### [🟠 Major] クリーンチェックアウトで要求された Gradle コマンドを実行できるタスクになっていない

**該当箇所**: `kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:23`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:34`、`kasane/changes/add-monorepo-scaffold/tasks.md:17`、`kasane/changes/add-monorepo-scaffold/tasks.md:23`

**問題点**: Scenario はクリーンチェックアウトで `./gradlew build` を要求しますが、Android・KMP のどちらにも Gradle wrapper の作成タスクがありません。Gradle 9.7.0 を保証する検証もなく、実装者がローカルの `gradle` に依存すると再現性を失います。

**推奨修正**: 両ルートに wrapper 一式を作成するタスクを追加し、`gradle-wrapper.properties` が ADR-0002 のバージョンを指すことを検証してください。KMP の Android/iOS テストについても、実際に実行する wrapper タスク名を Scenario に明記してください。

### [🟠 Major] composite build の成立をビルド成功だけでは証明できない

**該当箇所**: `kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:31`、`kasane/changes/add-monorepo-scaffold/specs/repo-scaffold/spec.md:33`、`kasane/changes/add-monorepo-scaffold/tasks.md:23`、`kasane/changes/add-monorepo-scaffold/tasks.md:34`

**問題点**: `./gradlew build` の成功だけでは、`jp.kamusoft:ksdialogs` が本当に included build の `:ksdialogs` へ置換されたことを直接証明できません。将来同座標がキャッシュやリポジトリに存在すると、外部成果物を解決しても Scenario が通る可能性があります。また、group・project name・明示的な dependency substitution のどれを正とするかも決まっていません。

**推奨修正**: composite 接続を観測可能にしてください。例えば、明示的な dependency substitution を定義したうえで、依存解決結果が included build を指すこと、または Android 側の対象タスクが実行グラフへ入ることを検証します。必要なら隔離した `GRADLE_USER_HOME` を使い、外部キャッシュによる偽陽性を防いでください。

## 追加で仕様へ織り込むべき検証

上記 Major の修正時に、次も受け入れ条件へ含めるのが妥当です。

- `README.md`、`.gitignore`、`LICENSE` は存在だけでなく必須内容を検証する。
- `.gitignore` の「4形態分」を、代表的な生成物の具体例で定義する。
- 「共通ビルドファイル」の禁止対象を `等` ではなく明示的なパターンで定義する。
- ADR-0005 にある SwiftPM パッケージ名 `KsDialogs` も、公開識別子 Scenario の検証対象に含める。
- パッケージング非ゴールとの境界として、Maven座標・NuGet IDをどのメタデータへ宣言するかを決める。

## アクションプラン

1. 変更を L 級として扱うかオーナー判断を確定する。
2. `swift-tools-version` を含む4件の ADR を確定し、`accepted` にする。
3. `design.md` でビルド・composite・OS 下限・識別子宣言を具体化する。
4. Scenario を、正確なコマンドと機械判定可能な期待値へ修正する。
5. tasks.md を修正版 Scenario と一対一に対応させてから実装へ進む。

## 突き合わせ結果

ホスト側自己レビュー (指摘なし) との突き合わせ。全7件が相方のみの指摘。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | 未承認 (proposed) の ADR を確定根拠にしている | **降格** | ハーネスのパイプライン上、ADR は起票時 proposed → 人間承認 / ksn-distill で確定が設計。4件の内容はオーナーが agenda 議論で決定済み (history に経緯あり)。ただし早期 accept はオーナーに提案する |
| 2 | M 級判定が変更内容と整合しない (L 級 + design.md を要求) | **未解決 → ユーザーへ** | 級の確定はオーナーの責務 (ksn-core)。ホスト推奨 M / 相方主張 L で矛盾のため NEEDS_DISCUSSION として提示 |
| 3 | 「kasane/ に触れない」と task 2.3 (ADR-0002 追記) の矛盾 | **採用** | 実在する記述矛盾。proposal の Impact を修正 (ADR-0002 が自ら phase-2 に委ねた残課題の追記のみ例外と明記)。task 2.3 は維持 (ADR-0002 は proposed であり追記可能) |
| 4 | swift test では iOS 向けビルドを検証できない | **採用** | 技術的に正しい (swift test はホスト macOS 向け)。iOS Simulator 向けビルドの Scenario と task を追加 |
| 5 | 最低対象 OS が検証可能な条件になっていない | **採用** | 「最低対象 OS の宣言」Requirement を追加し、4ルートの宣言値 (iOS 17 / minSdk 24) を機械照合可能にする |
| 6 | Gradle wrapper の作成タスクがない | **採用** | クリーンチェックアウトで ./gradlew を要求する Scenario と実在の齟齬。tasks 3.1 / 4.1 に wrapper 一式を明記 |
| 7 | composite 置換をビルド成功だけでは証明できない | **降格** | Scenario の GIVEN が「jp.kamusoft:ksdialogs は未公開」を明示しており、グリーンフィールドでは置換なしに解決が成功し得ない。将来の偽陽性懸念は本 change のスコープ外 |

追加検証 5項目: SwiftPM パッケージ名を識別子 Scenario へ含める分のみ採用 (spec 修正)。残りは Minor 相当として修正サイクルは回さない。

### 裁定 (2026-08-14)

- 指摘 #2 (級判定): オーナー裁定で **M 維持**。理由 = アーキテクチャ判断は ADR (cross/0002・0004・0005、core/0001) に捕捉済みで design.md が複写になる。相方の要求項目は実装詳細で tasks とコードの責務。正当な論点への裁定であり「的外れ却下」ではないため lessons 捕捉は行わない
- ADR の早期 accept 提案は見送り (通常フロー: ksn-distill で確定)
