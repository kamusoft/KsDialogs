---
id: 0005
title: 公開識別子の写像表 — 素の名前は Native へ、MAUI は namespace 素・NuGet ID 修飾
status: accepted
date: 2026-08-14
---

## Context

KsDialogs は iOS (SwiftPM) / Android (Maven) / KMP (Maven) / MAUI (NuGet) の4エコシステムへ公開する識別子を必要とする。ドメイン骨格は KsSettingsView cross/0002 (accepted) から翻案できる — 所有ドメイン `kamusoft.jp` を根拠に reverse-DNS は `jp.kamusoft.*`、Maven groupId は `jp.kamusoft`、.NET は PascalCase。

本プロジェクト固有の判断は2点: (1) KsSettingsView 時点には無かった Android Native と KMP の Maven artifact 並立で、素の artifactId をどちらに割てるか。(2) .NET で使う形態が MAUI だけである本プロジェクトの namespace / NuGet ID に platform 修飾を刻むか。

## Decision

| 形態 | コード上の識別子 | 配布上の識別子 |
|---|---|---|
| iOS | Swift モジュール名 `KsDialogs` | SwiftPM パッケージ名 `KsDialogs` |
| Android | Kotlin パッケージ `jp.kamusoft.ksdialogs` | Maven `jp.kamusoft:ksdialogs` |
| KMP | Kotlin パッケージ `jp.kamusoft.ksdialogs.kmp` | Maven `jp.kamusoft:ksdialogs-kmp` |
| MAUI | .NET namespace `KsDialogs` | NuGet ID `KsDialogs.Maui` |

- 素の artifactId `ksdialogs` はリブランド方針「Native 主」(cross/0001) に整合させ Android Native に割てる。KMP の Gradle multiplatform 公開は `ksdialogs-kmp-android` 等の variant を自動生成するため `-kmp` 接尾辞で衝突なく共存する
- KMP の Kotlin パッケージは Native への委譲構図 (core/ADR-0001) でのクラス名衝突を避けるため `.kmp` を切る
- MAUI の namespace は素の `KsDialogs` とする。namespace はコードの文脈内 (MAUI プロジェクト) で読まれるため MAUI であることは自明。NuGet ID は文脈のない場所 (csproj 依存リスト・nuget.org 検索) で読まれるため `.Maui` を残し、原典 `AiForms.Maui.Dialogs` や `CommunityToolkit.Maui` の流儀に合わせる。KsSettingsView は RootNamespace `KsSettingsView.Maui` だが PackageId の明示はなく、先例拘束はない

## Alternatives Considered

- **Maven artifactId を両方接尾辞 (`ksdialogs-android` / `ksdialogs-kmp`) にする案** — 却下。曖昧さはゼロだが素の名前が死蔵される
- **素の `ksdialogs` を KMP に割てる案** — 却下。薄いファサード (core/ADR-0001) に旗艦名を与えるのは主従が逆転する
- **NuGet ID も素の `KsDialogs` に揃える案** — 却下。ID 単体で MAUI 前提が伝わらず、「maui dialog」検索で ID トークンに当たらない。将来 MAUI 以外の .NET 形態が増えた場合に素の名前が MAUI に占有済みになる点も不利

## Consequences

- 正: 全識別子が `kamusoft.jp` の所有根拠で Maven Central 検証まで通せる
- 正: 土台である Native 実装が各エコシステムで最短の名前を持ち、「Native 主」がパッケージ名からも読み取れる
- 正: MAUI 利用者のコードは `using KsDialogs;` の最短形になる
- 負: MAUI だけ NuGet ID と namespace が不一致になる (ID `KsDialogs.Maui` / namespace `KsDialogs`)
- 負: 識別子は全 platform で同一形式にならず、lowercase reverse-DNS / Maven 座標 / PascalCase を使い分ける (KsSettingsView cross/0002 と同じ帰結)

出典: kasane/roadmaps/library-foundation/phases/phase-2-monorepo-scaffold/history.md (2026-08-14: 公開識別子の写像表)
