# Exploration: package-distribution (ロードマップ起案の出典)

ksn-explore (2026-09-04) の探索メモ。library-foundation の phase-11-packaging / phase-9-docs を独立ロードマップへ昇格させる起案の素材。

## 課題 / 動機

- library-foundation の残り 2 フェーズ (phase-11 パッケージング実装、phase-9 docs 整備) は「公開可能な成果物の発行検証まで」を範囲とし、継続運用 (リリース CI・署名・レジストリ運用) を非ゴールにしていた
- 姉妹ライブラリ KsSettingsView が `../KsSettingsView/kasane/roadmaps/package-distribution/` で、パッケージング・検証 CI・消費者検証・release workflow・利用者向け Skills (docs-refresh 追従)・public 化までを 13 フェーズで完了し、2026-09-04 に初回リリース (`0.1.0-beta.1`) まで到達した。release workflow は「固有値を冒頭 `env` に集約し、コピー + 値差し替えで KsDialogs へ逆流する」ことを決定済み (共有 workflow 化は却下)
- 実績ができたので、KsDialogs の maui / ios / android はこれを完全に踏襲する。踏襲できる論点は**解決済み論点**として決定事項に直書きし、議論なしで propose へ進められるフェーズにする。KsSettingsView に存在しない **KMP 形態**だけは議論を行う
- docs-refresh スキルも、利用者向けドキュメントを `skills/` に置く形も完全に踏襲する

## 現状 (コード・リポジトリの実物で確認)

- KsDialogs は git remote なし・CI なし・`main` 直コミット運用。`README.md` は英語 1 枚 (Status: Nothing is released yet)
- 配布モデルは phase-10 で設計済み: cross/ADR-0008 (標準 3 チャネル + KMP 手動 2 点)・cross/ADR-0009 (lockstep)・kmp/ADR-0003 (Swift 向け登録 API は Swift パッケージ側)・maui/ADR-0004 (NuGet 3 パッケージ)。いずれも proposed。PoC 2 本 (SwiftPM リモート配布 / MAUI NuGet pack) で成立は実証済み、スパイクブランチ `spike/phase-10-packaging-poc` は参考実装
- `ios/Package.swift`: target 1 本・`path:` 指定なし (既定の `Sources/` `Tests/`)。`ios/` 配下をそのまま配信リポジトリのルートへ置ける
- `kmp/ksdialogs-kmp/build.gradle.kts`: `isStatic = true`、`iosMinimumDeploymentTarget = 17.0`、開発参照は `localSwiftPackage(../ios)`
- Android は android/ADR-0001 で `ksdialogs` + `ksdialogs-compose` の 2 artifact
- phase-11 agenda には簡易起票からの合流 2 件 (MAUI Android binding の `BG8401` 警告 6 件、MAUI 1 行登録の View 生成失敗を `DialogException` へ合流) と申し送り 2 件 (KMP 公開面の `@Throws` 宣言の機械検査、MAUI iOS のビルド環境制約 = .NET for iOS 26.1.10502 が Xcode 26.1 を要求) が積まれている
- phase-9 agenda には利用者向け文書の内容論点 4 件 (Reduce Motion の扱い、原典 `IToast` からの移行対応表、iOS factory 契約 throws 化の破壊的変更、KMP `@Throws` の注意書き) が積まれている

## 姉妹ライブラリ KsSettingsView の実績 (踏襲元)

ksn-scout 調査 (2026-09-04) の要約。詳細は `../KsSettingsView/kasane/roadmaps/package-distribution/` と同 `kasane/decisions/` (cross/0018〜0028、android/0016、maui/0025)、`kasane/handbook/cross/release-procedure.md`。

