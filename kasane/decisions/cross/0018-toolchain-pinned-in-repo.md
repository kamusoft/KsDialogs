---
id: 0018
title: 検証に用いる toolchain の版はリポジトリ内で固定し、.NET SDK / workload set の固定は repo 直下の global.json で行う
status: accepted
date: 2026-09-08
amends: 0004
---

## Context

検証 CI (cross/ADR-0017) と手元で同じ版の toolchain を使うために、版の固定境界を決める必要があった。それまでの KsDialogs はリポジトリ内に `global.json` が無く .NET SDK の版が環境任せで、親ディレクトリの `global.json` (workload set 10.0.101 = .NET for iOS 26.1) が拾われて既定の Xcode 26.5 では MAUI iOS が組めず、`DEVELOPER_DIR` を付け替える運用になっていた。`Microsoft.Maui.Controls` の版は facade / Tests / ApiSurfaceCheck の 3 csproj に 10.0.1 と直書きされ、Sample は版を持たず workload set の既定に任せていた。

cross/ADR-0004 は「4 形態を独立ビルドルートとし、リポジトリルートには共通ビルドファイルを置かない」と決めている。`global.json` は .NET がカレントディレクトリから親へ遡って最初に見つけたものを使う SDK 解決の起点で、`maui/` と `samples/maui` の 2 つのビルドルートを同じ版で覆うにはリポジトリ直下に置く必要がある。

前提: CI のランナーイメージは版指定 (`macos-26` / `ubuntu-24.04`) で Xcode 26.5 を同梱し、Xcode の版を明示選択できる。.NET の workload set は SDK の feature band と対で解決される。

## Decision

cross/ADR-0004 の決定のうち「リポジトリルートには共通ビルドファイルを置かない」を、**toolchain の選択ファイル (`global.json`) に限ってルートに置いてよい**と置き換える。4 形態の独立ビルドルートと composite build の決定は維持する (`global.json` はビルド定義ではなく SDK 解決の起点)。

検証に用いる toolchain の版はリポジトリ内 (workflow 定義・`global.json`・版の一元宣言) で明示し、ランナーイメージの既定値や親ディレクトリの設定に依存しない。固定境界は次のとおり。

| 対象 | 固定の粒度 | 置き場 |
|---|---|---|
| ランナーイメージ | 版指定 (`macos-26` / `ubuntu-24.04`) | workflow |
| Xcode | メジャー.マイナー (26.5。パッチはイメージ同梱内の変動を許容。無ければ一覧を出して失敗) | workflow の変数。iOS を扱う全 job (ios / kmp / maui) に適用 |
| JDK | ディストリビューション + メジャー (Temurin 17) | workflow |
| .NET SDK と workload set | 完全指定 (10.0.300 / 10.0.300.3。指定 SDK が無ければロールフォワードせず失敗) | repo 直下 `global.json` |
| MAUI 本体 (`Microsoft.Maui.Controls`) | 10.0.70。`maui/` 配下は central package management で一元宣言、restore 元は nuget.org 単一 + source mapping | `maui/Directory.Packages.props` / `maui/nuget.config` |
| Sample の MAUI 本体 | 同じ版をプロパティ (`MauiVersion`) で明示 (利用者が真似する形を保つ) | `samples/maui` の csproj |
| Android Emulator | API レベル 36 | workflow |
| 外部 action | commit SHA (version はコメント) | workflow |

全 workflow の `permissions` は `contents: read` に限る。

## Alternatives Considered

- **SDK 10.0.101 のまま Xcode 26.1 に固定する** — 却下。`macos-26` に 26.1 が残る保証がなく、iOS Native job と MAUI job で Xcode が 2 本立てになる
- **`global.json` を置かずランナー任せにする** — 却下。再現性が無く、他の clone では版が環境任せのまま
- **`global.json` を `maui/` 配下に置く (ルートを避ける)** — 却下。`samples/maui` は別ビルドルートで同じ SDK を要し、両方を覆う位置はリポジトリ直下しかない。翻案元 KsSettingsView も直下に置く
- **`Microsoft.Maui.Controls` を各 csproj に直書きのままにする** — 却下。同じ版が 3 箇所に重複して取り残される (現に 10.0.1 のまま残っていた)。一元宣言なら版の変更が 1 箇所の diff に現れる
- **Sample には版を持たせず、推移的に facade の版へ上がることを期待する** — 却下 (実装時に撤回)。NuGet は推移参照の上位版へ持ち上げず、直接参照が下位だとダウングレード扱いのエラー (NU1605) になる。翻案元の Sample も版をプロパティで明示している
- **`rollForward` を省略する** — 却下 (実装レビューで撤回)。既定では同じ feature band の新しいパッチへ進み得て、「版が diff なしに変わらない」が守れない

## Consequences

- 正: 手元と CI が同じ版で動き、`DEVELOPER_DIR` の付け替えなしに既定の Xcode で MAUI iOS が組める
- 正: 版の変更が workflow / `global.json` / 一元宣言の diff として pull request に現れる
- 正: restore 元が nuget.org 単一に固定され、手元の追加フィードが混入しない
- 負: 版を上げる箇所が複数になる (`global.json`・`Directory.Packages.props`・Sample の csproj・workflow の変数)。Sample は CI で組まないため、ずれても検査に掛からず手で揃える
- 負: 指定した SDK が無い環境ではビルドが失敗する (近いパッチを黙って拾わない)
- 負: 外部 action の SHA と gitleaks 等の checksum は版を上げるたびに更新が要る

## Revisit When

- ランナーイメージから固定した Xcode のメジャー.マイナーが消えたとき
- .NET SDK / workload set / MAUI 本体 / Kotlin を更新するとき (更新は toolchain 更新の変更として扱う)

---
出典: kasane/roadmaps/package-distribution/phases/phase-4-verification-ci/agenda.md (決定事項: MAUI job の toolchain 固定) / kasane/changes/archive/2026-09-08-add-verification-ci/proposal.md / 同 specs/verification-ci/spec.md (Requirement: ツールチェーンの再現性) / 同 deviation.md (Sample の `MauiVersion` 明示) / 同 review-001.md (`maui/nuget.config` の欠落) / 同 second-opinion-code-001.md (#2 `rollForward`)
関連: cross/ADR-0004 (4 形態のビルドルート分離。本決定はルートの `global.json` に限って一部改訂) / cross/ADR-0017 (検証 CI の構成)
