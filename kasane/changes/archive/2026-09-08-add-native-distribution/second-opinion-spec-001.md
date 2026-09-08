# セカンドオピニオン: add-native-distribution (spec-001)
**相方**: codex / **label**: so-spec-add-native-distribution / **日付**: 2026-09-08 / **対象**: 提案一式 (proposal / design / specs / tasks)
---
# レビュー結果: add-native-distribution

**日付**: 2026-09-08  
**判定**: **NEEDS_DISCUSSION**  
**指摘件数**: Critical 0 / Major 6 / Minor 4 / Suggestion 0

## サマリー

配布方針そのものは具体的ですが、このままでは「全タスク完了」と判定しても、破壊的同期処理の運用規約違反、実行時依存の欠落した Maven artifact、署名未設定、Android テストの空振り、検証失敗後に残る公開 tag を見逃せます。

また、Android 座標変更後の Sample の依存形が未確定です。実装開始前に spec/design/tasks を修正し、再レビューすべきです。

静的レビューのみを実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — always
- `kasane/handbook/cross/test-execution.md` — 完了判定とテスト件数
- `kasane/handbook/cross/verification-ci.md` — CI の保証範囲
- `kasane/handbook/cross/local-development-setup.md` — Gradle root・Sample 参照方式
- `kasane/handbook/cross/diagnostic-message-language.md` — 改名対象パス
- `kasane/lessons/spec-review.md` L-001 / L-002
- cross/ADR-0006 / 0008 / 0009 / 0017 / 0018 / 0019、android/ADR-0001
- 翻案元 `add-spm-distribution` / `add-android-maven-distribution` の spec・deviation・review

## 指摘事項

### [🟠 Major] 翻案元の削除実装が本リポジトリの削除規則と衝突する

**該当箇所**: `design.md:14-25`、`tasks.md:34`、`../KsSettingsView/scripts/spm-snapshot/sync-snapshot.sh:141-147`

**問題点**: design/tasks は同期スクリプトを「配信リポジトリ名とコメントだけ変更してコピー」するとしていますが、翻案元は `rm -rf` を使用します。本リポジトリには「削除コマンドは rm ではなく trash」という上位指示があります。翻案元では CI 可搬性を理由に deviation として明示的に受容されましたが、その裁定は KsDialogs には継承されません。

`trash` は通常 GitHub Actions runner に存在しないため、これは実装時の単純差し替えでは解決できない設計判断です。

**推奨修正**: 次のいずれかを事前に決定してください。

- macOS/Linux の両方で使える `rm` 以外の削除方式を設計する。
- CI 用スクリプト内に限る例外としてオーナー承認を得て、deviation に記録する。

その決定までは `Open Questions: なし` としないでください。

### [🟠 Major] 検証失敗時の prerelease tag 削除を実行フローが保証しない

**該当箇所**: `specs/spm-distribution/spec.md:57-69`、`design.md:31-40`、`tasks.md:40-42`

**問題点**: Requirement は失敗時にも tag を削除するとしていますが、Scenario は「検証が完了した」成功経路しか扱いません。tasks も依存解決が成功した後に削除する直列手順なので、7.2 が失敗して作業が停止すると、iOS だけが公開された prerelease tag が残ります。

削除対象も「リモート tag」だけで、作業コピーに残る local tag の扱いが決まっていません。

**推奨修正**:

- tag 作成後は成功・失敗を問わず cleanup が走る `trap` / finally 相当の実行手順を定義する。
- 失敗経路の Scenario を追加する。
- 作成した特定 tag の local・remote 両方を削除し、既存の無関係な tag は保持することを検証する。

### [🟠 Major] Maven メタデータの受け入れ条件が実行時依存を検証しない

**該当箇所**: `specs/android-maven-distribution/spec.md:38-55`、`android/ksdialogs/build.gradle.kts:66-72`、`android/ksdialogs-compose/build.gradle.kts:57-66`

**問題点**: spec は compile 依存とテスト依存の不在だけを確認します。次の `implementation` 依存が runtime variant/POMから欠落しても、全 Scenario を満たせます。

- core: `kotlinx-coroutines-android`
- Compose: `compose-ui`、`lifecycle-runtime`、`savedstate`

この状態の artifact はコンパイルできても実行時に `NoClassDefFoundError` となり得ます。また「compile は annotation/runtime だけ」という Requirement は、自動追加され得る `kotlin-stdlib` を考慮しておらず、字義どおりなら正しい publication まで不合格にします。

**推奨修正**: module ごとに次を明示してください。

