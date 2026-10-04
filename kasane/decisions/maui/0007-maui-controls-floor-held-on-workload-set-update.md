---
id: 0007
title: MAUI 本体の下限版は workload set を上げても据え置き、同梱版には合わせない (0004 を一部改訂)
status: accepted
date: 2026-10-04
amends: 0004
---

## Context

maui/ADR-0004 は MAUI 本体 (`Microsoft.Maui.Controls`) の下限版を「リポジトリが固定する workload set が同梱する版」とし、`global.json` の workload set を上げるときは `maui/Directory.Packages.props` の版を同梱版に合わせると決めた。下限・検証する版・同じ SDK の利用者の既定値を一致させ、同じ workload set の素の利用者が版を書かずに導入できるようにするためである。

開発機の更新で、`global.json` の workload set を 10.0.300.3 から 10.0.401.1 へ上げる必要が生じた。10.0.401.1 は Xcode 27.0 を必須とし、同梱する MAUI 本体は 10.0.110 である (現在の下限は 10.0.20)。決まりどおりに下限を 10.0.110 へ上げると、Xcode 26 のままの利用者は、`MauiVersion` を 10.0.110 以上に明記しない限り復元がダウングレードのエラー (NU1605) で止まる。maui/ADR-0004 が「下限を検証済みの最新版に置く」案を退けた理由と同じ状況が、古い workload set の利用者に起きる。

workload set を上げる目的は開発環境を合わせることで、利用者要件を動かすことではない。姉妹ライブラリ KsSettingsView は同じ workload set の更新で、利用者に版の引き上げを強いないよう下限を据え置いている。

前提: リポジトリが固定する workload set より古い workload set を使う利用者がいる (workload set 10.0.401.1 は Xcode 27.0 が必須で、Xcode 26 の利用者は導入できない)。据え置いた下限版が、新しい workload set でビルド・テストを通る。

## Decision

maui/ADR-0004 の決定のうち「MAUI 本体の下限版は workload set 同梱の版」(下限版をリポジトリが固定する workload set の同梱版とし、workload set を上げるときに `maui/Directory.Packages.props` の版を同梱版に合わせる) を本決定で置き換える。他の決定は維持する。

MAUI 本体の下限版は、`global.json` の workload set を上げても据え置く。ライブラリをビルド・テストする版は下限版に揃えたままとし、検証 CI が下限そのものを検証する状態を保つ。workload set の同梱版 (版を書かない利用者の既定値) が下限より新しくなることは許容する。

据え置いた下限版で新しい workload set のビルド・テストが通らないときは、下限を上げる前に止めてオーナーの判断を仰ぐ。

## Alternatives Considered

- **決まりどおり下限を同梱版 (10.0.110) へ上げる** — 却下。Xcode 26 のままの利用者が、次の版から `MauiVersion` を 10.0.110 以上に明記しないと復元エラー (NU1605) で止まる。検証する版が新しい workload set の既定値と一致する利点より、利用者要件を動かさないことを優先した
- 出典に、上記以外の代替案の記載はない

上げる案では、MAUI 10.0.110 を Xcode 26 の環境で指定して使えるかも確かめていない。

## Consequences

- 正: workload set を上げても利用者要件 (MAUI 本体の下限) が動かず、古い workload set の利用者がそのまま更新できる
- 正: 検証 CI が下限そのものをビルド・テストし続ける
- 負: 下限・検証する版・同じ SDK の利用者の既定値の一致が崩れる。新しい workload set の既定値とライブラリの組み合わせは、版を書かない消費者検証アプリのビルドだけが確かめる
- 負: 下限が古い版に留まるため、その版が新しい workload set で通らなくなった時点で、下限を上げる判断が別に要る
- 負: maui/ADR-0004 の読み手は、下限版の決め方だけ本 ADR を開く必要がある

## Revisit When

- 据え置いた下限版が、リポジトリが固定する workload set でビルド・テストを通らなくなったとき
- 前提 (Context) が崩れたとき

---
出典: kasane/changes/archive/2026-10-04-align-toolchain-xcode27-jdk21-dotnet-10-0-401/exploration.md (論点 3: MAUI 本体の下限 — 検討した選択肢・決定事項) / ../KsSettingsView/kasane/changes/archive/2026-10-04-align-toolchain-xcode27-jdk21-dotnet-10-0-401/exploration.md (論点 3: `Microsoft.Maui.Controls` の下限の据え置き)
関連: maui/ADR-0004 (MAUI NuGet の 3 パッケージ構成。本決定は下限版の決め方だけを置き換える) / cross/ADR-0023 (toolchain 固定境界の MAUI 本体の行を maui/ADR-0004 に委ねる。委ね先の決め方が本決定で変わる)
