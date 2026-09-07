---
name: docs-refresh
description: "KsDialogs の利用者向け Agent Skills (skills/{en,ja}/) と README 群を kasane/concepts/ と現行実装の最新状態に追従させる差分更新スキル。skills/ は利用者向け派生物であり、知識の正本は concepts とコード・テスト。Use when: ユーザーが「skills を更新して」「docs-refresh して」「README 古くなってない？」と明示的に依頼したとき、または concepts の更新 (蒸留・再移行) 後にユーザーが skills/ への追従を求めたとき。**自発的な自動発動はしない** — skills/ はユーザー向け成果物であり、更新は必ずユーザーの承認フロー (Step 4) を通す。skills/ 一式の初期生成は本スキルの守備範囲外 (変更フローが担う)。"
license: MIT
metadata:
  author: kamusoft
  version: "3.0"
---

KsDialogs の利用者向け Agent Skills (`skills/{en,ja}/<name>/`) と各種 `README.md` を、`kasane/concepts/` と現行実装の最新状態に追従させるメンテナンスフロー。**初期生成 (skills/ 一式と manifest 初期版の作成) ではなく、継続運用のための差分更新スキル**である。

> 翻案元の出典: 本スキルは姉妹ライブラリ KsSettingsView の `.agents/skills/docs-refresh/` (commit `33ce94e`、2026-09-02 時点) を翻案したもの。固有値 (追従対象・3d の取得元・6-⑤ / 6-⑧ の検出パターン) を KsDialogs 向けに置き換えてある。`scripts/` 8 本のうち 7 本は無改変で、`scripts/link-resolution-check.py` のみ対象一覧のパスを環境変数 `DOCS_REFRESH_TARGETS` で受ける差分を持つ (既定値は翻案元と同じ)。

## 目的とゴール

このスキルは「`kasane/concepts/` とコード・テストを知識の正本、`skills/` を**利用者向け Agent Skills**」という関係を維持し続けることを目的とする (根拠: cross/ADR-0011 — `kasane/decisions/cross/0011-user-docs-as-agent-skills.md`)。concepts の改訂や実装変更が起きたときに、**追従漏れによる陳腐化**を最小コストで検知・修正する。

**読者翻訳の原則**: concepts は開発者・エージェント向けの契約文書 (責務境界・保証・禁止事項) であり、その文体・構成を skills/ へそのまま写さない。skills/ は利用者のエージェントが「何ができるか」を把握し、必要なレシピ 1 本だけを読んで目的を達成できる形へ**翻訳**する。逆に skills/ は知識の正本ではないため、concepts・実装と食い違う記述を見つけたら skills/ 側を直す (concepts 側に疑義がある場合は drift 所見としてユーザーへ報告し、独断で concepts を変更しない)。

### 追従対象

Skill 本体は 5 Skill × 2 言語。パス構成と各 Skill のファイル構成は初期生成で確定しており、**構成の見直し自体はこのスキルの守備範囲外**:

| Skill (`skills/{en,ja}/<name>/`) | 内容 |
| --- | --- |
| `ksdialogs-ios` | iOS 利用者向け。`SKILL.md` (能力マップ) + `references/` のレシピ |
| `ksdialogs-android` | Android 利用者向け。同上 |
| `ksdialogs-maui` | .NET MAUI 利用者向け。同上 |
| `ksdialogs-kmp` | KMP 利用者向け。1 本で共有コード側 (commonMain)・Android ホスト側・iOS ホスト側の 3 側を扱う。`SKILL.md` は「共有コード側 + 3 側の Setup + 最小コード」に絞り、ホスト側の登録レシピは `references/` で振り分ける |
| `ksdialogs-aiforms-migration` | AiForms.Maui.Dialogs からの移行者向け。`SKILL.md` + `references/api-mapping.md`。旧 Toast の View 登録入口は対応先なしとして扱い、新 Toast の message 入口は移行後に採用できる代替として挙動差とともに案内する (旧 API に message 入口は存在しない) |

README 群の追従対象は **manifest の `readmes` 配列が正**。初期生成時点で登載が想定されるのは次:

| ファイル | 役割 |
| --- | --- |
| `skills/README.md` / `skills/README_ja.md` | Skill 一覧・コピー手順・片言語コピーの前提 |
| `README.md` / `README_ja.md` (ルート) | リポジトリの顔・対応プラットフォーム表・インストール・最小コード例・skills 導線 |

追従対象はこの **4 枚だけ**。`samples/` 配下の旧開発者向け README は廃止済みで、Sample のビルド・実行手順は handbook、KMP iOS の統合構成は concepts が正を持つ。将来ほかの README が追加されても、変更フローが `readmes` を書き換えない限り docs-refresh の追従対象にしない。追従対象の README を増減させる判断は本スキルの守備範囲外である。ルート `README.md` と `README_ja.md` は **同一の委譲単位**として扱い、要追従になったときは常に両方を同時に更新する (下の言語ペアの原則)。

### 言語ペアの原則 (翻訳ロックステップ)

`skills/en/` と `skills/ja/` は常に同一構成・同時更新とする。更新対象は「言語抜きの Skill 相対パス」で特定し、en/ja のペアとして扱う。片言語のみの更新は発生しない。両言語とも日本語の concepts から直に書き起こす (英語版からの翻訳派生ではない)。

## コンテキスト節約方針

**メインオーケストレーターは concepts の本文を読み込まない**。差分検出と更新方針の判断は、ハッシュ計算・manifest 参照・コードを正とする機械チェックだけで行い、concepts 本文の読み込みと生成はサブエージェントへ委譲する。

サブエージェント (隔離コンテキスト) への委譲ができない実行環境では、**メインが代わりに concepts 本文を読んで処理するフォールバックを取らない**。Step 4 の承認提示と skills/・README 群・manifest への書き込みを行わないまま、委譲機構が使えないため実行不能である旨を案内して停止する。

**可否の判定は起動時 (Step 1) に行う。** 委譲できないと分かった時点で、それ以降の手順 (manifest 検証・差分検出・承認提示・書き込み) へ進まずに停止する。

**サブエージェントは常に器 `ksn-implementer` で起動する** (起動の指定方法は実行環境ごとに異なる — Claude Code では Task ツールの `subagent_type`、codex ではエージェント name を指定して spawn する。以降「器を指定する」はこの意味)。器を指定しない起動はメインのモデルを継承するため、メインをより高階層のモデルで運用しているときに、concepts 本文と en/ja 一式を読み書きする最も重い作業が同じ高コストのモデルで走ってしまう。器を指定すると、その作業は器定義が持つモデル・エフォートで走り、メインの編成から切り離せる (サブエージェントへの文脈隔離自体は器の有無によらず成立する — 器が担うのは編成の切り離しである)。器名は本スキル内に直書きで固定し、外部の編成定義 (Kasane の worker-dispatch や `kasane/config.yaml` の `workers:` 節) は参照しない (起動のたびの読み込みを増やさないため)。器名が変われば本スキルの記述を直す。

## Input

引数なしで起動できる。任意の引数:

