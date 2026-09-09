# レビュー結果: add-consumer-verification (002 回目)

**日付**: 2026-09-09
**判定**: APPROVED

## サマリー

review-001 と second-opinion-code-001 の突き合わせ結果で「確定 / 採用 / 対処」とされた 5 件はすべて解消している。Major (KMP フィード準備の合成 Swift マニフェスト 2 本の復元) は、発行前の HEAD 一致検査・`EXIT` trap による成否によらない復元・`git checkout HEAD --` への切り替え・Git 管理外での明示的失敗の 4 点が実物にあり、証跡 `evidence/review-fix-001/1-kmp-lock-guard.txt` の主張と実装は整合する。残り 4 件 (Android 消費者のプラグイン版のカタログ共有、`Select Xcode` の 3 本一致、README lint の節重複、`# shellcheck shell=bash`) も、手元で回して効果を実測できた。

修正による退行は見つからなかった。`prepare-feed.sh` (kmp) の標準出力は復元の 1 行が末尾 2 行より前に出るため `build-consumer.sh` の `tail -n 2` の契約は変わらず、`android` 側の `tail -n 1` も無影響。Android 消費者のプラグインは実際に `buildEnvironment` を回して共有カタログの `agp` 9.3.0 / `kotlin` 2.4.10 に解決されることを確認した。`Select Xcode` の 3 本は逐語一致し、README lint の自己テストは新設の負ケースを含めて 16 項目すべて OK になる。足場 (proposal / design / specs) は未変更で、`tasks.md` の差分はチェックボックスの反転のみ (本文の差分 0)。

新規の指摘は Minor 2 件と Suggestion 1 件で、いずれも優先度は低い。1 つは呼び出し元の `EXIT` trap を連結する防御的コードの脱エスケープの誤りで、現在の呼び出し元が `EXIT` trap を張らないため実害はない。もう 1 つは deviation.md の記録が修正前の実装 (`prepare-feed.sh` 側の `git checkout --`) のままである点で、蒸留に渡る前に現行へ合わせたい。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (`always: true`) |
| cross/verification-ci.md | `.github/workflows/` を変える・CI の検証範囲を確認する |
| cross/ci-script-deletion.md | `scripts/**`・`.github/workflows/**`・`verification/**` のスクリプトを作る・レビューする |
| cross/local-development-setup.md | Gradle build root のビルドを始める・環境構築 |

参照した実装スキル: kotlin-impl-skill (Gradle / Kotlin)、swift-ui-impl-skill (SwiftPM)、github-workflow-skill (workflow)、csharp-impl-skill / maui-skill (MAUI)。`kasane/lessons/code-review.md` は不在。`kasane/lessons/process.md` の L-002 (主張の範囲と実証の範囲の一致) を証跡の読み方に適用した。

## 前回指摘の解消状況 (自分で実行した確認)

