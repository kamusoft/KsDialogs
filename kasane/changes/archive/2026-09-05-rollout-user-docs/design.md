# Design: rollout-user-docs

## Context

phase-2 agenda (`kasane/roadmaps/package-distribution/phases/phase-2-docs-rollout/agenda.md`) の決定事項 (踏襲 5 件 + KsDialogs 固有 9 節) を実施する change。翻案元は KsSettingsView の rollout-user-skills (`../KsSettingsView/kasane/changes/archive/2026-08-28-rollout-user-skills/`) と consolidate-readmes-and-contribution (`../KsSettingsView/kasane/changes/archive/2026-08-30-consolidate-readmes-and-contribution/`)。KsDialogs との差は、形態が 4 つ (KMP がある)・Skill が 5 本・`samples/` 配下 README が正を握っている (廃止に移送と ADR 修正が伴う)・kmp の concepts が未整備、の 4 点。本文書は agenda で扱っていない実装寄りの判断 (references の分割・manifest の初期割当・新設文書の配置・fan-out の編成) と、agenda の決定を実装単位に写した対応表を Decision として置く。

現状の正 (2026-09-04 時点): concepts は core/api 8 本 + maui/api 1 本 + cross/reference 1 本の計 10 本。`skills/` は不在、docs-refresh は `.agents/skills/docs-refresh/` に配置済み (manifest 不在で停止案内)。ルート README は英語 1 枚 (35 行、開発者向け)。`samples/README.md` + 4 ルートの README が撮影用デモ ID・アプリ識別子・参照方式・ビルド手順・KMP iOS リンク構成の正。

## Goals / Non-Goals

Goals: proposal.md の What Changes。Non-Goals: proposal.md の Non-Goals。

## Decisions

### Decision 1: platform Skill の `references/` は利用者の「やりたいこと」6 本に割り、KMP はホスト側 2 本を足す

**採用案:** platform Skill (`ksdialogs-{ios,android,maui}`) の `references/` は次の 6 本。

| ファイル | 内容 (源泉 concept) |
|---|---|
| `dialogs.md` | ViewModel を登録して show する・結果 (completed / cancelled) の受け取り・インライン show・多段表示の保証 (registration-show / result-notification / multi-display) |
| `view-models.md` | ViewModel 主導の呼び出し: `notifier` による結果報告・型指定 show と VM factory・configure の順序 (model-binding) |
| `layout.md` | 配置・移動量・基準領域・外側タップキャンセルの添付 (layout) |
| `transitions.md` | 出入りの演出の添付・プリセット・結果が返る時点 (transition) |
| `loading.md` | Loading の show / hide / setMessage / start と進捗・カスタム View (loading) |
| `toast.md` | Toast の message 入口・登録経路・duration・配置・カスタム View (toast) |

MAUI Skill は `references/di-registration.md` を加える (源泉 maui/api/di-registration.md。`AddKsDialogs` / `RegisterForDialog` / fallback resolver)。KMP Skill (`ksdialogs-kmp`) は共有コード側の同 6 本 (共有コードから見た使い方: ViewModel 定義・show / Loading / Toast の呼び出し・添付) に `android-host.md` と `ios-host.md` を足した 8 本。ホスト側 2 本の責務は **Dialog / Loading / Toast の 3 機能ぶんの View 登録** (Android は Android Native の登録 API と同内容で、閉世界性のため重複を受け入れる — phase-1 決定 / iOS は Swift 側の型付き入口 `Dialog.shared.kmp` / `Loading.shared.kmp` / `Toast.shared.kmp` (`ios/Sources/KsDialogs/Kmp/`) と `@Throws` の経路表・統合手順の詳細)。機能別 6 本にはホスト側の登録を書かず、ホスト側 2 本へ振り分ける。移行 Skill は `references/api-mapping.md` の 1 本。SKILL.md 本文は「概念説明 (両入口の使い分け 1 段落を含む) → 能力マップ表 → Setup → 最小コード → references 振り分け」。

**理由:** phase-1 決定の分割軸は「利用者の状況」で、Skill 内の references は「やりたいこと」で割るのが翻案元 (cells / updates / styling / custom-cells) と同じ流儀。結果通知と多段表示は単独のレシピにならず (show の帰結として書く方が読める)、登録・表示と同じファイルに畳む。6 本は core/api 8 本と 1:1 ではないが、manifest の `targets` は多対多で書けるので追従は崩れない。

