# セカンドオピニオン: expand-api-surface (code-001)

**相方**: codex (器: ksn-reviewer / session <uuid>) / **日付**: 2026-08-19 / **対象**: HEAD (154d4fb) に対する未コミット作業ツリー差分全体 + `kasane/changes/expand-api-surface/` 一式

---

# レビュー結果: expand-api-surface

**日付**: 2026-08-19
**判定**: **CHANGES_REQUESTED**

## サマリー

ホスト側で実施済みの全ビルド・テスト成功を前提に静的レビューしました。KMP 向け Swift 公開 API に、凍結済みの宣言表と一致しない公開 enum case が追加されています。`deviation.md` もないため、未承認の仕様逸脱として修正が必要です。

## 指摘事項

### 🟠 Major: KMP 公開エラー型に宣言表外の case が追加されている

**該当箇所**: `ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift:12`

**問題点**:
`KsDialogsKmpError` に次の2 case が追加されています。

- `presentationHostUnavailable`
- `showFailed(description:)`

しかし、公開 API の正である `kasane/changes/expand-api-surface/design.md:129` は `notRegistered` と `resultTypeMismatch` の2 case だけを宣言しています。デルタスペックも `specs/kmp-facade/spec.md:76` で、エラー型を Decision 6 の宣言表どおり提供するよう要求しています。

公開 enum の case 追加は利用者の網羅的 `switch` に影響するため、無害な内部実装差ではありません。また、`ios/Tests/KsDialogsTests/KsDialogsKmpFacadeTests.swift:245` が宣言表外の case を期待しており、テストも逸脱を固定しています。全テスト green でも、この契約不一致は検出されません。

**推奨修正**:
この変更では `KsDialogsKmpError` を宣言表の2 case へ合わせてください。提示先不在など、それ以外の失敗は既存の基底エラーを維持して伝播させるなど、公開 enum を拡張しない経路にします。該当テストも修正し、可能なら2 case の網羅的 `switch` または API スナップショットによる形状検査を追加してください。

4 case へ統一すること自体が製品要件なら、凍結済み仕様を実装側だけで拡張せず、別途オーナー判断を経る必要があります。

## アクションプラン

1. `KsDialogsKmpError` と変換処理を宣言表へ一致させる。
2. 宣言表外の case を期待するテストを修正し、公開形状の退行検査を追加する。
3. iOS テストと公開 API コンパイル検査を再実行する。

**件数**: Critical 0 / Major 1 / Minor 0 / Suggestion 0
**総合判定**: **CHANGES_REQUESTED**

---

## 突き合わせ結果 (2026-08-19)

ホスト側レビュー `review-001.md` (判定 CHANGES_REQUESTED / Major 1・Minor 3・Suggestion 3) と突き合わせた。**両者の Major は重複ゼロで、互いに別の箇所を突いている** — ホスト側は Android のビルド検査の起動経路、相方は KMP Swift 公開面のエラー型形状。

| # | 指摘 | 状況 | 採否 |
|---|---|---|---|
| 相方 Major | `KsDialogsKmpError` に宣言表外の case 2つ (`presentationHostUnavailable` / `showFailed`) | 相方のみ + 根拠強 | **採用** |

採用理由: (1) 該当箇所が `KsDialogsKmpError.swift:12` と特定されている (2) 実害シナリオが具体的 — 公開 enum の case 集合は利用者の網羅的 `switch` に影響し、内部実装差では済まない (3) `specs/kmp-facade/spec.md:76` の Requirement「宣言表どおりの Swift 公開面」が「登録・show・**エラー型**は design.md Decision 6 の宣言表どおりの形で提供すること (SHALL)」と明記しており、客観的な spec 不適合である (4) `deviation.md` が存在せず、オーナー合意のない乖離である。

ホスト側レビュアーは「宣言レベルの形 (引数・型引数・result ラベル・別名) はすべて一致」と判定しており、enum の case 集合には触れていない — ここが見逃しにあたる。

なお `kmp/ADR-0004` はエラー型の case 集合を限定しておらず (「型不一致は `resultTypeMismatch` 相当を throw し内部表現を見せない」のみ)、Consequences でも「エラー型の分だけ公開面が広がる」と述べるにとどまる。よって ADR 上の制約ではなく、**凍結済み宣言表への適合**が論点である。

対応方針: 宣言表の2 case へ寄せる修正を委譲する。ただし提示先不在などの失敗を利用者が判別できなくなる場合は、公開 enum を広げない表現経路 (既存の公開エラー型の伝播など) を採る。2 case では契約を満たせないと実装側が判断した場合は、実装を変えずに理由を報告して停止させ、オーナー判断 (仕様側の修正か deviation 合意か) に回す。

**降格 / 未解決**: なし (相方の指摘は1件のみで、矛盾する指摘も発生しなかった)。

---

## 修正後の再確認 (2026-08-19 / 同一セッション turn 2)

採用した Major の修正を同じ相方セッションに提示し、解消判定を求めた。**判定: 解消 (新たな問題なし) / 総合判定: APPROVED**。

1. **宣言表との一致**: `KsDialogsKmpError.swift:12` は `notRegistered` / `resultTypeMismatch` の2 case のみで Decision 6 と一致。非 `@testable` 側の網羅 switch (`KmpApiSurfaceCompileChecks.swift:115`) により、将来の無断追加を検出できる
2. **エラー情報・判別可能性**: 未登録時の型名、型不一致時の期待型・実型は保持されている。それ以外は元のエラーをそのまま返し、提示先不在も `DialogError.presentationHostUnavailable` として判別可能なことがテストで固定されている (`KsDialogsKmpFacadeTests.swift:240`)
3. **内部表現の露出**: `DialogError` は既存の公開契約型であり内部表現ではない。内部マーカー `KsDialogsInteropResultTypeMismatch` は公開面へ出る前に `DialogError.resultTypeMismatch` へ変換され、さらに `KsDialogsKmpError.resultTypeMismatch` へ写されるため、「内部表現を公開面に出さない」契約にも反しない

修正範囲に Critical / Major / Minor / Suggestion の新規指摘なし。
