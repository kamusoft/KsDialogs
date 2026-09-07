# セカンドオピニオン: add-maui-ios-bridge-verification (code-001)
**相方**: codex / **label**: so-code-add-maui-ios-bridge-verification / **日付**: 2026-09-02 / **対象**: HEAD (8514b98) に対する未コミット差分 — maui/macios/native/KsDialogsMauiBridge.xcodeproj / KsDialogsMauiBridgeTestHost / KsDialogsMauiBridgeTests / maui/macios/KsDialogs.Binding.iOS.csproj / samples/maui/KsDialogs.Sample.Maui.csproj / scripts/scenario-id-coverage.py / changes 配下の verification・deviation・tasks
---
判定は **CHANGES_REQUESTED** です。Critical 0件、Major 1件、Minor 2件です。

提示されたテスト結果はすべて成功していますが、BV-MA-05 の SHALL と実測結果が一致していません。

## 指摘事項

### 🟠 Major: BV-MA-05 が変更なしビルドのたびには成立しない

**該当箇所**: `kasane/changes/add-maui-ios-bridge-verification/verification/incremental-build.md:98`

**問題点**: 仕様は、bridge と Sample に変更がないインクリメンタルビルドで、資源パッケージ再生成とネイティブリンクの両方がスキップされることを要求しています。

しかし実測記録では、`_CreateBindingResourcePackage` の stamp が IncrementalClean に削除されるため、変更がなくても資源パッケージが1ビルドおきに再生成されると明記されています。したがって、`specs/maui-binding/spec.md:40` の BV-MA-05 と `tasks.md:29` の完了チェックは、現在の証拠では一般には成立しません。

ネイティブリンクが継続してスキップされる点と、今回の入力補正が周期的再生成を新たに持ち込んでいない点は確認できますが、これは仕様の「資源パッケージ再生成もスキップ」とは別の主張です。

**推奨修正**: `_CreateBindingResourcePackage` がスキップされたビルドでも stamp が IncrementalClean の追跡対象として維持されるよう、Binding 側の MSBuild 配線を補正してください。その後、変更なしの連続ビルドを最低3回実施し、すべてで `_CreateBindingResourcePackage` と `_LinkNativeExecutable` がスキップされることを記録してから tasks を完了扱いにしてください。

### 🟡 Minor: Overlay の瞬間的な取り付けを検出できない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestOverlayObserver.swift:18`

**問題点**: `added` は現在の `window.subviews` と開始時点の差分だけです。そのため、Loading/Toast の器が一度取り付けられた後、アサーション前に撤去される退行は検出できません。BV-MA-02/03/07 は「表示されない」ことを要求していますが、現在のテストが証明するのは観測時点で残っていないことまでです。

**推奨修正**: テスト用 `UIWindow` で `didAddSubview` を記録するなど、観測開始後に一度でも追加された View の履歴を保持し、失敗対象の器が取り付けられなかったことを検証してください。

### 🟡 Minor: 共有ホスト状態の後片付け失敗がテスト成功のまま見逃される

**該当箇所**:

- `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgePresentationHostTests.swift:49`
- `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:125`

**問題点**: Dialog の撤去待ちと Toast の全撤去待ちの戻り値を捨てています。テスト群は同じ key window とプロセス状態を共有して直列実行されるため、後片付けが失敗しても当該テストが成功し、後続テストだけが順序依存で失敗する可能性があります。

**推奨修正**: 待機結果を `#require` で検証し、可能なら `defer` 相当の teardown で途中アサーション失敗時にも共有状態を片付けてください。

## Scenario 対応表

