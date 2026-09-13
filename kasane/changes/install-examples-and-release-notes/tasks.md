# Tasks: install-examples-and-release-notes

実施順に制約がある (design.md の Migration Plan)。
**グループ 1 は 2 の後**、**グループ 4 の印の解除は 1 の案内追加より前**、
**`.github/release.yml` の廃止と組み立て処理の導入は同時**、**グループ 3 の削除は 1 と 2 の後**。

## 1. 既発行 Release の prerelease 印の解除 (最初に行う)

- [x] 1.1 既に発行済みの Release (`0.1.0-beta.1`) の prerelease 印を解除し、最新のリリースとして
      解決されることを確認する (→ Scenario: 既に発行済みの Release も最新の選別に乗る)。
      README に最新版の案内を書く前に行う — 解除前に案内を書くと、その間 `/releases/latest` が 404 を返す

## 2. インストール例の契約の検査

- [x] 2.1 `scripts/install-example-lint.py` を新設する。プレースホルダ・SwiftPM の `exact:`・
      最新版の案内・英日の同一構成の 4 契約を検査する (→ Requirement: インストール例の契約の検査)
- [x] 2.2 検査対象を明示表 (`TARGET_FILES`) で持つ。README 2 枚 (SwiftPM / Maven core / Maven compose /
      Maven kmp / NuGet の 5 種) と `skills/` 14 ファイル。`ksdialogs-kmp/references/android-host.md` と
      同 `references/ios-host.md` も対象に含める (→ Requirement: インストール例の契約の検査)
- [x] 2.3 実構成との突合を、インストール宣言を持つ文書すべてに広げる。対象表に無い文書の存在、
      対象表にあるのに実在しない文書、英日の食い違いをいずれも失敗にする
      (→ Scenario: 検査対象に登録されていない文書を検出する)
- [x] 2.4 走査はコードブロック内に限る。散文は対象外とし、その限界を検査側に記録する
      (→ Scenario: 散文の記述は検査を通る)
- [x] 2.5 自己テストを持たせる。一時ディレクトリに木を組んで実行し、リポジトリ本体は読まない。
      契約を満たす木 / 具体 version への逆戻り / `from:` への変更 / 案内の欠落 / 対象表と実構成のずれ 2 種 /
      英日のずれ 2 種 / 期待しない種別 / 散文は見ない、の各ケースを検査する
      (→ Requirement: インストール例の契約の検査)

## 3. インストール例のプレースホルダ化

- [x] 3.1 README 2 枚の 10 行を `{version}` にする (SwiftPM / Maven core / Maven compose / NuGet / Maven kmp)
      (→ Requirement: インストール例の version 表記)
- [x] 3.2 `skills/` のコードブロック内 14 行を `{version}` にする
      (ios 各 1 / android 各 2 / kmp SKILL 各 1 / kmp references/android-host 各 1 / maui 各 1 /
      aiforms-migration 各 1) (→ Requirement: インストール例の version 表記)
- [x] 3.3 散文中の 4 行 (`skills/{en,ja}/ksdialogs-kmp/SKILL.md` と同 `references/ios-host.md`) も
      `{version}` にする (→ Requirement: インストール例の version 表記)
- [x] 3.4 各ファイルに最新版の案内 (`https://github.com/kamusoft/KsDialogs/releases/latest`) を置く
      (→ Scenario: 最新版の案内から目的の版に着地できる)
- [x] 3.5 SwiftPM の宣言が `exact:` であることを確認する。`from:` は上限が次のメジャーまで開くため固定にならない
      (→ Requirement: インストール例の version 表記)
- [x] 3.6 `python3 scripts/install-example-lint.py` が通ることを確認する

## 4. 置換機構の撤去

- [x] 4.1 `scripts/release/set-readme-version.py` (711 行) を削除する (`trash` を使う)
      (→ REMOVED Requirement: README と Skill の version 置換)
- [x] 4.2 `.github/workflows/release.yml` の validate 段から、インストール例と入力 version の一致検査 step を削除する
      (→ REMOVED Requirement: README と Skill の version 置換)
- [x] 4.3 同 package-maui job の pack 前の置換呼び出しを削除する。facade の nupkg に同梱される README は
      プレースホルダのままになる (→ Scenario: 配布物に同梱される README も具体 version を持たない)
