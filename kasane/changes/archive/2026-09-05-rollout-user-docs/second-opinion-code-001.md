# セカンドオピニオン: rollout-user-docs (code-001)
**相方**: claude / **label**: so-code-rollout-user-docs / **日付**: 2026-09-05 / **対象**: rollout-user-docs の変更成果物と作業ツリー差分
---
`ksn-review` の規律に従い、正本 (proposal / design / 3本の delta spec / deviation / tasks / ui/brief)、現行実装・テスト・Sample、accepted ADR、handbook を独立に読んで判定しました。既存の review-*・novice-review・second-opinion は結論として参照していません。

---

# レビュー結果: rollout-user-docs (独立レビュー)

**日付**: 2026-09-05
**判定**: **CHANGES_REQUESTED**

## サマリー

成果物の中核 — 5 Skill × 英日 10 部、manifest v3、ルート README 2 枚、`.github/` 一式、長命層への移送 — は質が高く、事実面の照合はほぼ全件通りました。座標・最小 OS・toolchain は取得元 (`ios/Package.swift` / `android/gradle/libs.versions.toml` / wrapper / MAUI csproj) と全件一致、README の最小コード例 4 本は実 API 署名 (`KsDialogs.show` の `@Throws` + suspend、`Toast.shared.show(message:)`、`Toast.instance` / `Toast.Instance`) と一致、handbook へ移送した安定デモ ID 14 件は Sample 実装から抽出した 14 件と完全一致、移植元 clone を直接読んだ結果 `deviation.md` の 2 件 (旧 message overload 不在・`IReusableLoading.Hide()` の `Task` 戻り) はどちらも正確でした。

一方で、機械検査が構造上見落とす種類の欠陥が 5 件残っています。とくに ①利用者向け Skill にリポジトリ内部の Sample 再生成コマンドが混入 (閉世界性違反)、②accepted ADR-0007 の Decision 本文が削除済み `samples/README.md` を規範の写像元として指したまま (spec Scenario 未達・task 7.4 のチェックが実態と不一致)、③`concepts/core/api/toast-semantics.md` の改訂が proposal・deviation・log のどこにも記録されていない、の 3 件は成果物側の修正が必要です。加えて完了条件 (レビュー 4 層の全件 APPROVED) が未達のまま tasks 6.4 が未チェックで残っています。

## 照合した規約

- `handbook/cross/index.md` 経由: `comment-policy.md` (常時)、`test-execution.md` (完了判定)、`sample-parity.md` (移送先)、`local-development-setup.md` (移送先)、`user-skill-api-listing.md` (新設・`skills/**`)
- `decisions/`: cross/0006・0007・0008・0010・0011、android/0001
- `concepts/`: core/api/toast-semantics、cross/reference/reference-repositories、kmp/api/ios-host-integration (新設)

---

## 指摘事項

### 🟠 Major 1: 利用者向け Skill にリポジトリ内部の Sample 再生成手順が混入している (閉世界性違反)

**該当箇所**: `skills/en/ksdialogs-kmp/references/ios-host.md:18-23`、`skills/ja/ksdialogs-kmp/references/ios-host.md:18-23`

**問題点**: 「The following command is only for explicitly regenerating **the repository Sample's** checked-in package」として、`XCODEPROJ_PATH="$PWD/samples/kmp/iosApp/KsDialogsSampleKmp.xcodeproj"` / `./samples/kmp/gradlew -p samples/kmp :shared:integrateLinkagePackage` を掲載しています。Skill は `.agents/skills/` へコピーされて単体で読まれる閉世界の文書であり、利用者のプロジェクトに `samples/kmp` は存在しません。user-skills spec の Requirement「閉世界性」(Skill ルート外のファイルを参照しない / リポジトリ内部の事情を含まない) に反します。docs-refresh 6-⑤ の grep は `kasane/`・ADR 番号・interop 名・相対リンクだけを見るため、この形は機械検査を素通りします。源泉が `kasane/concepts/kmp/api/ios-host-integration.md` の「Sample で合成 package を再生成する」節 (開発者向け知識) であり、移送先の切り分けを誤ったものです。

**推奨修正**: 英日ともこの段落とコードブロックを削除する (前段の「Step 2 は初回のみ、以後は通常の Gradle build が自動更新」で利用者に必要な情報は完結しています)。Sample 再生成手順は concept 側に残せば十分です。