**代替案:**
- **A: concept 1 本 = references 1 本の 8 本** — 却下。result-notification と multi-display が「やりたいこと」を持たない読み物になり、レシピ形式 (見出し + リード + 完動コード) にならない
- **B: references を 1 本にまとめる** — 却下。段階開示が効かず、エージェントが毎回全文を読む。翻案元でも分割している
- **C: KMP Skill を共有コード側だけにし、ホスト側は Native Skill を併用させる** — 却下済み (phase-1 決定「KMP Skill は 1 本」、閉世界性違反)

### Decision 2: `samples/` 配下 README の移送対応表

**採用案:** 廃止する 5 本 (`samples/README.md`、`samples/{ios,android,maui,kmp}/README.md`) の節を次のとおり移す。廃止は移送とレビュー通過の後 (tasks の最終群)。

| 出所 (節) | 分類 | 行き先 |
|---|---|---|
| `samples/README.md` 冒頭のディレクトリ表 (参照方式・実行できる OS) | B' | `kasane/handbook/cross/local-development-setup.md` 「Sample のビルドと実行」節の冒頭表 |
| `samples/README.md` 「パリティ」「してはいけないこと」 | A | 捨てる (正は sample-parity.md。差分があれば sample-parity.md 側へ補う) |
| `samples/README.md` 「撮影のための起動引数」(キー 2 つ・外部表現・アプリ識別子・安定デモ ID 14 件) | B | `kasane/handbook/cross/sample-parity.md` に「撮影支援の起動引数」節を新設 (規約: 4 ルートが同じキーと ID を受け付ける) |
| `samples/{ios,android,maui,kmp}/README.md` 「構成」「参照方式」「ビルドと実行」「注意」のうちビルド系 (composite build と `dependencySubstitution` の理由・`apply false` の理由・xcodebuild / adb / dotnet の起動手順・SDK 位置) | B' | `local-development-setup.md` 「Sample のビルドと実行」節のルート別小節 |
| `samples/{ios,android,maui}/README.md` 「注意」「実装メモ」のうち器の責務と演出の添付 | A | 捨てる (transition-semantics / registration-show-semantics に既出) |
| `samples/kmp/README.md` 「ビルドと実行 > iosApp」(3 点リンク・Run Script・`integrateLinkagePackage` の再生成) | C | 新設 `kasane/concepts/kmp/api/ios-host-integration.md` の「Sample での再生成手順」節 |
| `samples/kmp/README.md` 「出入りの演出 (Transition Dialog)」 | A | 捨てる (Sample 内部構造。必要ならソースコメントへ、任意) |
| `android/layout-case-fixtures/README.md` | D | 対象外 (現状維持) |

参照元の付け替え: cross/ADR-0010 の Decision 節「安定デモ ID 9 件とアプリ識別子の一覧は samples/README.md「撮影のための起動引数」節が正」を handbook の節へ書き換える (本文修正、オーナー許可済み) / `kasane/config.yaml` `ui.screenshot` の「samples/README.md「撮影のための起動引数」節が正」の 1 行 / sample-parity.md 「関連」節の `samples/README.md` 行を削除。

**理由:** phase-2 agenda 決定 (2026-09-04)。規範は handbook・記述は concepts・既出は捨てる、という翻案元 phase-9 の A〜D 分類をそのまま使う。

**代替案:**
- **A: 現状維持 (README を残し利用者の入口にしないだけ)** — 却下 (オーナー判断: ADR は記録であって規約ではなく、正は handbook へ寄せる)
- **B: handbook に `sample-build.md` を新設** — 却下。正の本数が増える。既存 2 本に節を足せば足りる
- **C: 撮影引数を config `ui.screenshot` へ吸収** — 却下。規範 (4 ルート一致) が config に埋もれる

### Decision 3: manifest 初期版の `targets` 割当

**採用案:** concept 集合 (10 本 + 新設 kmp concept = 11 本) を次のとおり割り当てる。`excluded` は `cross/reference/reference-repositories.md` のみ。

