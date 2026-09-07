# phase-3-harness-setup 議論履歴

## 2026-08-14: core concepts 2件の書き下ろしとフェーズまとめ

決定「core concepts の書き下ろし範囲は契約意味論2件」に基づき、ksn-scout による移植元調査 (artifacts/scout-origin-notification-multidisplay.md) で裏取りした上で core/api/ に書き下ろした。

- result-notification-semantics.md: ちょうど1回の completed/cancelled・型束縛・キャンセル3経路の写像。原典の粗 (object キャスト・cancel=default・二重通知無防御) を契約で解消。同一ハンドル再入 (原典は OS 不統一) は仕様化保留として phase-4 送り
- multi-display-semantics.md: OS 委譲の4点契約 + 保証しないこと (非 LIFO dismiss・アニメーション中タイミング・Toast/Loading との前後関係) を明文化。scout が見つけた iOS の非 LIFO 落とし穴 (提示元 VC の動的解決) を共通仕様テストの検証項目候補に落とし込み
- scout の収穫: README とコードの食い違い4件 (cancel は null でなく default(TResult)、IReusableLoading.Hide の戻り型、Toast Obsolete 未記載、多段表示が完全に未文書) — 「README 一次 + コード補完、食い違いはコードが正」(aiforms-origin-reference) の序列の妥当性を裏付けた

## 2026-08-14: core concepts 2件の平易化と初見レビュー (オーナー指摘起点)

オーナーから「意味論・写像が初見で意味不明。初見レビューを通したか」の指摘。通していなかったため2段で是正した。

- 第1段 (平易化): タイトルを「〜の意味論」→「結果通知のルール (show が返すもの)」「多段表示のルール (ダイアログの重ね出し)」に変更。「写像」「型束縛」「一級市民」「観察可能な意味論」等を平易な表現に書き換え
- 第2段 (初見レビュー): 新鮮な文脈のエージェントによる初見レビューで指摘15件 (判定: 直せば通用する)。主要指摘 — 「原典」が無定義かつ他文書の「移植元」と用語が割れている / 「共通仕様テスト」「縦串」が無定義の内輪語 / ADR が proposed なのに「この文書が正」と言い切る矛盾 / KMP が表に明示されていない / 結果を報告する部品の KsDialogs 側の名前が未定と明示されていない / テスト項目候補は読者向けでなく執筆者向け — をすべて反映。用語は「移植元」に統一して初出で定義+リンク、proposed の間は「暫定の正」と明記、テスト項目候補は phase-4 agenda へ移動
- オーナー追加指摘: 移植元の bool 系 ShowAsync / 任意型 ShowResultAsync の2系統に対する completed(結果) の割り当て (統合か併存か) が未確定 →「まだ決めていないこと」に追加
- 教訓: 長命層の文書はオーナー提示前に初見レビューを通す — ksn-lesson で捕捉

### フェーズまとめ

論点7件 + 確認作業2件をすべて解消。成果物: concepts 5件 (cross/conventions 3件 + core/api 2件) 新規、rules.md 確定、config.yaml 確定、ADR 昇格2件 (cross/0001・0003)、artifacts 1件、phase-4 への申し送り2件 (ui.screenshot 実測・sample-parity は既存論点で継承)。「実物のない規約は書かない」に従い、レイアウト規則 (phase-4/5)・ui.screenshot (phase-4)・Loading/Toast 意味論 (phase-7/8) を先送りした。

## 2026-08-14: ドメイン導出規則の確定 (ビルドルート基準)

phase-2 で ios/ / android/ / kmp/ / maui/ の4ビルドルートが実体化した (cross/ADR-0004) のを受け、concepts/rules.md のドメイン導出規則を実物に合わせて確定した。

- 選択肢: A. ビルドルート基準を主軸に明文化 / B. 現行の系統ベース記述のまま確定
- 採用: A。判定が「どのビルドルートのコードか」で機械的に決まり、境界例 (kmp/ の androidMain actual = kmp、maui/ の platform handler = maui) を判定例として明記できるため
- core は「ビルドルートを持たない全形態共有の契約・意味論」、cross は「ビルドルート横断の実物 (バージョンカタログ共有・scripts/・CI) とリポジトリメタ」と位置づけを明確化
- 既存 ADR 10件 (core 7 + cross 3) の配置は変更なし
- ADR は起票しない: rules.md 自体が長命の policy であり規約の正。cross/ADR-0004 (ビルドルート構成) の帰結を配置規則に写しただけで、独立の決定として覆すコスト・境界越え・将来拘束のいずれも rules.md 更新で完結する