- POM の compile/runtime scope
- `.module` の API/runtime variant
- first-party `ksdialogs-core`
- Kotlin 標準ライブラリ
- 公開 ABI 由来の `api` 依存
- 内部実装に必要な runtime 依存
- テスト依存の不在

### [🟠 Major] Android の「既存テスト維持」を現在の手順では判定できない

**該当箇所**: `specs/android-maven-distribution/spec.md:14-17`、`tasks.md:5-7`、`kasane/handbook/cross/test-execution.md:31-35,61-79`

**問題点**:

- `./gradlew test` は負の API surface 検査を1件も実行しません。各プロパティを付けた個別の失敗ビルドが必要です。
- 物理的に移動する `src/androidTest` の333件は `test` では実行されません。
- `--rerun-tasks` がなく、移動した build 出力を再利用して実行件数を誤認する余地があります。
- spec は「肯定・否定ケースが同じ結果」と要求しますが、WHEN に否定ケースを起動する操作がありません。

**推奨修正**:

- `./gradlew test --rerun-tasks` と module ごとの件数確認を明記する。
- 全 Android negative flag を個別に実行し、期待診断で失敗することを確認する。
- 少なくとも CI と同じ API 36 の `connectedDebugAndroidTest` を改名後に実行し、2 module の実行件数を確認する。
- CI に委ねるなら、対象 workflow の実行結果を完了条件として tasks に明記する。

### [🟠 Major] Compose 利用者が最終的に書く依存形が確定していない

**該当箇所**: `design.md:82-90`、`specs/android-maven-distribution/spec.md:62-69`、`samples/android/app/build.gradle.kts:46-51`、`kasane/decisions/cross/0008-distribution-model-standard-channels.md:28-32`

**問題点**: 現行 Android Sample は本体と Compose artifact の2行を直接依存しています。機械的にリネームすると、

```kotlin
implementation("jp.kamusoft:ksdialogs-core:...")
implementation("jp.kamusoft:ksdialogs:...")
```

の2行が残ります。一方、cross/ADR-0008/0019 は Compose 利用者が `jp.kamusoft:ksdialogs` 1点だけを追加し、core は推移解決される契約です。

design は substitution の2座標を定めていますが、アプリ側の最終依存集合を定めていません。Sample が利用者境界を表さなくなる可能性があります。

**推奨修正**: spec/tasks に次を明記してください。

- `samples/android` のアプリ依存は `jp.kamusoft:ksdialogs` 1点。
- `samples/kmp` の Android app も Compose artifact 1点。
- core は Compose artifactまたはKMP facadeからの推移依存で解決される。
- substitution は必要な座標を明示してよいが、アプリの直接依存数とは区別する。

### [🟠 Major] 署名設定が完全に欠落しても受け入れ条件を満たす

**該当箇所**: `specs/android-maven-distribution/spec.md:38-45`、`tasks.md:16-18`

**問題点**: Requirement は「鍵があるときのみ署名必須」としますが、tasks が実行するのは鍵なしのローカル発行と、SNAPSHOT による Central 拒否だけです。`signAllPublications()` や `SigningExtension.setRequired(...)` を実装しなくても全検証が通ります。

phase-9 で初めて発覚すると、release workflow が署名不足で止まります。

**推奨修正**: 一時生成したテスト用署名鍵を用い、ネットワーク送信なしで以下を確認する Scenario を追加してください。

- 鍵なしの `publishToMavenLocal` は成功し `.asc` を要求しない。
- 鍵ありでは両 publication に署名成果物が生成される。
- 不完全な鍵設定では署名タスクが失敗する。

### [🟡 Minor] `-Pversion=` の入力範囲が未定義で SNAPSHOT ガードを迂回できる

**該当箇所**: `design.md:42-51`、`specs/android-maven-distribution/spec.md:19-36`

**問題点**: 空文字、空白付き値、SemVerでない値も「注入値あり」として採用されます。これらは `-SNAPSHOT` で終わらないため Central ガードの対象外です。

**推奨修正**: build root で非空の SemVerまたは許容する prerelease 形式を検証するか、入力検証をphase-9へ委ねるなら、その責務境界と直接 Gradle 実行時の挙動を明記してください。

### [🟡 Minor] 改名後に虚偽となる handbook と docs-refresh 本体が追随対象から漏れている

**該当箇所**: `proposal.md:15,24`、`tasks.md:7`、`kasane/handbook/cross/local-development-setup.md:128-130`、`.agents/skills/docs-refresh/SKILL.md:490-518`

**問題点**: proposal/tasks は handbook 2本だけを追随対象にしていますが、`local-development-setup.md` も旧座標と旧project名を断定しています。

