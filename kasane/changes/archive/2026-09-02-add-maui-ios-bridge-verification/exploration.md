# Exploration: add-maui-ios-bridge-verification

2026-09-02 に簡易起票 2 件 (`fix-maui-ios-stale-native-link` / `add-maui-ios-bridge-nil-supply-test`) を統合。どちらも「MAUI iOS bridge (`maui/macios/native/KsDialogsMauiBridge/`) を直したあとの確かめ方が薄い」という同じ根を持ち、xcodeproj・ビルド成果物に触る作業が重なるため 1 change に束ねた。

## 課題 / 動機

### 1. bridge 更新がインクリメンタルビルドで Sample iOS に反映されない (旧 fix-maui-ios-stale-native-link)

.NET for iOS のネイティブリンク工程が、参照する xcframework (KsDialogsMauiBridge) の更新をインクリメンタルビルドで検知しない。bridge の Swift ソースだけを直しても binding の C# アセンブリ (`KsDialogs.Binding.iOS.dll`) が変わらないため、Sample アプリの `obj/Debug/net10.0-ios/<rid>/nativelibraries/` に**修正前のネイティブ実行ファイルが残り続け**、`dotnet build` が成功と報告しながら古いバイナリの .app を配備する。

実害の実例: add-toast の失敗系実機観測 (tasks 8.5) で、修正済みソースから「ビルド成功」した Sample が修正前バイナリのまま動き、MAUI iOS の Loading / Dialog factory 例外で SIGSEGV を再現 → ライブラリ欠陥と誤診しかけた (実際はコード正常・成果物が古いだけ。`.app` 実行ファイルの mangled シンボル `…CyF` (throws なし = 修正前) と faulting アドレスの逆アセンブルで特定)。bridge を直すたびに再発しうり、「ソースが正しいことの検証 (テスト・レビュー) では検出できない」種類の欠陥。

発見の文脈: add-toast 修正サイクル3周目のクラッシュ診断 (`kasane/changes/archive/2026-08-28-add-toast/ui/verification/sample-walkthrough.md` の「失敗系の実機観測 (A 方式・2026-08-28)」節と追試節。当面の手順として「bridge の Swift だけを直したときは Sample の iOS ビルド成果物を捨ててからビルドする / 入れ替わりは .app のシンボルで確かめる」を同ノートに記録済み)。

### 2. bridge に自動テスト標的が無い (旧 add-maui-ios-bridge-nil-supply-test)

MAUI iOS の互換面には XCTest のテスト標的が無く、C# と iOS native を繋ぐ結び目 — `MauiLoadingViewModel.makeContentView()` / `MauiDialogViewModel.makeContentView()` / `MauiToastViewModel.makeContentView()` が nil (C# 側が中身を作れなかった — core/ADR-0033) を受けて `MauiDialogBridgeError.contentUnavailable` を投げ、それが完了 / 閉鎖の通知として MAUI 側へ届く経路 — が自動テストの射程外にある。