- `--all`: ハッシュ差分検出をスキップし、manifest の `targets` に列挙された全 Skill ファイル (en/ja ペア) と `readmes` を要追従とする全再生成。網羅検査 (3c) と API 名網羅検査 (3e) はスキップしない。**manifest 前提は維持する** (不在・破損なら Step 2 で停止)
- `--readme-only`: manifest の `readmes` に列挙された README 群のみを追従対象とする軽量チェック。skills/ 本体は検出・更新の対象にしない
- `--all` と `--readme-only` の**同時指定はエラー**。何も書き換えずに停止し、どちらか一方を指定するよう案内する

## Steps

### 1. プロジェクトの状態取得 (メインコンテキストで実行)

**最初にサブエージェント委譲機構が使えるかを確認する。** 具体的には「器 `ksn-implementer` を指定してサブエージェントを起動できるか (器が実行環境に配置済みか)」を確認する。確認の手段は、利用可能なサブエージェント種別の一覧に `ksn-implementer` があること、または実行環境の器定義ファイルの存在確認 (Claude Code: `~/.claude/agents/ksn-implementer.md` か `.claude/agents/ksn-implementer.md` / codex: `~/.codex/agents/ksn-implementer.toml`) のいずれかでよい (片方が成立すれば配置済みとみなす)。使えない場合は、以降の手順 (Step 2 以降) へ進まず、何も書き換えないまま実行不能として停止する (「コンテキスト節約方針」)。

器が未配置の場合は、次の趣旨を案内して停止する (メインが代わりに書くフォールバックも、器なしのサブエージェント起動もしない):

```
サブエージェントの器 ksn-implementer がこの環境に配置されていません。

docs-refresh は concepts 本文の読み込みと skills/ の生成をサブエージェントへ委譲する前提で、
メインが代わりに読んで書くフォールバックを持ちません。

Kasane リポジトリの scripts/deploy.sh (../Kasane/scripts/deploy.sh) を実行して器を配置してから
再実行してください。

今回は skills/・README 群・manifest のいずれも変更していません。
```

委譲可否を確認したうえで、並列で次を確認する:

- `kasane/concepts/` 配下の全概念ファイル一覧を取得 (`index.md` / `log.md` / `rules.md` は追従対象外)
- `skills/` 配下の現状ファイル一覧を取得
- `git log --oneline -20` で最近のコミットを把握 (何が変わったかの傍証)
- `git status` で作業ツリーの状態を確認

### 2. manifest の読み込みと検証 (メインコンテキストで実行)

`skills/.manifest.json` を読み込む。次のいずれかに該当する場合は **skills/ と README 群を一切書き換えずに停止する**:

- ファイルが存在しない
- JSON として parse できない
- `version` が `3` でない
- 必須キー (`version` / `concepts` / `targets` / `excluded` / `readmes`) の欠落、または型不正 (`concepts` / `targets` / `excluded` が object でない、`readmes` が配列でない、`targets` の値が配列でない 等)

**フルリフレッシュ (skills/ の再生成) にフォールバックしない。** 停止時は次の趣旨を案内して終了する:

```
skills/.manifest.json を読めませんでした (理由: <不在 / parse エラー / version=N / 必須キー "targets" 欠落 等>)。

docs-refresh は差分更新専用のスキルで、skills/ 一式と manifest 初期版の初期生成は行いません。
初期生成は「どの concept をどの Skill に載せるか」「レシピをどう設計するか」という創作を含むため、
承認を伴う変更フロー (Kasane の change) の実装として行う必要があります。

- skills/ が未生成の場合: 初期生成の change を起こしてください (ksn-explore / ksn-propose)。
- skills/ はあるが manifest が壊れている場合: manifest を規範スキーマ (本スキル Step 7) に沿って
  修復してから再実行してください。

今回は skills/・README 群・manifest のいずれも変更していません。
```

検証を通ったら、manifest の内容を以降の正として使う。

#### manifest v3 の構造 (規範)

```json
{
  "version": 3,
  "generatedAt": "<ISO 8601 タイムスタンプ>",
  "concepts": { "<concepts ルート相対パス>": "<sha256>" },
  "targets": { "<言語抜きの Skill 相対パス (例: ksdialogs-ios/references/dialog-basics.md)>": ["<concepts ルート相対パス>", "..."] },
  "excluded": { "<concepts ルート相対パス>": "<除外理由 (文字列)>" },
  "readmes": ["<リポジトリ相対パス>", "..."],
  "lastUpdatedFiles": ["<このリフレッシュで更新したファイルのリポジトリ相対パス>"]
}
```

不変条件:

1. `targets` のキーは `skills/en/` と `skills/ja/` の双方に同一相対パスで実在するファイルを指す
2. `targets` の値と `excluded` のキーは concepts に実在するパスを指す (実在しないものは削除済み concept として Step 3c で扱う)
3. `version` / `concepts` / `targets` / `excluded` / `readmes` は必須キー

#### 移行 Skill の源泉規則

`ksdialogs-aiforms-migration` 配下のファイル (`SKILL.md` / `references/api-mapping.md`) の `targets` には、**新 API 側の concepts (`core/api/*` のうち対応表が触れるもの + `maui/api/*` の MAUI 公開面 concept — `di-registration.md` と各 `*-surface.md`) だけ**を置く。

- 移植元 (AiForms.Maui.Dialogs) の旧 API 側は concepts に源泉を持たず、**差分検出・要追従判定の対象外**とする。移植元リポジトリが更新されても移行 Skill のファイルは要追従リストに載らない
- 旧 API 側 (旧メンバー一覧と対応先) の書き起こしは**初期生成時の一度限り**で、移植元のローカル clone を前提とする。clone の在り処は `kasane/concepts/cross/reference/reference-repositories.md` の対応表で解決する
- 逆向き — 新 API 側の concept が変われば `targets` の逆引きで `ksdialogs-aiforms-migration/...` が en/ja ペアとして要追従に載る — は通常どおり働く。docs-refresh が守るのはこの方向だけである

### 3. 差分検出

`--all` 指定時は**ハッシュ差分検出 (3a・3b) のみをスキップ**し、`targets` の全キー (en/ja ペア) と `readmes` を要追従とする。**網羅検査 (3c) は `--all` でも実行する** — スキップするのはハッシュ比較に依る差分検出であり、ハッシュに依存しない網羅検査まで外すと未配置 concept の配置判断が Step 4 に載らず、Step 6-① が `UNCOVERED` を出しても直しようがない (生成物の不備ではない) ループに陥るため。全再生成は配置漏れを潰す好機でもある。

`--readme-only` 指定時は 3a〜3c および 3e をスキップし、`readmes` のみを要追従候補とする。3d は両モードとも実行する。

#### 3a. concepts ハッシュの一括計算

```bash
find kasane/concepts -name "*.md" -not -name "index.md" -not -name "log.md" -not -name "rules.md" -exec shasum -a 256 {} \;
```

#### 3b. 変更分類と targets の逆引き

manifest の `concepts` と比較して分類する:

- **変更あり**: ハッシュが異なる concept
- **新規追加**: manifest の `concepts` にない concept
- **削除済み**: manifest にあるがファイルが存在しない concept

変更あり・新規追加の concept について、`targets` を**値から逆引き**して更新対象の Skill ファイル (言語抜きパス) を求める。求めたパスは常に `skills/en/<path>` と `skills/ja/<path>` の**言語ペア**として要追従リストに載せる。