| 指摘 (出典) | 判定 | 確認したこと |
|---|---|---|
| KMP のフィード準備が失敗すると合成 Swift マニフェストが `file://` のまま残る / 復元が既存変更を破棄する (host Minor 1 / 相方 Major) | **解消** | `verification/lib/gradle-publish.sh:38-129` を実読。`ksv_require_pristine_swiftpm_locks` が (1) work tree 判定 (2) `ls-files --error-unmatch` (3) `git diff --quiet HEAD --` の 3 段で発行前に止め、`ksv_arm_swiftpm_lock_restore` が `EXIT` trap を張り、復元は `git checkout HEAD --`。`prepare-feed.sh:50` に同じ検査の前倒しがある。スクラッチの足場で `( ... )` サブシェル内では trap が発火せず親の終了時に 1 回だけ動くこと、SIGINT / SIGTERM でも `EXIT` trap が動くことを実測 |
| Android 消費者のプラグイン版がカタログ共有の外にある (host Minor 2) | **解消** | `verification/android/build.gradle.kts:5-10` は `libs.versions.agp.get()` と `alias(libs.plugins.composeCompiler)`。実際に `verification/android/gradlew -q ... buildEnvironment` を回し、`com.android.application:...:9.3.0` / `org.jetbrains.kotlin.plugin.compose:...:2.4.10` に解決されること、`android/gradle/libs.versions.toml` の `agp` / `kotlin` と一致することを確認。誤った説明コメントは消えている |
| `Select Xcode` の実体パス解決が consumer-maui だけにある (host Minor 3) | **解消** | 3 本の `Select Xcode` step を `diff` して逐語一致 (maui vs kmp / maui vs ios とも差分なし)。`actionlint` の指摘は SC2012 の 3 件のみで、変更前と同じ行・同じ型 |
| README の `## Minimal examples` 節自体が重複すると素通りする (相方 Minor) | **解消** | `scripts/readme-example-lint.py:51-67`・`154-161` に見出し数の検査が入り、fence 内外を区別する。`--selftest` を実行して 16 項目 OK (新設 3 項目「見出しを 1 件だけ数える」「fence の中は数えない」「節が 2 つあれば exit 1」を含む)。本検査も exit 0 |
| `verification/lib/*.sh` の `# shellcheck shell=bash` 欠落 (host Suggestion) | **解消** | 3 本の 1 行目に付与済み。`shellcheck verification/lib/*.sh` の SC2148 は消え、残るのは `SC2034` (source する側が使う変数、レビューでも除外していた型) のみ。`shellcheck -x -e SC2034 -e SC1091` は全スクリプトで exit 0 |

そのほか自分で実行した確認:

- `python3 verification/maui/check-dependencies.py --selftest` / `verification/kmp/check-dependencies.py --selftest` → exit 0 (退行なし)
- `identity-lint` / `local-path-lint` / `comment-policy-lint` / `scenario-id-coverage` / `ci-skip-lint` → 違反 0 件。`doc-structure-lint` の指摘は `kasane/roadmaps/**` の既存分のみで、今回変更した handbook 2 本・`concepts/log.md` には 0 件
- `actionlint .github/workflows/ci.yml .github/workflows/verify-consumer-*.yml` → SC2001 (ci.yml、変更前から) と SC2012 (消費者 3 本、同一行) のみ
- `git status --porcelain -uall verification/` → 65 ファイル (review-001 と同数)。`build/` `.gradle/` `.kotlin/` は ignore 済みで追跡候補に入らない。`git status kmp/` は差分なし (実装者の実行で `.swiftpm-locks` が汚れていない)
- `ci.yml` の lint job の検査 step は 8 本 (gitleaks / ローカル絶対パス / 個体情報 / コメント規約 / Scenario ID / CI 限定スキップ / SPM 同期スクリプト自己テスト / README 最小例) で、handbook の内訳表と順序まで一致
- 追跡 fixture 2 本 (`KotlinMultiplatformLinkedPackage/subpackages/.../Package.swift`・`VerificationApp/Package.swift`) は `https://github.com/kamusoft/KsDialogs-SPM` + `exact: "0.0.0-alpha.0"` の smoke 形のまま
- 足場: `proposal.md` / `design.md` / `specs/**` は未変更。`tasks.md` はチェックボックス以外の本文差分 0 (5.5 / 5.6 のみ未チェック)

## 指摘事項

### [🟡 Minor] 呼び出し元の EXIT trap を控える処理が、本体内の単一引用符を復元できない

**該当箇所**: `verification/lib/gradle-publish.sh:102` (と、控えた本体を戻す `:114`、連結して実行する `:105`)

**問題点**: `trap -p EXIT` の出力は本体を単一引用符で囲み、本体内の `'` を `'\''` に展開する。直前のコメント (`:95`) はこの規則を正しく書いているが、脱エスケープの置換は結果が `'` ではなく `\'` になる。

手元で実測した (呼び出し元が `trap 'echo "CALLER '\''q'\'' TRAP"' EXIT` を張った場合):

