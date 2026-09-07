# scout 調査: KsSettingsView 先例まとめ (2026-08-14)

phase-4 論点3 (MAUI binding 構成)・論点6 (Sample 構成)・論点7 (sample-parity 規約) の判断材料。ksn-scout による KsSettingsView (`reference-repositories.md` 参照) の読み取り調査要約。

## 1. maui ドメイン ADR 0001〜0009 の要約

出典ディレクトリ: KsSettingsView `kasane/decisions/maui/` (index.md に全22件の一覧あり。0010以降は Cell 輸送・accessory 等の後続フェーズ知見)

**0001 — MAUI Bridge は内部所有 Store を持つ DSL 方式の類型**
Bridge は Native 側に `SettingsRootStore` を内部所有し、公開 API (`setRoot`/`applyDiff`/`setTheme` 等) を Store 公開操作へ変換して既存の「Store → Native Host」収束経路に乗せる。状態の所有者は C# 側の BindableObject ツリー。
却下: (a) Store 迂回で `controller.applyDiff` 直呼び (第三経路 = Store の再発明)、(b) Native Store handle を C# に公開する Store 方式 (API 表面積・実装コスト最大。将来拡張として保留)。

**0002 — Bridge 公開 API は Store 操作と 1:1**
union DTO をやめ、Store 公開操作 1:1 の12メソッドにする。`replaceCells` は iOS Store 本体へ追加して Android と対称化。
却下: union DTO + `applyDiff` 1本 (interop 境界での union 表現が不格好、decode→振り分け層が必要)。

**0003 — ユーザー操作通知は単一 delegate に集約**
Native → C# の通知を View あたり1個の delegate/listener に集約し、種別はメソッド名で識別。
却下: 単位 callback 登録 (interop ハンドルの生成・解放追跡が爆発しリークテストが破綻)、単一メソッド + シリアライズ payload (型安全性喪失)。

**0004 — Theme / CellStyle は MAUI 慣例型で公開**
公開 API は `Microsoft.Maui.Graphics` 等の MAUI 型、interop DTO は非公開の輸送表現。platform 固有項目は接頭辞付き nullable。
却下: 共通論理型 (KsColor 的中間型) の導入、platform 別 Theme を `#if` で公開 (XAML から書けなくなる)。

**0005 — Bridge が Store と Host を所有、独立モジュールで生成・破棄**
Host は Bridge が生成・公開 (同時1個)。破棄は冪等・破棄後 no-op。UI スレッド保証は呼び出し側契約 (Bridge 内で marshal しない)。
却下: 所有逆転案・interop API の既存 UI モジュール直付け・Bridge 側 marshal (薄い Bridge 原則違反・二重 dispatch)。

**0006 — Android Binding は gradlew 直接実行方式** (重点)
.NET Native Library Interop の標準ビルドアイテム `AndroidGradleProject` / `XcodeProject` を使うか否かの話。当初実装の Exec 方式が「無記録の逸脱」として指摘され、ksn-dual-research (相方 codex + ホスト実験) で裏取りした結果:
- **iOS の「SDK 制約」主張は誤り** → 標準 `XcodeProject` アイテム (+ `CreateNativeReference=false` + 手動 `NativeReference`) へ復帰。当初失敗の真因は scheme 衝突の可能性が高い。
- **Android の制約は実在 (確度 98-99%)** → dotnet/android SDK の init script が `rootProject.allprojects` の buildDirectory を単一パスへ束ねるため、project 依存を持つ複数モジュール構成では Gradle validation エラーでビルド不能 (実測)。`ModuleName` 指定でも依存連鎖がある限り回避不能。
決定: Android は `android/gradlew` を Exec で直接呼んで aar を生成し、公式アイテム `AndroidLibrary` (Bind=true) で束縛。pack 経路は公式アイテム経由なので `dotnet pack` は標準方式と共通 (実測済み)。SDK 側が複数モジュール対応したら見直す。再検証の入口は `maui/README.md` の「SDK 更新時に再検証する箇所」の表と対で維持。
却下: A) `AndroidGradleProject` 標準アイテム (実測ビルド不能。参考プロジェクトの成立例は全て単一モジュール構成)、B) Gradle 側の再構成で標準アイテムに載せる (費用対効果が見合わない)。
負の帰結: SDK のアンダースコア付き内部ターゲット (`_CategorizeAndroidLibraries` / `_ResolveLibraryProjectImports`) 依存が残り、workload/SDK 更新で最初に壊れる。

**0007 — Bridge に Host 単独解放 `releaseHost` を追加**
MAUI Handler の DisconnectHandler で `dispose()` すると再接続時に Store 内容が消える問題への対応。`releaseHost()` は Host のみ解放し Store 維持 (冪等)、再 `makeHost*` は Store 現在状態から復元した新 handle を返す。DisconnectHandler ⇄ releaseHost、ConnectHandler ⇄ makeHost を 1:1 対応。

**0008 — AiForms 互換 API 表面ポリシー**
対応概念があるものは AiForms 命名を踏襲、現行コア契約に無い機能は互換提供せず Native から再設計。
却下: 完全 AiForms 互換 (「コア契約が正」の前提が崩れる)、互換を捨てた新規命名 (移行性・学習容易性を損なうだけ)。

