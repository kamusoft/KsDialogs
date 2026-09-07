# Proposal: rollout-user-docs

## Why

package-distribution ロードマップは、利用者向けドキュメントを Agent Skills (`skills/{en,ja}/`、5 Skill) として提供し docs-refresh で concepts に追従させる形を phase-1 (adopt-docs-refresh) で据えた ([cross/ADR-0011](../../decisions/cross/0011-user-docs-as-agent-skills.md))。しかし `skills/` の実体・manifest・索引はまだ無く、docs-refresh は manifest 不在で停止案内を返すだけの状態にある。ルート README は開発者向け (ビルドルート表とビルドコマンド) の日本語混じり英語 1 枚で、インストール座標・最小コード例・Skills への導線・貢献方針を持たない。

public 化 (phase-3) の前にこれらを揃える必要がある — 公開履歴に旧文書 (開発者向け README・`samples/` 配下の README 5 本) を載せない、という実行順の制約 (roadmap.md 前提) のため。あわせて、`samples/` 配下 README が「撮影用デモ ID・アプリ識別子」「各ルートの参照方式とビルド手順」「KMP iOS アプリのリンク構成」の正を握っている状態を解消し、知識の正を handbook / concepts へ寄せる (phase-2 agenda 決定「`samples/` 配下 README 5 本は廃止し、正を handbook / concepts へ移送する」)。

設計判断は phase-2 agenda の決定事項 (踏襲 5 件 + KsDialogs 固有 9 節) で確定済みであり、本 change はその実施である。

## What Changes

影響する能力: **user-skills** (利用者向けドキュメントの提供形態。新規)、**repository-docs** (ルート README と貢献導線。新規)、**docs-refresh** (manifest 初期版と追従対象の確定)。

- **`skills/` 一式の初回生成 (5 Skill × en / ja = 10 部)**: `ksdialogs-{ios,android,maui,kmp}` (SKILL.md + `references/`) と `ksdialogs-aiforms-migration` (SKILL.md + `references/api-mapping.md`)。生成は Skill 単位の fan-out で各ワーカーが en / ja ペアを同一文脈で同時生成する。内容規範は概念説明 → 能力マップ表 → Setup → 最小コード → references 振り分け、閉世界性、コード例は原則コメントレス。KsDialogs 固有の内容規範 (両入口の使い分けは冒頭 1 段落・DI レシピは MAUI / KMP のみ、KMP の `@Throws` は最小コードに宣言 + 本文 3 行、KMP iOS ホスト側 Setup は前提 1 + 手順 3、移行 Skill の Toast 節 2 行) は design.md の Decision に列挙する
- **`skills/.manifest.json` v3 初期版と索引 2 枚** (`skills/README.md` / `README_ja.md`: Skill 一覧・コピー手順・片言語コピーの前提の 3 要素)。`targets` は core/api 8 本 + maui/api/di-registration.md + 新設 kmp concept を覆い、`excluded` は `cross/reference/reference-repositories.md` の 1 本、`readmes` はルート 2 枚 + 索引 2 枚
- **ルート README の英日 2 枚化** (`README.md` 英語 + `README_ja.md` 日本語、翻訳ロックステップ): 冒頭の配信準備中バナー 1 行 / 概要と主な特徴 / スクリーンショット (iOS / Android × Dialog / Loading / Toast の 6 枚、ルート `assets/` に配置) / 対応プラットフォーム (最小 OS + ビルドに使った toolchain の 3 列 + 利用側下限の注記、4 形態) / インストール (4 形態の座標と prerelease の指定方法) / 最小コード例 (4 形態、対応する platform Skill の最小コードと一致) / Skills 導線 / リポジトリ構成 (ディレクトリ表 + AGENTS.md・kasane/concepts/ へのリンク。`samples/` は 1 行のみでリンクなし) / 貢献 / ライセンス。現行の「Build roots」「Building」節は落とす
- **貢献導線 `.github/` 一式**: Issue Forms 3 本 (bug_report / feature_request / question、英語、証拠項目を必須化、Platform は形態 × ホスト OS の 7 択、blank issue 無効) と `CONTRIBUTING.md` / `CONTRIBUTING_ja.md`
- **`samples/` 配下 README 5 本の廃止と移送**: 撮影のための起動引数 (キー・安定デモ ID 14 件・アプリ識別子・外部表現) → `kasane/handbook/cross/sample-parity.md` に節を追加 / 各ルートの参照方式とビルド・実行コマンド → `kasane/handbook/cross/local-development-setup.md` に「Sample のビルドと実行」節 / KMP iOS アプリの 3 点リンクと `integrateLinkagePackage` の再生成手順 → 新設 kmp concept。既出の内容 (パリティ写像・器の責務と演出の添付の注意・KMP の演出分担メモ) は捨てる。参照元の付け替え: cross/ADR-0010 本文の「正は samples/README.md」を handbook へ差し替え (オーナー許可済みの本文修正)、`kasane/config.yaml` `ui.screenshot` のポインタ、sample-parity.md「関連」節
- **長命層の新設 2 本**: kmp concept `kasane/concepts/kmp/api/ios-host-integration.md` (KMP 利用者の iOS ホスト統合 — 発行 metadata による SwiftPM 参照の推移・合成パッケージの生成・登録 API 用の手動 1 点・Sample の再生成手順。出典は phase-10 PoC 記録・samples/kmp/README・cross/ADR-0008・kmp/ADR-0002。KMP Skill の源泉) と handbook `kasane/handbook/cross/user-skill-api-listing.md` (利用者向け Skill の API 掲載基準 — 「簡潔でも網羅」の方針・意図的な掲載除外の基準・KsDialogs の現行除外リスト。docs-refresh 3e の仕分けの正で、除外リストは Skill 本文の生成と同時に確定する)。concepts / handbook の index と log を更新し、docs-refresh SKILL.md の 3e 注記 (「phase-2 で起こす」) を規約への参照に差し替える
- **レビュー 4 層**: 機械検査 (docs-refresh の検査スクリプト 8 本 + README ↔ Skill の最小コード一致 + Issue Forms の静的検査) → 独立レビュー (ksn-review、Skill 単位) → 初見レビュー (concepts もコードも読んでいないエージェントに Skill 本文だけを渡す) → オーナー目視検収 (少なくとも ja 5 部の通し読み)