- 実行順: Skills 化 (10→11→12) → README (9) → public 化 (2) → 検証 CI (3) → パッケージング (4/5/6) → 消費者検証 (7) → release workflow + 初回リリース (8) → CI トリガー整理 (13)
- 順序制約: public 化は CI 構築より前 (public なら macOS ランナー無料・SwiftPM の https 実リモート検証が可能) / 利用者向け文書の完成は public 化より前 (公開履歴に旧文書を載せない)
- そのまま流用できるもの: `release.yml` の 6 段構成 (validate → test ∥ package → dry-run → publish 直列 → 反映待ち → smoke) と冪等化パターン、`scripts/release/` 5 本、`scripts/spm-snapshot/`、`verification/` の 2 段スクリプト構造 (`prepare-feed.sh` / `build-consumer.sh`) と `verification-args.sh`、reusable workflow (`workflow_call`) 3 本 + 入口 1 本の分割と status check 名固定、実行件数 0 件 = fail、`.agents/skills/docs-refresh/` (manifest v3・検査スクリプト 8 本)、`skills/{en,ja}/<name>/` の構造と閉世界性、README ルート英日 2 枚 + 貢献は Issue のみ、public 化は新規リポジトリへ単一 initial commit、CI トリガーのブランチ役割分け、`jp.kamusoft` の Central Portal 名前空間検証 (共用済み・再実施不要)
- 差し替える値: 配信リポジトリ名 `KsDialogs-SPM`、Maven 座標、NuGet Package ID、`release.yml` 冒頭 `env` 一式、artifact 名、GPG 鍵 ID、Skill 名
- 別途新設する外部設定: nuget.org の Trusted Publisher Policy (repo 単位)、GitHub Environment `release` + secrets 7 件、配信リポジトリ本体と deploy key、`main` の branch protection、Issue Forms / CONTRIBUTING、`.github/release.yml`
- 実測の落とし穴 (要所): iOS binding resource の manifest に発行マシンの絶対パスが乗る (CI で pack する前提) / .NET Android SDK の自 assembly 用 aar が nupkg に入り利用者側で `XA4301` / consumer-maui が約 20 分 / API 版付き TFM を下回る消費者では警告なく platform 中立アセットにフォールバック / Gradle の `content { includeGroup }` は排他でないため `exclusiveContent` / Central Portal に「公開済みか」の API は無く `repo1.maven.org` の HEAD で判定 / 初回リリース所要 39 分

## 論点の区分

- **踏襲 (解決済み論点)**: KsSettingsView の決定をそのまま KsDialogs の決定事項に写す。agenda の「論点」には置かず「決定事項」に出典付きで直書きする
- **KsDialogs の既決を維持**: KsSettingsView と違う結論を既に持つもの (下記 D)
- **議論**: KMP 形態と、KsDialogs にしか無い前提 (下記 F・G)

## 検討した選択肢 (却下案と理由を含む)

### A. SwiftPM の配り方

- 採用: KsSettingsView cross/ADR-0018 の配信リポジトリ方式 (`KsDialogs-SPM` へ release CI がスナップショットを commit + tag)。→ [cross/ADR-0008](../../decisions/cross/0008-distribution-model-standard-channels.md) (proposed)
- 却下: cross/ADR-0008 のルート Package.swift 一本化。KsSettingsView が同案から出発して問題化した (SwiftPM 利用者がモノレポを履歴ごと full clone し、`kasane/changes/` の証跡媒体が利用者のコストになる)。オーナー判断: 「KsSettingsView ADR-0018 を採用する。0008 は KsSettingsView 側で問題が発生して 0018 の形になったため」。cross/0008 は proposed のため本文を直接改訂した

### B. 逆流の方式

- 採用: `release.yml` はリポジトリ内に閉じ、コピー + 冒頭 `env` の値差し替え (KsSettingsView phase-8 の決定)
- 却下: 別リポジトリの共有 workflow を両者から `uses:`。逆流先 (KMP を含む 4 形態) の形が未確定で抽象の境界を当てられず、共有側の変更が両リリースに同時に効く

### C. フェーズの粒度

