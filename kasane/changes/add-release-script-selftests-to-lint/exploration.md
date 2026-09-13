# Exploration: add-release-script-selftests-to-lint

## 課題 / 動機

release workflow (`.github/workflows/release.yml`) と `scripts/release/` の判定は、リリースのときにしか本番で通らない。退行を見張るのは各スクリプトの `--selftest` だけだが、それは検証 CI の lint job に載っておらず、手元で回さない限り印が残らない (handbook cross/local-development-setup.md「リリース用スクリプトの自己テストを回す」)。同じ理由で、workflow の構文退行を `actionlint` で CI 上で検出する手段も無い。

発見の文脈: add-release-workflow の tasks 2.5 (lint job に載せない判断) と review-001 Suggestion、fix-release-published-wait の review-001 / 002 Suggestion (自己テストが CI で回らない・`actionlint` が CI から呼ばれていない)、verify-001 所見 (monorepo tag の別 commit 検出だけが自己テストを持たない)。いずれも phase-9 の蒸留 (2026-09-10) で後続の change へ申し送られた。

1. ~~**`scripts/release/*.sh --selftest` と `set-readme-version.py --selftest` を lint job に載せる**~~
   **→ change backport-registry-wait-hardening へ移管 (2026-09-13)**。KsSettingsView の知らせを受けた翻案に同梱する。
   なお `set-readme-version.py` は change install-examples-and-release-notes で撤去するため接続対象から外れた。
   以下は移管時点の記述 — 数秒で終わりネットワークへ出ない (HTTP 送信関数はモックへ差し替わる)。`set-readme-version.py` の自己テストは実物の README と Skill を複写して置換を試すため、docs-refresh や skills の改稿でインストール例の行の形が変わった change の CI で気づける
2. **`actionlint` を lint job に載せる** — 現状 `release.yml` は shellcheck の info / style 3 件のみで error 0 件。既存 workflow 7 本にも同種の info があるため、error だけを失敗にする形にするか、指摘を先に消すかを決める
3. **monorepo tag の別 commit 検出のスクリプト化** — `release.yml` の validate と publish の tag 照合だけが自己テストを持たない。1 に同梱するなら `scripts/release/` に切り出して `--selftest` を持たせる
4. ~~**自己テストの逆対照が hang で表れる件**~~ **→ 同じく backport-registry-wait-hardening へ移管 (2026-09-13)**。 (fix-release-published-wait review-002 Suggestion) — `central-portal.sh` の複数 ID 待ちの台本を 1 件伸ばすと、誤実装が「終わらない」ではなく NG で出る。CI に載せる前に入れておくと赤で出る

## 検討した選択肢 (却下案と理由を含む)

(未探索)

## 決定事項

(未探索)

## ADR 候補 (作成済み: なし / 未起票: cross/ADR-0022 の一部改訂)

lint job の検査の集合は cross/ADR-0022 (accepted、8 検査) が持つ。1 と 2 を載せるなら検査の集合が変わるため、0022 を amends する ADR を起票する (0020 → 0021 → 0022 と同じ型)。

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- `actionlint` の info / style 指摘 (SC2012 / SC2001) を先に消すか、error のみを失敗条件にするか
- 3 を同梱するか、tag 照合は workflow の step のままにするか (照合は `git rev-parse` 2 回の比較で、スクリプト化の価値は自己テストの有無だけ)
- ~~姉妹ライブラリ KsSettingsView 側も同じ状態 (自己テスト未搭載) のため、knowledge の逆流 (outbox の知らせ) に含めるか~~
  **決着 (2026-09-13)**: 2026-09-10 の知らせに含めて送り、向こうが先に実装して kind: change で返してきた
  (`../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-release-mechanism-hardening-and-notes-rework.md`)。
  こちらは向こうの最終形を翻案する形で backport-registry-wait-hardening を起票した

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 残った範囲と変更級

2026-09-13 に項目 1・4 を change backport-registry-wait-hardening へ移管したため、本起票に残るのは
**項目 2 (`actionlint` を lint job に載せる)** と **項目 3 (monorepo tag の別 commit 検出のスクリプト化)** の 2 つ。
phase-9 agenda の申し送り「actionlint の CI 未搭載」もここに紐づく。

## 変更級の推奨: 未判定

暫定: `ci.yml` の lint job への step 追加と `scripts/release/` の小改修、cross/ADR-0022 の amends 1 本のため **S 級 + ADR** (独立レビューは必須)。`main` は保護済みのため、変更は `develop` → リリース PR の経路で `main` に入る。