さらに docs-refresh 本体が旧座標を「正しい識別子」と定義しています。利用者向け `skills/` を後で正しく更新しても、次回 docs-refresh が旧座標を正として扱う経路が残ります。

**推奨修正**:

- `local-development-setup.md` をパス・依存1点契約に追随する。
- `.agents/skills/docs-refresh/SKILL.md` の座標表、正当例、説明を新座標へ更新する。
- 派生物である `skills/` とREADMEの更新は、現在のNon-Goalのままで構いません。

### [🟡 Minor] 破壊的同期スクリプトのテストが継続的に実行されない

**該当箇所**: `tasks.md:34-36`、`.github/workflows/ci.yml:239-251`

**問題点**: 同期テストは実装時に一度実行するだけです。将来スクリプトが変更されても、現在のCIはそのテストを実行しません。翻案元では実装レビュー後にCIへ追加された安全網です。

一方で cross/ADR-0017 はlint jobを5検査として固定しているため、無断追加も避ける必要があります。

**推奨修正**: lint jobへ正式に追加してADRの扱いを決めるか、専用checkまたはphase-9 workflowの必須事前処理として継続実行経路を定義してください。

### [🟡 Minor] sources jar の存在しか検証されない

**該当箇所**: `specs/android-maven-distribution/spec.md:42-45`、`tasks.md:23`

**問題点**: sources jar が空、片方のmoduleのソースが欠落、パッケージ配置が壊れていても合格します。翻案元は同じ指摘を受け、代表パッケージのソース含有検査を追加しています。

**推奨修正**: `ksdialogs-core` と `ksdialogs` の各sources jarについて、公開パッケージの代表ソースまたは全 `.kt` inventory が含まれることを確認してください。

## アクションプラン

1. 削除方式と検証tagの失敗時ライフサイクルをオーナー判断で確定する。
2. Maven metadata・署名・version入力の契約を検証可能なScenarioへ改める。
3. Sampleの直接依存を1点に固定し、Androidのunit・negative API・instrumented検証をtasksへ追加する。
4. `local-development-setup.md` とdocs-refresh本体を追随対象へ加える。
5. 修正版を独立スペックレビューへ再提出する。



## 突き合わせ結果

ホスト側の自己レビュー (2 周) では検出していなかった指摘が多く、クロスモデルの価値が出た回。採否:

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| Major 1 | `rm -rf` と全体ルール `trash` の衝突 | **採用 → オーナー裁定で確定** | オーナー判断 (2026-09-08)「CI はルール外なので rm -rf でよい」。design Decision 1 に例外として記載、task 6.1 |
| Major 2 | 失敗時の tag 削除が保証されない | **採用** | spm spec の Requirement と Scenario (成功時 / 失敗時) を分け、local + remote 削除・他 tag 不変を明記。design Decision 2、task 7.1 / 7.3 |
| Major 3 | runtime 依存が検証されない | **採用** | android spec の Requirement を compile / runtime スコープごとの列挙に書き直し (Kotlin 標準ライブラリは対象外)。design Decision 6、task 4.1 |
| Major 4 | 既存テスト維持を判定できない | **採用** | Scenario を「ユニット + API 形状検査 (否定ケース個別)」と「instrumented (API 36)」に分離、`--rerun-tasks` と件数 (294 / 39) を明記。task 1.2 |
| Major 5 | Compose 利用者の依存形が未確定 | **採用** | Sample の直接依存を `jp.kamusoft:ksdialogs` 1 行に固定し本体は推移。spec Requirement / Scenario、design Decision 7、proposal、task 5.1 |
| Major 6 | 署名設定の欠落を検出できない | **採用** | Requirement「発行物の署名」を新設 (鍵なし / 鍵あり)。design Decision 4、task 3.5 |
| Minor 1 | `-Pversion=` の入力範囲 | **採用** (Minor のまま) | 形式検査を導出式に組み込み、Scenario「不正な注入値の拒否」を追加。design Decision 3、task 2.1 / 2.2 |
| Minor 2 | `local-development-setup.md` と docs-refresh 本体の漏れ | **採用** | proposal What Changes、task 1.3 / 1.4 |
| Minor 3 | 同期テストの継続実行経路 | **採用 → オーナー裁定で確定** | lint job に step を追加し、cross/ADR-0017 は蒸留時に amends (design ADR 候補)。task 6.4 |
| Minor 4 | sources jar の内容検証 | **採用** | Requirement と Scenario に公開パッケージの `.kt` 含有を追加。task 4.1 |

未解決: なし (Major 1 / Minor 3 は 2026-09-08 のオーナー裁定で確定)。降格: なし。