## 2026-08-14: config.yaml の追随 (最小反映)

- 選択肢: A. 今確定できる最小限だけ反映し実物待ちは phase-4 送り / B. 想定で先埋め (ui.screenshot に simulator 手順等)
- 採用: A。「実物のない規約は書かない」のフェーズ方針に従う
- 反映内容: core の domain-skills を「契約層でコードを持たないため割り当てなし」で確定 (コメント更新)。domains は論点6 (ドメイン導出規則) で実物整合を確認済みのため変更なし。共通 skills は実物が出るまで空のまま
- 先送り: ui.screenshot は phase-4 で Sample の実物を実測してから記載 — phase-4 agenda の TODO に申し送りを追加
- ADR は起票しない: config.yaml のコメント確定と先送りの整理のみで、独立の決定に当たらない

## 2026-08-14: aiforms-origin-reference の翻案設置

KsSettingsView 版 (cross/conventions/aiforms-origin-reference.md) を出典に、移植元参照の時限規約を cross/conventions/ に設置した。

- 選択肢: A. 構造翻案 + KsDialogs 規約への3点適応 / B. KsSettingsView 版ほぼそのまま / C. reference-repositories.md へ統合
- 採用: A。B はローカルパス直書きが reference-repositories.md のパス集約規約に違反、C は恒久文書 (在り処表) と時限規約が混ざり廃止時の切り分けが難しくなるため却下
- 適応3点: (1) パスは reference-repositories.md の対応表参照に置換 (2) 仕様の正の序列 (README 711行が一次情報源、レイアウト計算等の詳細はコードで補完、食い違いはコードが正) を明記 (3) 適用対象を Dialog / Loading に限定し、Toast (原典 Obsolete → 新実装) を対象外と注記。成り立ちリンクは cross/ADR-0017 相当を cross/ADR-0001 (rebrand-policy) に差し替え
- 時限の終期: Dialog / Loading の移植完了 (phase-7 完了想定)。廃止時は概念を削除し index / log に記録、ksn-drift の重点棚卸し対象
- upstream 実在確認済み: github.com/muak/AiForms.Maui.Dialogs、README 実測711行
- ADR は起票しない: 規約本体が concepts の policy として正であり、成り立ちの決定は既存の cross/ADR-0001 が持つ

## 2026-08-14: runtime-behavior-verification の移植設置

KsSettingsView 版 (cross/conventions/runtime-behavior-verification.md) を出典に、実行時挙動不具合の完了判定規約を cross/conventions/ に設置した。

- 選択肢: A. 骨格そのまま移植 + ダイアログ向け適応3点 / B. 出典事案を省いて規約だけ移植 / C. 自プロジェクトの不具合事案が出るまで先送り
- 採用: A。B は規約の説得力の源 (「なぜ」の実話) が消えて形骸化しやすく、C は「事故が起きてから導入」になり本末転倒 (規約自体が先例の事故から生まれた教訓) のため却下
- 適応3点: (1) 例示をダイアログ向けに差し替え — 表示/dismiss アニメーション・多段表示のタイミング (core/ADR-0006) を先頭に、IME も残す (2) 4ビルドルート (cross/ADR-0004) での「実環境」定義を追加 — 症状が報告された形態の実環境で再現する (Native 単体の再現では binding 層の原因を捕まえられない) (3) 出典事案 (fix-entrycell-ime-composition) の帰属を KsSettingsView と明記して保持
- ADR は起票しない: 検証プロセスの policy であり concepts が正。成り立ちは出典節が持つ

## 2026-08-14: test-execution の実測ベース翻案設置

KsSettingsView 版 (cross/conventions/test-execution.md) の原則を翻案し、コマンドと検証限界を4ビルドルートで実測し直して cross/conventions/ に設置した。

