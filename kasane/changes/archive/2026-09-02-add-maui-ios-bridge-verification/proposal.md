# Proposal: add-maui-ios-bridge-verification

## Why

MAUI iOS の互換面 (bridge: `maui/macios/native/KsDialogsMauiBridge/`) を直したあとの「確かめ方」が二重に薄い。

1. **直した bridge が Sample に載らない**: .NET for iOS SDK の再ビルド判定は、binding の資源パッケージ再生成 (`_CreateBindingResourcePackage`) の入力にも、アプリのネイティブリンク (`_LinkNativeExecutable`) の入力にも、xcframework の中身 (静的 framework の実バイナリ) を含めていない。bridge の Swift だけを直すと xcframework は作り直されるのに、Sample の `obj/.../nativelibraries/` には修正前の実行ファイルが残り、`dotnet build` が成功と報告しながら古いバイナリの .app を配備する。add-toast の失敗系実機観測ではこれがライブラリ欠陥の誤診を招きかけた (`kasane/changes/archive/2026-08-28-add-toast/ui/verification/sample-walkthrough.md` の「原因 (古いネイティブリンク成果物)」節)。上流に既知 issue は見つからず (探索時の SDK targets 読解、exploration.md 参照)、修正を待てる類ではない
2. **bridge に自動テスト標的が無い**: C# と iOS Native を繋ぐ結び目 — MAUI 側が中身を作れず nil を返したときに互換面が `contentUnavailable` の失敗として呼び出し側へ届ける経路 (core/ADR-0033 の iOS 側) — は Sample の実機観測でしか検証されていない。MAUI Android 側には同役の JVM テスト (`maui/android/native/ksdialogs-maui-bridge/src/test/.../MauiToastContentSupplyTests.kt` 他) があり、iOS 側だけが射程外

どちらも「bridge を直したあと、ソースが正しいことの検証 (テスト・レビュー) では検出できない」種類の欠陥で、xcodeproj とビルド成果物に触る作業が重なるため 1 change に束ねる。

## What Changes

- **Binding のビルド連携 (maui/macios/KsDialogs.Binding.iOS)**: xcframework の中の実バイナリ (`_BuildXcodeProjects` の出力) を、資源パッケージ再生成の入力に足す。既存の `_AdjustKsBridgeXcodeProjectInputs` と同じ流儀 (SDK の公開されたターゲット名に AfterTargets でぶら下がり、入力 item を補正する)。「変わったときだけ」再生成し、毎回の強制再生成にはしない
- **Sample のビルド連携 (samples/maui/KsDialogs.Sample.Maui)**: bridge の静的 framework の実バイナリをネイティブリンクの入力 (`_LinkNativeExecutableInputs`) に足し、bridge が変わったときだけ再リンクさせる。モノレポ開発 (ProjectReference) のための手当てであることをコメントで明示する (NuGet 利用者は dll 更新で追随するため不要)
- **bridge のテスト標的 (maui/macios/native/KsDialogsMauiBridge.xcodeproj)**: Simulator で走るユニットテスト標的と、その Host Application となる最小のテスト用アプリ target を新設し、scheme の TestAction に結線する。ホストアプリを付けるのは、ホストなしの Unit Testing Bundle ではシーンが前面アクティブにならず、bridge が使う実物の提示先解決が成立しないため (`verification/presentation-host-probe.md`)。実行方式は ios/ と同じ Swift Testing + `xcodebuild test`。最初の 1 本として Dialog / Loading / Toast の 3 面それぞれで「中身なしの供給 → 互換面の失敗 → 呼び出し側への届き方」を固定する。観測点は MAUI Android 側の同役テストと揃える
- **検証記録**: SDK 内部ターゲットへの依存箇所 (どの item をどの target の後で補正したか・タイムスタンプで「変わったときだけ」になることの実測) と、テスト実行体で提示先が得られたことの実測を `verification/` に残す (maui/ADR-0003 と同じく、SDK が変わったときの見直し材料)

影響する能力: maui-binding (bridge のビルド連携とテスト。Sample 側の手当ては binding の消費者側にある同じ問題の半分なので、samples ではなくここに含める)