**0009 — net10.0 TFM + internal gateway 抽象** (重点)
問題: facade の本丸ロジックを高速テストしたいが、Bridge を運ぶ Binding assembly は platform TFM でしか参照できず素の `dotnet test` から到達不能。
決定: TargetFrameworks を `net10.0;net10.0-ios;net10.0-android` とし、純ロジックは platform 非依存コードに置く。Bridge 呼び出しは **internal な gateway インターフェース**越しにのみ行い、platform TFM だけが Binding 参照・gateway 実装を持つ。ユニットテストは素の net10.0 で fake gateway (呼び出し記録 + ID 採番 + Bridge の no-op/null 契約の再現) を注入。
却下: platform TFM のみでデバイス/シミュレータテストに寄せる (遅い)、純ロジックの別アセンブリ分離 (公開アセンブリが割れる。internal seam で同効果)。
帰結: `dotnet test -f net10.0` で115件が高速回帰。負: net10.0 は「テスト用の顔」で NuGet パッケージング時に TFM 構成の再検討が必要、platform 固有経路は E2E 依存。

## 2. トップレベル構成と Sample の配置

- トップレベル: `android/` `ios/` `maui/` `samples/` `docs/` `scripts/` `kasane/` + AGENTS.md / CLAUDE.md / README.md
- ビルドルートは platform 別、ルートに共通ビルドファイルを置かない (cross/ADR-0001、concepts/cross/architecture/repository-boundaries.md)。「一つの platform のビルド成立は別 platform を保証しない」を明文化。ルート統合 build.gradle.kts は「KMP 導入時に再検討」と保留
- **Sample は集約 `samples/` 配下** (samples/ios・samples/android・samples/maui、各 README 付き)
- **consumer 境界** (repository-boundaries.md「Sample の consumer 境界」節) — Sample は配布物ではなく「利用者アプリと同じ側から公開 product を組み合わせる実行可能な reference」:
  - iOS: `ios/` を Local Swift Package として参照し product を link
  - Android: `android/` を Gradle composite build (`includeBuild`) + GAV → included project の `dependencySubstitution` を Sample 側に明示 (AGP が Maven publication を生成しないため自動置換が発火しない)。Sample と本体の build root は統合しない
  - MAUI: Sample csproj は `KsSettingsView.Maui` への ProjectReference 1本だけ、Binding 層は推移参照。TFM は `net10.0-ios;net10.0-android` (テスト用 net10.0 は含まない)。物理配置は samples/、slnx 上は `/samples/` フォルダで可視
  - 禁止: Sample を配布物・挙動契約の SSoT・自動テストの代替として扱う / local source reference の成功を公開配布の成立と説明する

## 3. sample-parity 規約 (cross ドメイン)

規約本文: KsSettingsView `kasane/concepts/cross/conventions/sample-parity.md` / 決定: `kasane/decisions/cross/0016-sample-cross-platform-parity.md`

- **位置づけ**: Sample はプラットフォーム間パリティの**検証装置**。Sample 自体がばらつくと、画面差が「本体の仕様差 (=バグ)」か「Sample の書き方の差」か判別不能になる。ゆえに全 platform で一字一句同じ文言・同じ画面構成。却下案: platform ごとに idiomatic なサンプル
- **一致の単位**: 「対応するデモ画面」。対応は画面タイトル (= ルートメニュー文言) で取る。揃える4項目: (1) 画面の集合 (2) 表示文言の完全一致 (タイトル・メニュー・header/footer・title/description・デモデータ初期値と選択肢。表記ゆれも不一致) (3) 画面構成 (Section 数・Cell 数・並び順・パラメータ・色。platform 固有 semantic color 禁止、同一 RGBA を共通 SampleTheme に置く。dark mode 追随より一致優先) (4) メニュー文言 = 遷移先画面タイトル (同一文字列)
- **許容差異**: OS 標準ナビ chrome / 既定フォント / 描画差 (むしろ確認対象)、コード上の命名差、Sample が渡さないパラメータの本体既定値の platform 差 (deviation.md に記録)、実装順序による一時的な片側先行 (追随タスクの追跡必須。追跡が切れたら違反)
- **例外枠2つ**: (1) デモ対象の公開 API がその platform に存在しない場合は作らない + オーナー裁定 (2026-08-11) で「デモの主対象が platform 固有の公開 API と意味論ならページ全体を platform 固有画面としてよい」(ルートメニューで固有区分に置き、パリティ対象と区別) (2) platform 固有の技術検証画面はメニュー上「検証」等で区別しデモ集合に数えない
- **禁止**: 片側だけの変更を追跡なしで放置 / platform 単独の「改善」(改善は全 platform 一斉) / 一致不能箇所の黙認 (deviation.md に記録) / **この規約を製品契約と混同しない** (Sample の文言変更は breaking change ではない。パリティは内部の検証規約)

## 補足

- KMP 形態は KsSettingsView に実体がなく先例なし (cross/ADR-0001 で保留のみ)
- KsDialogs で参考になりうる追加: maui/ADR-0010 (AndroidX 版競合を Binding 層で吸収、NuGet 経路未検証)・ADR-0011 (per-type DTO 輸送)
