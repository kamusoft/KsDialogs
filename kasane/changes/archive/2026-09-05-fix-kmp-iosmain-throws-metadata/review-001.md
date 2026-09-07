# レビュー結果: fix-kmp-iosmain-throws-metadata (001 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

合意済みスコープ (iosMain の override 3 箇所から `@Throws` を外し、参照付きコメントを残す) と実装が完全に一致しており、余計な変更は無い。手元で再実行した metadata compile・framework link・`allTests` (96 件 / 0 failures)・consumer 側の `:shared:compileCommonMainKotlinMetadata` はすべて成功し、生成 ObjC ヘッダで Loading / Toast / Dialog の VM 経路 3 本が `@note ... converts instances of DialogException(, CancellationException) to errors` を保ったままであることも確認したので、Swift の NSError 契約が不変という主張は裏が取れている。Critical / Major は無い。指摘は証跡の可読性 1 件と、蒸留・利用者ドキュメントへの申し送り 2 件のみ。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加コメント 2 箇所を規約本文で判定 (機械検査だけに頼らない)
- `kasane/handbook/cross/test-execution.md` (テストの実行・結果の報告・完了判定) — kmp/ の全件実行は `./gradlew allTests`、件数は `ksdialogs-kmp/build/test-results/<ターゲット>/TEST-*.xml` で確認
- `kasane/handbook/cross/local-development-setup.md` (Gradle ルートのビルド開始) — 実行ルートの前提確認のみ
- 非該当: `sample-parity.md` (`samples/` を触らない)、`runtime-behavior-verification.md` (実行時挙動の不具合ではなくビルド時診断)、`user-skill-api-listing.md` (`skills/**` を生成・更新しない)、`aiforms-origin-reference.md` (移植作業ではない)。kmp ドメインの handbook は現時点で規約なし
- 決定: `kmp/ADR-0001` (accepted) — 現行照合 3 行はいずれも「**公開面**の `@Throws`」を対象としており、公開面 (`KsLoading` / `KsToast` の宣言) は本変更で無傷。実装側 override の宣言有無に触れた決定は無く、抵触なし
- 概念: `core/api/loading-semantics.md` / `core/api/toast-semantics.md` / `core/api/result-notification-semantics.md` — いずれも「契約 (interface) が VM 経路にだけ `@Throws` を宣言する」と書いており、記述と現状の乖離は生じていない
- `kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)

## 実行した検証 (すべて本レビューで再実行)

| 実行 | 結果 |
|---|---|
| `kmp/` `./gradlew :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL (`e:` 行なし。残る `w:` は expect/actual Beta の既存警告のみ) |
| `kmp/` `./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL。iosSimulatorArm64Test 49 tests / 0 failures + testAndroidHostTest 47 tests / 0 failures = **96 tests / 0 failures** (handbook の実測値と一致) |
| `kmp/` `./gradlew :ksdialogs-kmp:linkDebugFrameworkIosSimulatorArm64` | 成功 |
| `samples/kmp/` `./gradlew :shared:compileCommonMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL (`kmp/` の実行完了後に逐次で実行) |
| 生成 ObjC ヘッダ (`KsDialogsKmp.framework/Headers/KsDialogsKmp.h`) | `KsDialogs.show(viewModel:)` / `KsLoading.show(viewModel:)` / `KsLoading.start(viewModel:)` / `KsToast.show(viewModel:)` の 4 本すべてに変換注記が残存。`IosLoadingGateway` / `IosToastGateway` の出現は 0 件 (internal のため非 export) |
| `python3 scripts/comment-policy-lint.py --summary` / `--advisory` | 禁止 0 件 (検査対象 919 ファイル)、追加コメント 2 箇所の advisory 検出なし |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 検出 0 件 |

## 確認した観点 (指摘に至らなかったもの)

