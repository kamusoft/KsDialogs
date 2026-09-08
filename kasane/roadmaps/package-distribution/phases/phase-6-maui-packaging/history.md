# phase-6-maui-packaging 議論履歴

## 2026-09-08: 議論開始 — 論点の整理と状態把握

roadmap の phase-6 を in-progress に (phase-5 と並走。roadmap 全体図が phase-5 / 6 / 7 を並行可と定める)。移送されてきた論点 (library-foundation phase-11 のメタデータ・未検証領域・MAUI iOS 環境制約、簡易起票 2 件の `BG8401` と `DialogException` 合流、KsDialogs 固有の MAUI 下限版と最低 OS ガード) を 7 論点に番号付けし直した。状態把握で分かったこと: (1) MAUI iOS の Xcode 不整合は phase-4 の `global.json` 固定 (workload set 10.0.300.3 = .NET for iOS 26.5) で解消済みで、残るのは iOS 面 Sample 通しの未回収のみ。(2) `BG8401` は 4 型に減っており、全て `@JvmStatic` companion で facade は outer 型の static 経由で呼ぶ。(3) README の MAUI 互換表 (10.0.1) と props (10.0.70) が食い違う。(4) facade csproj に `Version` 0.1.0 が直書きされている。(5) icon 素材がリポジトリにない。

## 2026-09-08: 論点 1 — MAUI iOS 環境整合と iOS 面 Sample 通しの回収

環境の不整合は phase-4 で解消済みと確認 (handbook local-development-setup の 2026-09-08 実測)。残る iOS 面 Sample 通しの回収先を A: 本 change の受け入れ条件に同梱 / B: いま scout に通させ結果を artifacts に置く / C: phase-8 に委ねる から A を採用。理由: 固める直前に iOS 面の証跡を残すのが phase の趣旨に合い、不具合の修正も同じ change で扱える。B は受け皿が無く、C は phase-8 の範囲 (起動なし) に入らない。

## 2026-09-08: 論点 2 — `BG8401` (Companion 4 型)

Release ビルドで実測し 4 型 (Dialog / Loading / Toast の Bridge と DialogContent の `.Companion`) と確認。全て `@JvmStatic` 付きで facade は outer 型の static 経由、`Companion` 型は .NET 側で未使用。A: Metadata.xml の `remove-node` で除く / B: Kotlin 側で companion を無くす / C: 警告を受け入れる から A を採用。理由: 公開契約に影響なく Kotlin 側に触れず 8 行で済む。

## 2026-09-08: 論点 3 — View 生成失敗の `DialogException` 合流

実物の確認で、実機では View 生成が受理後の UI スレッドで走り、iOS は `BridgeContentSupply` の預かり口で元例外が返るが Android は `onFailed(message)` 経由で型が落ちると判明 (失敗の素通りは ActivatorUtilities と Android 通知経路の 2 段)。A: 専用型 `ViewCreationFailed` 新設 + InnerException + Android にも預かり口 / B: 既存 `ViewFactoryNotRegistered` に包む / C: 現状維持 から A を採用。理由: 意味が違う失敗を同じ型にしない、診断情報を残す、公開前に公開面を固める phase であること。包む範囲はライブラリ自身の生成経路に限定し、利用者 factory の例外は素通し。

## 2026-09-08: 論点 4 — MAUI 本体の下限版と API 版付き TFM

ksn-scout に裏取りを委譲: API 差分なし (全て .NET 6〜8 世代の API)、10.0.0 で 3 TFM ビルドとテスト 155 件が通る、テンプレート既定は workload 同梱値で現行 workload set では 10.0.20、現状の 10.0.70 では同じ SDK の素の利用者が NU1605。A: 10.0.20 (workload 同梱版) に下限とビルド版を揃える / B: 10.0.70 のまま / C: 10.0.0 を宣言しビルドは 10.0.70 から A を採用。理由: 下限・ビルド版・利用者既定値が一致し CI が下限を常時検証する。API 版付き TFM は KsSettingsView と同じく受け入れ、phase-8 と README へ申し送り。

## 2026-09-08: 論点 5 — 最低 OS 版のビルド時ガード

KsSettingsView の `buildTransitive/` (props + targets、`KSSV0001`) を実物確認。KsDialogs では iOS が「黙って通って端末で落ちる」、Android が「読みにくい merger エラー」になる前提から、A: 両 OS ガード + README / B: 文書のみ / C: iOS だけ から A を採用。診断 ID は `KSDLG0001` (綴りから KsDialogs と読み取れる)。ADR-0004 の「自作 pack MSBuild を足さない」との整理は KsSettingsView と同じ (利用者ビルド資産は対象外)。

## 2026-09-08: 論点 6 — パッケージメタデータの固有値

LICENSE・phase-5 の POM 文言・KsSettingsView の props / csproj・原典 AiForms.Maui.Dialogs の icon (300×300) を確認し、固有値 (URL・Description・Tags・icon の出どころ・Version 既定値と csproj 直書きの廃止) を 1 枚の表で提示、そのまま確定。名乗りと icon の帰属は KsSettingsView 裁定の踏襲。

## 2026-09-08: 論点 7 — 検証範囲と文書追随の仕分け

文書は 3 段仕分け (change 同梱: handbook local-development-setup の 1 ルールと README 絶対 URL 化 / 蒸留: concepts maui 2 本 + binding 構成の記述 + ADR-0004 昇格 / docs-refresh: README 互換表と skills) を表で確定。検証範囲は A: pack + 消費者 Release ビルド + 両 OS の起動確認 / B: 起動なし / C: pack のみ から A を採用。理由: phase-8 は起動を含まないため Android の NuGet 経由実行時は本 phase でしか見られず、ADR-0004 の未検証 3 点を全部埋めて蒸留で accepted に上げられる。全 7 論点が解消しフェーズ議論を終了。

## 2026-09-08: ADR 捕捉 — maui/ADR-0004 の改訂

論点 4 (下限 = workload 同梱版) と論点 5 (buildTransitive ガード) は将来の `global.json` 更新と facade パッケージの構成を縛るため ADR に残す。新規ではなく proposed のままの maui/ADR-0004 に本文ごと溶かした (Context の前提・Decision 2 項・却下案 4 件・Consequences・Revisit When を追記、date を 2026-09-08 に更新)。論点 3 の `ViewCreationFailed` は公開 API の詳細として concepts に任せ ADR 化しない。昇格は蒸留時。
