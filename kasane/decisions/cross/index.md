# cross ADR 一覧

リポジトリ横断のメタ事項 (リポジトリ構成・ブランド方針・ハーネス運用) の決定記録。

| ID | タイトル | status | date |
|---|---|---|---|
| [0001](0001-rebrand-policy.md) | リブランド方針 — Native 主・互換 shim なし・独立ブランド | accepted | 2026-08-13 |
| [0002](0002-tech-stack-2026-08.md) | 技術セット — 2026-08 最新安定セットで開始し、最低対象 OS は iOS 17 / Android minSdk 24 | accepted | 2026-08-13 |
| [0003](0003-knowledge-intake-hybrid.md) | 既存資産知識の取り込みはハイブリッド型 — 原則系は即時翻案、機構系はオンデマンド参照 | accepted | 2026-08-13 |
| [0004](0004-monorepo-four-build-roots.md) | モノレポは4形態分離のビルドルートとし、KMP→Android Native は composite build で接続する (一部改訂: 0018 — ルートの `global.json`) | accepted | 2026-08-14 |
| [0005](0005-public-identifier-mapping.md) | 公開識別子の写像表 — 素の名前は Native へ、MAUI は namespace 素・NuGet ID 修飾 (一部改訂: 0019 — Android の配布上の識別子) | accepted | 2026-08-14 |
| [0006](0006-samples-aggregated-consumer-boundary.md) | Sample は集約 samples/ に置き、利用者と同じ側から公開 product を参照する | accepted | 2026-08-14 |
| [0007](0007-sample-parity-demo-item-unit.md) | Sample は4ルートでパリティを保ち、一致単位は「デモ項目」とする | accepted | 2026-08-14 |
| [0008](0008-distribution-model-standard-channels.md) | 配布は標準3チャネルのみとし、SwiftPM は配信リポジトリ (KsDialogs-SPM) で配り、KMP の Swift 参照は version から導出する (サポートする Kotlin 範囲は同 minor) | accepted | 2026-09-08 |
| [0009](0009-lockstep-single-version.md) | 全形態は lockstep 単一バージョンで一斉リリースし、版間互換を提供しない (版の単一ソースはカタログ、リリース版は `-Pversion=` で注入、SNAPSHOT は Central へ発行しない) | accepted | 2026-08-17 |
| [0010](0010-sample-capture-demo-driven-mode.md) | Sample 撮影はデモ駆動モード (起動引数) で行い、専用撮影スクリプトは持たない | accepted | 2026-08-27 |
| [0011](0011-user-docs-as-agent-skills.md) | 利用者向けドキュメントは Agent Skills (skills/、en/ja 2 版・5 Skill) として提供し、docs-refresh で concepts から追従させる | accepted | 2026-09-04 |
| [0012](0012-readme-root-only-and-developer-knowledge-in-handbook-concepts.md) | README はルート 2 枚 (英語 + README_ja) に集約し、samples/ 配下 README は廃止して開発者向け知識は handbook / concepts に一本化する | accepted | 2026-09-04 |
| [0013](0013-contributions-via-issues-no-external-pull-requests.md) | 貢献は Issue Forms で受け、外部からの Pull Request は受け付けない (Platform は形態 × ホスト OS の 7 択) | accepted | 2026-09-04 |
| [0014](0014-concepts-core-contract-platform-surface.md) | concepts は core に platform 非依存の契約だけを残し、公開名・署名・コード例は <platform>/api/ へ分離する | accepted | 2026-09-05 |
| [0015](0015-diagnostic-messages-english-only.md) | ライブラリが外へ出す診断文言 (例外メッセージ・警告ログ) は英語固定とし、ローカライズしない | accepted | 2026-09-07 |
| [0016](0016-branch-model-develop-main.md) | ブランチは develop / main の 2 本とし、develop へ直 push、main はリリース候補だけが PR で入り release は main からのみ起動する (一部改訂: 0024 — README のインストール例の version 置換の時点 / 0025 — 既定ブランチ) | accepted | 2026-09-07 |
| [0017](0017-verification-ci-structure-and-guarantee.md) | 検証 CI は platform 別 reusable workflow 5 本と入口 1 本で構成し、緑の意味を「ロジック全件通過 + native 配線のコンパイル」に限り、トリガーはブランチの役割で分ける (一部改訂: 0020 — lint job の検査の集合) | accepted | 2026-09-08 |
| [0018](0018-toolchain-pinned-in-repo.md) | 検証に用いる toolchain の版はリポジトリ内で固定し、.NET SDK / workload set の固定は repo 直下の global.json で行う (0004 を一部改訂。一部改訂: 0023 — MAUI 本体の版) | accepted | 2026-09-08 |
| [0019](0019-android-maven-coordinates-core-suffix.md) | Android の Maven 座標は View 系本体を ksdialogs-core、Compose 側を素の ksdialogs とする (0005 と android/0001 を一部改訂) | accepted | 2026-09-08 |
| [0020](0020-lint-job-includes-spm-sync-script-selftest.md) | 検証 CI の lint job に SwiftPM スナップショット同期スクリプトの自己テストを加え、6 検査とする (0017 を一部改訂。一部改訂: 0021 — lint job の検査の集合) | accepted | 2026-09-08 |
| [0021](0021-ci-only-skip-owner-allowlist-and-lint.md) | CI 上だけのテスト skip はオーナーが書く許可リスト (`lint.ci-skip.allow`) と `scripts/ci-skip-lint.py` で統制し、検証 CI の lint job に許可リスト検査を加えて 7 検査とする (0020 を一部改訂。一部改訂: 0022 — lint job の検査の集合) | accepted | 2026-09-09 |
| [0022](0022-lint-job-includes-readme-example-lint.md) | 検証 CI の lint job に README 最小例と消費者ソースの一致検査 (`scripts/readme-example-lint.py`) を加え、8 検査とする (0021 を一部改訂) | accepted | 2026-09-09 |
| [0023](0023-maui-controls-floor-follows-maui-adr-0004.md) | toolchain 固定境界のうち MAUI 本体の版は maui/ADR-0004 の「workload set 同梱版」に従う (0018 を一部改訂) | accepted | 2026-09-09 |
| [0024](0024-release-dispatch-serial-publish-spm-tag-before-kmp.md) | release は dispatch 起動・取り消せる順で直列に publish し、SPM tag は KMP の Maven 発行より前に置く (0016 を一部改訂) | accepted | 2026-09-10 |
| [0025](0025-default-branch-main.md) | 既定ブランチは main とし、リポジトリの入口が最新リリースの README を指すようにする (0016 を一部改訂) | accepted | 2026-09-10 |