README 群は concepts ハッシュ逆引きの対象外である (`readmes` は追従対象の列挙のみで、源泉 concepts を持たない)。README の concept 由来記述は 3d のコード正チェックと `--all` / `--readme-only` 実行時の見直しで維持する。

#### 3c. 網羅検査 (未参照かつ未除外 concept の検出)

`targets` のどの値にも現れず、`excluded` のキーにもない concept があれば**検査失敗**として報告する。該当 concept の配置判断 (どの Skill に載せるか / 理由つきで除外するか) は**ユーザーに提示し、スキルが独断で決めない**。manifest への `targets` / `excluded` 追記は判断が確定してから行う。

`targets` / `excluded` に列挙されているがファイルが存在しない concept (削除済み) は、影響する Skill ファイルの扱い (該当記述の除去) をユーザーに提示し、承認後の manifest 書き出しで `targets` / `excluded` から取り除く。

**`excluded` の初期値**は `cross/reference/reference-repositories.md` の 1 本だけ (理由: 開発環境のローカルパス対応表であり利用者向け Skill の対象外)。残る concepts (`core/api/*` と `maui/api/di-registration.md`) はいずれかの Skill の `targets` に載るのが規範である。

**`architecture/` カテゴリ (core / cross) の concept は既定で除外候補**として扱う — 利用者が書くコードではなくリポジトリ内部の責務境界・ビルド基盤を述べるものが多いため。ただし**既定は候補であって自動除外ではない**: 網羅検査が未参照・未除外として報告した時点で「architecture カテゴリのため除外候補 (既定)」を添えてユーザーに提示し、理由つきで確定するまで `excluded` にも `targets` にも書かない。利用者に効くもの (例: Compose 分離 module の構成) はユーザーの選択で `targets` へ回す。

検査対象の manifest は環境変数 `DOCS_REFRESH_MANIFEST` で差し替えられる (既定はディスクの `skills/.manifest.json`)。Step 3c ではディスクの manifest をそのまま使い、Step 6-① では承認済み判断を反映した予定 manifest を渡す。

```bash
python3 .agents/skills/docs-refresh/scripts/concepts-coverage-check.py
```

出力の読み方: `UNCOVERED` は未参照かつ未除外の concept (配置判断が必要)、`DELETED` は manifest にあるが実在しない concept (`targets` / `excluded` の整理が必要)。どちらも出なければ `concepts coverage OK`。

#### 3d. コードを正とする機械チェック (1 種)

concepts ハッシュ差分とは独立に、**コードを正**として次の 1 種を突合する。源泉が concepts ではないため manifest には載せず、この手順として持つ。差分があった項目は該当 README / Skill ファイルを要追従リストへ追加する。ただし `--readme-only` 実行時は、この突合で Skill ファイル (各 `SKILL.md` の導入節) 側に差分が出ても要追従リストへは載せず**報告のみ**とする — 「skills/ 本体は検出・更新の対象にしない」というフラグの意味を優先し、完了サマリに「次回の通常実行で処理される」旨を添える。

**先に前提を確認する。** ① の取得元は「KMP ルートが android の version catalog を共有している」ことで Android / KMP 両形態を賄う。`kmp/settings.gradle.kts` の `versionCatalogs` が `from(files("../android/gradle/libs.versions.toml"))` で android のカタログを読んでいることを確認し、**外れていたら android 側の値を KMP の正として読まず、前提が外れた旨を報告して停止する** (KMP 側の取得元を決め直す判断が要るため、この手順の中で代替を推測しない)。

取得元は次の 4 行:

| # | 項目 | 取得元 (コード = 正) | 抽出方法 | 突合先 |
| --- | --- | --- | --- | --- |
| ① | AGP / Kotlin / minSdk / compileSdk | `android/gradle/libs.versions.toml` の `[versions]` (`agp` / `kotlin` / `android-minSdk` / `android-compileSdk`) | 該当キーの行を読む。KMP ルートは同じカタログを共有するため Android / KMP 両形態でこの 1 ファイルが単一取得元 | ルート README 群の対応プラットフォーム表・開発環境要件、および該当記載を持つ `ksdialogs-android` / `ksdialogs-kmp` の `SKILL.md` 導入節 |
| ② | Gradle | `android/gradle/wrapper/gradle-wrapper.properties` と `kmp/gradle/wrapper/gradle-wrapper.properties` の `distributionUrl` (**2 本読む**) | 2 本の `distributionUrl` から版を取り出す。食い違う場合は README 突合とは別に**食い違いそのものを報告する** | 同上 |
| ③ | Swift tools / iOS Deployment Target | `ios/Package.swift` | 先頭の `// swift-tools-version:` 行と `platforms:` の `.iOS(.vNN)` を読む | ルート README 群の対応プラットフォーム表・開発環境要件、および該当記載を持つ `ksdialogs-ios` / `ksdialogs-kmp` の `SKILL.md` 導入節 |
| ④ | .NET TFM / 対象 OS 下限 / MAUI 本体下限 | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` | `<TargetFrameworks>`、ios / android 条件つき `PropertyGroup` の `<SupportedOSPlatformVersion>` (2 つ)、`Microsoft.Maui.Controls` の `PackageReference` の `Version` を読む | ルート README 群の対応プラットフォーム表・開発環境要件、および `ksdialogs-maui/SKILL.md` (en/ja) の導入節 |

> 突合先の読み方: いずれの行も、差分があればまずルート README 群 (`README.md` / `README_ja.md`) を要追従にする。加えて、その値を導入節に書いている Skill があればその Skill の `SKILL.md` を en/ja ペアで要追従にする (例: ④ の `Microsoft.Maui.Controls` の版が食い違えば `ksdialogs-maui/SKILL.md` の en/ja)。README の表に toolchain 版を載せるかどうかは README 側の内容判断で、この手順は読む場所と突合先の対応だけを持つ。

> 取得元の注記: 各 module の `build.gradle.kts` は `libs.versions.android.minSdk` のようにカタログを参照するだけで値を持たないため**取得元にしない**。バージョンの単一宣言元は常に version catalog 側である。

> 従前の「モジュール一覧」と「Sample デモ画面一覧」の突合は**行わない**。ルート README は利用者の入口に純化しモジュール表を持たず、`samples/` 配下の README は開発者向けで追従対象 (`readmes`) に載せないため。Sample の実ソースにデモ画面が増減しても、この手順は要追従リストに何も追加しない。

#### 3e. API 名の網羅検査 (concepts → skills の内容突き合わせ)

3a〜3c はファイル単位の追従しか守れない — concepts に昔から書かれている API 名が skills 側に一度も登場しない「翻訳時の取りこぼし」は、ハッシュ不変のため永久に検出されない。この検査は `targets` の紐付けを **Skill 単位に集約**し、その Skill の源泉 concepts に登場する API トークンが、その Skill のどのファイル (ja 版で代表 — コード・API 名は en/ja で一致するため片側で足りる) にも登場しないものを報告する。

- **結果は要追従リストへ自動昇格させない (報告のみ)**。Step 4 の提示に「API 名の未掲載候補」として載せ、ユーザーが項目指定で承認したファイルだけを要追従に加える (未掲載が意図的な絞り込み — 低頻度 API の非掲載方針 — である場合を独断で潰さないため)
- `--readme-only` ではスキップする (skills/ 本体を対象にしないため)。`--all` でも実行し、結果は全再生成のサブエージェントへ「変更理由」の補足として渡す
- トークン抽出はバッククォート括りの識別子に限る。ヒューリスティックゆえ誤検出はあり得る — 報告のみの位置づけを変えないこと。既知の誤検出源は2つ: ① core (クロスプラットフォーム) concepts に書かれた**他プラットフォームのトークン** (iOS Skill に対する Android 実装名等) — concepts が「core = platform 非依存の契約 / `<platform>/api/` = 公開面」へ分割された後 (cross/ADR-0014) は原則発生しない。報告されたら誤検出として畳まず、まず manifest の `targets` を見る: 源泉が**自 Skill の承認済み源泉**なら配置違反ではなく、掲載漏れか除外規約 (`kasane/handbook/cross/user-skill-api-listing.md`) での仕分けの対象である。承認済み源泉に他 platform の `<platform>/api/` が含まれる構成は意図的な例外で、`ksdialogs-kmp` (共有コード側 + Android ホスト側 + iOS ホスト側の 3 側: `references/layout.md` / `transitions.md` が `ios/api/` `android/api/` の layout / transition surface を、`references/android-host.md` が `android/api/` の surface 5 本を源泉に持つ) と `ksdialogs-aiforms-migration` (`maui/api/` の surface 6 本を源泉に持つ) がそれに当たる。**concepts 側の配置違反**を疑うのは、`core/api/` の concept に platform 固有の識別子が残っている場合と、`targets` に無い他 platform の `<platform>/api/` が源泉に現れた場合に限り、そのときは `kasane/concepts/rules.md` の振り分けに照らして concepts を直す。② concepts が実装解説のために挙げる**内部型・プラットフォーム標準型**。② の掲載不要の判断はユーザーが行う (Step 4 提示時に、明白な内部型・標準型はまとめて「候補外」として畳んで提示してよい)

```bash
python3 .agents/skills/docs-refresh/scripts/api-coverage-check.py
```

出力の読み方: `  <Skill 名> <- <concept パス>: <トークン列>` の行が、その Skill の源泉 concepts に出てくるのに Skill 側 (ja 版で代表) に一度も現れない API 名。何も無ければ `API-name coverage OK`。

> 仕分けの基準 (掲載すべき漏れか、意図的な除外か) は `kasane/handbook/cross/user-skill-api-listing.md` が正である。検出結果を同規約の掲載基準と除外リストに照らし、未分類の候補だけを Step 4 の「API 名の未掲載候補」として提示する。掲載可否はユーザーが個別に判断する。

### 4. 更新方針の提示 (実行前確認)

要追従リストを**要約してユーザーに提示**する。例:

```
skills 追従提案:
  concepts 変更検出: 2 件
    1. core/api/loading-semantics.md (ハッシュ変化)
       → ksdialogs-ios/references/loading.md (en/ja)
       → ksdialogs-android/references/loading.md (en/ja)
    2. maui/api/di-registration.md (ハッシュ変化)
       → ksdialogs-maui/SKILL.md (en/ja)
  新規 concept: 1 件 (網羅検査 失敗)
    3. cross/architecture/build-roots.md
       → architecture カテゴリのため除外候補 (既定)。
         除外でよければ理由をご指定ください。利用者に効く内容ならどの Skill の
         targets に載せるかをご指定ください
  削除済み concept: なし
  ツール最低バージョン変化:
    4. AGP 9.3.0 → 9.4.0 (android/gradle/libs.versions.toml)
       → README.md, README_ja.md

