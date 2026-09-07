# Delta Spec: docs-refresh (初期生成後の追従対象の確定)

対象能力: docs-refresh — 利用者向けドキュメントの追従更新。adopt-docs-refresh (`kasane/changes/archive/2026-09-04-adopt-docs-refresh/specs/docs-refresh/spec.md`) で据えた契約に、初期生成の完了で確定する事項を足す。

## MODIFIED Requirements

### Requirement: 追従対象の規範

SKILL.md は追従対象を **5 Skill × 2 言語** と README 4 枚として定義する SHALL。Skill は `ksdialogs-ios` / `ksdialogs-android` / `ksdialogs-maui` / `ksdialogs-kmp` / `ksdialogs-aiforms-migration` の 5 本で、各 Skill は `SKILL.md` (能力マップ) + `references/` (レシピ) の構成をとる。`ksdialogs-kmp` は 1 本で共有コード側 (commonMain) と Android ホスト側・iOS ホスト側を扱い、`SKILL.md` は共有コード側 + 3 側の Setup + 最小コードに絞り、ホスト側の登録レシピは `references/` で振り分ける SHALL。`ksdialogs-aiforms-migration` は `SKILL.md` + `references/api-mapping.md` の構成で、移植元で Obsolete だった Toast は互換 shim を持たない新実装として扱う SHALL: 旧 message 入口 (`Toast.Instance.Show(message)`) には新 Toast の message 入口と挙動差 (上限クランプなし・多重は重なる・完全非対話) の移行案内を持ち、旧 View 登録 (`Show<TView>()`) は対応先なしとしてカスタム View の登録経路は MAUI Skill へ案内する。README 4 枚は `skills/README.md` / `skills/README_ja.md` / ルート `README.md` / `README_ja.md` で、manifest の `readmes` が正である。Skill の構成の見直し (新設・廃止・references の分割方針) は docs-refresh の守備範囲外である SHALL NOT。

#### Scenario: 追従対象の特定

- **GIVEN** 配置済みの SKILL.md
- **WHEN** 追従対象の節を読む
- **THEN** 5 Skill の名前と構成 (KMP は 1 本でホスト側は references/ 振り分け、移行は SKILL.md + api-mapping.md) と README 4 枚が特定でき、KsSettingsView の Skill 名 (`kssettingsview-*`) は本文に残っていない

#### Scenario: Toast の移行方針の一致

- **GIVEN** 本 change 適用後の `.agents/skills/docs-refresh/SKILL.md` 追従対象の表と、生成された `api-mapping.md` の Toast 節
- **WHEN** 両者の Toast の扱いを読み比べる
- **THEN** どちらも「message 入口は移行案内あり・View 登録は対応先なし」で一致し、「Toast は対応先なし」とだけ書いた記述が SKILL.md に残っていない

## ADDED Requirements

### Requirement: 追従対象の README 群

追従対象の README は manifest の `readmes` 配列を正とし、その内容は `skills/README.md`・`skills/README_ja.md`・ルート `README.md`・ルート `README_ja.md` の 4 枚とする SHALL。`android/layout-case-fixtures/README.md` は追従対象に含めない SHALL NOT。ルート README 2 枚は同一の委譲単位として扱う SHALL。

#### Scenario: 追従対象の列挙

- **GIVEN** 本 change を適用した `skills/.manifest.json`
- **WHEN** `readmes` 配列を読む
- **THEN** 上記 4 枚だけが列挙されている

#### Scenario: 初期生成後の起動

- **GIVEN** 本 change 完了後の作業ツリー
- **WHEN** docs-refresh を `--readme-only` で起動する
- **THEN** manifest 不在の停止案内を返さず、4 枚を対象に取って差分なしで完了する

### Requirement: API 掲載基準の参照

docs-refresh SKILL.md の 3e (API 名の網羅検査) は、仕分けの基準として `kasane/handbook/cross/user-skill-api-listing.md` を参照する SHALL。「phase-2 で起こす」旨の暫定注記を持たない SHALL NOT。

#### Scenario: 注記の差し替え

- **GIVEN** 本 change 適用後の `.agents/skills/docs-refresh/SKILL.md`
- **WHEN** 3e 節を読む
- **THEN** handbook の規約への参照があり、暫定注記が残っていない

### Requirement: 源泉 concept の追加

kmp ドメインに新設する `kmp/api/ios-host-integration.md` は manifest の concept 集合に含まれ、`ksdialogs-kmp` の `targets` の源泉として現れる SHALL。

#### Scenario: 新設 concept の追従

- **GIVEN** 本 change 適用後の manifest
- **WHEN** `concepts` と `targets` を読む
- **THEN** `kmp/api/ios-host-integration.md` のハッシュがあり、`ksdialogs-kmp/SKILL.md` と `ksdialogs-kmp/references/ios-host.md` の源泉に含まれる
