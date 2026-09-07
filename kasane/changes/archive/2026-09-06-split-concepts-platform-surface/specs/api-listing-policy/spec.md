# api-listing-policy (利用者向け Skill の API 掲載基準)

## MODIFIED Requirements

### Requirement: 利用者向け Skill の API 掲載基準

`kasane/handbook/cross/user-skill-api-listing.md` (kind: rule) が、利用者向け Skill に公開 API をどこまで載せるかの方針 (簡潔でも網羅)、意図的な掲載除外の基準、KsDialogs の現行除外リスト、コード例のコメント規約、してはいけないこと (除外リストに無い未掲載 API を独断で除外しない・除外 API を concepts から消さない) を持つ SHALL。docs-refresh SKILL.md の 3e はこの規約を参照する SHALL。現行除外リストは、concepts の契約 / 公開面分割後の docs-refresh 3e の報告に対するオーナー判断で組み直したものとし (書き込みは ksn-concept の経路で行い、frontmatter の `timestamp`・handbook index・`concepts/log.md`・構造 lint を伴う)、各行は platform・除外 API・基準・実装上の経路と理由を持つ SHALL。分割前のリストにあった「内部層・interop 層」「低頻度の細部 API」「機械的に導出できる名前」「可視性引き下げ候補」の行は、分割後も報告される限り理由をそのまま引き継ぐ SHALL。「対象 Skill 外・機械検査由来」の基準行は、分割後に platform 側に残る非 API token (ファイル名・例示型名・Gradle DSL 名など) に限って残す SHALL。

#### Scenario: 3e の仕分け

- **GIVEN** 再生成後の skills/ に対する docs-refresh 3e の報告
- **WHEN** 報告された未掲載 API 名を規約に照らす
- **THEN** 各名前が「掲載する (Skill を修正)」か「除外リストに載っている」のどちらかに仕分けられ、除外リストの各行に基準が付いている

#### Scenario: 実判断の行の引き継ぎ

- **GIVEN** 分割前の除外リストで基準が「内部層・interop 層」「低頻度の細部 API」「機械的に導出できる名前」の行 (例: `LoadingCoordinator` / `DialogViewRegistry.Shared` / `localSwiftPackage` / `IMauiInitializeService`) と、分割後の 3e の報告
- **WHEN** 分割後も報告される名前を組み直したリストで探す
- **THEN** 同じ基準と理由で載っている

#### Scenario: 他 platform 名の行の消滅

- **GIVEN** 組み直した除外リスト
- **WHEN** 基準が「対象 Skill 外・機械検査由来」の行を数える
- **THEN** 他 platform の公開名・framework 型を理由とする行が 0 件で、残る行があれば非 API token のみ