### 🟠 Major 2: 削除済み `samples/README.md` への参照が accepted ADR の Decision 本文に残存している

**該当箇所**: `kasane/decisions/cross/0007-sample-parity-demo-item-unit.md:20` (Decision 節)、副次的に同 `:37`・`:39`、`kasane/decisions/cross/0006-samples-aggregated-consumer-boundary.md:38`

**問題点**: ADR-0007 の Decision 本文が「規約の正は handbook が持つ。**`samples/README.md` はその利用者向け抜粋 (写像)** であり、食い違ったら handbook が勝つ」と、本 change で `trash` した文書を規範関係の一方として宣言し続けています。repository-docs spec の Scenario「廃止した README への参照の解消」は除外を `kasane/changes/archive/` とロードマップ過去記録に限定しており、`decisions/` は除外対象外です。task 7.2 は ADR-0010 のみを付け替え対象としており、ADR-0007 / 0006 が漏れています。それにもかかわらず task 7.4「`samples/README` へのリンク・文字列言及が 0 件」は `[x]` になっており、tasks.md のチェックが実態と一致していません (`kasane/concepts/log.md`・`kasane/lessons/inbox/` にも言及が残りますが、こちらは append-only の履歴記録であり実害は小さい)。

**推奨修正**: ADR-0007 の Decision 該当行を「写像の存在」ごと削除するか、`handbook/cross/sample-parity.md` が唯一の正である旨に書き換える (ADR-0010 と同じくオーナー許可のうえで本文修正)。ADR-0006:38 の現行照合行は footer なので、参照方式の一覧の在り処を `handbook/cross/local-development-setup.md`「Sample のビルドと実行」節へ更新するのが自然です。task 7.4 の検査範囲 (除外パス) を成果物側に明記し、チェックを実態に合わせてください。

### 🟠 Major 3: `concepts/core/api/toast-semantics.md` の改訂がどこにも記録されていない

**該当箇所**: `kasane/concepts/core/api/toast-semantics.md:31-34` (既定エントリ列: Android `Toast` (object) → `Toast.instance`、MAUI `Toast` (静的クラス) → `Toast.Instance`、KMP → `Toast.instance`)

**問題点**: 改訂内容そのものは正しく、実装と一致することを確認しました (`android/.../Toast.kt:52-55` の `companion object { val instance }`、`maui/.../Toast.cs:20-23` の `sealed class Toast` + `Instance`、`kmp/.../Toast.kt` の `expect object Toast { val instance }`) — つまり既存 concept 側の誤りを直したもので、内容に異論はありません。問題は記録の欠落です。

- proposal「Impact / 長命層への波及」は handbook 2 改訂 + 1 新設・concept **1 新設**・ADR 本文修正のみを列挙しており、concept の**改訂**は含まれていません
- `deviation.md` に `[付随修正]` としての記録がありません (docs-refresh SKILL.md の付随修正は記録されているのに、こちらだけ欠落)
- `kasane/concepts/log.md` の追記 (2026-09-04) は新設 2 本と移送のみで、本改訂に触れていません
- frontmatter の `timestamp` が `2026-09-02` のまま更新されていません

user-skills spec の Requirement「生成の内容規約」③は「concepts と実装の矛盾は drift 所見として報告し、独断でどちらも書き換えない」と定めており、無記録の書き換えはこの規律に正面から抵触します。manifest の SHA は最終状態で再計算済みのため機械検査では検出できません。

**推奨修正**: `deviation.md` に `[付随修正]` として (発見経緯・実装での裏取り・オーナー判断の有無を含めて) 記録し、`concepts/log.md` に 1 行追記、`timestamp` を `2026-09-05` へ更新してください。

### 🟠 Major 4: 完了条件 (レビュー 4 層) が未達のまま、KMP の文書化した経路がビルドで再現しない

**該当箇所**: `kasane/changes/rollout-user-docs/tasks.md:59` (6.4 が `[ ]`)、`deviation.md` の Task 9.1 記載、`skills/{en,ja}/ksdialogs-kmp/**`

