# Exploration: backport-registry-wait-hardening

## 課題 / 動機

姉妹リポジトリ KsSettingsView から knowledge の逆流 (kind: change) を受け取った
(`../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-release-mechanism-hardening-and-notes-rework.md`)。
こちらが 2026-09-10 に送った知らせ (`kasane/outbox/KsSettingsView/2026-09-10-release-workflow-fixes-and-improvements.md`)
を受けて向こうが 2 本の change を実装し、`0.1.0-beta.3` のリリースまで通している。

本 change はそのうち publish 内部の堅牢化
(`../KsSettingsView/kasane/changes/archive/2026-09-12-backport-release-workflow-hardening/`) を翻案する。
向こうの**最終形** (review-001〜006 の指摘修正と deviation を織り込んだ状態) を写すのが方針で、
設計の議論はやり直さない。KsDialogs 固有の差分だけを決める。

こちらに欠けているのは次の 4 点。公開待ちの上限分離 (`KSR_PUBLISHED_TIMEOUT_SECONDS` 既定 5400 = 90 分) は
change fix-release-published-wait で実装済みのため対象外:

1. **レジストリ照会に状態分類が無い** — `scripts/release/wait-for-registries.sh` (306 行) は
   `http_head_status` / `maven_central_ready` / `nuget_ready` の真偽判定だけで、通信の失敗・5xx・解釈不能を
   すべて「未反映」へ畳み込む。上限 45 分まで待って失敗したとき、レジストリが遅いのか壊れているのかが分からない
2. **待ちの時間予算が機械検査の外にある** — 待ちの上限はスクリプト側の定数、job の打ち切りは workflow 側の
   `timeout-minutes` という二重管理で、片方だけ延ばしても平時は緑のまま進む
3. **publish の step 順序に検査が無い** — 引き継ぎ deployment ID の読み込みが他の成果物取得より後ろへ動いても気づけない
4. **`scripts/release/` 9 本の `--selftest` が CI から一度も呼ばれていない** —
   簡易起票 add-release-script-selftests-to-lint の項目 1・4 と同じ課題。本 change に吸収する
   (同起票の項目 2 `actionlint` と 3 tag 照合のスクリプト化は別テーマとして起票側に残す)

## 検討した選択肢 (却下案と理由を含む)

KsSettingsView 側で議論・実装・レビューを終えているため、本 change では選択肢の比較を行わない (オーナー指示)。
向こうの最終形を写し、KsDialogs 固有の差分だけを「決定事項」に記す。

向こうのレビューが Major として検出し、最終形に反映済みの判断 2 件は前提として引き継ぐ:

- 照会の応答上限を残り時間で切り詰める処置は**採らない**。正常な相手を自分の都合で打ち切るため、
  分類がいちばん必要な瞬間 (期限到達時) に未反映が「通信そのものの失敗」へ化ける。
  歯止めは「期限を過ぎた照会を送らない」側に置く
- `HTTP_MAX_TIME_SECONDS` を引き上げると反映待ち job の `timeout-minutes` を追い越し、
  job 打ち切りで「対象ごとの最後の分類」の出力そのものが失われる。だから時間予算の機械検査が要る

## 決定事項

### 翻案の方針 (KsSettingsView 最終形の踏襲)

- `scripts/release/wait-for-registries.sh`: 4 状態分類 (`UNPROBED` / `REFLECTED` / `PENDING` /
  `UNKNOWN_TRANSPORT` `UNKNOWN_STATUS` `UNKNOWN_PARSE`) を導入。反映済みは sticky (再照会しない)、
  判定不能でも待機は継続、上限到達時は対象ごとの分類と種別を出力に残す。
  `http_get` を唯一のネットワーク関数にして自己テストで差し替える
- `scripts/release/check-time-budget.py` を新設。publish job と wait-for-registries job の 2 つを検査する。
  定数が読めない形へ退化したとき / 同じ定数の値が割れたときも fail-closed
- `scripts/release/check-publish-step-order.py` を新設。引き継ぎ読込が他の成果物取得より前にあることを検査
- `.github/workflows/ci.yml` の lint job へ `scripts/release/` の自己テスト群を接続

### KsDialogs 固有の差分 (向こうと違う点)

- **待機対象が 10 件** — 向こうは 4 件 (Maven 座標 1 + NuGet 3)。こちらは Maven Central の Android 2 座標
  (`ksdialogs-core` / `ksdialogs`) + KMP 5 publication (`ksdialogs-kmp` の root / -android / -iosarm64 /
  -iossimulatorarm64 / -iosx64) + nuget.org 3 Package ID (`KsDialogs.Maui` / `KsDialogs.Binding.iOS` /
  `KsDialogs.Binding.Android`)。分類器の構造は同じで `reset_targets` が積む対象が増えるだけ
- **Maven Central の枠が 2 つ** (android / kmp)。時間予算の式で数える待ちの本数が向こうと異なるため、
  `VALIDATION_WAITS` / `PUBLISH_WAITS` / `OUT_OF_WAIT_REQUESTS` は実物から数え直す。
  現状値: publish の `timeout-minutes` は 150、wait-for-registries は 60、
  `wait-for-registries.sh` の上限は 2700 秒 (45 分)、`central-portal.sh` は検証待ち 1800 / 公開待ち 5400
- **引き継ぎ ID が枠ごとに 2 本** — こちらは既に `Download previous deployment ids` →
  `Prepare deployment id files` (id=`deployment-ids`) と `central-resume.sh` / `check-resume-eligibility.sh` を
  持つ。向こうの `deployment-handover.sh` 新設をそのまま写すのではなく、順序の要件
  (引き継ぎの読み込みが他の成果物取得より前) を満たす形に留めるかを実装時に確定する
- **自己テストの CI 接続範囲が広い** — 向こうは既存の並びに 4 step 足すだけ。
  こちらは `scripts/release/` 9 本すべてが未接続 (`central-portal.sh` / `central-resume.sh` /
  `check-distribution-tag.sh` / `check-nuget-version.sh` / `check-resume-eligibility.sh` /
  `check-signatures.sh` / `compare-maven-artifacts.sh` / `wait-for-registries.sh` / `set-readme-version.py`)。
  `set-readme-version.py` は change install-examples-and-release-notes で撤去するため接続対象から外す
- **`central-portal.sh` の複数 ID 待ちの逆対照** — 簡易起票の項目 4。台本を 1 件伸ばすと誤実装が
  「終わらない (hang)」ではなく NG で表れるようにする。向こうが `wait-for-registries.sh` の自己テストで
  入れた「台本切れを印ファイルに残す」処置と同じ考え方

## ADR 候補 (作成済み: なし / 未起票: cross/ADR-0022 の amends 1 本)

lint job の検査の集合は cross/ADR-0022 (accepted、8 検査) が持つ。本 change で検査が増えるため
amends を 1 本起票する (0020 → 0021 → 0022 と同じ型)。

## 未決の論点

- 引き継ぎ読込のスクリプト切り出し (`deployment-handover.sh` 相当) を行うか、既存 step の順序要件だけで足すか — 実装時に確定
- 時間予算の式の各定数の値 — 実物から数え直す

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: M

判定材料: 触る能力は release-workflow 1 つ。公開 API の変更なし。可逆 (スクリプトと workflow のみ)。UI なし。
新規スクリプト 2 本 + 既存 1 本の大改修 + lint job への step 追加のため、デルタスペックを持つ重さが要る。
向こうも M 級で実装している。
