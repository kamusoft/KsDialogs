# phase-3-public-readiness 議論履歴

## 2026-09-07: ブランチモデル

- 起点: roadmap 前提「ブランチモデルは phase-3 で決める」。現状は remote なし・`main` 直コミット
- 選択肢: A `develop` / `main` 2 本 (KsSettingsView 踏襲) / B `main` 1 本
- 判断軸: 日常の手数 (両案とも直 push で同じ) / リリース対象 commit の一意性 / 消費者検証 dry-run が走る場所 / README の version 置換位置 / phase-4・9 への逆流コスト / README 画像 URL のブランチ
- 採用: A。理由は翻案元 cross/ADR-0020 の却下理由 (1 本だとリリース対象 commit が一意に定まらない) が有効なままであることと、`ci.yml` / `release.yml` / release-procedure を読み替えなしで逆流できること。B はリリース前 PR の手間が消える代わりに dry-run の手動運用と翻案の書き直しが要る
- 派生論点として 1b (README 画像 URL のブランチ名) を論点に残した
- 事前確認した実物: spike ブランチは main から 4 commit 先 (6 ファイル)、archive 媒体 340 件 / 43 MB、追跡 `.log` 136 KB、`DEVELOPMENT_TEAM` 0 件、両 lint exit 0、author は noreply 済み、`gh` ログイン済み
- ADR: cross/ADR-0016 を proposed で起票 (昇格は phase-4 / 9 の change の蒸留時)

## 2026-09-07: README 画像 URL のブランチ名

- 選択肢: `develop` (KsSettingsView と同じ) / `main` (リリース時点の画像に固定)
- 判断軸: 公開直後〜初回リリースまでの表示 (`main` は存在せず 404) / 配信済み README の画像の固定性 / 逆流の手間
- 採用: `develop`。画像の撮り直しは稀で `main` の利点 (固定性) の実利が小さい。README の置換は public 化の実施手順に含める。ADR にはしない (可逆で局所的)

## 2026-09-07: 公開ツリーから除外するもの

- 実測: archive 媒体 png 340 件 / 43 MB (4 change に集中、`ui/` 163 + `verification/` 177)、スパイクブランチは 4 commit・6 ファイル差分、追跡 `.log` 23 件 / 136 KB のうち 3 件に絶対パス・1 件に UUID (lint 除外で未走査だった)
- 選択肢: 3 つとも外す / `.log` は log-sanitize で書き換えて残す / スパイクも push する
- 採用: 3 つとも外す。生ログは lint の外にある経路で、公開する価値より漏れのリスクが勝つ。スパイクの push は履歴を引き継がない方針に反する
- 派生: 今後の verification ログを追跡し続けるか (lint 除外のまま public 運用すると新しい漏れ口) は論点 3 で扱う

## 2026-09-07: 走査の対象

- 実測: 識別子 lint を `samples` / `ios` / `android` / `maui` / `kmp` の追跡 1025 ファイルに直接掛けて 0 件 (`--paths` にディレクトリを渡すと未追跡の DerivedData まで歩いて終わらないため、`git ls-files` の一覧を渡した)。xcodeproj は 3 つ。gitleaks は全履歴 no leaks
- (a) 検査範囲: 5 ルート全部 / `samples` だけ / `samples` + `maui` → 5 ルート全部を採用 (誤検出 0 のいまなら無料、xcodeproj 3 つを全部覆う)
- (b) verification 生ログ: 追跡をやめる / 追跡は続けて検査対象に / 現状維持 → 追跡をやめるを採用 (ksn-core の evidence 設計に合わせ、抜粋は sanitize して evidence/ へ)
- ADR にしない (lint 設定と .gitignore の可逆な変更)

## 2026-09-07: remote が無い状態からの手順 (履歴の保管先)

- 前提: remote なし、履歴はローカルのみ。rename の罠は発生しない
- 選択肢: A private 保管リポジトリを作成して push + Archive + ローカル退避 / B ローカル退避だけ / C 保管しない
- 採用: A。ディスク故障への安全網と、除外物 (媒体・生ログ・spike) の参照先を GitHub 側にも持つため

## 2026-09-07: 配信リポジトリ `KsDialogs-SPM` の初期設定

- 設定項目は KsSettingsView phase-4 の決定と実物 (`KsSettingsView-SPM`: 5 点構成・機能すべて無効・tag のみ) で確認済み。決めたのは「このフェーズで作るときの中身」
- 選択肢: B 誘導 README + LICENSE で作成 (スナップショットは phase-5) / A 作成を phase-5 へ / C `ios/` を手でコピー
- 採用: B。名前を公開直後から実在させ、GitHub 設定を monorepo と同じセッションで済ませる。ADR にしない (cross/ADR-0008 が配信リポジトリ方式を既に持つ)
- これで論点はすべて決定事項へ移動 (決定 6 件)

## 2026-09-07: 実施手順書のドラフト

- KsSettingsView の手順書を翻案して artifacts/publish-procedure.md を作成 (1 下ごしらえ / 2 公開ツリー / 3 GitHub: 保管 repo・新 repo・配信 repo / 4 ローカル切り替え / 5 後続)。実施は未着手
- 翻案で変えた点: rename → Archive を「private 保管 repo の新規作成 + push + Archive」に、既定ブランチを `develop` に、除外物に `.log` を追加、配信リポジトリの作成を 3c として同梱、`lint.exclude` を外す順序の注意 (追跡中の `.log` が識別子 lint に掛かる)