- [x] 4.4 同 publish job の `Update install examples on develop` step (`develop` への書き戻し) を削除する。
      あわせて `Summarize` の `KS_README_WARNING` の行と env も外す
      (→ REMOVED Requirement: README と Skill の version 置換)
- [x] 4.5 `AGENTS.md` の例外規定 (インストール例の version 置換は release workflow が行う) の行を削除する
      (`CLAUDE.md` がプロジェクト側にも同文を持つなら同時に直す)

## 5. Release ノートの組み立て

- [x] 5.1 `scripts/release/build-release-notes.py` を新設する。サブコマンドは `collect` (API と git を使う) /
      `render` (純粋な組み立て) / `--selftest` (→ Requirement: Release ノートの内容)
- [x] 5.2 `## Changes` の解析を実装する。見出しはちょうど 1 つ、範囲の空行でない行はすべて
      `- <種別>: <説明>` または `- none`、種別は 4 つ、説明は非空、`- none` は単独のみ、空セクションは失敗
      (→ Scenario: 認識できない記載があれば止まる)
- [x] 5.3 起点の選択を実装する。今回の version でない / draft でない / `main` の first-parent 上で
      対象 commit の祖先、を満たす Release のうち距離最小。該当が無ければ履歴の最初から
      (→ Scenario: Release を持たない tag は起点にならない / draft の Release は起点にならない /
      対象 commit の祖先でない Release は起点にならない / 今回の tag がある再実行でも対象が変わらない)
- [x] 5.4 対象 pull request の収集を実装する。base が `main` のものだけを残し、pull request 番号で重複除去、
      マージ順に並べる。ページ送りは最後のページまで辿る
      (→ Scenario: 開発ブランチ宛ての pull request は対象にならない / 複数の pull request 分が連結される)
- [x] 5.5 ノートの整形を実装する。種別ごとにまとめ、項目の無い種別の見出しは出さない。
      起点があれば比較の位置を、無ければ最初のリリースであることを末尾に置く。定型文言は英語で書く
      (Release ページは閲覧者の言語圏を仮定しない公開物のため)
      (→ Scenario: 利用者向けの変更が種別ごとに並ぶ / 初回のリリースでは比較の位置を含めない /
      対象が無いときも本文が決まる)
- [x] 5.6 自己テストを持たせる。解析 / 起点の選択 (一時 git 履歴を作る) / ページ送り (本体の実装を通す) の
      3 群。ページ送りの検査は本体を写した別実装ではなく本体を呼ぶこと
      (→ Scenario: ノートの組み立てが単独で検査できる)
- [x] 5.7 `.github/pull_request_template.md` を新設する。`## Changes` 節には解析が受理する行 (`- none`) だけを置き、
      種別一覧と記入例は次の見出し以降に置く (未編集のテンプレートがそのまま解析を通る必要があるため)。
      説明は英語で書く旨を明記する
- [x] 5.8 `.github/release.yml` (ラベル分類の設定) を削除する。5.1〜5.6 と**同時に行う** —
      分類設定だけ先に消すと、次のリリースでノートが空になる

## 6. workflow の組み替え

- [x] 6.1 validate job に `permissions` を置く。`contents: read` と `pull-requests: read` の**両方**を書き切る
      (job レベルの `permissions` は列挙しなかった権限を `none` にするため)
      (→ Requirement: Release ノートの内容)
- [x] 6.2 収集の要否を決める step を置き、条件式を 1 箇所で評価して出力に渡す
      (→ Scenario: 開発ブランチからのリハーサルでは対象を集めない / main からのリハーサルでは実際の経路を通る)
- [x] 6.3 ノートを組み立てて成果物として保存する step を置く。`if-no-files-found: error` と `overwrite: true` を付ける
      (`overwrite` が無いと `Re-run all jobs` が validate で止まる) (→ Requirement: Release ノートの内容)
- [x] 6.4 収集を省いたことをログに残す step を置く (省いたのか、走って 0 件だったのかを後から区別するため)
- [x] 6.5 publish job に成果物を受け取る step を置く (→ Scenario: 検査の後に本文が編集されても公開内容が変わらない)
- [x] 6.6 Release 作成 step を置き換える。prerelease の分岐を削除し、最新のリリースとして明示指定する。
      確定したノートが空なら失敗する (→ Requirement: tag と GitHub Release)