- 採用: 9 フェーズ (下記「決定事項」)。KsSettingsView の 13 フェーズから、rename を伴わない KsDialogs では次を統合した
  - Skill 設計 + docs-refresh 導入 (研究 + 導入が同じ流れ)
  - Skill 本文生成 + README (README の最小コード例が Skills と逐語一致で lint 検査される結合がある)
  - iOS SPM + Android Maven (KsSettingsView で別だったのは Android の module 統合 + ディレクトリ改名のため。KsDialogs は artifact 2 本のまま発行機構を配線するだけ)
  - CI トリガー整理 (KsSettingsView phase-13) は検証 CI に最初から織り込む
  - KMP 消費者検証の「形」の議論は kmp-packaging の agenda へ集約 (packaging と表裏)。実装は consumer-verification に置く
- 却下: MAUI を native packaging と統合 — `BG8401`・`DialogException` 合流・Xcode / workload 整合など配布物以外のライブラリ変更を抱え、単独で L 級相当
- 却下: 消費者検証 + release workflow の統合 — release は消費者検証 workflow を artifact 経由で呼ぶ構造で、消費者検証 (特に MAUI の 20 分) が単独で安定してから release を組む順序が実測で効いていた

### D. KsDialogs の既決を維持するもの (翻案しない)

- Android の artifact 粒度: KsSettingsView android/ADR-0016 の単一 module は「ui が既に Compose 一式に依存していた」固有事情による。KsDialogs は android/ADR-0001 (Compose API を別 module にして MAUI binding 経由で compose-ui が推移しないようにする) と cross/ADR-0008 の 2 artifact を維持
- MAUI の 3 パッケージ構成と名前空間: maui/ADR-0004 = KsSettingsView maui/ADR-0025 と同型、名前空間は cross/ADR-0005 で既に素の `KsDialogs`。追加の rename は不要

### E. 利用者向け Skills の本数

- 採用候補: platform 4 本 (ios / android / maui / kmp) + AiForms.Maui.Dialogs からの移行 1 本 (原典が存在するため。phase-9 の `IToast` 移行対応表・factory throws の破壊的変更はここに載る)。KMP Skill の切り方 (Android 側 + iOS 側の Swift パッケージ登録を 1 本で扱うか) は skills-foundation で決める

### F. ブランチ運用 (議論)

- KsSettingsView は `develop` / `main` の 2 本で CI トリガーを分ける (cross/ADR-0028)。KsDialogs は remote なし・`main` 直コミット運用。public 化で決める: 踏襲 (2 本) か `main` 1 本か。release の起動ブランチ制限・README の version 置換を PR の中で行う手順 (cross/ADR-0020) が 2 本前提なので、1 本にするなら手順の読み替えが要る

### G. KMP 形態 (議論)

- KMP publication の形: Maven Central へのマルチターゲット publication (klib + metadata + swiftpm-metadata)、`ksdialogs-kmp` → `ksdialogs` の同版厳密依存 (cross/ADR-0009)
- dev (`localSwiftPackage(../ios)`) / publish (`swiftPackage(url(KsDialogs-SPM), exact(x.y.z))`) の参照切り替え機構 (phase-10 からの申し送り。`localSwiftPackage` のまま発行すると絶対パスが伝播して消費者ビルドが壊れる)
- サポートする Kotlin バージョン範囲の宣言 (SwiftPM import が Alpha のため) と integrateLinkagePackage の利用者手順
- KMP 公開面の `@Throws` 宣言の機械検査 (phase-11 申し送り。`androidHostTest` のリフレクション or `kmp/api-surface-check`)
- KMP 消費者検証の形: Android app + iOS の linkage package (`samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` 相当) を dry-run / smoke でどう組むか。release の package / dry-run / smoke 各段の 4 本化
- KMP の検証 CI (KsSettingsView の 3 本に 4 本目を足す。iOS Simulator でのテスト実行を含むか)

## 決定事項

- 新ロードマップ `package-distribution` を起案し、library-foundation の phase-11-packaging / phase-9-docs を `promoted` にする。library-foundation は残りが全 completed になるためアーカイブする
- SwiftPM は配信リポジトリ `KsDialogs-SPM` 方式 (cross/ADR-0008 (2026-09-04 改訂))
- 逆流はコピー + 値差し替え (共有 workflow 化しない)
- フェーズ構成 (9 フェーズ):