**問題点**: user-skills spec の Requirement「レビュー 4 層」Scenario は「独立レビューの最終判定が**全件 APPROVED**」を明示的な完了条件としています。tasks.md 自身が「KMP は `review-019.md` が CHANGES_REQUESTED のまま」と記録し、6.4 も未チェックです。加えて提示された検証結果では、現 revision で `:ksdialogs-kmp:compileIosMainKotlinMetadata` と Sample 側 `:shared:compileCommonMainKotlinMetadata` が `Member overrides different '@Throws' filter` (`IosLoadingGateway.kt` 2 件 / `IosToastGateway.kt` 1 件) で失敗します。これは製品既存不具合で本 diff 起因ではなく、`fix-kmp-iosmain-throws-metadata` に簡易起票済みである点は妥当な処理ですが、**失敗するのは KMP consumer の commonMain metadata compile 経路**であり、まさに `ksdialogs-kmp` Skill が案内する統合形です。ksn-review の規律上、ビルド失敗を伴う状態を APPROVED にはできません。

**推奨修正**: 判断が要る論点です。(a) `fix-kmp-iosmain-throws-metadata` の修正を先に通してから本 change を締める、(b) 文書 change として先に締め、KMP Skill の完了だけを別 change へ引き継ぐ — のいずれをとるかをオーナー判断とし、選んだ側を `deviation.md` と tasks 6.4 に明記してください。(b) を採る場合でも、spec の完了条件 (全件 APPROVED) を満たさない形で締めることになるため、spec 側の Scenario に対する合意済み差分として明示的に記録が必要です。

### 🟠 Major 5: 配布前 (未公開) の表現が 5 Skill 間で割れている

**該当箇所**: `skills/{en,ja}/ksdialogs-android/SKILL.md:28`、`skills/{en,ja}/ksdialogs-kmp/SKILL.md:31` (明記あり) ⇔ `skills/{en,ja}/ksdialogs-ios/SKILL.md:28`、`skills/{en,ja}/ksdialogs-maui/SKILL.md:28`、`skills/{en,ja}/ksdialogs-aiforms-migration/SKILL.md:27` (明記なし)

**問題点**: Android と KMP の Skill は「Public Maven distribution has not started yet, so these coordinates cannot currently be resolved and Setup cannot be completed from public artifacts」と明記しているのに、iOS Skill は SwiftPM URL をそのまま追加させ、MAUI Skill と移行 Skill は `Version="0.1.0"` を確定版のように提示していて、未公開である旨の記載がまったくありません。4 形態はいずれも未配信なので、状態は同じです。Skill は単体でコピーされる閉世界文書 (ルート README の「Release preparation」バナーは読者に届かない) なので、MAUI / iOS の読者は解決できない座標をそのまま実行して失敗します。逆に「未配信を書かない」方針で揃えるなら Android / KMP の記述が過剰です。どちらに寄せるかは方針判断ですが、**現状の不統一は誤案内**です。

なお `0.1.0` は `maui/KsDialogs.Maui.csproj:12` の `<Version>` と一致しており出所は正当ですが、他 3 形態が `<version>` プレースホルダなのに MAUI だけ具体値である点も、同じ不統一の一部です。

**推奨修正**: 5 Skill で表現を統一する。KMP の座標は phase-7 未確定 (proposal Non-Goals) なので「暫定」の印は必要ですが、「未配信ゆえ解決できない」の注記は 5 部すべてに置くか、5 部すべてから外して README のバナーに一本化するかを選んでください。

### 🟡 Minor 6: handbook の表直後に空行がなく、段落が表の行として描画される

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md:142-143`

**問題点**: 除外リストの最終行 (`| iOS | ...AnyObject...`) の直後に空行なしで「リストの更新は、API 名網羅検査の報告に対して…」の段落が続いています。GFM では表は空行または別のブロック構造の開始で終端するため、この行は表の 1 セル行として吸われて描画が崩れます。運用ルールの本文が表の末尾に紛れ、読み落とされます。

**推奨修正**: 142 行目と 143 行目の間に空行を 1 行入れる。

### 🟡 Minor 7: 新設・改訂した長命文書の `timestamp` が実際の確定日と合っていない

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md:9` (`2026-09-04`)、`kasane/concepts/core/api/toast-semantics.md:6` (`2026-09-02`)

**問題点**: `user-skill-api-listing.md` は task 1.2 で「除外リストは空表で置く」として 09-04 に新設され、実際の除外リスト (約 100 行) は task 6.3 で 09-05 に確定していますが、`timestamp` は 09-04 のままです。`local-development-setup.md` は 09-05 に更新されており、扱いが揃っていません。`toast-semantics.md` は Major 3 のとおり。`timestamp` は drift 棚卸しの最終検証日として使われるため、確定日とずれると次回の棚卸しで誤った鮮度判断を招きます。