更新対象ファイル: 8 (Skill 6 = 3 ペア / README 2 = 1 ペア)
  - skills/en/ksdialogs-ios/references/loading.md      + ja
  - skills/en/ksdialogs-android/references/loading.md  + ja
  - skills/en/ksdialogs-maui/SKILL.md                  + ja
  - README.md
  - README_ja.md

進めて良ければ「yes」、特定の項目だけ進めたい場合は項目番号 (例: 1, 4) を指定してください。
```

3e の検出結果がある場合は、提示の末尾に「API 名の未掲載候補 (報告のみ — 反映するファイルを項目指定で承認してください)」の節として載せる。承認されたファイルだけを要追従リストへ加え、未承認分は完了サマリ (Step 8) に報告として残す。

ユーザーが承認したら 5 へ。**承認なく中止する場合は skills/・README 群・manifest のいずれも変更しない。** 部分承認 (一部項目のみ) の場合、未承認項目の源泉 concept は Step 7 でハッシュを更新しない。

### 5. サブエージェント委譲によるドキュメント更新

委譲単位:

- **Skill 単位**: 1 つの Skill の更新対象ファイル群 (en/ja 両言語をまとめて) を 1 サブエージェントが処理する。`SKILL.md` の能力マップと `references/` のレシピの整合を同一文脈で保つため、同じ Skill の複数ファイルを分割しない
- **README 単位**: README は Skill に属さないため、対象 README ごと (en/ja ペアがあればペア) に委譲する

**サブエージェントの起動は常に器 `ksn-implementer` の指定付きで行う** (Claude Code: `subagent_type: ksn-implementer` / codex: エージェント name `ksn-implementer` で spawn)。器を指定しない起動はしない (メインのモデルを継承し、編成の切り離しが成立しなくなる。「コンテキスト節約方針」)。器が未配置なら Step 1 で停止しているはずである。

並列実行できる環境では**最大 3 並列**のバッチに分割してバッチ単位で直列実行する。並列実行できない環境でも委譲単位は変えず、1 単位ずつサブエージェントへ委譲して直列実行する。**サブエージェントへの委譲自体ができない環境では、メインが代わりに書くフォールバックを取らずに停止する** (「コンテキスト節約方針」の停止条件。この時点で skills/・README 群・manifest はいずれも未変更)。委譲可否の判定は Step 1 で済ませておく規律であり、ここでの停止は Step 1 で見落とした場合の最終防波堤として置く。

#### 5a / 5b. プロンプトテンプレート

委譲するプロンプトの本文は別ファイルに置く。委譲の直前に該当ファイルを読み、`{{...}}` のプレースホルダを埋めてサブエージェントへ渡す:

- Skill 単位: [references/prompt-skill.md](references/prompt-skill.md)
- README 単位: [references/prompt-readme.md](references/prompt-readme.md) — README は Skill に属さず源泉 concepts も持たない (更新の根拠は 3d のコード正チェックと `--all` / `--readme-only` の見直し) ため別テンプレートを使う。`--readme-only` 実行時に委譲するのはこちらだけである

どちらのテンプレートも先頭にコンテキストパッケージ節を持つ。器 `ksn-implementer` は「渡されたコンテキストパッケージの読むべきスキルを読み、パッケージなしで起動されたら作業しない」と定められているため、この節を削って渡さない。

各バッチ完了後、メインオーケストレーターが進捗リストの該当ファイルを完了にマークする (サブエージェントには進捗リストを直接更新させない)。

### 6. 整合性チェック (メインコンテキストで実行)

すべての更新完了後、次の 8 検査を実行する。失敗した生成物は再修正対象に追加し、修正後に再度この一式を実行する。単発 concept の小さな修正 (typo 修正など) でも省略しない。

以降のスクリプトはリポジトリルートで実行する。

#### `--readme-only` 実行時のモード分岐

`--readme-only` は skills/ 本体を検出・更新の対象にしない (Input の定義)。したがって Step 6 でも、**再修正対象に載せてよいのは `readmes` 由来のファイルに限る**。`targets` 由来の Skill ファイルに出た失敗は再修正対象に追加せず**報告のみ**とし、次回の通常実行で処理される旨を完了サマリ (Step 8) に添える。8 検査それぞれの扱いは次のとおり:

| 検査 | 通常 / `--all` | `--readme-only` |
| --- | --- | --- |
| ① concepts 網羅 | 予定 manifest に対して実行。失敗は再修正対象 | 実行するが**報告のみ** (concept 差分を消費しないため `UNCOVERED` / `DELETED` が出るのが正常状態) |
| ② en/ja 節構成一致 | `targets` の Skill ペア + `readmes` の言語ペア | **`readmes` の言語ペアのみ** (スクリプトに `DOCS_REFRESH_README_ONLY=1` を渡して `targets` 分を外す)。再修正対象も README のみ |
| ③ コードブロック byte 一致 | 同上 | 同上 |
| ④ frontmatter 検査 | `targets` 由来の各 `SKILL.md` | **N/A** (Skill 専用検査。実行しない) |
| ⑤ 閉世界性・機械面の漏れ | 対象一覧 (Skill + README) | **README のみ**。再修正対象も README のみ (Skill ルート外リンクの検査は `skills/` 配下限定のため実質 N/A) |
| ⑥ 内部リンク解決 | 同上 | 同上 |
| ⑦ identity-lint / local-path-lint | 同上 | 同上 |
| ⑧ 配信識別子の表記ゆれ | 同上 | 同上 |

⑤⑥⑦⑧ の対象の絞り込みは、下の検査対象一覧生成が `--readme-only` で `readmes` だけを書き出すことで機械的に担保される。②③ は対象一覧ファイルを読まず `language_pairs()` が manifest の `targets` を直接参照するため、対象一覧の絞り込みでは絞られない — `--readme-only` では必ず `DOCS_REFRESH_README_ONLY=1` をスクリプトに渡して `targets` 分を外す。

**Step 6 の検査は、ディスクの `skills/.manifest.json` ではなく「この実行で書き出す予定の manifest」に対して行う。** ディスクの manifest は Step 7 まで旧状態のままであり (`targets` / `excluded` への追記は判断確定後・書き込みは最後)、それを検査入力にすると新規 concept の配置を承認した直後でも 6-① が `UNCOVERED` を出し続けて再修正ループから抜けられないためである。まず Step 4 で承認された配置判断・削除整理を反映した**予定 manifest** を一時ファイルへ書き出し、以降の検査はこれを読む:

> 一時ファイル名の注記: 判断 JSON・予定 manifest・対象一覧の `/tmp` 上のファイル名にプロジェクト名 (`ksdialogs`) を入れてあるのは、同じ docs-refresh を持つ姉妹リポジトリ (冒頭の翻案元) の実行と `/tmp` で衝突させないため。片方の実行が書いた予定 manifest や対象一覧をもう片方が読んでしまうと、他リポジトリの状態を検査したまま「適合」と読み違える。

承認により確定した判断は JSON ファイルに書き、環境変数 `DOCS_REFRESH_DECISIONS` で渡す (スクリプト本体は書き換えない)。判断が何も無い実行では、この環境変数ごと省略してよい:

```bash
cat > /tmp/docs-refresh-ksdialogs-decisions.json <<'JSON'
{
  "addTargets":   {},
  "addExcluded":  {},
  "dropConcepts": []
}
JSON
DOCS_REFRESH_DECISIONS=/tmp/docs-refresh-ksdialogs-decisions.json \
  python3 .agents/skills/docs-refresh/scripts/planned-manifest.py > /tmp/docs-refresh-ksdialogs-manifest-planned.json
