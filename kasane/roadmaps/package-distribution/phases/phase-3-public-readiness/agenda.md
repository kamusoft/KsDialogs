# public 化の準備 (public-readiness)

リポジトリを public にするための手順 (新規リポジトリへの単一 initial commit・証跡媒体の除外・機密 / 個人情報の走査・ブランチモデル) を確定し実施する research フェーズ。配信リポジトリ `KsDialogs-SPM` の作成もここで行う。

## 論点

(すべて決定事項へ移動済み — 2026-09-07)

## 素材

- [public 化の実施手順書](artifacts/publish-procedure.md) — 決定事項を実行順に並べたチェックリスト (2026-09-07 ドラフト、実施記録欄つき)

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView cross/ADR-0021 (public は新規リポジトリ、履歴を引き継がない) と同 phase-2 の実施手順書 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md`)、配信リポジトリの設定は同 phase-4 の決定事項。

- 現ツリーを単一 initial commit で新規 public リポジトリに置く。noreply メールで commit。公開ツリーには開発ハーネスの記録 (`kasane/` `.claude/` `.codex/`) を含め、外すのは archive の媒体などの大容量物だけ
- 以後の change 記録・議論もすべて公開されるため、書く時点で公開前提の規律 (ローカル絶対パス禁止・個体 / 個人特定値の禁止) と lint / hook が要る (KsDialogs は導入済み: `scripts/local-path-lint.py` / `identity-lint.py` / `git-gate-lint.py` と .githooks)
- public 化は CI 構築 (phase-4) より前、利用者向け文書 (phase-1・2) の完成より後
- `main` の branch protection は完全な payload を PUT し、必須 status check は `{"context": ..., "app_id": 15368}` 形式 (app_id 15368 = GitHub Actions。省くと同名 check を出す他アプリで満たせる)
- 却下済み: `git filter-repo` で書き換え / そのまま public 化 / private 経路で配信パイプラインを先に確立する段階方式 (理由は cross/ADR-0021・0018 の Alternatives)

### ブランチモデルは `develop` / `main` の 2 本を踏襲する (2026-09-07)

KsSettingsView と同じ 2 本構成にする。既定ブランチは `develop` で、日常のコミットはトピックブランチを切らず `develop` へ直 push する (現行の `main` 直コミット運用と手数は同じ)。`main` の先端は「最新リリース、またはリリース進行中のリリース候補」で、`develop` → `main` の pull request だけが入り、release workflow は `main` からのみ起動する (KsSettingsView cross/ADR-0020・0028 の踏襲)。README の version 置換はリリース PR の中の commit、消費者検証 (dry-run) は `main` 宛て PR で走る (phase-4 / phase-9 はこの前提で `ci.yml` / `release.yml` / release-procedure をそのまま逆流させる)。

- 理由: 翻案元 ADR-0020 が「開発ブランチから起動し `main` は作らない」案を却下した根拠 (作業途中の commit からも起動できてリリース対象 commit が一意に定まらない) が KsDialogs でも成り立つ。2 本前提で書かれた CI / release / 手順書を読み替えなしで逆流できる
- 却下: `main` 1 本 — リリース前の PR を切る手間は消えるが、消費者検証の自動実行の場が無くなり dry-run を手で回す運用になる。トリガー・起動制限・手順書を 1 本用に書き換え ADR-0028 相当の翻案も書き直す逆流コストが上回る
- 派生: 公開時の initial commit は `develop` に置く (`main` はリリース時に作る。KsSettingsView と同じ)。README 画像 URL のブランチは論点 1b。→ [cross/ADR-0016](../../../../decisions/cross/0016-branch-model-develop-main.md) (proposed)

### README 画像 URL のブランチ名は `develop` (2026-09-07)

ルート README 2 枚が `assets/` の 6 枚を指す raw.githubusercontent の絶対 URL のブランチ名は `develop` にする (KsSettingsView と同じ形)。公開した瞬間から GitHub 上で画像が表示され (`main` は初回リリースの PR まで存在しない)、翻案元の README との差はリポジトリ名だけになる。対価は、配信済み README の画像が `develop` の最新画像を指すこと (撮り直しは稀で実害は小さい)。現在の README は `main` 暫定で書かれているため、public 化の実施手順の下ごしらえで `develop` へ置換する (README の更新規律は AGENTS.md に従い、実施時に docs-refresh 経由か手順書内の置換かを決める)。

### 公開ツリーから外すのは archive の媒体・archive の `.log`・スパイクブランチの 3 つ (2026-09-07)

- `kasane/changes/archive/**` の媒体 (png 340 件 / 43 MB。archive-media 削除の運用より前の 4 change の `ui/` 163 件 + `verification/` 177 件) を外し、`.md` は残す (踏襲)。画像リンクが壊れるのは想定内
- `kasane/changes/archive/**/verification/**/*.log` 23 件 / 136 KB を外す。lint の除外対象で未走査だったものを中身まで見ると、3 件にローカル絶対パス (38 行)、1 件 (MAUI iOS のクラッシュログ) に UUID 23 行が入っていた
  - sanitize して残す案は置換漏れの余地が残るため採らない。sanitize 済み抜粋は各 change の `.md` / `evidence/` に既にある
- スパイクブランチ `spike/phase-10-packaging-poc` (`main` から 4 commit・6 ファイル差分) は新リポジトリへ push しない。履歴を引き継がない方針 (cross/ADR-0021 踏襲) に沿い、PoC の結論は cross/ADR-0008・maui/ADR-0004 と phase-10 の記録に蒸留済み。参考実装はローカルの履歴保管先に残す
- 進行中 change (簡易起票 4 件) は exploration.md のみで媒体は無く、除外対象なし

### 識別子 lint はソース 5 ルートも検査し、verification の生ログは追跡をやめる (2026-09-07)

**検査範囲**: `lint.identity.scope` に `samples` / `ios` / `android` / `maui` / `kmp` を足す。Xcode の実機ビルドが `DEVELOPMENT_TEAM` を書き戻す xcodeproj は `samples/ios`・`samples/kmp/iosApp`・`maui/macios/native` の 3 つで、いまの範囲 (`kasane` / `skills` / README 2 枚) はどれも見ていない。実測 (2026-09-07、追跡 1025 ファイル) は 5 ルートとも 0 件で、誤検出なしで始められる。正当な UUID 定数が入ったら `allow` に足す。config の「本体ソースは含めない」コメントは書き換える。却下は `samples` だけ (KsSettingsView 踏襲。`maui/macios/native` の xcodeproj が漏れる) と `samples` + `maui` (理由付きの中間だが、全部足しても現時点のコストは同じ)。

**生ログ**: `verification/**/*.log` の追跡をやめる。`.gitignore` の救済行 (`!kasane/**/verification/**/*.log`) と `lint.exclude` の同 glob を外し、未走査の生ログが履歴に入る経路を閉じる。必要な抜粋は log-sanitize.py を通して `evidence/` か `.md` に置く (ksn-core の evidence 設計どおり)。`evidence/**/*.log` の救済行は残す (lint を通る前提の置き場)。却下は「追跡を続けて検査対象にする」(毎回 sanitize が要る割に置換後の全文を残す価値が薄い) と現状維持 (漏れ口が残る)。

- 走査の実測 (2026-09-07): ローカル絶対パス lint 0 件・識別子 lint 0 件・gitleaks 8.30.1 は全履歴 119 commit で no leaks found・`DEVELOPMENT_TEAM` 0 件・author は noreply 済み。公開直前に同じ 4 種を再走査する

### 履歴の保管先は private リポジトリ `kamusoft/KsDialogs-private-archive` を新規作成して push し、ローカルも退避する (2026-09-07)

remote が無いため全履歴 (119 commit・媒体 43 MB・スパイクブランチ) はローカルディスクにしか無い。公開ツリーは単一 initial commit なので、保管先として private リポジトリを GitHub に新規作成し、`main` と `spike/phase-10-packaging-poc` を push してから GitHub Archive (読み取り専用) にする。ローカルは KsSettingsView と同じく旧クローンを `../KsDialogs-private-archive` へ退避し、公開ツリーが元のパス `../KsDialogs` を引き継ぐ (`../<リポジトリ名>/` 規約と Claude Code のパス紐づけを保つ)。旧リポジトリの rename → Archive は不要で、同名作成のリダイレクトの罠も発生しない。

- 却下: ローカル退避だけ (ディスク故障で blame・spike・媒体をすべて失う) / 保管しない (論点 2 の「除外物はローカルの履歴保管先に残す」が成り立たない)

### 配信リポジトリ `KsDialogs-SPM` はこのフェーズで作成し、初回 commit は誘導 README + LICENSE の 2 点 (2026-09-07)

設定は踏襲どおり (public で作成・default `main`・Issues / Wiki / Projects / Discussions 無効・PR は collaborators only・workflow と branch protection なし・GitHub Release は作らず tag のみ・description と Website は monorepo 向け)。中身は誘導 README (ソース・Issue 窓口・インストール手順は monorepo へのリンク) と monorepo ルート `LICENSE` のコピーだけで、`Package.swift` / `Sources` / `Tests` のスナップショットは phase-5 がスナップショット生成スクリプトと一緒に初回 push する。README と Skill 4 箇所が既に名前を指しているため公開直後から実在させ、tag が無い間は誰も解決しないので中身が揃っていなくても困らない。

- 却下: 作成を phase-5 へ送る (名前が phase-5 まで 404、ロードマップの「作成は phase-3」を改訂する手間) / `ios/` を手でコピーして初回スナップショットまで置く (phase-5 のスクリプトの結果と食い違う余地)

## 調査結果 (2026-09-07 完了)

public 化を完了した。公開リポジトリは [kamusoft/KsDialogs](https://github.com/kamusoft/KsDialogs) (既定ブランチ `develop`)、配信リポジトリは [kamusoft/KsDialogs-SPM](https://github.com/kamusoft/KsDialogs-SPM) (誘導 README + LICENSE のみ)、全履歴は `kamusoft/KsDialogs-private-archive` (private、Archive 済み) に保管した。

- **履歴を引き継がず新規リポジトリで公開した** (cross/ADR-0021 踏襲)。remote が無かったため rename は不要で、保管先は新規の private リポジトリに `main` とスパイクブランチを push した
- **公開ツリーは 1788 件 / 15 MB** — 追跡 2150 件から archive の媒体 340 件 (43 MB) と verification の生ログ 23 件を除いた。開発ハーネスの記録 (`kasane/` `.claude/` `.codex/` `.agents/`) は含めている
- **公開前提の規律を広げた**: 識別子 lint の検査範囲にソース 5 ルートを追加 (xcodeproj への `DEVELOPMENT_TEAM` 書き戻りを捕捉)、verification の生ログは追跡をやめた
- 点検は gitleaks・2 lint・`DEVELOPMENT_TEAM` grep の 4 種で公開前後とも 0 件
- **ブランチモデルは `develop` / `main` の 2 本** (cross/ADR-0016 proposed)。`develop` は force-push 禁止 + 削除禁止のみで、必須 status check は phase-4 の CI 後
- 実施の全過程と実行制約 (エージェントの push 検査が全履歴の push を止めるため保管先への push はオーナーが手動、`curl` は実行分類器に止められ Browser で代替) は [実施手順書](artifacts/publish-procedure.md) の「実施記録」節にある

### 後続フェーズへの影響

- **phase-4 (検証 CI)**: ブランチモデル `develop` / `main` (cross/ADR-0016)、`develop` への必須 status check の追加、識別子 lint 5 ルート検査の CI 化。public になったので macOS ランナーが無料で使える
- **phase-5 (native packaging)**: `KsDialogs-SPM` は誘導 README + LICENSE だけの状態。初回スナップショット (`Package.swift` / `Sources` / `Tests`) の push は phase-5 の生成スクリプトで行う
- 以後の change 記録・議論はすべて公開される。ローカル絶対パス・個体 / 個人特定値は hook と git-gate が止める

## TODO

- [x] 論点の解消 (2026-09-07、決定 6 件: ブランチモデル・画像 URL・除外物・走査 scope・履歴の保管先・配信リポジトリ)
- [x] phase-2 からの申し送り (2026-09-05): README 画像 URL のブランチ名は `develop` で確定し、README 2 枚を置換 (2026-09-07 実施)
- [x] phase-2 からの申し送り (2026-09-05): GitHub の Pull requests 設定 (collaborators only) を実施手順 3b に含め、実施済み (2026-09-07、cross/ADR-0013)
- [x] **[実施手順書](artifacts/publish-procedure.md) に沿って public 化を実施 (2026-09-07 完了)** — 下ごしらえ → 公開ツリー 1788 件 / 15 MB を単一 commit → 保管先の Archive・新 repo の public 化・配信リポジトリ作成 → ローカル切り替えとビルド確認。下ごしらえで実施したもの:
  - README 画像 URL の `main` → `develop` 置換
  - `lint.identity.scope` の 5 ルート追加とコメント修正
  - `.gitignore` の verification ログ救済行と `lint.exclude` の削除 (追跡中の 23 件は公開ツリーで除外)
  - 公開直前の再走査 4 種 (ローカルパス lint・識別子 lint・gitleaks・`DEVELOPMENT_TEAM` grep)
- [x] phase-5 へ申し送り (2026-09-07 agenda に追記済み): 配信リポジトリ `KsDialogs-SPM` は誘導 README + LICENSE だけの状態で存在する。初回スナップショット (`Package.swift` / `Sources` / `Tests`) の push は phase-5 の生成スクリプトで行う
- [x] phase-4 へ申し送り (2026-09-07 agenda に追記済み): ブランチモデルは `develop` / `main` (cross/ADR-0016 proposed)。`develop` の branch protection (force-push 禁止 + 削除禁止) は public 化で設定し、必須 status check は phase-4 で足す
- [x] 調査結果のまとめ (2026-09-07、下の「調査結果」節)
- [x] ksn-roadmap で research 完了をマーク (2026-09-07)