| concept | 載せる Skill ファイル (言語抜き相対パス) |
|---|---|
| core/api/registration-show-semantics.md | `ksdialogs-{ios,android,maui,kmp}/SKILL.md`・同 `references/dialogs.md`・`ksdialogs-kmp/references/android-host.md`・`ios-host.md`・`ksdialogs-aiforms-migration/references/api-mapping.md` |
| core/api/result-notification-semantics.md | 4 platform の `references/dialogs.md`・`ksdialogs-kmp/SKILL.md`・`ksdialogs-kmp/references/ios-host.md` (`@Throws`)・`api-mapping.md` |
| core/api/multi-display-semantics.md | 4 platform の `references/dialogs.md` |
| core/api/model-binding-semantics.md | 4 platform の `references/view-models.md`・`ksdialogs-kmp/references/{android-host,ios-host}.md`・`api-mapping.md` |
| core/api/layout-semantics.md | 4 platform の `references/layout.md`・`api-mapping.md` |
| core/api/transition-semantics.md | 4 platform の `references/transitions.md` |
| core/api/loading-semantics.md | 4 platform の `references/loading.md`・`ksdialogs-kmp/references/{android-host,ios-host}.md`・`api-mapping.md` |
| core/api/toast-semantics.md | 4 platform の `references/toast.md`・`ksdialogs-kmp/references/{android-host,ios-host}.md`・`api-mapping.md` (Toast 節) |
| maui/api/di-registration.md | `ksdialogs-maui/SKILL.md`・`references/di-registration.md`・`api-mapping.md` |
| kmp/api/ios-host-integration.md (新設) | `ksdialogs-kmp/SKILL.md`・`references/ios-host.md` |

割当は生成ワーカーの報告 (ファイル別の源泉マップ) で最終確定し、この表は初期値。網羅検査 (3c / 6-①) は「concept がどこか 1 つに載る」ことしか見ないため、**ファイル単位の源泉完全性** (各ファイルの内容が依拠する concept がその `targets` にすべて載っていること) は独立レビューの明示的な合格条件にする。初期 manifest は `planned-manifest.py` が既存 manifest を前提とするため、まず bootstrap (`version` 3・`concepts` 空・`targets` 空・`excluded` に reference-repositories・`readmes` 4 枚) を書き、ワーカー報告を `DOCS_REFRESH_DECISIONS` の `addTargets` として重ねる。`api-mapping.md` の源泉は対応表が実際に触れた concept に絞る (phase-1 決定: 移行 Skill の源泉は新 API 側のみ)。

**理由:** phase-1 決定「core/api は 4 platform Skill、di-registration は maui と移行、移行 Skill は対応表が触れる core/api」の具体化。`architecture/` カテゴリの concept は現時点で存在しないため既定除外候補の適用はない。

**代替案:**
- **A: SKILL.md にだけ全 concept を紐づける** — 却下。追従時に SKILL.md だけが要追従になり references が取り残される
- **B: ワーカー報告だけで組み、初期値を置かない** — 却下。網羅不変条件の見通しが立たず、ワーカーごとの割当がぶれる
- **C: ホスト側の登録を機能別 6 本に混ぜ、ホスト側 2 本は統合手順だけにする** — 却下。共有コードの読者に Android / iOS 両方の登録コードを読ませることになり、3 側の振り分け (phase-1 決定) が崩れる

### Decision 4: 新設する長命層 2 本の配置と名前

**採用案:**
- `kasane/concepts/kmp/api/ios-host-integration.md` (type: description、kmp ドメイン初の concept。`kmp/index.md` を新設し concepts/index.md の kmp 行を更新)。内容: 消費者の共有モジュールが Maven 依存 1 点で済み SwiftPM 参照は発行 metadata で推移する / `integrateLinkagePackage` が合成パッケージ (`KotlinMultiplatformLinkedPackage`) を生成し xcodeproj へ書き込む (初回 1 回、以後自動、VCS に含める) / Swift 側の登録 API (`Dialog.shared.kmp`) 用に `KsDialogs-SPM` を Package Dependencies へ 1 点足す (同じ identity にデデュープ) / SwiftPM 連携は Kotlin 側で Alpha / Sample (`samples/kmp/iosApp`) での再生成手順 (Decision 2 の C)。出典: phase-10 PoC 記録 `kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-10-packaging-model/artifacts/poc-swiftpm-remote-distribution.md`、cross/ADR-0008、kmp/ADR-0002
- `kasane/handbook/cross/user-skill-api-listing.md` (kind: rule、paths: `skills/**`、tasks: docs-refresh 3e の仕分け)。翻案元 `../KsSettingsView/kasane/handbook/cross/user-skill-api-listing.md` の構成 (方針「簡潔でも網羅」・意図的な掲載除外の 4 基準・現行除外リスト・コード例のコメント・してはいけないこと) を KsDialogs へ翻案し、除外リストは Skill 生成後の 3e 実行結果に対するオーナー判断で初期化する (候補: KMP cinterop 機械面 `KsDialogsInteropBridge` / `KsDialogsInteropResultType` = 内部層、MAUI の binding assembly = 内部層、`ksDialogTransition` 等の添付 API は掲載)。`handbook/cross/index.md` に登載、`concepts/log.md` に記録、docs-refresh SKILL.md 3e の注記を規約参照へ差し替え