- **修正の網羅性**: `ksdialogs-kmp/src/` の `@Throws` を全数確認した。commonMain の interface 側 4 箇所と androidMain の 3 箇所は維持され、iosMain には 1 件も残っていない。`IosDialogGateway` が override する `DialogGateway.present` は commonMain 側にも `@Throws` が無く、`KsDialogs.show` の実装は commonMain の `GatewayKsDialogs` にあるため、同型の罠に残っている箇所はライブラリ内に無い (取りこぼしなし)
- **androidMain を触っていないこと**: JVM 側の `@Throws` は Java 相互運用の `throws` 節を生む実効のある宣言で、対称性のために外すのは不適切。触っていないのが正しい
- **未使用 import**: `IosLoadingGateway.kt` から外した `kotlin.coroutines.cancellation.CancellationException` は他に参照が無く、除去は妥当。`IosToastGateway.kt` の `DialogException` は同一 package のため import 不要で、`throw DialogException(...)` の本体は残っている
- **コメント規約の本文判定**: 2 箇所とも `internal` クラスの KDoc であり公開 doc コメントではないので設計根拠を置いてよい。外部参照は恒常 URL (YouTrack) のみで、作業文書パス・変更識別子・ローカル通番の混入なし。「2.4.10 で失敗し 2.5.0 で修正」は過去仕様の履歴記述ではなく現在の toolchain 条件の記述と読める。Kotlin 版数 2.4.10 は `android/gradle/libs.versions.toml` (kmp/ が version catalog として参照) と一致しており、記述は事実
- **コメントの自己完結性**: 「なぜ書かないか」「書くと何が起きるか」「契約が変わらない理由」の 3 点が揃っており、そのファイルだけを読んで意味が通る
- **付随修正の有無**: diff は合意済みスコープの 3 行削除 + コメント 2 箇所のみ。スコープ膨張なし。deviation.md は不在で、無断の逸脱も検出せず

## 指摘事項

### [🟡 Minor] 証跡の実行番号に欠番があり、落とした検証があるように読める
**該当箇所**: `evidence/metadata-compile-recovery.txt`「## 修正後」節 (箇条 1・2・3・5)
**問題点**: 番号が 1, 2, 3, 5 と飛んでおり、4 番の実行が失敗して削られたのか、単なる採番ミスなのかが読者に判別できない。証跡はアーカイブ後に残る記録なので、後から本件の完了度を検分する人が同じ疑問を持つ。なお決定事項が要求した検証 (metadata compile / samples 側 metadata compile / target compile / framework link / ObjC ヘッダ / 全テスト) は別節を含めて全項目が記載されており、内容としての欠落は無い (本レビューで全件を再実行して成功を確認済み)。
**推奨修正**: 1〜4 に採番し直すか、欠番の行に何を実行したかを補う。

### [🔵 Suggestion] 回避を撤回できる条件が短命層にしか無い
**該当箇所**: `kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:24-28`、`IosToastGateway.kt:17-21`
**問題点**: 「Kotlin 2.5.0 に上げたら override 側へ `@Throws` を戻せる (戻さなくても契約は同じ)」という撤回条件が、ソースコメントと `exploration.md` の申し送りにしかない。exploration はアーカイブされるため、toolchain 更新の担当者がこの分岐に気づく経路がソースコメント 1 本になる。
**推奨修正**: 蒸留時に `kmp/ADR-0001` の現行照合行、または `core/api/loading-semantics.md` / `toast-semantics.md` の `@Throws` 記述へ「実装側は継承に頼る (KT-88548 回避、2.5.0 で撤回可)」を 1 行足す。exploration の ADR 候補節が既に同趣旨を予告しているので、実装への追加変更は不要。

### [🔵 Suggestion] 同じ罠は利用者コードにも当たりうる
**該当箇所**: `skills/ja/ksdialogs-kmp/SKILL.md:66` および `skills/en/ksdialogs-kmp/SKILL.md:66` (本 change の diff 範囲外)
**問題点**: 現行の Skill 記述は「Swift から呼ぶ共有コードの関数に `@Throws` が要る」を説明しており、これ自体は正しい (commonMain のラッパー関数は罠に当たらない)。一方、利用者が `iosMain` で `KsLoading` / `KsToast` を実装する (テストダブル・独自ゲートウェイ) 場合に override へ `@Throws` を書くと、ライブラリ側と同じ false positive を踏む。Kotlin 2.4.10 を使う consumer には再現しうる。
**推奨修正**: exploration が申し送っている KMP Skill の再レビュー時に、この 1 ケース (iosMain で契約を実装するときは override に `@Throws` を書かず宣言を継承する) を注意書きとして扱うか判断する。本 change での対応は不要。

## アクションプラン

1. (任意・低優先) 証跡の実行番号を詰めるか欠番を補う — Minor 1 件。実装の再修正は不要
2. 蒸留時に撤回条件の 1 行を長命層 (`kmp/ADR-0001` の現行照合 または concepts の `@Throws` 記述) へ移す
3. `rollout-user-docs` の KMP Skill 再レビュー時に、iosMain 実装側の `@Throws` に関する注意書きの要否を判断する
