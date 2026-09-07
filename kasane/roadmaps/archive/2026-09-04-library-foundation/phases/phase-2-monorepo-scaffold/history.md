# phase-2-monorepo-scaffold 議論履歴

## 2026-08-14: ビルドルート構成 — KMP と Android Native を分けるか兼ねるか

- **前提**: core/ADR-0001 により KMP androidMain actual は Android Native 実装への薄い委譲層で、KMP は Android Native への依存を必ず持つ。KsSettingsView cross/0001 (accepted) は却下案の中で「KMP 風統合ビルドは KMP 導入時に再検討しうる」としており、今回がその再検討。KMP モジュール自体を Android Native 配布物と兼ねる案は、core/ADR-0001 の「Native 実装と actual (委譲層) を分ける」構図と衝突するため検討外とした
- **選択肢**: A. 分離 — android/ と kmp/ を独立 Gradle ビルドとし composite build (includeBuild) で接続 / B. 兼用 — 1つの Gradle ルートに Android Native と KMP の2モジュールを置き project 依存で接続
- **採用**: A (分離)。ios/ / android/ / kmp/ / maui/ の4独立ビルドルート、リポジトリルートに共通ビルドファイルなし
- **理由**: 4形態との対称性と IDE ワーキングセットの独立を優先 (エージェント推奨は B だったがオーナー判断で A)。A の負担 (AGP 込み composite build の癖、Kotlin / AGP バージョンの2ビルド管理) は、バージョンカタログのファイル共有で緩和する

## 2026-08-14: 公開識別子の写像表

- **前提**: KsSettingsView cross/0002 (accepted) からドメイン骨格 (`kamusoft.jp` 根拠の `jp.kamusoft.*` / Maven groupId `jp.kamusoft` / .NET PascalCase) を翻案。本プロジェクト固有の論点は、KsSettingsView 時点に無かった「Android Native と KMP の Maven artifact 並立」の割り振りと、MAUI の識別子
- **採用**: iOS = Swift モジュール / SwiftPM パッケージとも `KsDialogs` / Android = `jp.kamusoft.ksdialogs` + Maven `jp.kamusoft:ksdialogs` / KMP = `jp.kamusoft.ksdialogs.kmp` + Maven `jp.kamusoft:ksdialogs-kmp` / MAUI = namespace `KsDialogs` + NuGet ID `KsDialogs.Maui`
- **理由**: 素の artifactId `ksdialogs` は「Native 主」(cross/0001) に整合させ Android Native へ (却下 — 両方接尾辞: 素の名前が死蔵 / KMP に素の名前: 薄いファサードに旗艦名を与え主従逆転)。KMP の Kotlin パッケージは Native への委譲構図でのクラス名衝突を避けるため `.kmp` を切る。MAUI namespace はオーナー判断で素の `KsDialogs` (.NET で使う形態は MAUI だけで自明、KsSettingsView の `KsSettingsView.Maui` とは異なる判断)。NuGet ID は文脈のない場所 (csproj 依存リスト・nuget.org 検索) で読まれるため `.Maui` を残し、原典 `AiForms.Maui.Dialogs` や `CommunityToolkit.Maui` の流儀に合わせる。KsSettingsView に PackageId の明示はなく先例拘束なし

## 2026-08-14: 最小疎通 — 各ビルドルートの最小ターゲット構成

- **前提**: バージョンは cross/ADR-0002、識別子は cross/0005 に従う。テストの中身 (フレームワーク選定・スモークテスト) は「テスト基盤」論点に分離し、ここではモジュール構成のみ確定
- **採用**: ios/ = Package.swift + `KsDialogs` (空ソース1枚) / android/ = `:ksdialogs` (com.android.library、minSdk 24) / kmp/ = `:ksdialogs-kmp` (androidTarget + iOS 3ターゲット、includeBuild で android へ実依存) / maui/ = KsDialogs.slnx + `KsDialogs.Maui` (TFM `net10.0-ios;net10.0-android`)。疎通の定義は各ルートのビルド成功
- **理由**: (1) composite 接続は cross/0004 で背負うと決めた最大リスクなので空のうちに実証する。KMP iOS→Swift の cinterop は Swift 側 XCFramework の実物が要るため phase-4 送り。(2) MAUI の TFM は core/ADR-0001 (全形態は Native 2実装に収束) の帰結として maccatalyst / windows を対象外に。原典 (ローカルは Xamarin 期 AiForms.Dialogs、xamarin.ios10 / monoandroid10.0) も iOS + Android のみで整合。(3) KMP の iosX64 は Intel Mac 退場済みだが「広い間口」方針 (cross/ADR-0002) に合わせ、空ターゲットのコストゼロなので含める

