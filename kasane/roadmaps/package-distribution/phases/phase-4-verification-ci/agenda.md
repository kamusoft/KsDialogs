# 検証 CI (verification-ci)

4 形態のビルド・テスト + lint を GitHub Actions で回す検証 CI を、KsSettingsView の reusable workflow 構成とブランチ役割別トリガーを踏襲して整備する change フェーズ。

## 論点

- KMP の検証 job の形: `kmp/` の Android ターゲットのテスト (JVM / androidHostTest) に加えて iOS ターゲットのテストを Simulator で回すか。KMP → Android Native は composite build、KMP → Swift は `localSwiftPackage` のため macOS ランナーが要る
- MAUI job の Xcode / .NET for iOS workload の版整合 (library-foundation phase-6 からの申し送り: .NET for iOS 26.1.10502 が Xcode 26.1 を要求し、手元の Xcode 26.5 でビルド不可だった)。CI で使う Xcode 版と workload 版をここで固定する
- 実行件数の下限 (0 件 = fail) を 4 形態で何件にするか、テスト件数は着手時の実測で決める
- トリガーはブランチモデル (phase-3 の結論) に従う。`main` 1 本なら「push で本体検証、PR で消費者検証」の読み替え
- KMP job に階層化 source set の metadata compile を含める (fix-kmp-iosmain-throws-metadata からの申し送り、2026-09-05)。詳細は次項

### KMP job と metadata compile (申し送り)

target 本体の compile と `allTests` だけでは `compileIosMainKotlinMetadata` の失敗を見落とす。Kotlin 2.4.10 の既知バグ KT-88548 で実際に起き、rollout-user-docs のレビューが Sample consumer 側の metadata compile で発見した。library 側の `compileIosMainKotlinMetadata` を直接載せるか、消費者検証 (phase-8) の `:shared:compileCommonMainKotlinMetadata` に含めるかをここで決める。あわせて Kotlin 2.5.0 以降へ上げるときは iosMain の override に `@Throws` を書き戻せる (kmp/ADR-0001 の現行照合 2026-09-05)。 撤回時は concept kmp/api/ios-host-integration.md の注意点 (`KsDialogs` / `KsLoading` / `KsToast` の override 全般、generalize-kmp-iosmain-throws-note で一般化) と KMP Skill en / ja の同節も docs-refresh で追随させる。

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView cross/ADR-0025 (reusable workflow + 入口)・cross/ADR-0026 (CI の保証範囲)・cross/ADR-0028 (トリガーをブランチの役割で分ける) と同 phase-3 / phase-13 の決定事項。

- platform 別 reusable workflow (`workflow_call`) + 入口 workflow 1 本。release も同じ workflow を呼ぶ。変更パスによる job の絞り込みはしない (必須 check の素通り経路になる)。status check 名は「呼び出し側 job 名 / 呼ばれた側 job 名」で両側を固定
- 緑の意味は「ロジック全件通過 + native への配線がコンパイルできる」まで。実行ホスト起動 (E2E) は CI に載せない。iOS はホスト上の `swift test` を成否判定に使わず Simulator で実行。テストが 1 件も実行されなければ失敗
- lint job に gitleaks + local-path-lint + identity-lint + comment-policy-lint (KsDialogs の `scripts/` 既存分) を載せる
- トリガー整理は最初から織り込む: 開発ブランチへの push は lint + 本体検証、リリース対象ブランチ宛て PR はそれに消費者検証 (phase-8) を足す。入口は 1 本のまま job に `if: github.event_name == 'pull_request'`。concurrency の group を `github.ref` にして cancel-in-progress
- JDK は Temurin 17 を `setup-java`、キャッシュは依存のみ、`macos-26` + Xcode 版を `env` で固定 (版は着手時に実測)
- 却下済み: 入口に手順直書き / paths 絞り込み / platform ごとの独立入口 / E2E を CI に載せる / 消費者検証を開発ブランチで毎回 (理由は cross/ADR-0025・0026・0028 の Alternatives)

## TODO

- [ ] 論点の解消 (KMP job の形と metadata compile の載せ方・MAUI の Xcode / workload 整合・実行件数の下限)
- [ ] ksn-propose で変更提案を起こす