**蒸留への申し送り (実装タスクには含めない — handbook は規範層のため蒸留で更新する)**:
- handbook/cross/test-execution の件数表に bridge テスト標的の実行コマンドと件数を 1 行足す
- handbook/cross/runtime-behavior-verification に「bridge 更新後の Sample は .app のシンボルで入れ替わりを確かめられる」を検出手段として 1 行足す
- maui/ADR-0003 の Consequences「SDK 内部ターゲットへの依存を抱える」に本変更の 2 か所を現行照合の補足として載せる

## Non-Goals

- **NuGet 経由の利用者向けの再ビルド対策** — 不要。パッケージ更新で binding の dll が変わるため SDK の判定が追随する。本事象はモノレポの ProjectReference でだけ起きる
- **MAUI Android 側 (maui/android) の同種対策** — 別の能力・別の接続方式 (gradlew Exec + 自前の入力リスト、maui/ADR-0003)。同症状は報告されておらず、起きたら別 change
- **bridge の他の経路のテスト** (`unsupportedResult`・提示先不在 (`presentationHostUnavailable`) の通知・閉鎖 handle・Loading の進捗中継・Toast の style 反映など) — 標的の新設と最初の 1 本が本 change の範囲。残りは標的ができたあと、結び目ごとに別 change で積む
- **SDK 上流 (dotnet/macios) への issue 起票** — 実装作業ではない。実測記録が揃ったらオーナー判断で別途
- **bridge への提示面の注入口 (テスト専用 init) の追加** — 却下 (2026-09-02)。提示先はテスト用ホストアプリで実物の経路のまま確保する方針にしたため不要。bridge にテスト都合の init が入り、偽の提示面 (iOS Native の内部型) のために bridge テストがモジュール境界を越える点も採らない理由
- **提示先の解決規則 (前面アクティブなシーンの key window に限定) の緩和** — 却下。製品挙動であり、限定は意図的な決定

## Impact

- **破壊的変更なし**: 公開 API (C# facade・ObjC 互換面・Native) には触れない。変更はビルド配線とテスト基盤のみで、外すのも容易
- **SDK 内部ターゲットへの依存が 2 か所増える** (Binding 側の資源パッケージ入力・Sample 側のリンク入力)。maui/ADR-0003 の Consequences の範囲内の増分。SDK 更新で target / item 名が変わったときの見直し材料として実測記録を残す
- **Sample の iOS ビルド時間**: 入力補正を「変わったときだけ」にしないと毎回リンクが走り遅くなる。実装で bridge 未変更時のインクリメンタルビルドが従来どおりスキップされることを実測で確認する
- **テスト実行体での提示先の確保**: 結び目は「提示先 (前面アクティブなシーンの key window) が確保できた後」に通る経路。bridge は公開 init (`Dialog()` 等) で実物の提示先解決 (`ApplicationKeyWindowProvider`) を使う。提案段階のプローブ (`verification/presentation-host-probe.md`) で、**ホストアプリなしの Unit Testing Bundle では window を key にしてもシーンが前面アクティブにならず提示先不在になる**ことを実測した (ios/ の既存テストは偽の提示面を内部 init で注入しており先例にならない)。そのためテスト標的には Host Application (最小のテスト用アプリ target) を付け、実アプリのプロセス内でテストを走らせる。ホスト付きで前面アクティブなシーンと key window が得られることは Xcode の標準構成として成立見込みが高いが未実測のため、tasks 1.2 で実装の先頭に再確認を置く。それでも得られなければ実装を止めてユーザーに諮る (deviation.md に記録)
- **xcodeproj の手編集**: pbxproj にテスト標的とテスト用ホストアプリ target を追加する。ホストアプリは binding の xcframework 生成 (`_BuildXcodeProjects` が呼ぶ scheme) に混ざらないよう、framework の scheme のビルド対象に入れない。Xcode で生成した構造に合わせ、ビルドが通ることと Swift パッケージ (ios/) の依存がテスト標的から解決できることを確認する

## 級: M

公開 API に触れず可逆だが、ビルド連携とテスト基盤の 2 能力にまたがり、どちらも実測を伴う工程を含む。独立レビュー必須。

domain: maui