```

`addTargets` は `{"<言語抜き Skill 相対パス>": ["<新規 concept パス>", ...]}`、`addExcluded` は `{"<concept パス>": "<除外理由>"}`、`dropConcepts` は削除済み concept のパス一覧 (`targets` の値と `excluded` から取り除く)。

予定 manifest は検査の入力にすぎず、ディスクへの反映は Step 7 で行う (中断時に次回が同じ差分を再検出できる規律は変わらない)。Step 7 で書き出す `targets` / `excluded` は、この予定 manifest と同一内容にする。

**環境変数はコマンド行にインラインで渡す。** エージェント実行環境ではコードブロックごとにシェルが分かれるため、`export` は次のブロックへ持続しない。予定 manifest を読ませたいスクリプト (下の対象一覧生成、6-①、6-②、6-③、6-④) はすべて、コマンド行の先頭に `DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json` を付けて起動する。付け忘れると既定値のディスク manifest (旧状態) へ**エラーを出さずにフォールバックする**ので、ブロックごとに必ず書く。`--readme-only` 実行時は同じ要領で `DOCS_REFRESH_README_ONLY=1` も併記し、検査対象を `readmes` に絞る。 6-⑥ (`link-resolution-check.py`) も同様に `DOCS_REFRESH_TARGETS=/tmp/docs-refresh-ksdialogs-targets.txt` を必ず付ける — 付け忘れると既定値 (翻案元と共通の `/tmp/docs-refresh-targets.txt`) を読み、姉妹リポジトリの残骸があればその対象一覧を黙って検査して `All internal links resolve` を返す。

検査対象ファイルの一覧も予定 manifest から導く:

```bash
DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json \
  python3 .agents/skills/docs-refresh/scripts/targets-list.py > /tmp/docs-refresh-ksdialogs-targets.txt
```

`targets` を en/ja に展開したものと `readmes` を 1 行 1 パスで書き出す。`--readme-only` 実行時は `DOCS_REFRESH_README_ONLY=1` も併記して起動する (対象一覧は `readmes` のみになる)。

#### 6-① concepts 網羅検査

Step 3c のスクリプトを、上で書き出した**予定 manifest** を入力として再実行し、`UNCOVERED` / `DELETED` が出ないことを確認する (承認された配置判断を反映してなお漏れている concept がないかの再確認):

```bash
DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json \
  python3 .agents/skills/docs-refresh/scripts/concepts-coverage-check.py
```

`--readme-only` 実行時はこの検査を**報告のみ**とする。`--readme-only` は設計上 concept 差分を消費しない (Step 7) ため、Skill 本体に未反映の concept があるのが正常状態であり、`UNCOVERED` / `DELETED` が出ても再修正対象に載せない。完了サマリに「次回の通常実行で処理される」旨を添えて報告する。

#### 6-② en/ja 節構成一致

見出し階層の並びを en/ja で比較する (見出しの文言は言語が違うので比較しない)。検査対象は `targets` の Skill ファイルペアに加え、`readmes` の**言語ペア** — 同一ディレクトリの `<stem>.md` と `<stem>_ja.md` を 1 組とみなす (例: `README.md` ↔ `README_ja.md`、`skills/README.md` ↔ `skills/README_ja.md`)。対になる `_ja` 版が `readmes` に無い README は単独扱いで、この検査の対象外。`--readme-only` 実行時はコマンド行に `DOCS_REFRESH_README_ONLY=1` も付けて起動し、`targets` の Skill ペアを対象から外す (6-③ も同じ):

```bash
DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json \
  python3 .agents/skills/docs-refresh/scripts/heading-parity-check.py