```
stripped : echo "CALLER '\''q'\'' TRAP"
impl     : echo "CALLER \'q\' TRAP"     ← 実装の置換結果
```

この壊れた本体は、`EXIT` trap での連結実行 (`eval "${KSV_PREVIOUS_EXIT_TRAP}"`) でも、`ksv_disarm_swiftpm_lock_restore` が元へ戻す `trap "${KSV_PREVIOUS_EXIT_TRAP}" EXIT` でも使われるため、呼び出し元の後片付けが意図と違う動きになる (上の例では `CALLER \'q\' TRAP` が出力される)。本体の構文次第では `eval` 自体が失敗しうる。

現在この経路を踏む呼び出し元は無い (`verification/kmp/prepare-feed.sh` は `EXIT` trap を張らない) ので、いまの動作は壊れていない。証跡 `evidence/review-fix-001/1-kmp-lock-guard.txt` の (d) 「既存 trap との連結」も、単一引用符を含まない本体でしか実証していない (足場の trap は `echo "呼び出し元の EXIT trap が動いた"` と読める) — 主張の範囲は実証の範囲に収まっているが、規則を扱う分岐は実証されていない。

**推奨修正**: 置換先を `\'` ではなく `'` にする (`sq="'"` を経由すれば期待どおり `echo "CALLER 'q' TRAP"` になることを実測済み)。または、字面を再構成せず `trap -p EXIT` の出力を丸ごと控えて `eval` でそのまま戻す形にし、連結して実行する本体だけを別に扱う。いずれにせよ、単一引用符を含む trap 本体の負ケースを足場の確認に加えたい。

### [🟡 Minor] deviation.md の記録が修正前の実装のまま

**該当箇所**: `deviation.md:6` (オーナー判断 A の項)

**問題点**: 「KMP の `prepare-feed.sh` は kmp/ の発行の後にこの 2 本を `git checkout --` で復元し」と書かれているが、現行の実装は (1) 復元の主体が `verification/lib/gradle-publish.sh` の `ksv_publish_kmp` (2) 復元コマンドが `git checkout HEAD --` (index に依存させない) (3) 発行の後だけでなく発行前の HEAD 一致検査と `EXIT` trap を伴う、の 3 点で記録と食い違う。差分の中身 (Scenario への影響・オーナー判断 A の狙い) は変わっていないので合意の逸脱ではないが、deviation.md は蒸留へそのまま渡る記録なので、`git checkout --` (index からの復元) という古い機構が長命層へ持ち込まれる。

**推奨修正**: 該当項の機構の記述を現行へ更新する (復元の置き場・`HEAD` 指定・発行前検査と `EXIT` trap の 2 段)。判断そのものは変えない。

### [🔵 Suggestion] README lint の 2 つの fence 判定が非対称

