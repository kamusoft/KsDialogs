# セカンドオピニオン: add-consumer-verification (spec-001)
**相方**: codex / **label**: so-spec-add-consumer-verification / **日付**: 2026-09-09 / **対象**: 提案一式 (proposal.md / design.md / specs/ / tasks.md)
---
# レビュー結果: add-consumer-verification

静的レビューのみ実施しました。ビルド・テスト・ファイル書き込みは行っていません。

## サマリー

全体方針は具体的ですが、現行 MAUI 契約との衝突、smoke とフィード準備の矛盾、未公開版を参照する KMP 追跡物、現変更内で検証できない Scenario が残っています。このまま実装すると仕様適合を判定できないため、実装前に設計判断と仕様修正が必要です。

指摘件数: Critical 0 / Major 7 / Minor 2 / Suggestion 0

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — always
- `kasane/handbook/cross/verification-ci.md` — workflow 変更・完了判定
- `kasane/handbook/cross/test-execution.md` — 検証の空振り防止
- `kasane/handbook/cross/local-development-setup.md` — Gradle build root と Android SDK
- `kasane/handbook/cross/ci-script-deletion.md` — verification/ スクリプト
- `kotlin-impl-skill` — Gradle Kotlin DSL・検査コード
- `swift-ui-impl-skill` — Swift/Xcode 消費者面
- 関連 ADR: cross/0008・0009・0017・0018・0019・0020・0021、maui/0004
- 関連 concept: `kasane/concepts/kmp/api/ios-host-integration.md`

## 指摘事項

### [🟠 Major] MAUI Controls 10.0.70 は現行の決定・実装と衝突する

**該当箇所**: `specs/verification-ci/spec.md:95`

**問題点**: Modified Requirement は `Microsoft.Maui.Controls` を 10.0.70 としていますが、現行値は `maui/Directory.Packages.props:17` の 10.0.20 です。後発の accepted ADR `kasane/decisions/maui/0004-nuget-three-package-structure.md:35` は workload 同梱版を採用し、同ファイル `:49` で 10.0.70 を明示的に却下しています。proposal は発行構成を変更しないとしており、tasks にも 10.0.70 へ戻す作業がないため、仕様を満たせません。

なお `cross/ADR-0018:29` 自体にも古い 10.0.70 が残っており、長命層同士にも既存衝突があります。

**推奨修正**: Requirement を「workload set 同梱版（現在 10.0.20）」へ修正してください。併せて、cross/ADR-0018 と maui/ADR-0004 の衝突をどの決定が正として解消するか確定してください。

### [🟠 Major] lint の8検査目を追加するのに ADR 候補なしとしている

**該当箇所**: `design.md:93`

**問題点**: `specs/verification-ci/spec.md:76` は README 一致検査を加えて lint を8検査へ変更します。一方、accepted な `kasane/decisions/cross/0021-ci-only-skip-owner-allowlist-and-lint.md:15` は、7検査に固定されており無断追加できないことを明記しています。にもかかわらず design は ADR 候補を「なし」としています。

agenda でオーナー判断済みでも、既存 ADR の決定を置き換える事実は残ります。

**推奨修正**: ADR 候補に「README 一致検査を加えて lint を8検査とする」を追加し、蒸留時に cross/ADR-0021 を amend する計画を tasks に明記してください。

### [🟠 Major] smoke・artifact・フィード準備の契約が相互に矛盾する

**該当箇所**: `specs/verification-ci/spec.md:6`

**問題点**: 同 Requirement は「artifact がなければ job 内でフィード準備」と規定しますが、artifact は dry-run 専用で、smoke では指定を拒否します。したがって文面上、smoke は必ずフィード準備を行うことになります。これは公開レジストリだけを使う `specs/consumer-verification/spec.md:69` と衝突します。

また artifact Scenario の `specs/verification-ci/spec.md:13` はフィード準備を一切行わないと読めますが、KMP は artifact 指定時も clone/tag と kmp/ 発行を行う契約です。

**推奨修正**: 次のモード表を Requirement と Scenario に明記してください。

- dry-run、artifact なし: 全フィード準備を実行
- dry-run、artifact あり: 通常は準備なし。KMP だけ clone/tag と kmp/ 発行を実行
- smoke: artifact 禁止、フィード準備なし、公開レジストリから解決

