# Delta Spec: user-skills (skills/ 一式の初回生成)

対象能力: user-skills — 利用者向けドキュメントの提供形態。本デルタは `skills/` 一式 (5 Skill × en / ja + 索引 2 枚 + manifest) の初回生成後の状態を契約として定義する。manifest v3 のスキーマ規範と追従の規則は adopt-docs-refresh のデルタスペック (`kasane/changes/archive/2026-09-04-adopt-docs-refresh/specs/docs-refresh/spec.md`) と docs-refresh SKILL.md (`.agents/skills/docs-refresh/SKILL.md`) が正であり、本スペックはそれに準拠する側の契約を書く。

## ADDED Requirements

### Requirement: Skill 一式の構成

`skills/` は `skills/{en,ja}/<name>/` の 2 言語トップ分離で、各言語配下に自己完結した Skill ディレクトリ 5 本を持つ SHALL。name は `ksdialogs-{ios,android,maui,kmp,aiforms-migration}` で en / ja 同名 SHALL。各 Skill は `SKILL.md` を持ち、`references/` の構成は次のとおり SHALL: `ksdialogs-ios` と `ksdialogs-android` は `dialogs.md` / `view-models.md` / `layout.md` / `transitions.md` / `loading.md` / `toast.md` の 6 本、`ksdialogs-maui` は同 6 本 + `di-registration.md`、`ksdialogs-kmp` は同 6 本 (共有コード側から見た使い方) + `android-host.md` + `ios-host.md` (ホスト側で行う Dialog / Loading / Toast の View 登録と、iOS はさらに Swift 側の型付き入口 `Dialog.shared.kmp` / `Loading.shared.kmp` / `Toast.shared.kmp` と `@Throws` の経路表)、`ksdialogs-aiforms-migration` は `api-mapping.md` の 1 本。これ以外のファイルを Skill ディレクトリに置かない SHALL NOT。

#### Scenario: ディレクトリ構成の検査

- **GIVEN** 生成完了後の `skills/`
- **WHEN** ディレクトリ構成を検査する
- **THEN** en / ja 各配下に上記 5 Skill が同一相対パス構成で存在し (references は 6 / 6 / 7 / 8 / 1 本)、規定外のファイルが存在しない

### Requirement: frontmatter の標準準拠

各 `SKILL.md` の frontmatter は `name` / `description` / `license` / `metadata` (`language` / `source`) のみを持ち、これ以外のフィールドを使わない SHALL NOT。`metadata.language` は配置パスの言語と一致し、`metadata.source` は本リポジトリを指す SHALL。ja 版 `description` は日本語本文に英語キーワードを併記する SHALL。

#### Scenario: frontmatter の機械検査

- **GIVEN** 生成された 10 部の SKILL.md
- **WHEN** frontmatter 検査 (docs-refresh 6-④: 許可フィールドの範囲・`name` / `description` の存在・en / ja の `name` 同一・`metadata.language` とパスの一致) と、本 change の追加静的検査 (`license` の存在・`metadata.source` が本リポジトリの URL であること) を実行する
- **THEN** 両方とも違反 0 件

#### Scenario: ja 版 description の英語キーワード

- **GIVEN** 生成された ja 版 SKILL.md 5 部
- **WHEN** 独立レビューが description を読む
- **THEN** 発火に効く英語キーワード (製品名・API 名・プラットフォーム名) が併記されていることを確認項目として合格させる (機械検査の対象ではない)

### Requirement: Skill 本文の構成

各 `SKILL.md` は、概念説明 (ライブラリの位置づけと、公開 API の両入口 — 既定エントリで始め、テストや DI 構成では契約 interface を注入する — の使い分け 1 段落) → 能力マップ表 → Setup (依存宣言と最低バージョン、Skill ルート外への参照なし) → 最小コード (既定エントリのみを使う) → `references/` への振り分け、の順で構成する SHALL。`references/` の各ファイルは「やりたいこと」の自然言語見出し + 1〜2 行のリード文 + 完動コードのレシピ形式で構成する SHALL — ただし `ksdialogs-aiforms-migration/references/api-mapping.md` は例外で、内容クラスごとの節に旧 API → 新 API の対応表 (表の各行は旧メンバー・対応先または対応先なし・代替手段) を持つ読み物とし、レシピ形式を要求しない (cross/ADR-0011)。アーキテクチャ解説など利用に直結しない読み物を含めない SHALL NOT。DI の具体レシピ (`references/`) を持つのは `ksdialogs-maui` (`di-registration.md`) と `ksdialogs-kmp` (共有コードでの契約注入) だけで、`ksdialogs-ios` / `ksdialogs-android` は概念説明の 1 段落 (契約を自分の DI に登録すれば既定エントリと同じ実体を注入できる) に留める SHALL。

#### Scenario: 本文構成と両入口の説明

- **GIVEN** 生成された platform Skill 4 本の SKILL.md
- **WHEN** 節の並びと概念説明を読む
- **THEN** 上記の順で構成され、概念説明に両入口の使い分けが 1 段落あり、最小コードは既定エントリだけを使い、iOS / Android の references に DI レシピが存在しない

