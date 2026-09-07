---
id: 0026
title: Loading の器は iOS = key window 直貼り / Android = 専用の全画面透過 Window とし、レイアウト・演出の部品は Dialog の器から切り出して共有する
status: accepted
date: 2026-08-25
---

## Context

Loading の器を OS 提示スタックから分離し部品を Dialog と共有する方針 (ADR-0022) の、実装形 (画面への載せ方の具体と、共有部品の持ち方) を確定する必要がある。

- 既存の Dialog の器は、iOS が present 連鎖の先端に載せる `DialogContainerViewController`、Android が `android.app.Dialog` 派生の `DialogContainer` (Activity とは別の Window に表示される)
- レイアウト適用とトランジション実行 (フック起動・覆いの別レイヤフェード) の実装は、この Dialog の器の内部に埋まっていた
- Loading は「ダイアログ表示中に、視覚・入力の双方でダイアログより手前に出る」必要がある

## Decision

- **iOS**: key window への `addSubview` (直貼り) で Loading 専用オーバーレイを載せる (移植元で実証済みの方式)
- **Android**: **Loading 専用の全画面透過 Window** を最前面に出す (移植元と同方式)。既存の Dialog の器は Activity と別の Window に表示されるため、Activity の decorView への直貼りでは手前に出られない。この Window は Dialog 機構の提示経路・多段表示の意味論には参加しない
- **Android の Activity 再生成 (回転等)**: 器 (Window) は使い捨てとし、状態の正である coordinator (ADR-0027) が保持する合流状態から新しい resumed Activity へ再取り付けする (`ResumedActivityTracker` の入れ替わり購読を利用)
- **部品の切り出し**: レイアウト適用とトランジション実行は、Dialog の器の内部実装を挙動不変のリファクタリングで共有部品 (iOS `DialogLayoutApplier` / `DialogTransitionRunner`、Android `DialogLayoutHost` / `DialogTransitionRunner`) へ切り出し、Dialog / Loading 両方の器から使う。切り出しは既存 Scenario テストとレイアウト共通ケース表を回帰ガードにして先行実施する

「ダイアログ表示中に Loading が視覚・入力の双方で最前面になる」ことは、両 OS の実提示 Scenario テストで固定する。

## Alternatives Considered

- **Android も decorView への addView** — 却下: `android.app.Dialog` ベースの既存 Dialog 器は Activity と別の Window に表示され、decorView の子はその背面になる。「ダイアログより手前・背後の操作遮断」を満たせない (提案レビューで成立不能と判明)
- **iOS も専用 UIWindow / Android は WindowManager への直接 addView** — 却下: ウィンドウレベル管理・キーウィンドウ切り替え・トークンとライフサイクルの自前管理が増える。iOS は直貼り、Android は Dialog ベースの透過 Window という移植元方式で要件は満たせる
- **部品を切り出さず Loading 器に同型実装を持つ** — 却下: ADR-0022 が却下した完全独立機構の縮小再生産。トランジション・レイアウト適用の修正が常に2箇所になる

## Consequences

- 正: 移植元で実証済みの方式により「ダイアログより手前・入力遮断」が両 OS で成立する
- 正: レイアウト・演出の実装が1箇所になり、Dialog 側の修正がそのまま Loading にも効く
- 負: 切り出しリファクタリングは既存 Dialog の器に触れるため回帰リスクがある (既存テスト全通過を器の変更前後で確認して吸収)
- 負: Android は Window の使い捨てと再取り付けの実装 (resumed Activity の入れ替わり購読) を持つ必要がある

出典: kasane/changes/archive/2026-08-26-add-loading/design.md (Decision 4) / kasane/changes/archive/2026-08-26-add-loading/second-opinion-spec-001.md (Critical: decorView 直貼りの不成立)