**該当箇所**: `scripts/readme-example-lint.py:62` (`stripped.startswith("```")`) と `:37`・`:99` (`FENCE_RE = ^```(\w*)$`)

**問題点**: 見出し数を数える側は「``` で始まる行」で状態を反転し、ブロックを取り出す側は「``` + 言語だけの行」に限る。属性付きの fence (```` ```swift title="x" ````) や 4 連バッククォートが README に入ると、片方だけが fence と見なして `in_fence` がずれ、節の見出し数が実際と食い違う (数え漏らしなら検査が緩み、数え過ぎなら誤検出になる)。現行の `README.md` には該当する記法が無いため、いま誤判定は起きていない。

**推奨修正**: 両者で同じ fence 判定 (開始は `FENCE_RE`、終了は `^``` $`) を共有する。

## 確認して問題がなかった観点 (指摘なし)

- **修正による退行**: `verification/kmp/prepare-feed.sh` の標準出力は「復元の 1 行 → Swift 参照の URL → 参照先」の順で、`build-consumer.sh:53` の `tail -n 2` が読むのは後ろ 2 行のまま。`verification/android/prepare-feed.sh` は `ksv_publish_android` しか呼ばず復元の行を出さないので `tail -n 1` も無影響。`build-consumer.sh` 3 本とも `set -euo pipefail` があり、`| tee /dev/stderr | tail` のパイプでフィード準備の失敗が握り潰されない
- **`ksv_publish_kmp` の復元経路の網羅**: 発行タスクの失敗・`ksv_require_maven_artifact` 5 件のいずれかの失敗・SIGINT / SIGTERM のすべてで `EXIT` trap が 1 回だけ動く。成功経路は `ksv_disarm_swiftpm_lock_restore` が trap を解いてから復元するので二重実行にならない。`( cd ... && ./gradlew ... )` のサブシェルでは trap が発火しない (実測)
- **`ksv_require_pristine_swiftpm_locks` の判定**: `git diff --quiet HEAD --` なので作業ツリーの変更と index の変更の両方を捉える。`ls-files --error-unmatch` で追跡されていない場合を分け、`rev-parse --is-inside-work-tree` で Git 管理外を分ける。3 つとも `ksv_fail` で発行前に止まる
- **Android 消費者のツールチェーン共有**: `settings.gradle.kts:76-80` の `versionCatalogs` 由来の `libs` がルートの `plugins` ブロックで解決でき、`:app` / `:app-core` の `compileSdk` / `minSdk` も同じカタログから読む。カタログの `agp` / `kotlin` を上げれば消費者も同時に上がる状態になった
- **`Select Xcode` のコメント**: 3 本とも自己完結した理由 (`xcrun` 経由の native ツール探索と `DEVELOPER_DIR` 配下 SDK の絶対パス解決) を持ち、外部文書の ID だけに依存していない。本体 workflow (`verify-ios.yml` / `verify-kmp.yml`) は解決を持たない元の形のままで、消費者側だけを揃えた形になっている
- **README lint の新設検査の検出力**: 節が 2 つある README は、見出し数の判定が無いと 1 つ目だけを見て成功していた (証跡の写しによる確認と、自己テストの実行結果が一致)。見せかけの負ケースではない
- **`# shellcheck shell=bash` の副作用**: 3 本とも `shell=bash` 指定後に新しい指摘が出ていない (bash 配列 `KSV_SWIFTPM_LOCKS` を含む)
- **足場の凍結**: `proposal.md` / `design.md` / `specs/consumer-verification/spec.md` / `specs/verification-ci/spec.md` は未変更。`tasks.md` の変更はチェックボックスのみ
- **deviation との整合 (上記 Minor 以外)**: 8 件のうち残り 7 件は実装と一致する。`<uses-sdk>` (見送り) と tasks 5.5 / 5.6 (保留) は突き合わせ結果の扱いのとおりで、本レビューでは指摘しない
- **配信先への副作用**: `verification/` に push / api-key / token / 署名系の経路はなく、workflow 4 本は `permissions: contents: read` のみ・`secrets` 不在。証跡のパスは `<repo>` `<work>` `<USER>` 等のプレースホルダで、生の絶対パスは残っていない (`verification/kmp/shared/build/` 配下の実パスは ignore 済みの生成物)

## アクションプラン

1. `verification/lib/gradle-publish.sh:102` の脱エスケープを直し、単一引用符を含む trap 本体の負ケースを確認に足す — Minor 1 (実害は現状なし。commit 前に直すのが望ましい)
2. `deviation.md:6` の機構の記述を現行実装へ更新する — Minor 2 (蒸留へ渡る前に)
3. `scripts/readme-example-lint.py` の fence 判定を 2 か所で揃える — Suggestion (任意)
4. オーナーが tasks 5.5 / 5.6 (CI 側) を通し、消費者 4 job の所要時間と `Select Xcode` の効きを証跡へ記録してから蒸留へ進む (前回と同じ完了条件。指摘としては再掲しない)