**推奨修正**: 両文書の `timestamp` を `2026-09-05` に更新する。

### 🔵 Suggestion 8: 除外リストが検査ノイズで肥大しており、長命規約としての可読性が落ちている

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md:100-142`

**問題点**: 確定した除外の相当部分が API ではないトークンです — `Bool` / `AnyObject` / `TimeInterval` / `LazyColumn` などの framework 標準型、`build.gradle.kts` のようなファイル名、`C05` / `C19` / `approvedBy` / `approvedDiff` / `A.min` のような concept 内の case ID・数式断片。これらは `api-coverage-check.py` のトークン抽出が広すぎることの帰結であり、掲載方針の判断ではありません。規範層の文書が検査実装のノイズを恒久的に抱え込むと、本当に判断が要る除外 (例: `DialogViewRegistry.Shared`・`LoadingCoordinator`) が埋もれます。

**推奨修正**: 本 change の範囲外で構いませんが、(a) 抽出側でこの種のトークンを除外する、または (b) 「機械検査のトークン抽出由来の非 API 名」を基準ごと 1 行にまとめる、のいずれかを docs-refresh 側の改善として申し送ってください。

### 🔵 Suggestion 9: 移行対応表に移植元の `#if DEBUG` 限定メンバーが公開 API として載っている

**該当箇所**: `skills/{en,ja}/ksdialogs-aiforms-migration/references/api-mapping.md` (Toast 節の `Concrete Toast.Show(ToastView view, object viewModel = null)` 行)

**問題点**: 移植元 `IToast.cs` ではこのオーバーロードは `#if DEBUG` の中にあり、リリース構成の公開面には存在しません。「旧公開 API の網羅」としては過剰で、移行者が使っていないメンバーの移行方針を読むことになります。実害は小さく、網羅側に倒した判断も理解できます。

**推奨修正**: 残すなら「移植元の DEBUG ビルド限定」の一言を添える。

---

## 確認して問題がなかった観点

- **delta spec との一致**: `skills/` の構成 (en/ja × 5 Skill、references 6/6/7/8/1)、frontmatter 4 フィールド、索引 README の 3 要素・5 行、manifest v3 (concept 11 / target 33 / excluded 1 / readmes 4)、Issue Forms の Platform 7 択と `blank_issues_enabled: false`、公開ドキュメント面の README がちょうど 5 枚 — いずれも spec どおり
- **公開 API・実挙動との照合**: README / Skill の最小コード例 4 本を `KsDialogs.kt` / `Toast.swift` / `Toast.kt` / `Toast.cs` の実署名と突き合わせて一致。KMP の `@Throws(DialogException::class, CancellationException::class)` は commonMain 契約の宣言と同一
- **セットアップ条件**: 対応プラットフォーム表の全値が取得元 4 か所と一致 (iOS 17 / swift-tools 6.3、minSdk 24 / compileSdk 36、Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.7.0、net10.0 / Maui.Controls 10.0.1)。座標は cross/ADR-0008・android/ADR-0001 の確定値と一致。Kotlin 利用側下限は spec どおり値を書かず「確定前」
- **移送の完全性**: 安定デモ ID 14 件が Sample 実装から抽出した 14 件と完全一致。起動引数キー 2 件・アプリ識別子 4 ルート・外部表現・4 形態のビルド/起動手順が handbook に到達可能。`local-development-setup.md` → `sample-parity.md#撮影支援の起動引数` と `ios-host-integration.md#sample-で合成-package-を再生成する` のアンカーはいずれも解決する
- **deviation の裏取り**: 移植元 clone を直接読み、旧 `IToast` に message overload が無いこと、`IReusableLoading.Hide()` が `Task` を返し README の `void` 記載が誤りであることを確認。2 件とも記録どおり
- **Toast 挙動差 3 点**: `toast-semantics.md:56 / 73 / 79` (上限クランプなし・タッチ素通し・重なって並存) と移行 Skill の記述が一致
- **英日等価性**: 全 34 ファイルペアで表の行数・箇条書き数が完全一致。ルート README 2 枚の画像参照 6 件も同一パス
- **配信準備中表記**: ルート README の冒頭 1 行のみで、インストール節には無し。API 安定性 (0.x) の常設記述と分離されている
- **Reduce Motion**: `README*.md` と `skills/**` に記載なし (非成果物の決定どおり)