- 選択肢: A. 原則翻案 + コマンド今実測 / B. 原則だけ設置しコマンドは phase-4 送り / C. KsSettingsView 版丸ごと翻案
- 採用: A。C は他リポジトリの実測値の混入で「実物のない規約」になり、B は phase-4 の縦串実装初日からテスト規律を効かせられないため却下
- 実測結果 (BuildProbe 各1件時点):
  - ios: scheme は `KsDialogs`。`xcodebuild test` 成功。**件数行が2系統** — BuildProbe は Swift Testing 製のため XCTest 側は `Executed 0 tests` と表示され、Swift Testing の `Test run with N tests` 行と合算が必要と判明。`swift test` (macOS) も現時点は 1件 pass するが、完了判定は Simulator 実行 (canImport ガードの先例トラップ)
  - android: `./gradlew test` = testDebugUnitTest のみ実行 (release variant の単体テストは現構成では走らない)。件数は TEST-*.xml
  - kmp: **`./gradlew test` は Ambiguous エラーで失敗** (`testAndroid` / `testAndroidHostTest` と衝突)。正は `allTests` (iosSimulatorArm64Test + testAndroidHostTest の2ターゲット)
  - maui: `dotnet test` 成功。件数はコンソール出力
- 環境整備: android/ と kmp/ に local.properties (sdk.dir、KsSettingsView と同じ Xamarin 同梱 SDK を指定、VCS 管理外) を新規作成した — 未作成だと `SDK location not found` でビルド失敗する
- 未実測の先例知見 (Robolectric legacy graphics の描画検証限界等) は書き写さず「先例にある未実測の落とし穴」節の参照ノートに留めた
- ADR は起票しない: 実行手順の policy であり concepts が正

## 2026-08-14: sample-parity 方針の持ち込み確認 (追加成果物なし)

- 選択肢: A. phase-4 agenda への引き継ぎ済み確認のみで閉じる / B. 方針メモを concepts に先行設置
- 採用: A。phase-4 agenda の論点「sample-parity 規約の確定」に翻案元・再定義ポイント・concepts 化タイミングまで記載済みで、先行メモは二重管理かつ「実物のない規約」になるため

## 2026-08-14: core concepts の書き下ろし範囲 (契約意味論2件)

- 選択肢: A. 候補3件すべて phase-3 で書き下ろす / B. 1件も書かず phase-4 のデルタスペックに任せる / C. 契約意味論2件 (結果通知 core/ADR-0003・多段表示 core/ADR-0006) のみ書き、レイアウト規則 (core/ADR-0007) は段階化
- 採用: C。A はレイアウト規則の全量抽出 (移植元約3,900行からの仕様化) が phase-5 級の重作業で research フェーズが重くなりすぎる。B は長命の契約仕様が change 寿命のデルタスペックにしか残らず ADR-0007 の「core 仕様として1本化」と矛盾する
- レイアウト規則の段階化: 縦串に必要な最小は phase-4 の論点「共通仕様テストの器」で、全量は phase-5 (dialog-completion) で実物と共に書く
- 意味論2件は phase-4 縦串の直接入力 (結果通知は縦串の本丸、多段表示は共通仕様テストの器の対象) であり、実物根拠は ADR + 移植元の挙動 (README / コード) で足りる

## 2026-08-14: proposed ADR の昇格レビュー (cross 2件のみ昇格)

proposed 8件 (core/0002〜0007・cross/0001・0003) を「その決定を検証する実物が何か」の軸でレビューした。

- 昇格 (accepted 化): cross/0001 (rebrand-policy)・cross/0003 (knowledge-intake-hybrid)。いずれも実装ではなく運用で検証される方針決定で、0001 は aiforms-origin-reference 規約の根拠として参照・運用中、0003 は comment-policy 先行導入と本フェーズの翻案4件で実践済み
- 維持 (proposed のまま): core/0002〜0007 の6件。実装で検証される決定であり、phase-4 縦串の最優先疎通確認 (ObjC キー同一性 = 0004 の前提、async 変換経路 = 0003) がまさに検証の場。縦串で崩れた場合に proposed のまま改訂できる健全性を優先し、Kasane のパイプライン (実装検証 → 蒸留で昇格) に従う
- 付随: decisions/index.md の cross 件数表記を実態 (5件) に修正

## 2026-08-14: 確認作業: comment-policy の実態突き合わせ

規約 (cross/conventions/comment-policy.md) が参照する機械検査一式と実態の対応を確認した。参照3ファイル (scripts/comment_policy_rules.py・scripts/comment-policy-lint.py・.claude/hooks/comment-policy-check.py) すべて実在、.claude/settings.json に PreToolUse hook (Write|Edit|MultiEdit) 登録済み、lint --selftest 全件 OK。規約の更新は不要。

## 2026-08-14: 確認作業: reference-repositories のパス実在確認

対応表3件 (AiForms.Maui.Dialogs / KsSettingsView / KsAppKMP) のローカルパスすべて実在を確認。KsSettingsView / KsAppKMP は kasane/ 構造の存在も確認した。表の更新は不要。