#### Scenario: レシピ形式

- **GIVEN** 生成された platform Skill 4 本の `references/` ファイル (api-mapping.md を除く)
- **WHEN** 各節の形を検査する
- **THEN** 「やりたいこと見出し + リード文 + 完動コード」の形になっている

#### Scenario: 対応表の形

- **GIVEN** 生成された `api-mapping.md`
- **WHEN** 各節の形を検査する
- **THEN** 内容クラスごとの節に対応表があり、各行が旧メンバー・対応先 (または対応先なし) ・代替手段を持つ

### Requirement: 閉世界性

Skill 本文と references は、Skill ディレクトリの外にあるファイル・URL (配布座標の URL を除く) を参照しない SHALL NOT。リポジトリ内部用語 (`kasane/`、ADR 番号、change-id) と利用者向けでない機械面 (`KsDialogsInteropBridge` / `KsDialogsInteropResultType`) を含まない SHALL NOT。

#### Scenario: 閉世界性の機械検査

- **GIVEN** 生成された 10 部
- **WHEN** 閉世界性・機械面の漏れ検査 (docs-refresh 6-⑤) と内部リンク解決 (6-⑥) を実行する
- **THEN** 違反 0 件

### Requirement: KMP Skill の共有コード側と iOS ホスト側

`ksdialogs-kmp` の SKILL.md の最小コードは、共有コードで show を包む関数に `@Throws(DialogException::class, CancellationException::class)` を付けた形で示す SHALL。Setup の iOS ホスト側は、前提 1 つ (KMP プロジェクト標準の iOS 連携が済んでいること) と手順 3 つ (共有モジュールに Maven 依存 1 点、`integrateLinkagePackage` を xcodeproj パス指定で 1 回実行し生成された合成パッケージを VCS に含める、Xcode の Package Dependencies に `KsDialogs-SPM` を 1 点足す) で構成し、`@Throws` について「Swift から呼ぶ共有コードの関数は suspend でも非 suspend でも宣言が要る / ライブラリの公開面は失敗しうる VM 経路にだけ宣言し message 経路は宣言しない / 無いと Kotlin/Native が例外を NSError に変換せずクラッシュする」の 3 点を本文に持つ SHALL。経路ごとの宣言の有無の表など詳細は `references/ios-host.md` に置く SHALL。SwiftPM 依存の再宣言を利用者に求めない SHALL NOT。

#### Scenario: `@Throws` の最小コードと注意書き

- **GIVEN** `ksdialogs-kmp/SKILL.md` (en / ja)
- **WHEN** 最小コードと iOS ホスト側の Setup を読む
- **THEN** 最小コードの show を包む関数に `@Throws` 宣言があり、Setup に前提 1 + 手順 3 と `@Throws` の 3 点が本文にある

#### Scenario: 統合手順の源泉

- **GIVEN** `ksdialogs-kmp` の SKILL.md と `references/ios-host.md`
- **WHEN** manifest の `targets` を読む
- **THEN** 両ファイルの源泉に `kmp/api/ios-host-integration.md` が含まれる

### Requirement: 移行 Skill の対応表

`ksdialogs-aiforms-migration/references/api-mapping.md` は、移植元 AiForms.Maui.Dialogs の公開 API (移植元 README を `kasane/concepts/cross/reference/reference-repositories.md` の対応表で解決したローカル clone から初期生成時に一度だけ書き起こす) を内容クラスごとの節で網羅する対応表 (対応先 API、または対応先なし + 代替手段) を持つ SHALL。Toast の節は次の 2 行を持つ SHALL: (1) 旧 `Toast.Instance.Show(message)` → 新 Toast の message 入口、挙動差 3 点 (duration の上限クランプなし・多重は重なって表示・完全非対話でタッチ素通し) を添える (2) 旧 `Show<TView>()` の View 登録 → 対応先なし、カスタム View の登録経路は `ksdialogs-maui` Skill を読むよう案内する。iOS の factory 契約 throws 化 (core/ADR-0033) を破壊的変更として記載しない SHALL NOT。新 API 自体の説明は `ksdialogs-maui` への Skill 名での案内で済ませる SHALL。

#### Scenario: 旧公開 API の網羅

- **GIVEN** 生成された api-mapping.md と移植元 README の公開 API 一覧
- **WHEN** 旧公開メンバーを突き合わせる
- **THEN** すべてが対応表に現れる (対応先、または対応先なし + 代替手段の別つき)

#### Scenario: Toast 節の内容

- **GIVEN** api-mapping.md の Toast 節
- **WHEN** 内容を読む
- **THEN** 上記 2 行があり、挙動差 3 点が添えられ、throws 化の記載がない

### Requirement: 生成の内容規約