- [x] 6.7 `.github/workflows/ci.yml` の lint job を更新する。`set-readme-version.py --selftest` の呼び出しを外し、
      `install-example-lint.py` の本検査と自己テスト、`build-release-notes.py --selftest` を足す
      (→ Requirement: インストール例の契約の検査)
- [x] 6.8 publish の step が 1 本増えるため、`scripts/release/check-publish-step-order.py` の自己テストが
      追随できているか確認する (先行 change で「他の download を全部消す」ケースは本数をハードコードしない形にしてある)

## 7. リリース用スキル

- [x] 7.1 `.agents/skills/release/SKILL.md` を新設する。handbook のリリース手順を実行時に読んで従う薄い層とし、
      段の順序・節の並び・コマンドは持たない (→ Requirement: リリース手順の定型実行)
- [x] 7.2 スキルが自分で持つのは `## Changes` の下書き手順と、判断を人へ返す境界の 2 つに限る。
      下書きの範囲は `origin/main..origin/develop` 相当
      (→ Scenario: 先行する pull request の変更が下書きに混ざらない)
- [x] 7.3 `.claude/skills/release` を `.agents/skills/release` への symlink として置く (docs-refresh と同じ形)

## 8. handbook

- [x] 8.1 `kasane/handbook/cross/install-examples.md` を新設する (`kind: rule`)。守る形 4 点、
      新しい文書を足すときの対象表への登録、機械検査とその限界 (散文は検査対象外で、検出 0 件は適合の証明にならない)
      (→ Requirement: インストール例の契約の検査)
- [x] 8.2 `kasane/handbook/cross/release-procedure.md` を改訂する。version 置換の手順を削除し、
      `## Changes` の記入を加える。「上から順に読めば 1 回のリリースが完了する単一の入口」として整える。
      失敗時の節を validate 段と publish 段に分け、記載の不備を手元で確かめる手段を案内する
      (→ Requirement: リリース手順の定型実行)
- [x] 8.3 `kasane/handbook/cross/index.md` と `kasane/handbook/index.md` に install-examples.md の行を足す

## 9. 決定の記録

- [x] 9.1 ADR「インストール例は具体 version を持たない」を `status: proposed` で起票する
- [x] 9.2 ADR「Release ノートは `main` 宛て pull request 本文から組み立てる」を `status: proposed` で起票する
- [x] 9.3 cross/ADR-0026 (lint job の検査の集合) を amends する ADR を起票し、install-example-lint を加えた数に更新する
- [x] 9.4 `kasane/decisions/cross/index.md` と `kasane/decisions/index.md` を更新する

## 10. 検証

- [x] 10.1 `python3 scripts/install-example-lint.py` と `--selftest` が通ることを確認する
- [x] 10.2 `python3 scripts/release/build-release-notes.py --selftest` が通ることを確認する
- [x] 10.3 `render` サブコマンドに pull request 本文を模した入力を与え、期待するノートが得られることを確認する
      (→ Scenario: ノートの組み立てが単独で検査できる)
- [x] 10.4 lint job の全検査が手元で通ることを確認する
- [ ] 10.5 `main` から `dry-run` で release を起動し、収集・検査・整形・成果物の受け渡しが通ることを確認する
      (→ Scenario: main からのリハーサルでは実際の経路を通る)
- [x] 10.6 インストール例を写した消費者で `{version}` を埋めると解決できることを確認する
      (`verification/` の消費者は version を変数で受け取るため既存の経路で確認できる)

## 蒸留への申し送り (実装タスクではない)

- `kasane/concepts/cross/architecture/release-workflow.md` — 置換機構と prerelease 印の記述を落とし、
  Release ノートの組み立てと インストール例の契約を反映する
- `kasane/handbook/cross/docs-refresh-timing.md` — インストール例の version が docs-refresh の対象外である旨の
  記述があれば見直す (置換機構の撤去で前提が変わる)
- `skills/` の利用者向けドキュメントは docs-refresh 経由でのみ更新する原則に対し、本 change は
  インストール例の行を直接触る。`AGENTS.md` の例外規定を撤去したうえで、この変更が承認済み change による
  構成の見直しにあたることを蒸留で確認する