## アクションプラン

1. **Major 1** — KMP `ios-host.md` の Sample 再生成ブロックを英日とも削除 (閉世界性違反・修正は局所)
2. **Major 3** — `toast-semantics.md` の改訂を `deviation.md` `[付随修正]` と `concepts/log.md` に記録し、`timestamp` を更新
3. **Major 2** — cross/ADR-0007 (と 0006 の現行照合行) の `samples/README.md` 参照を付け替え、task 7.4 の検査範囲とチェック状態を実態に合わせる
4. **Major 5** — 5 Skill の配布前表現をオーナー判断で統一 (全部に注記 / 全部から削除)
5. **Major 4** — 完了条件の扱い (KMP 修正待ちか、別 change への分割か) をオーナー判断し、選択を `deviation.md` と tasks 6.4 に明記
6. **Minor 6・7** — 空行 1 行の挿入と `timestamp` 2 件の更新
7. **Suggestion 8・9** — 本 change 外への申し送りで可

## 突き合わせ結果 (2026-09-05)

相方の 9 件を現行差分・正本・既決定と個別に照合した。採用して本 change で修正 7 件、採用して Kasane の簡易起票へ申し送り 1 件、既知 blocker として確認し未解決 1 件、却下 0 件。

| # | 指摘 | 採否 | 根拠と対応 |
|---|---|---|---|
| 1 | KMP iOS host Skill に repository Sample 再生成手順が混入 | **採用・修正** | `user-skills` の閉世界性に反することを確認。英日 `ios-host.md` から Sample 固有段落と command を削除し、開発者向け concept にだけ残した |
| 2 | accepted ADR に廃止 README の active な参照が残る | **採用・扱いを修正** | 当初は既存行を直接修正したが、ホスト再レビューで accepted ADR 不変規律への違反と判明したため取り消した。cross/ADR-0006・0007 は既存本文・過去 footer を履歴として保持し、2026-09-05 の現行照合 footer で handbook が現在の所在であることを明示。承認済み spec も凍結版へ戻し、達成不能な全文字列 0 件との差は deviation と task 7.4 に記録した。active な参照は 0 件 |
| 3 | Toast concept の事実訂正に記録がない | **採用・修正** | 承認済み proposal は凍結版を保持し、deviation の付随修正と concepts/log に記録。timestamp と manifest hash を更新し、Android / MAUI / KMP の singleton 名は実装に一致することを再確認した |
| 4 | KMP metadata compile 失敗によりレビュー完了条件が未達 | **確認・未解決** | 既知の製品不具合と同一。簡易起票は成功証跡や免除ではないため、task 6.4 は未チェック、KMP の CHANGES_REQUESTED も維持する。本 change を完了扱いしない |
| 5 | 配布前表現が Skill 間で不統一 | **採用・修正** | 不統一は実在。ただし agenda の確定事項は「配信準備中の解除箇所をルート README 冒頭 1 箇所だけに限定」なので、全 Skill へ増やす案は不採用。Android / KMP と KMP Android host に混入した現況表記を削除し、KMP 座標が暫定である事実だけ残した |
| 6 | handbook の表直後に空行がない | **採用・修正** | 段落境界を明確にする空行を追加した |
| 7 | 長命文書の timestamp が確定日と異なる | **採用・修正** | `toast-semantics.md` と `user-skill-api-listing.md` を 2026-09-05 に更新した |
| 8 | API 名網羅検査の token noise が長命除外表を肥大化 | **採用・簡易起票** | 現 change では確定済みオーナー仕分けを維持し、抽出器の改善を `refine-docs-refresh-api-token-extraction` に簡易起票した |
| 9 | 移植元の具象 Toast route が DEBUG build 限定と分からない | **採用・修正** | 対応行を増やさず、英日同じ行に DEBUG build 限定を注記した |

修正後の docs-refresh 検査 (concept coverage / heading parity / code-block parity / frontmatter / internal links)、local path / identity lint、変更した長命文書の構造 lint、manifest 11 concept の SHA-256、残存参照 grep、`git diff --check` はすべて成功した。KMP metadata blocker だけは解消していない。