## 2026-08-14: swift-tools-version の実物確定 — 論点から TODO へ付け替え

- 確定手段が「scaffold で実際に Package.swift を書いてビルドが通る値を見る」ことであり、議論で選べる自由度が存在しないため、論点ではなく実装タスク (TODO) として扱う。確定後は cross/ADR-0002 へ追記して残課題を閉じる

## 2026-08-14: テスト基盤の最小構成

- **前提**: スモークテストは「再構成の検証装置」— 各ビルドルートのテストタスク配線が生きていることを自明なテスト1本ずつで証明する。CI・カバレッジはスコープ外 (KsAppKMP phase-2 の判断型)。先例実測: KsSettingsView は iOS = XCTest (66 import)、Android = JUnit 5 (core) + JUnit 4/Robolectric (UI)、MAUI = NUnit 4 + ライブラリ TFM に素の net10.0 を含む形
- **採用**: ios/ = Swift Testing / android/ = JUnit 5 (Jupiter) / kmp/ = kotlin.test (commonTest を androidTarget + iosSimulatorArm64 で実行) / maui/ = NUnit 4 (net10.0 + UseMaui のテストプロジェクト)。あわせて最小疎通の決定を1点修正し、MAUI ライブラリ TFM を `net10.0;net10.0-ios;net10.0-android` に (素の net10.0 追加)
- **理由**: iOS のみ先例と違える — Swift Testing は Xcode 16 以降 Apple の標準で、グリーンフィールドが今から XCTest で始める理由がない (UI 自動化到来時のみ XCTest 併用)。Android / MAUI は先例踏襲。TFM 追加は platform TFM だけだと net10.0 の unit test プロジェクトからライブラリを参照できないため (KsSettingsView が同理由で同形)。maccatalyst / windows 対象外の判断は不変。kmp の iOS シミュレータテストは Mac ローカル前提だが、composite + multiplatform のテスト配線こそ検証対象なので scaffold に含める
- **ADR**: 起票せず。テストフレームワーク選定は tooling の選択で、先例も ADR 化していない。決定事項 + history の記録で十分

## 2026-08-14: Sample の器 — phase-4 まで作らない

- **選択肢**: A. 置き場だけ確保 (空ディレクトリ) / B. phase-4 まで作らない
- **採用**: B
- **理由**: roadmap 前提「Sample 構成とパリティ規約は最初の実物 (phase-4 の最小 Sample) と同時に確定」に従い、実物なしの先取りで phase-4 の構成自由度を拘束しない。scaffold の疎通定義 (各ルートのビルド成功) に関与しない空の器を混ぜない。consumer 境界の忘却リスクは roadmap 前提 + phase-4 agenda の明記で担保。ADR は不要 (いつでも覆せる工程順序の判断)

## 2026-08-14: README・.gitignore 等の初期整備

- **採用**: README / .gitignore / LICENSE の3点に限定。README = ルートに英語主で最小限 (名前・コンセプト1段落・開発中ステータス・4ビルドルートの地図)、README-ja 併設は phase-9 docs で検討。.gitignore = ルート1本に4形態分をまとめる。LICENSE = MIT (c) kamusoft
- **理由**: 一般公開予定のため README は英語主 (原典も英語主 + README-ja 併設の形、オーナー同意済み)。.gitignore は per-root 案もあるが ignore パターンの重複が多く、先例 KsSettingsView のルート1本に運用実績がある。MIT は原典 (AiForms.Dialogs)・先例とも同一で既定路線。.editorconfig・lint 等の規約系は空の器への先取りを避け、実コードが生まれるフェーズ (phase-3 or phase-4) に送る
- **ADR**: 起票せず。先例も README / LICENSE 類を ADR 化しておらず、決定事項 + history で十分

これで論点はすべて解消。残 TODO は swift-tools-version の実物確定 (実装時) と ksn-propose での提案化のみ。