## Non-Goals

- **`skills/` と README 群の継続的な追従更新** — docs-refresh の責務 (ユーザーの明示依頼で起動)。本 change は初期生成のみ
- **配布座標・対応 Kotlin 範囲の確定** — KMP の公開座標と Kotlin 範囲は phase-7 の結論で確定する。本 change は暫定値に印を付けて書き、phase-7 後に docs-refresh で追従させる。他形態の座標は ADR (cross/0008・maui/0004・android/0001) の確定値を書く
- **README 画像の絶対 URL のブランチ名の確定** — phase-3 のブランチモデルで決まる。本 change は現在唯一のブランチ `main` で暫定的に書き、phase-3 が追随させる (agenda TODO に申し送り済み)
- **README 最小コード例の消費者プロジェクトでのコンパイル検査 (`scripts/readme-example-lint.py` 相当)** — 突合先の `verification/` は phase-8 で作る。本 change では README ↔ Skill の逐語一致だけを検査する
- **GitHub の Pull requests 設定 (collaborators only) と Discussions の無効化** — リポジトリ設定の操作であり、public 化 (phase-3) の実施手順で行う
- **翻案 ADR (KsSettingsView cross/ADR-0023 相当「README はルート 2 枚」・ADR-0024 相当「貢献は Issue のみ」) の起票** — roadmap.md の前提どおり蒸留時に判断する
- **Reduce Motion への言及** — phase-2 agenda で「取り扱わない」と確定 (文書に書かず、機能も起こさない)
- **iOS factory 契約 throws 化 (core/ADR-0033) の破壊的変更としての記載** — 未リリースで読者不在 (agenda 決定)
- **`android/layout-case-fixtures/README.md`** — テスト補助コードのディレクトリ注記で `samples/` 配下ではなく利用者の入口でもない (agenda 決定 D 分類)。現状維持
- **Sample のコード変更** — README の廃止に伴うコード側の変更はない。KMP の演出分担メモを Sample のソースコメントへ移すかは実装時の任意 (捨ててよい)

## Impact

- **破壊的変更**: `samples/` 配下 README 5 本の廃止 (trash)。リポジトリは private で外部参照はなく、リポジトリ内の参照元 (config・handbook・ADR-0010) はすべて本 change で付け替える。製品コード・公開 API・テストへの変更なし
- **長命層への波及**: handbook 2 本の改訂 (sample-parity / local-development-setup) + 1 本の新設 (user-skill-api-listing)、concept 1 本の新設 (kmp/api/ios-host-integration)、accepted ADR (cross/0010) の本文修正。いずれも agenda で決定済みで、蒸留を待たず change 内で行う (壊れた参照のコミットを挟まないため。翻案元 phase-12 と同じ扱い)
- **docs-refresh**: manifest が存在するようになり、以後の追従が機能する。`readmes` 4 枚と `excluded` 1 本は SKILL.md の記述と一致させる
- **画像**: `assets/` に PNG 6 枚。UI が変わったら撮り直し (docs-refresh の追従対象外)
- **識別子・パス lint**: `skills/` とルート README 2 枚は `lint.identity.scope` 登載済み。撮影はシミュレータ / エミュレータで端末固有情報を写さない
- **テスト実行規約**: 製品コード・テストに触れないため、全ビルドルートの全件実行の省略を合意済み例外として扱う (adopt-docs-refresh と同じ扱い。オーナー確認は本提案の承認で兼ねる)
- **リスク**: 生成 10 部の品質のばらつき → 4 層レビューで担保 / manifest の網羅漏れ → 網羅検査で担保 / 移送の取りこぼし → 廃止前に 5 本の全節を design.md の対応表と突き合わせる

## 級: L

3 能力 (user-skills / repository-docs / docs-refresh) を横断し、長命層 (handbook の改訂と新設・concept 新設・accepted ADR の本文修正) に及ぶ。翻案元では phase-12 (M) と phase-9 (L) の 2 change に分かれていた範囲を、ロードマップの「1 フェーズ = 1 change」原則に従い 1 change で行う。

domain: cross
roadmap: package-distribution/phase-2-docs-rollout