**理由:** kmp concept を `api/` に置くのは、利用者が踏む手順であり Skill の源泉として `targets` に載せるため (phase-1 決定: architecture カテゴリは既定で除外候補、利用者に効くものは targets へ)。handbook 名は翻案元と同名にして docs-refresh SKILL.md の参照差し替えを最小にする。

**代替案:**
- **A: kmp concept を `kmp/architecture/` に置く** — 却下。既定除外候補になり、`targets` に載せる例外を毎回説明することになる
- **B: 3e の仕分け基準を docs-refresh SKILL.md に直書き** — 却下。SKILL.md は道具の手順で、掲載範囲の規約 (オーナー判断の蓄積) は handbook の性格。翻案元も分けている
- **C: 除外リストを生成前に決め打つ** — 却下。3e の報告を見ずに決めると根拠のない除外になる (翻案元の「してはいけないこと」)

### Decision 5: ルート README の KsDialogs 固有節

**採用案:**
- 対応プラットフォーム表 (4 行: iOS Native / Android Native / .NET MAUI / Kotlin Multiplatform) は「最小 OS」「ライブラリのビルドに使った toolchain」の 3 列。値は docs-refresh 3d の取得元 4 行 (android version catalog・wrapper 2 本・`ios/Package.swift`・MAUI csproj) から実装時に読む。表の下に利用側の下限 (Kotlin の最小版・minSdk / compileSdk・MAUI 本体下限) と「表の版はビルドに使った版」の注記。利用側の Kotlin 最小版 (Android Native / KMP) は決定元が無いため値を書かず「確定前 (初回リリースまでに確定)」とし、SwiftPM 連携が Alpha である旨は注記に置く
- インストール節は 4 形態の依存宣言だけ: SwiftPM (`https://github.com/kamusoft/KsDialogs-SPM`、product `KsDialogs`)、Maven `jp.kamusoft:ksdialogs` (+ Compose 利用時 `ksdialogs-compose`)、NuGet `KsDialogs.Maui`、KMP は Maven `jp.kamusoft:ksdialogs-kmp` + iOS アプリ側の SwiftPM 1 点 (詳細は KMP Skill へ)。prerelease (`X.Y.Z-{alpha|beta|rc}.N`) の指定方法を ecosystem ごとに 1 行
- 最小コード例は 4 形態 (iOS / Android / MAUI / KMP 共有コード) を 1 例ずつ、対応する platform Skill の SKILL.md の最小コードブロックと byte 一致させる (既定エントリのみ使う)
- スクリーンショット 6 枚は `assets/{ios,android}-{dialog,loading,toast}.png`。参照は raw.githubusercontent の絶対 URL でブランチ名は `main` (暫定。phase-3 で確定)
- リポジトリ構成表: `ios/` `android/` `maui/` `kmp/` `samples/` `skills/` `assets/` `kasane/` の 8 行。`samples/` は「4 形態のサンプルアプリ」の 1 行でリンクなし
- 貢献節 3〜4 行 + `.github/CONTRIBUTING.md` へのリンク。ライセンス節は MIT。サードパーティ通知は Sample に該当依存が無ければ置かない (実装時に `samples/` の依存を確認する)

**理由:** phase-2 agenda 決定 (両入口・対応プラットフォーム表・スクリーンショット・`samples/` の扱い) と踏襲決定 (座標のみ・状態表記 1 行・翻訳ロックステップ) の写し。

**代替案:** agenda の各決定事項の却下欄を参照 (最小 OS だけの表 / 4 枚・0 枚のスクリーンショット / README にも両入口を書く / `samples/README` へのリンク)。

### Decision 6: Issue Forms と CONTRIBUTING