生成 (fan-out の委譲プロンプト) は次の制約を明記し、生成物はこれに従う SHALL: ①コード例は原則コメントを書かず、やむを得ない最小限のコメントは英語で統一する ②ローカル絶対パスを書かない ③API 署名とコード例は concepts の記載に加えて実装コード・テストで最終確認する。concepts と実装の矛盾は drift 所見として報告し、独断でどちらも書き換えない SHALL NOT ④各ワーカーは生成した各ファイル (言語抜きの Skill 相対パス単位) の源泉 concepts のマップを成果物と併せて報告する。

#### Scenario: コード例のコメント規約

- **GIVEN** 生成された全コードブロック
- **WHEN** コメントを検査する
- **THEN** コメントは原則存在せず、存在する場合は英語である

### Requirement: 翻訳ロックステップ

en / ja の対応する Skill ファイルは同一構成 (見出し階層の並び・コードブロックの数と順序) で、対応するコードブロックは byte 一致する SHALL。両言語とも日本語の concepts から直に書き起こす SHALL (英語版からの翻訳派生ではない)。片言語のみの生成は発生しない SHALL NOT。

#### Scenario: ロックステップの機械検査

- **GIVEN** 生成された en / ja の全ファイルペア
- **WHEN** 節構成一致 (6-②) とコードブロック byte 一致 (6-③) を実行する
- **THEN** 全ペアで違反 0 件

### Requirement: manifest 初期版

`skills/.manifest.json` を docs-refresh SKILL.md の「manifest v3 の構造 (規範)」に従って書き出す SHALL。manifest が扱う concept の集合は `kasane/concepts/` 配下の `*.md` から `index.md` / `log.md` / `rules.md` を除いたもの (本 change 完了時点で 11 本) とする SHALL。`concepts` のキー集合はこの集合と過不足なく一致し、各値は本 change の全タスク完了時点のファイル内容の SHA-256 と一致する SHALL。`targets` はワーカー報告の源泉マップから構成し、生成された全 Skill ファイルが言語抜き相対パスでちょうど 1 キーとして存在する SHALL。`excluded` は `cross/reference/reference-repositories.md` の 1 本 (理由文字列つき) とし、他の全 concept は `targets` のいずれかの値に現れる SHALL (網羅不変条件)。`ksdialogs-aiforms-migration` 配下のファイルの源泉は新 API 側の concepts のみとする SHALL。`readmes` は `skills/README.md` / `skills/README_ja.md` / `README.md` / `README_ja.md` の 4 枚とする SHALL。

#### Scenario: 網羅不変条件の検査

- **GIVEN** 書き出された manifest と `kasane/concepts/` の全 concept ファイル
- **WHEN** 網羅検査 (docs-refresh 6-①) を実行する
- **THEN** 未参照かつ未除外の concept が 0 件

#### Scenario: スキーマ準拠とハッシュの最終状態一致

- **GIVEN** 全タスク完了後の作業ツリーと書き出された manifest
- **WHEN** 必須キー・不変条件を検証し、concept 集合の SHA-256 を再計算して `concepts` と突き合わせる
- **THEN** すべて満たされ、キー集合・ハッシュ値ともに過不足なく一致する

### Requirement: 索引 README

`skills/README.md` (英語) と `skills/README_ja.md` (日本語) は、① Skill 一覧表 (name / 対象 / 1 行説明 / en・ja リンク、5 行)、②コピー手順 (`.agents/skills/` を共通コピー先の第一候補、Claude Code は `.claude/skills/`)、③利用者は片言語のみコピーする前提の明記、の 3 要素のみで構成する SHALL。

#### Scenario: 索引の構成検査

- **GIVEN** 生成された索引 2 枚
- **WHEN** 内容を検査する
- **THEN** 3 要素がすべて存在し、Skill 一覧が 5 行で、それ以外の節が存在しない

### Requirement: レビュー 4 層

初期生成の完了条件として、①機械検査 (docs-refresh の検査スクリプト 8 本: concepts 網羅 / en・ja 節構成一致 / コードブロック byte 一致 / frontmatter / 閉世界性・機械面 / 内部リンク解決 / identity-lint / 配信識別子の表記ゆれ、および 3d ツール最低バージョン一致・3e API 名網羅の報告) ②独立レビュー (Skill 単位、源泉 concepts との内容整合・ファイル単位の源泉完全性 (manifest `targets` の各ファイルの源泉に、その内容が依拠する concept がすべて載っていること) ・構成規約・en / ja の等価性。最終判定が APPROVED であること — 修正が入ったら再レビューする) ③初見レビュー (concepts もコードも読んでいないエージェントに Skill 本文だけを渡し、この文書だけで利用目的を達成できるか・宙に浮いた参照や意味の取れない新造語がないかを報告させる。指摘反映で SKILL.md の節が追加・削除された Skill は再実施する) ④オーナー目視検収 (少なくとも ja 5 部の通し読み) をこの順に通過する SHALL。

#### Scenario: 4 層の通過

- **GIVEN** 生成・修正反映後の skills/ 一式
- **WHEN** 4 層を順に実行する
- **THEN** 機械検査は違反 0 件、独立レビューの最終判定が全件 APPROVED、初見レビューの指摘は反映済み (再実施条件に当たれば再実施済み)、オーナー検収の指摘が処理済み (反映または起票) である