add-toast の factory 例外境界修正 (core/ADR-0033) では、境界の両端 (C# 側 `BridgeContentSupplyTests` が失敗を値に変えること・iOS native 側で factory 例外が show / start の失敗になること) は自動テストで固定できたが、両者を繋ぐこの結び目は Sample の実機観測 (失敗系 A/B) でしか検証されていない。

出典: add-toast の review-003 (Suggestion — 「将来 bridge にテスト標的を作るときの最初の 1 本として起票しておく価値がある」)。bridge にテスト標的が無いこと自体は add-toast 以前からの構造で、新規の負債ではない。MAUI Android 側は `MauiToastContentSupplyTests` (JVM) が同役を既に持つため、iOS 側だけが対象。

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: 課題 1 (stale link) の扱い — 2026-09-02 探索

前提となる調査結果 (ksn-scout、.NET for iOS SDK パック `Microsoft.iOS.Sdk.net10.0_26.5/26.5.10284` の targets を直接読解):

- 上流 (dotnet/macios・CommunityToolkit/Maui.NativeLibraryInterop) に本事象を主題にした既知 issue は見つからなかった (語句検索ベースのため網羅性は中〜低)。**上流修正を待つ選択肢は取れない**
- 原因は SDK の再ビルド判定がそう作られていること。二段構え:
  1. **Binding 側**: `tools/msbuild/Xamarin.Shared.targets` の `_CreateBindingResourcePackage` (Inputs に `_XCFrameworkNativeReference` が含まれない。SDK 自身が「xcframework はディレクトリなので Inputs に使えない」とコメント) → xcframework の中身が変わっても binding の資源パッケージ (`.resources`) が再生成されない。binding プロジェクト内では `ResolveNativeReferences` が `IsBindingProject != true` 条件で走らず、xcframework が実バイナリへ展開されない
  2. **アプリ側**: `targets/Xamarin.Shared.Sdk.targets` の `_LinkNativeExecutableInputs` に `_FrameworkNativeReference` (静的 framework) が含まれない → `_LinkNativeExecutable` が up-to-date でスキップ。`NativeLink.cache` の Hash はパス文字列のみで内容を見ない。古い `nativelibraries/<AppName>` が `PreserveNewest` で .app へコピーされる
- Slim binding (`NoBindingEmbedding=true`) では管理 dll が変わらないため、資源 unpack 側の stamp 判定 (`ReferencePath` の mtime) も追随しない
- NuGet 経由の利用者はパッケージ更新で dll が変わるため本事象は起きない。**モノレポの Sample (ProjectReference) だけで起きる問題**

| 案 | 評価 | 結論 |
|---|---|---|
| (a) 規約化のみ (handbook に「bridge 更新時は Sample の iOS 成果物を破棄」を書き、change から外す) | 再発防止の確実性が低い。前回は誤診寸前まで行った | 却下 (補助として handbook に一言添えるのは採用) |
| (b) 機械化 — Binding 側 (資源パッケージ再生成の入力に xcframework 内の実バイナリを足す) と Sample 側 (リンク入力に足す、または古い実行ファイルを落とす) の 2 か所を MSBuild で手当て | 確実性が高く上流依存なし。SDK の公開されたターゲット名に 2 か所ぶら下がる (maui/ADR-0003 の Consequences の範囲内の増分)。毎回リンクし直す設計にすると Sample のビルドが遅くなるため「変わったときだけ」の判定にする必要あり | **採用** |
| (c) 検出型 — 撮影・検証手順に .app のシンボル確認を組み込む | 起きた後に気づけるが防げない | 主役にはしない。(b) が効いていることの確認手段として handbook/cross/runtime-behavior-verification に一行残す |
| Sample 側のみ機械化 (Binding 側は触らない) | 二段のうち片方しか塞がず、資源パッケージが古いままだと Sample 側の手当てが空振りしうる | 却下 |

### 論点 2: 課題 2 (テスト標的) の置き場と最初の 1 本の範囲

| 候補 | 結び目に届くか | 結論 |
|---|---|---|
| bridge の xcodeproj (`KsDialogsMauiBridge.xcodeproj`) に Simulator で走るテスト標的を追加 (Swift Testing、ios/ と同じ `xcodebuild test` 方式) | 届く | **採用** |
| ios/ の Swift パッケージのテストに置く | 届かない (パッケージから bridge を逆参照できない — 依存が逆) | 却下 |
| C# 側 (`dotnet test`) から | 届かない (MAUI ユニットテストは素の net10.0 で、ネイティブに届かない — maui/ADR-0002) | 却下 |
| Sample の実機観測のまま | 届くが自動化されない (起票理由そのもの) | 却下 |

最初の 1 本の範囲: 3 面すべて (標的を作るコストは同じなので結び目 3 か所を一度に固定) を採用。2 面 (Dialog / Loading のみ)・1 面 (Dialog のみ) は却下。

観測点は Android 側の同役テスト (`maui/android/native/ksdialogs-maui-bridge/src/test/.../MauiToastContentSupplyTests.kt` 他) と揃える:

| 面 | 観測点 |
|---|---|
| Dialog | 閉鎖の通知に失敗 (エラー) としてちょうど 1 回届く |
| Loading | 完了の通知に失敗の理由が載って届き、表示は起きない |
| Toast | 受理は失敗せず、その 1 枚だけが破棄されて他の表示に影響しない |

## 決定事項

1. **課題 1 は機械化 (b) でこの change に残す** — Binding 側 (`maui/macios/KsDialogs.Binding.iOS.csproj`) と Sample 側 (`samples/maui/KsDialogs.Sample.Maui`) の 2 か所で、bridge の xcframework 更新を再ビルド判定に追随させる。「変わったときだけ」再生成・再リンクする形にし、毎回の強制リンクにはしない。handbook に補助の規約 (bridge 更新後の成果物確認) と検出手段 (.app のシンボル確認) を一言ずつ添える
2. **課題 2 は bridge xcodeproj にテスト標的を新設し、3 面の nil 供給経路を最初の 1 本として固定する** — 実行方式は ios/ と同じ Simulator 上の `xcodebuild test`。handbook/cross/test-execution の件数表に 1 行追加する
3. **変更級は M** — 2 能力にまたがり、どちらも実測を要する工程を含むため。SDK 内部ターゲットへの依存箇所は design に実測記録を残す (maui/ADR-0003 と同じ扱い)

4. **(提案段階で追加 2026-09-02) bridge テストの提示先はテスト用ホストアプリで確保する** — ホストなしの Unit Testing Bundle では実物の提示先解決が成立しないことを提案段階のプローブで実測 (`verification/presentation-host-probe.md`)。bridge への注入口 (テスト専用 init) と解決規則の緩和は却下。経緯は `second-opinion-spec-001.md` の突き合わせ結果 #2

## ADR 候補 (作成済み: なし / 未起票: なし)

- 決定 1 は maui/ADR-0003 (iOS は標準 XcodeProject アイテムで接続) の Consequences「SDK 内部ターゲットへの依存を抱える」の範囲内の補強。単独 ADR は起こさず、蒸留時に ADR-0003 へ現行照合の補足として載せる
- 決定 2・3 はテスト構成・変更級の判断で ADR の選別基準に該当しない

## 未決の論点

実装前 (提案の design 段階) に実測で確かめること:

1. **再リンク判定の掛け所**: Binding 側は `_CreateBindingResourcePackage` の Inputs に xcframework 内の実バイナリ (`_BuildXcodeProjects` の出力) を足せるか。Sample 側は `_LinkNativeExecutableInputs` に静的 framework の実バイナリを足す形か、古い `nativelibraries/<AppName>` を条件付きで落とす形か。どちらも「変わったときだけ」になることをタイムスタンプで確認する
2. **Sample 側の手当ての置き場**: Sample の csproj に直接書くか、`samples/maui/` の Directory.Build.targets に置くか。Sample は「利用者と同じ側から公開 product を参照する」(cross/ADR-0006) ため、手当てはモノレポ開発用であることが読み取れる形にする
3. ~~テスト実行体で提示先 (key window) が得られるか~~ → 提案段階で解消。ホストなしでは得られない (実測)。ios/ のテストは偽の提示面を注入しており先例ではなかった。テスト用ホストアプリで確保する (決定事項 4)
4. **未確認の残り**: SDK の `ResolveNativeReferences` / `CreateBindingResourcePackage` タスク本体 (C#) が内部で内容比較をしている可能性は targets の読解では否定できていない。実測で外れたらここを疑う

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: M (確定 2026-09-02)

理由: 公開 API に触れず可逆だが、ビルド連携とテスト基盤の 2 能力にまたがり、どちらも実測を伴う。SDK 内部依存の実測記録を design に残す必要がある。独立レビュー必須。