**採用案:** `.github/ISSUE_TEMPLATE/` に `bug_report.yml` / `feature_request.yml` / `question.yml` (英語) と `config.yml` (`blank_issues_enabled: false`、contact_links なし)。翻案元 `../KsSettingsView/.github/ISSUE_TEMPLATE/` を写し、製品名と Platform dropdown を差し替える。Platform は bug_report と question で同じ 7 択: `iOS` / `Android` / `.NET MAUI on iOS` / `.NET MAUI on Android` / `Kotlin Multiplatform on iOS` / `Kotlin Multiplatform on Android` / `Multiple platforms`。`CONTRIBUTING.md` / `CONTRIBUTING_ja.md` は翻案元を写し、製品名と 4 形態の表現を差し替える。

**理由:** phase-2 agenda 決定 (2026-09-04) と踏襲決定。

**代替案:** agenda の却下欄 (5 択 / 2 dropdown)。

### Decision 7: 生成の編成とレビュー 4 層

**採用案:** Skill 生成は ksn-orchestrator が ksn-implementer を Skill 単位で fan-out (5 ワーカー、最大 3 並列 — docs-refresh の並列上限と揃える)。各ワーカーは源泉 concepts (日本語正本) + 実装コード・テスト + 本 design の Decision 1 / 3 + docs-refresh `references/prompt-skill.md` の内容規約を受け取り、en / ja ペアを同一文脈で同時生成し、ファイル別の源泉マップを報告する。移行 Skill のワーカーは移植元のローカル clone (`kasane/concepts/cross/reference/reference-repositories.md` で解決) を追加で読む。README 2 枚・索引 2 枚・`.github/` は Skill 完成後に 1 ワーカー (または直接作業) で書く (最小コードを Skill から写すため)。レビューは agenda 決定の 4 層: 機械検査 → 独立レビュー (ksn-review、Skill 単位 + README 群で 1 本) → 初見レビュー (新鮮なエージェントに Skill 本文だけを渡し、この文書だけで利用目的を達成できるか / 宙に浮いた参照・意味の取れない新造語がないかを報告させる) → オーナー目視検収 (少なくとも ja 5 部)。

**理由:** 翻案元 phase-12 の決定と phase-1 の並列上限を合わせた。README は Skill の最小コードと一致させる契約があるため後段。

**代替案:**
- **A: 5 ワーカー同時起動** — 却下。docs-refresh の運用上限 (3 並列) と揃えないと、初期生成だけ別のリソース前提になる
- **B: README を Skill と並行生成** — 却下。最小コードの一致を後から合わせる手戻りが出る

## Risks / Trade-offs

- 生成 10 部の品質ばらつき: 4 層レビューで受ける。翻案元の落とし穴 (閉世界性・冒頭概念説明・導入節のパッケージ前提化がオーナー検収で追加要求として出た) は最初から内容規範に入れる
- 移送の取りこぼし: 廃止前に 5 本の全節を Decision 2 の表と突き合わせる (tasks 1.1)
- 暫定値 (KMP の Kotlin 範囲・公開座標、画像 URL のブランチ名) の追随漏れ: agenda TODO と各フェーズの申し送りで受ける。README 本文で「確定前」と書くのは Kotlin 最小版だけ (座標と URL は値として書き、印は付けない — 利用者向け文書に内部の進捗を漏らさない)
- Android の KMP ホスト側 references が Android Skill と重複: 閉世界性の代償として受け入れ済み (phase-1)

## Migration Plan

順序依存: (1) kmp concept と handbook 3e 規約の器を先に作る (Skill の源泉と 3e の参照先) → (2) Skill 5 本の fan-out → (3) manifest 草案・索引・README・`.github/` → (4) 機械検査と 3 層レビュー → (5) 検収 → (6) `samples/` README の廃止と参照付け替え・3e 除外リストの初期化 → (7) manifest の最終書き出しと最終検査。廃止を最後に置くのは、Sample README を生成の素材 (KMP iOS 手順・起動引数) として使い終えてから消すため。

## Open Questions

なし (agenda で解消済み)。

## ADR 候補

- Decision 2 と 5 の「README はルート 2 枚 + `samples/` 配下 README を持たない、開発者向け知識は handbook / concepts」— 翻案元 cross/ADR-0023 相当。境界 (利用者向け / 開発者向け文書の置き場) を越え将来を制約する。蒸留時に起票を判断 (proposal Non-Goals)
- Decision 6 の「貢献は Issue のみ、外部 PR なし」— 翻案元 cross/ADR-0024 相当。同上
- Decision 1 / 3 / 4 / 7 は候補にしない (Skill 内部の分割・manifest の初期値・文書の配置・編成は覆しやすい)