```

不一致・欠落があれば行が出る。無ければ `en/ja heading structure OK`。

#### 6-③ コードブロックの byte 一致

コード例は言語に依らず同一であること (数・順序・内容)。対象は 6-② と同じく `targets` の Skill ファイルペア + `readmes` の言語ペアで、`--readme-only` の絞り込み方も 6-② と同じ:

```bash
DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json \
  python3 .agents/skills/docs-refresh/scripts/code-block-parity-check.py
```

不一致があれば行が出る。無ければ `code blocks byte-identical`。

#### 6-④ frontmatter 検査

`SKILL.md` の frontmatter は Agent Skills 標準 6 フィールド (`name` / `description` / `license` / `metadata`、`metadata` は `language` / `source`) の範囲内。`name` は en/ja 同名、`metadata.language` はパスの言語と一致。これは Skill 専用の検査なので、`--readme-only` 実行時は **N/A** (実行しない):

```bash
DOCS_REFRESH_MANIFEST=/tmp/docs-refresh-ksdialogs-manifest-planned.json \
  python3 .agents/skills/docs-refresh/scripts/frontmatter-check.py
```

違反があれば行が出る。無ければ `frontmatter OK`。

#### 6-⑤ 閉世界性・機械面の漏れ検査

利用者向け Skill は**閉世界**である — リポジトリ内部の事情を知らない読者だけを相手にする。次の 3 種が生成物に漏れていないかを見る:

1. **リポジトリ内部用語**: Skill ファイルにある `kasane/` 配下への参照、ADR 番号の参照。ルート README の「リポジトリ構成」で要求される `kasane/` 行と `kasane/concepts/` へのリンクは対象外
2. **利用者向けでない機械面の名前**: `KsDialogsInteropBridge` / `KsDialogsInteropResultType`
3. **Skill ルートの外へ出る相対リンク**: `skills/` 配下のファイルに限り、その Skill のルート (`skills/<lang>/<name>/`) の外へ解決される相対 markdown リンク

```bash
TARGETS=($(cat /tmp/docs-refresh-ksdialogs-targets.txt))
if [ ${#TARGETS[@]} -eq 0 ]; then
  echo "検査対象が空です (manifest の targets / readmes を確認してから再実行)"
else
  SKILL_TARGETS=()
  for TARGET in "${TARGETS[@]}"; do
    case "$TARGET" in
      skills/*) SKILL_TARGETS+=("$TARGET") ;;
    esac
  done
  if [ ${#SKILL_TARGETS[@]} -gt 0 ]; then
    grep -rn -E "(^|[^A-Za-z0-9_/-])(\.{1,2}/)*kasane/" "${SKILL_TARGETS[@]}"
    grep -rn -E "ADR-[0-9]{4}" "${SKILL_TARGETS[@]}"
  fi
  grep -rn -E "KsDialogsInteropBridge|KsDialogsInteropResultType" "${TARGETS[@]}"
fi
```

③ は相対リンクの解決先を見る必要があるため、次の断片で検査する (`scripts/` には持たせない — 検査対象一覧を読むだけの短い判定であり、スクリプト 8 本は翻案元との差分を最小に保つ — 冒頭の出典注記のとおり):

```bash
python3 - <<'PY'
import os, re, pathlib

targets = pathlib.Path("/tmp/docs-refresh-ksdialogs-targets.txt").read_text().split()
if not targets:
    print("検査対象が空です (manifest の targets / readmes を確認してから再実行)")
    raise SystemExit(0)

# インライン形式 [text](path) と、行頭の参照定義 [label]: path (任意の title 付き) の両方を拾う
link_re = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
ref_re = re.compile(r"^ {0,3}\[[^\]]+\]:\s*(\S+)")
ng = 0
for t in targets:
    if not t.startswith("skills/"):
        continue
    parts = t.split("/")
    if len(parts) < 4:
        continue
    root = pathlib.PurePosixPath(parts[0], parts[1], parts[2])
    for lineno, line in enumerate(pathlib.Path(t).read_text().splitlines(), 1):
        raws = link_re.findall(line)
        ref = ref_re.match(line)
        if ref:
            raws.append(ref.group(1))
        for raw in raws:
            tokens = raw.split()
            href = (tokens[0] if tokens else "").strip("<>").split("#")[0]
            if not href or "://" in href or href.startswith(("#", "mailto:")):
                continue
            resolved = os.path.normpath(os.path.join(os.path.dirname(t), href))
            if not pathlib.PurePosixPath(resolved).is_relative_to(root):
                print(f"{t}:{lineno}: Skill ルート外への相対リンク -> {href}")
                ng += 1
print("relative links stay inside each Skill root" if ng == 0 else f"{ng} 件の Skill ルート外リンク")
PY
```

いずれかが行を出したら該当ファイルを再修正対象に追加する。

> 検査範囲の注記: ③ は `skills/` 配下のファイルにだけ適用する — README 群はリポジトリの入口であり、`skills/` への導線など Skill ルートの外を指すリンクを持つのが正しいため対象外。**外部 URL は検査しない**: SwiftPM の package URL や frontmatter の `metadata.source` は参照ではなく配布座標・出所の識別子として正当に現れる。文書としての外部参照 (他リポジトリの解説へ読者を送る等) を書かない規約は、生成プロンプト (`references/prompt-skill.md` / `prompt-readme.md`) の内容規約とレビューで担保する。
>
> ③ のリンク記法の注記: インライン形式 `[text](path)` だけでなく、行頭の参照定義 `[label]: path` (省略可能な title 付き) も解決対象に含める — 参照形式で書けば検査をすり抜けられる、という抜け道を残さないため。宛先を `<…>` で囲む形も剥がして解決する。`[]( )` のように宛先が空の記法は解決先を持たないので、例外を出さずに読み飛ばす。
> ② の注記: `KsDialogsInteropBridge` / `KsDialogsInteropResultType` は**現存する public ABI** であり、消えた API ではない。KMP cinterop がリンクするために public を保っているだけの機械面で、利用者向けの導線からは退いている (KMP 利用者の入口は Swift パッケージ側の型付き面)。したがって「これは廃止された」と書くのも誤り — 検出したら、利用者向けの入口を使う記述へ差し替える。
>
> ① の注記: README 群は利用者向け入口であると同時にリポジトリ構成への導線を持つため、リポジトリ内部用語と ADR 番号の grep は `skills/` 配下だけに適用する。拾うのは、行頭または英数字・`_`・`-`・`/` 以外の 1 文字 (空白・括弧・引用符・区切り記号に加え、「」や `**` のような全角区切り・強調記号を含む) の直後に現れる `kasane/` と、そこへ `./` `../` を任意段重ねた相対パス形 (`](../../kasane/concepts/index.md)` のようなリンク先・`- ./kasane/…` のような箇条書き・`「kasane/concepts/index.md」` のような日本語文中の言及) である。除外を文字クラスで書く (許可文字の列挙にしない) のは、`skills/ja/` の生成物で多用される全角記号を取りこぼさないため。直前が `/` や英数字の場合は当たらない — URL の途中の `…/kasane/…` (外部 URL は機械検査の対象外という上の規範と揃える) と、`mykasane/` のような単語内一致を誤検出しないため。ADR 番号は `ADR-0004` のような明示形を拾う。
>
> シェル注記: このプロジェクトの既定シェルは zsh。zsh は未クォートの変数展開を単語分割しないため、`TARGETS="a b c"` + `grep ... $TARGETS` は「`a b c` という 1 ファイル」として解決に失敗する。上記のとおり **配列 + `"${TARGETS[@]}"`** で渡すこと (bash / zsh 両対応)。配列が空のまま `grep` を呼ぶとファイル引数なしとなり標準入力待ちで固まるため、冒頭の空チェックを省略しない。

#### 6-⑥ 内部リンク解決

```bash
DOCS_REFRESH_TARGETS=/tmp/docs-refresh-ksdialogs-targets.txt \
  python3 .agents/skills/docs-refresh/scripts/link-resolution-check.py
```

未解決の相対リンク・欠落ファイルがあれば行が出る。無ければ `All internal links resolve`。対象一覧 (`/tmp/docs-refresh-ksdialogs-targets.txt`) が空のときは検査せずその旨を出す (0 件を適合と誤読しないための空ガード)。

#### 6-⑦ ローカル絶対パス・個体/個人/秘密の検査 (identity-lint)

`skills/` とルート README 2 枚は公開対象の成果物なので、Kasane 標準 lint の検査範囲に含める (`kasane/config.yaml` の `lint.identity.scope` に `skills` / `README.md` / `README_ja.md` を登載済み)。

```bash
if [ ! -s /tmp/docs-refresh-ksdialogs-targets.txt ]; then
  echo "検査対象が空です (manifest の targets / readmes を確認してから再実行)"
else
  python3 scripts/local-path-lint.py --paths $(cat /tmp/docs-refresh-ksdialogs-targets.txt)
  python3 scripts/identity-lint.py   --paths $(cat /tmp/docs-refresh-ksdialogs-targets.txt)
fi
```

どちらも exit 0 (違反 0 件) であること。違反があれば該当ファイルを再修正対象に追加する。

> 空ガードの注記: 対象一覧が空だとコマンド置換も空になり、`--paths` の後に何も続かない引数列で lint が起動する。`local-path-lint.py` はこのとき検査対象をリポジトリ全体 (`.`) へ切り替えるため「対象 0 件なのに全体検査の結果が返る」。`identity-lint.py` は逆に候補 0 件のまま「違反 0 件」で正常終了する。どちらも本来検査したかったものを検査していないのに結果だけが返るので、6-⑤ / 6-⑧ と同じ空チェックを省略しない。

> 実効範囲の注記: `identity-lint.py` は `--paths` で渡したファイルも `lint.identity.scope` (パス第 1 セグメント) で絞り込む。現状の scope は `kasane` / `skills` / `README.md` / `README_ja.md` で、追従対象に関わるのは後ろ 3 つなので、追従対象 4 枚と Skill 本体はすべて実効する — ただし `readmes` に scope 外のパス (scope に未登録の先頭セグメント配下) を足すと、渡しても**対象外として素通りする**ので「8 検査で全対象をカバーした」と誤認しないこと。`local-path-lint.py` は scope を持たず `lint.exclude` のみで判定するため、ローカル絶対パス検査は scope に依らず効いている。追従対象を scope 外へ広げるときは、`kasane/config.yaml` の `lint.identity.scope` 拡張を別 change として起票する (本スキル側で範囲を広げない)。

#### 6-⑧ 配信識別子の表記ゆれ grep

公開識別子の正は 2 本の ADR — 写像表 (`kasane/decisions/cross/0005-public-identifier-mapping.md`) と Compose 系 API の分離 (`kasane/decisions/android/0001-compose-api-separate-module.md`)。正しい識別子は次のとおり:

| 形態 | コード上の識別子 | 配布上の識別子 |
| --- | --- | --- |
| iOS | Swift モジュール `KsDialogs` | SwiftPM パッケージ `KsDialogs` |
| Android | Kotlin パッケージ `jp.kamusoft.ksdialogs` (Compose 系は `jp.kamusoft.ksdialogs.compose`) | Maven `jp.kamusoft:ksdialogs` / `jp.kamusoft:ksdialogs-compose` |
| KMP | Kotlin パッケージ `jp.kamusoft.ksdialogs.kmp` | Maven `jp.kamusoft:ksdialogs-kmp` |
| MAUI | .NET namespace `KsDialogs` | NuGet ID `KsDialogs.Maui` |

ブランド名で始まる**正しい API 名**もある — 4 形態の Dialog 表示契約 `KsDialog` / `IKsDialog` (MAUI) と、Compose 系の公開 API `KsDialogAttributes` (`android/ksdialogs-compose`。`kasane/concepts/core/api/` の layout-semantics / registration-show-semantics / transition-semantics の 3 本に記載があり、Compose のレシピを書けば生成物に現れる) がその代表で、これらは誤表記ではなく素通りさせる対象である。

ecosystem ごとの表記規則 (SwiftPM / .NET は PascalCase、Kotlin パッケージと Maven 座標は lowercase でブランド名の内部にハイフンや下線を入れない、reverse-DNS の根は `jp.kamusoft`) を崩した表記を検出する:

```bash
TARGETS=($(cat /tmp/docs-refresh-ksdialogs-targets.txt))
if [ ${#TARGETS[@]} -eq 0 ]; then
  echo "検査対象が空です (manifest の targets / readmes を確認してから再実行)"
else
  grep -rn -E \
    "Ksdialogs|KSDialogs|KSdialogs|ksDialogs|Ks[ _-]Dialogs|ks[-_]dialogs|com\.kamusoft|jp\.kamusoft[.:]KsDialogs|KsDialogsMaui|KsDialogs\.(MAUI|maui)" \
    "${TARGETS[@]}"
fi
```

行が出たら該当ファイルを再修正対象に追加する。

> パターンの読み方: **単数形 `KsDialog` を拾うパターンは持たない** — Dialog の表示契約が `KsDialog` / `IKsDialog` (MAUI) という単数形の正しい公開名であり (`kasane/decisions/core/0034-contract-type-name-singular-feature.md`)、`KsDialogAttributes` と合わせて単数形はブランド名の誤表記と区別できないため、この検査は綴りの崩れ (大文字小文字・区切り文字・reverse-DNS の根) だけを見る。契約の旧名 `KsDialogs` / `IKsDialogs` が文書に残っていないことは、改名を行う変更側の残存検査で担保する。`ks[-_]dialogs` は Maven artifactId / Kotlin パッケージのブランド名にハイフン・下線が割り込んだ形、`jp\.kamusoft[.:]KsDialogs` は lowercase であるべき Maven 座標・Kotlin パッケージへ PascalCase のブランド名が混入した形、`KsDialogsMaui` と `KsDialogs\.(MAUI|maui)` は NuGet ID の崩れを拾う。`ksdialogs-compose` / `ksdialogs-kmp` は正しい artifactId なので当たらない (ハイフンはブランド名の**内部**ではなく接尾辞の区切りである)。
>
> Maven の配布座標: group は `jp.kamusoft` で、artifactId は `ksdialogs` (Android Native) / `ksdialogs-compose` (Compose 系 API) / `ksdialogs-kmp` (KMP) の 3 本。ただし Maven Central への公開は未実施のため、**利用者向けの配布座標には公開が未導入である旨を添えて記述する**。

### 7. manifest の更新

全更新と整合性チェックを通過したら、`skills/.manifest.json` を Step 2 の規範スキーマに従って**全体を書き直す** (差分更新ではない)。書き込みは更新フローの**最後**に行う。

ハッシュ更新の規則:

- **`concepts` のハッシュは書き込み直前に再計算する** — Step 3a の計算値を使い回さない。実行中に concepts 側が変わることがある (サブエージェントの drift 所見を受けて同じセッションで concepts を手入れした場合など) ため、3a の値をそのまま書くとディスクの内容より**古いハッシュ**が残り、次回実行がその concept を「変更あり」と誤検出し続ける。実例: 2026-09-06 の実行が付随修正した concepts のハッシュを取り直さずに書いたため、翌日の実行で `{ios,android}/api/{loading,toast}-surface.md` の 4 本が偽陽性として要追従に載り、サブエージェント 2 体が「変更不要」に至るまで無駄に走った
- `concepts` のハッシュを現在値へ更新するのは、**この実行で対象の全 Skill ファイルが更新され、検査を通過した concept に限る**
- 未処理の concept (未承認 / 部分承認で対象外 / 検査失敗) は**旧ハッシュを保持**する。manifest に無い新規 concept は、配置判断が確定して targets / excluded に載り、対象ファイルが更新されるまで `concepts` にも載せない
- `targets` / `excluded` は、承認された配置判断と削除済み concept の整理を反映する — 内容は Step 6 で検査に通した予定 manifest (`/tmp/docs-refresh-ksdialogs-manifest-planned.json`) と同一にする (検査した状態とディスクに書く状態を食い違わせない)
- **削除済みと判定され、整理が Step 4 で承認されて Skill ファイル側の該当記述の除去まで完了した concept は、`targets` / `excluded` だけでなく `concepts` からもエントリを取り除く** (残すと Step 3b が毎回同じ削除を再検出し、Step 4 の提示に居座り続ける)。整理が未承認・未完了の削除済み concept は、上の「未処理の concept は旧ハッシュを保持する」規律どおり `concepts` に残したままにし、次回実行で再検出させる
- `--readme-only` の実行では `concepts` / `targets` / `excluded` を**更新しない** (Skill 本体に未反映の concept 差分を消費しないため)。更新するのは `generatedAt` と `lastUpdatedFiles` のみ
- 途中中断時は manifest を更新しない (次回実行が同じ差分を再検出できるようにするため)

タイムスタンプはシェルで `date -u +"%Y-%m-%dT%H:%M:%SZ"` を実行して生成する。

### 8. 完了サマリ表示

ユーザーに次を表示する:

- モード: `--all` / 差分 / `--readme-only`
- 更新したファイル一覧 (en/ja ペア単位で示す)
- スキップした項目 (変更なし / 未承認 — 次回再検出される旨を添える)
- 整合性チェック 8 種の結果 (`--readme-only` では 6-① は報告のみ・6-④ は N/A。②③⑤⑥⑦⑧ は README のみを対象とし、Skill ファイル側に出た失敗があればそれも報告のみとして挙げ、いずれも次回の通常実行で処理される旨を添える)
- `--readme-only` で 3d (ツール最低バージョン) の Skill ファイル側 (各 `SKILL.md` 導入節) に差分を検出した場合はその報告 (更新は行わず、次回の通常実行で処理される旨を添える)
- manifest の更新状況 (`--readme-only` では concepts スナップショット非更新である旨)
- サブエージェントから上がった drift 所見 (concepts と実装の矛盾) があればその一覧

## Guardrails

- **skills/ を知識の正本として扱わない**。concepts・コード・テストと食い違う skills/ の記述は skills/ 側を直す。concepts に疑義があれば drift 所見としてユーザーへ報告する (独断で concepts を変更しない — concepts の書き込みは Kasane の 4 経路のみ)
- **自発的な自動発動をしない**。ユーザーの明示依頼で起動し、Step 4 の承認なしに skills/ と README 群を書き換えない
- **メインコンテキストで concepts 本文を読み込まない**。ハッシュ計算・manifest 参照・コード正の機械チェックのみで判断する。サブエージェントへの委譲ができない環境では、メインが代わりに読んで書くフォールバックを取らず、Step 1 の起動時判定で実行不能として停止する (承認・書き込みより前)
- **Step 6 の検査はディスクの manifest ではなく予定 manifest に対して行う**。ディスクへの書き込みは Step 7 のまま最後に行い、検査に通した予定 manifest と同一内容を書く。予定 manifest の受け渡しは `export` ではなくコマンド行へのインライン指定で行う (ブロックを跨ぐと環境変数が失われ、黙って旧 manifest へフォールバックする)
- **`--readme-only` 実行では Step 6 の再修正対象を `readmes` 由来のファイルに限る**。`targets` 由来の Skill ファイルに出た失敗は報告のみとし、skills/ 本体を書き換えない
- **初期生成をしない**。manifest 不在・破損時はフルリフレッシュにフォールバックせず停止し、初期生成 (変更フロー) または manifest 修復を案内する
- **skills/ の構成自体の見直し**(Skill の新設・廃止、references の分割方針の変更) はこのスキルの守備範囲外。ユーザーに変更フローの起票として提案する
- **en/ja を片方だけ更新しない**。更新は常に言語ペア単位で、同一構成・同時更新 (翻訳ロックステップ)
- 新規 concept の配置判断 (どの Skill に載せるか / 除外するか) をスキルが独断で決めない。網羅検査の失敗としてユーザーへ提示する
- **器の指定なしでサブエージェントを起動しない**。サブエージェントは常に器 `ksn-implementer` を指定して起動し (Claude Code: `subagent_type` / codex: エージェント name)、器が未配置なら Step 1 で停止する (器なしの起動はメインのモデルを継承し、編成の切り離しが成立しなくなる)
- サブエージェントにメイン側の進捗リストを直接更新させない (競合回避。完了報告はテキストで返させる)
- 機械面 (`KsDialogsInteropBridge` / `KsDialogsInteropResultType`) は現存する public ABI だが**利用者向け導線に載せない**。利用者の入口は Swift パッケージ側の型付き面であり、機械面の名前を利用者コードとして書かない (消えた API ではないので「廃止された」とも書かない)
- 生成物は**閉世界**に保つ。`kasane/` 配下への参照・ADR 番号・他の Skill やリポジトリ内ファイルへの相対リンクを書かない (配布座標の URL は可)
- 削除コマンドは `rm` ではなく `trash` を使う (プロジェクト規約)
- manifest は更新の**最後**に書き込む (途中中断時に次回が再検出できるよう)
- **manifest に書く `concepts` のハッシュは書き込み直前に再計算する** (Step 3a の値を使い回さない。実行中に concepts が変われば古い値が残り、次回実行の偽陽性になる)
- `--all` 指定時も manifest を更新する。`--readme-only` 指定時は concepts スナップショットを更新しない
- `--all` でスキップするのはハッシュ差分検出 (3a・3b) だけで、網羅検査 (3c) と API 名網羅検査 (3e) は実行する (未配置 concept の配置判断と API 名の未掲載候補を Step 4 に載せ、6-① の抜け出せない失敗ループを防ぐ)
- 単発 concept の小さな修正 (typo 修正など) でも整合性チェック (Step 6 の 8 種) は省略しない