### [🟠 Major] smoke 正ケースを現変更では受け入れ確認できない

**該当箇所**: `specs/consumer-verification/spec.md:77`

**問題点**: Scenario は4形態の公開レジストリ解決とビルド成功を要求しますが、`proposal.md:34` と `tasks.md:43` は未公開を理由に、その実証を phase-9 へ送っています。したがって本変更の完了時点では Scenario を満たしたか判定できません。

同様に `specs/consumer-verification/spec.md:36` の「全形態に同じ version」は、iOS の exact を確認するには smoke が必要ですが、指定版が未公開なら依存解決まで通せません。

**推奨修正**: 現変更の Scenario は参照設定・座標・exact の生成検査までに限定し、公開レジストリからの解決成功 Scenario は phase-9 の change へ移してください。現在の Scenario を残すなら、本変更の完了条件に実在する公開 fixture を用意する必要があります。

### [🟠 Major] 追跡する KMP の smoke 形が解決不能な version を指す

**該当箇所**: `design.md:52`

**問題点**: design は、公開しない合成版 `0.0.0-alpha.0` を dry-run 用と決めていますが、追跡する linkage package と `VerificationApp/Package.swift` は配信リポジトリの同版を exact 指定する smoke 形です。その tag は意図的に公開されないため、追跡状態は解決可能な smoke fixture になりません。

それにもかかわらず `design.md:55` は clone 直後に Xcode project を開ける根拠としており、`tasks.md:19` もこの状態を `integrateLinkagePackage` でどう生成するかを定めていません。

**推奨修正**: 次のいずれを契約とするか決めてください。

- 追跡物は構造確認用の非解決 fixture とし、ビルド前の再生成を必須と明記する
- 初回公開後の実在版を追跡する
- 追跡用テンプレートと生成済み linkage package の役割を分け、concept の「VCS に含める」要件との関係を明記する

### [🟠 Major] 新しい Gradle root の Android SDK 解決が設計されていない

**該当箇所**: `tasks.md:17`

**問題点**: `verification/android` と `verification/kmp` は新しい Gradle build root ですが、SDK の解決方法がありません。`kasane/handbook/cross/local-development-setup.md:17` のとおり、AGP は root ごとに `local.properties` を解決します。そのため本体が `android/local.properties` だけでビルドできる環境でも、Scenario「本体がビルドできる状態なら引数なし dry-run」が新しい root で失敗します。翻案元でも同じ欠落が実装レビューの Major になっています。

**推奨修正**: `ANDROID_HOME` / `ANDROID_SDK_ROOT` が無い場合に、既存 `android/local.properties` の `sdk.dir` を検証スクリプトから引き継ぐ契約と負ケースを追加してください。新しい2 rootを handbook の build root 一覧へ追記するタスクも必要です。

### [🟠 Major] KMP 固有検査に空振りを検出する負の確認がない

**該当箇所**: `tasks.md:27`

**問題点**: KMP 検査は5 publication、core の版、metadata の URL/exact/deployment target、2つの Package.swift、pin の一意性を独自に解析します。しかし tasks 5.2 には、これらを壊して検査自体が失敗することを示す負ケースがありません。複雑な出力パーサーが対象行を取り違えたり、検査対象が空でも成功したりしても、正ケースだけでは検出できません。

**推奨修正**: 少なくとも次の負ケースまたは checker の自己テストを追加してください。

- iOS publication を1件欠落させる
- `ksdialogs-core` を別版にする
- metadata の URL / exact / deployment target を変える
- 2つの Package.swift のURLを不一致にする
- pin が0件または2件になる

### [🟡 Minor] develop push の2つの Scenario が矛盾する

**該当箇所**: `specs/verification-ci/spec.md:33`

**問題点**: この Scenario は任意の develop push で本体5 jobがすべて走るとしていますが、Requirement と `:48` の Scenario は記録だけの push では本体 job をスキップします。

**推奨修正**: GIVEN を「ビルド・テストの入力を含む commit」に限定するか、THEN を変更種別による条件分岐として記述してください。

### [🟡 Minor] artifact の展開レイアウトが再利用契約に含まれていない

**該当箇所**: `specs/verification-ci/spec.md:5`