| Scenario | 実装 | テスト | 実測記録 | 判定 |
|---|---|---|---|---|
| BV-MA-01 | `MauiDialogViewModel.makeContentView` の nil→`contentUnavailable` | `MauiBridgeContentSupplyTests.swift:21` | `verification/bridge-test-target.md:36`、検出力 `:61` | 適合 |
| BV-MA-02 | `MauiLoadingViewModel.makeContentView` と Loading 完了通知 | `MauiBridgeContentSupplyTests.swift:43` | `verification/bridge-test-target.md:37`、検出力 `:62` | 適合。Overlay 観測に Minor |
| BV-MA-03 | `MauiToastViewModel.makeContentView` と Toast の破棄経路 | `MauiBridgeContentSupplyTests.swift:94` | `verification/bridge-test-target.md:39`、検出力 `:64` | 適合。Overlay 観測に Minor |
| BV-MA-04 | Binding `csproj:111` と Sample `csproj:71` の入力補正 | 自動テスト除外 | `verification/incremental-build.md:73`、A/B `:105` | 適合 |
| BV-MA-05 | BV-MA-04 と同じ入力補正 | 自動テスト除外 | `verification/incremental-build.md:88` | **不適合**。資源パッケージが1ビルドおきに再生成 |
| BV-MA-06 | `project.pbxproj:120` のホスト、`:138` のテスト標的、scheme `:32` | 6 tests / 3 suites | `verification/bridge-test-target.md:6` | 適合 |
| BV-MA-07 | Loading スコープ形の失敗経路 | `MauiBridgeContentSupplyTests.swift:64` | `verification/bridge-test-target.md:38`、検出力 `:63` | 適合。Overlay 観測に Minor |

## 照合した規約

- `ksn-review` の仕様充足・堅牢性・テスト・設計品質チェックリスト
- `ksn-core` のデルタスペック凍結、deviation、付随修正、リポジトリ相対パス規約
- `handbook/cross/comment-policy.md` — 新規コメントに違反なし
- `handbook/cross/test-execution.md` — Swift Testing の件数、全件実行、Scenario ID 網羅を照合
- `handbook/cross/sample-parity.md` — Sample は facade への ProjectReference 1本を維持
- `handbook/cross/runtime-behavior-verification.md` — Simulator 上の提示先プローブとビルド成果物のA/Bを照合
- `maui/ADR-0003` — 標準 `XcodeProject`、静的 framework、SDK内部ターゲット依存
- `core/ADR-0033` — nil を `contentUnavailable` に変換する3面の失敗境界
- `cross/ADR-0006` — Sample の consumer 境界
- `lessons/process.md` L-001 — 姉妹面照合と修正前失敗による検出力確認
- `maui-native-binding-skill` — 静的 framework、NativeReference、インクリメンタル入力の観点

テストは依頼どおり再実行しておらず、提示された成功結果と記録をレビュー根拠にしています。


## 突き合わせ結果 (2026-09-02、ホスト側 review-001.md との照合)

| # | 相方の指摘 | 採否 | 根拠・反映先 |
|---|---|---|---|
| 1 | [Major] BV-MA-05 の資源パッケージ再生成が変更なしビルドのたびには成立しない | **確定** (双方一致。重要度は相方の Major を採る) | ホスト側も Minor で同じ食い違いを指摘 (review-001.md)。処方が割れる — ホスト: deviation.md への記録 (SDK 既定挙動で趣旨は満たす) / 相方: Binding 側配線で stamp を守り毎回スキップにする。spec の SHALL の扱いを決める判断のためオーナーへ (ksn-orchestrator NEEDS_DISCUSSION 扱い) |
| 2 | [Minor] Overlay の瞬間的な取り付け (取り付け後に撤去) を検出できない | **採用** (相方のみ・根拠強) | 該当箇所 (`BridgeTestOverlayObserver.swift`) と退行シナリオが具体的。ホスト側の BV-MA-03 settle 待ち指摘と同じ「表示されない」の検出力の論点。テスト用 window で追加履歴を保持する形へ修正 |
| 3 | [Minor] 共有ホスト状態の後片付け待ちの戻り値を捨てている | **採用** (相方のみ・根拠強) | 直列実行される suite 間で順序依存の失敗に化ける実害シナリオあり。`#require` で検証する形へ修正 |

確定: 1 / 採用: 2 / 降格: 0 / 未解決: 0 (#1 の処方はオーナー判断待ち)。