| 順 | フェーズ | 種別 | 扱い | 中身 |
|---|---|---|---|---|
| 1 | skills-foundation | change | 踏襲 + Skill 本数のみ判断 | Agent Skills 化・manifest v3・閉世界性 (cross/ADR-0022 の翻案)、docs-refresh を検査スクリプト込みでコピーし対象を KsDialogs の concepts へ向ける |
| 2 | docs-rollout | change | 踏襲 + 内容は新規 | Skill 本文の初期生成 (phase-9 の論点 4 件は内容として吸収)、ルート README 英日 2 枚・貢献は Issue のみ・Issue Forms・CONTRIBUTING (cross/ADR-0023・0024) |
| 3 | public-readiness | research | 踏襲 + ブランチ運用の判断 (F) | 新規 repo に単一 initial commit (cross/ADR-0021)、配信リポジトリ `KsDialogs-SPM` の作成、証跡媒体の除外、lint / hook の再発防止 |
| 4 | verification-ci | change | 踏襲 + KMP 1 本追加 | reusable workflow 4 本 + 入口、実行件数検査、ブランチ役割別トリガー (cross/ADR-0025・0026・0028) |
| 5 | native-packaging | change | 踏襲 (cross/ADR-0008 (2026-09-04 改訂)) | `scripts/spm-snapshot/` コピー、vanniktech plugin + Central Portal で `ksdialogs` / `ksdialogs-compose` 発行、toolchain 版の追随要否は着手時に実測 |
| 6 | maui-packaging | change | 踏襲 + phase-11 の同梱分 | 3 パッケージ pack (maui/ADR-0004)、nuget.org メタデータ、`XA4301` 対処、`BG8401`、`DialogException` 合流、MAUI iOS の Xcode / workload 整合と未実施の MAUI iOS 面 Sample 通し |
| 7 | kmp-packaging | change | **議論** (G) | KMP publication、dev / publish 参照切替、Kotlin 範囲宣言、`@Throws` 検査、KMP 消費者検証の形 |
| 8 | consumer-verification | change | 踏襲 + KMP 4 本目の実装 | `verification/` 2 段スクリプト構造、README 最小例の逐語一致 lint |
| 9 | release-workflow | change | 踏襲 + 4 本化 | `release.yml` コピー + `env` 差し替え、Trusted Publisher / Environment / secrets / deploy key の新設、初回リリース |

- 依存: 1 → 2 → 3 → 4 → (5 ∥ 6 ∥ 7) → 8 → 9

## ADR 候補

- 改訂済み: [cross/ADR-0008](../../decisions/cross/0008-distribution-model-standard-channels.md) (proposed のまま SwiftPM 節を配信リポジトリ方式へ直接改訂。「別の開発線にした」判断自体は ADR 化しない)
- 未起票 (起案時に ksn-roadmap のチェックで判断): 「配信は KsSettingsView の実績を踏襲し、逆流はコピー + 値差し替えで行う」の方針そのもの。KsSettingsView 側 phase-8 の決定を KsDialogs 側でも ADR に残すかは起案時に判断する
- 踏襲する KsSettingsView の ADR 群 (cross/0019〜0028、maui/0025 相当) は、各フェーズの蒸留時に KsDialogs 側の翻案 ADR として起票するか、既存 (cross/0009 = 0019 相当、maui/0004 = 0025 相当) の改訂で済ませるかをフェーズごとに決める

## 未決の論点

- F (ブランチ運用) — phase 3 で決める
- G (KMP 形態一式) — phase 7 の agenda
- KMP Skill の切り方 — phase 1 で決める

## UI 素材

なし。

## 変更級の推奨: ロードマップへエスカレーション (1 change に収まらない)

9 フェーズ・9 change 相当。ksn-roadmap の「フェーズ昇格」フロー (library-foundation phase-11 / phase-9 → 新ロードマップ) で起案する。