**問題点**: 入力として定義されているのは artifact 名だけで、download 後に `--reference` が指すディレクトリの内容が未規定です。iOS のスナップショットルート、Maven repository root、NuGet feed、KMP の Android 発行物について、phase-9 の package job がどの階層を upload すべきか一意に決まりません。

**推奨修正**: 形態ごとの artifact ルート構造と必須ファイルを design に定義し、5.6 の一時 upload/download 確認で同じレイアウトを検証してください。

## アクションプラン

1. MAUI 10.0.20 / 10.0.70 の長命層衝突を解消する。
2. smoke・artifact・フィード準備のモード表を確定する。
3. 未公開合成版を持つ KMP 追跡物の位置づけを決める。
4. smoke 正ケースを phase-9 の仕様へ移すか、現変更で検証可能にする。
5. lint 8検査目を ADR 候補として捕捉する。
6. Android SDK 解決、KMP 検査の負ケース、artifact レイアウトを tasks に追加する。
7. CI Scenario の矛盾を修正してから実装へ進む。

NEEDS_DISCUSSION


## 突き合わせ結果 (2026-09-09)

ホスト側の自己レビュー (2 周、チェックリスト通過) と突き合わせ。いずれもホスト側では検出していない「相方のみ」の指摘で、根拠を実物 (`maui/Directory.Packages.props:17`、maui/ADR-0004、cross/ADR-0018、cross/ADR-0021、handbook cross/local-development-setup.md、翻案元 `verification/android/android-sdk.sh`) で確認して採否を決めた。

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| 1 | MAUI 本体 10.0.70 が現行 (10.0.20、maui/ADR-0004) と衝突 | 採用 (Major) | verification-ci spec「ツールチェーンの再現性」を workload 同梱版に修正。cross/ADR-0018 と maui/ADR-0004 の衝突は design「ADR 候補」に申し送り (蒸留時に ADR-0018 を amends)、proposal Impact に記載 |
| 2 | lint 8 検査目は cross/ADR-0021 の改訂を伴うのに ADR 候補なし | 採用 (Major) | design「ADR 候補」に追加。amends ドラフトの起票はオーナー判断 (ksn-propose Step 2 の衝突時の手順) |
| 3 | smoke / artifact / フィード準備の契約が相互に矛盾 | 採用 (Major) | verification-ci spec の Requirement にモード表を明記、Scenario「smoke はフィード準備を行わない」を追加、artifact Scenario に KMP の但し書き |
| 4 | smoke 正ケースが本変更で受け入れ確認できない | 採用 (Major) | consumer-verification spec から Scenario「公開レジストリからの解決」を削除し、Requirement に phase-9 で立てる旨を明記。「同じ version」Scenario は生成物と dry-run の解決結果で判定する形に修正。tasks 5.3 / 5.4 追随 |
| 5 | 追跡する KMP の smoke 形が解決不能な version を指す | 採用 (Major) | design Decision 4 を「構造確認用の非解決 fixture、ビルド前の再生成が必須、dry-run 生成物の URL 書き換えで作る」に修正。tasks 2.5 に README 明記を追加 |
| 6 | 新しい Gradle root の Android SDK 解決が未設計 | 採用 (Major) | consumer-verification spec「消費者プロジェクトの構成」に SDK 引き継ぎの契約と Scenario を追加。tasks 2.0 (`verification/lib/android-sdk.sh`)、4.4 (handbook の build root 一覧)、5.2 (h) |
| 7 | KMP 固有検査に負の確認がない | 採用 (Major) | spec「KMP 消費者の依存検査」に自己テストの要件と Scenario を追加。tasks 3.4 (`--selftest`)、5.2 (i) |
| 8 | develop push の Scenario が矛盾 | 採用 (Minor) | Scenario の GIVEN を「ビルド・テストの入力を含む commit」に限定 |
| 9 | artifact の展開レイアウトが未規定 | 採用 (Minor) | design Decision 3 に形態ごとの配置表を追加。spec「分離」から参照、tasks 5.6 で 4 形態の upload / download を確認 |

確定 0 / 採用 9 / 降格 0 / 未解決 0。判定 NEEDS_DISCUSSION の論点のうち #2 (ADR-0021 の改訂) と #1 の長命層の衝突はオーナーに提示する。
