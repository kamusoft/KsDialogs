# レビュー結果: rollout-user-docs (017 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

cross / repository-docs bundle は、現在のレビュー段階で要求される README 英日ペア、Skill 索引、貢献導線、スクリーンショット、長命層への知識移送、planned manifest の網羅性を満たしている。docs-refresh の機械検査、README と Skill の最小例、Issue Forms、画像採用元、移送した Sample 契約を現物から再照合し、Critical / Major は見つからなかった。長命層の索引・鮮度表示・除外理由に局所的な Minor 3 件があるため、後続の指摘反映で整えることを推奨する。

なお、`samples/` README の廃止と参照付け替え、manifest の concept hash 確定は tasks 7.1〜8.1 が明示的に後段へ置いている。現在の planned manifest で `concepts` が空であること、および旧 README / ADR / config の参照が残ることは、このレビュー時点では未実装の虚偽チェックではなく順序どおりの中間状態として扱った。

## 照合した規約

- `kasane/handbook/cross/test-execution.md` — 変更の完了判定と、全 suite 省略条件の確認
- `kasane/handbook/cross/sample-parity.md` — 撮影支援契約と採用画像の比較条件
- `kasane/handbook/cross/local-development-setup.md` — Sample の参照方式・ビルド・起動手順
- `kasane/handbook/cross/user-skill-api-listing.md` — docs-refresh 3e 候補の掲載・除外分類
- ksn-core の handbook / concepts / delta-spec / domain-axis / paths / UI artifact 規律
- docs-refresh の planned manifest 運用と 6-①〜6-⑧の検査手順

## 検証証拠

### docs-refresh と manifest

- planned manifest から対象 70 ファイルを展開した。concept 網羅、英日見出し、コード byte、frontmatter、内部リンクの各検査はすべて成功した。
- 閉世界性、機械面名、表記ゆれの grep は該当 0 件。`local-path-lint.py` と `identity-lint.py` も exit 0 だった。
- 10 個の `SKILL.md` はすべて `license: MIT` と本リポジトリを指す `metadata.source` を持つ。
- API 網羅報告は 38 組・候補出現 239 件。platform ごとの未分類は 0 件、同一 platform / API の重複除外も 0 件だった。

### README、貢献導線、画像

- ルート README の英日は見出し階層と全コードブロックが一致した。最小例 4 組も対応 platform Skill と byte 一致した。
- 座標と minSdk 24、compileSdk 36、Kotlin 2.4.10、AGP 9.3.0、Gradle 9.7.0、Swift 6.3、.NET 10、MAUI 10.0.1 は build 設定と一致した。
- Skill 索引は 5 行・3 節で対応する。英日 Skill 集合は各 33 ファイルで同一相対パスだった。
- Issue Forms は必須項目、同一の platform 7 択、blank issue 無効を満たす。CONTRIBUTING 英日ペアも方針・案内・相互リンクが意味等価だった。
- README の画像は各言語 6 パス・6 通りで同一。`assets/` は brief の採用元と SHA-256 が一致した。
- 6 枚を目視し、Dialog、50% / `Soon...` の Loading、3 枚の Toast、ステータス / ナビゲーション領域の不在を確認した。

### 長命層と実装への接地

- `sample-parity.md` の 14 stable demo ID は 4 実装と完全一致し、application identifier も project 設定と一致した。
- Sample の参照方式は Local Swift Package、Gradle composite、単一 MAUI ProjectReference、KMP の composite 2 本と `apply false` を現行設定で確認した。
- iOS host concept の static framework、Native registry 委譲、合成 package、Swift 登録入口を build 設定・生成物・Sample・公開実装と突き合わせた。
- 変更した長命文書群の構造 lint は違反 0 件。`concepts/log.md` の既存 29 件は `deviation.md` の継承 baseline と一致した。
- 製品コード・テストを変更していないため、proposal Impact の合意済み例外に従い全 build root の suite は実行していない。

## 指摘事項

### 🟡 Minor handbook の上位ドメイン地図が配下 index に追随していない

**該当箇所**: `kasane/handbook/index.md:14`
**問題点**: `cross/index.md` は `user-skill-api-listing.md` を含む 7 文書を列挙しているが、上位 index は依然「6件」のままで、API 掲載基準を内容一覧にも含めていない。配下文書へは到達できるため機能を阻害しないが、ksn-core の index 規律と長命層の地図が食い違っている。
**推奨修正**: cross 行を 7 件へ更新し、「利用者向け Skill の API 掲載基準」を内容一覧に加える。

### 🟡 Minor Sample 手順を増補した guide の最終検証日が古い

**該当箇所**: `kasane/handbook/cross/local-development-setup.md:8`
**問題点**: 2026-09-04 の concepts log は同 guide に 4 形態の Sample 手順を追加・照合したことを記録しているが、frontmatter の `timestamp` は増補前の 2026-09-02 のままである。ksn-core ではこの値を最終検証日として扱うため、内容の鮮度表示が実態より古い。
**推奨修正**: 今回の現物照合日へ `timestamp` を更新する。

### 🟡 Minor migration の `Easing` 除外理由が実装 platform を取り違えている

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md:104`
**問題点**: AiForms migration で検出された `Easing` を「Android / Compose framework」と説明しているが、源泉 concept が列挙する同名は MAUI のネイティブ表現であり、実装も `Microsoft.Maui.Controls.Easing` を `DialogTransition.Fade` / `Slide` / `Zoom` の公開引数に使う。機械的に導出できる標準型として除外する結論は維持できるが、現行の実装経路・理由は事実と一致しない。
**推奨修正**: 「.NET MAUI framework の標準型で、MAUI の transition preset の `easing` 引数から導出できる」旨へ直す。

## アクションプラン

1. 上記 3 件を長命層の指摘反映として修正する。
2. tasks 6.4 の機械検査再実行で、planned manifest を使った 6-①〜6-⑧、3e 全候補分類、長命文書の構造・リンクを再確認する。
3. オーナー検収後に tasks 7〜8 を順に実施し、旧 Sample README 参照の解消と 11 concept の最終 hash を持つ manifest を確定する。
