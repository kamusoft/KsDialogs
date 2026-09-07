# セカンドオピニオン: add-toast (spec-001)
**相方**: codex / **label**: so-spec-add-toast / **日付**: 2026-08-27 / **対象**: 提案一式 (kasane/changes/add-toast/ の proposal / design / specs / tasks / ui/brief)
---
# レビュー結果: add-toast

**日付**: 2026-08-27  
**判定**: **NEEDS_DISCUSSION**

## サマリー

MAUI のレジストリ共有契約が accepted ADR と両立せず、このままでは全 Requirement を実装できません。また、多重 Toast の配置、duration と演出の時間モデル、fire-and-forget 後の失敗処理、各形態の公開 API／bridge 形状が未確定です。

指摘件数: Critical 1 / Major 7 / Minor 3 / Suggestion 0

## 指摘事項

### [🔴 Critical] MAUI と Native が同一レジストリを共有する契約は既存 ADR と両立しない

**該当箇所**: `specs/dialog-contract/spec.md:17`, `specs/dialog-contract/spec.md:44`, `specs/maui-binding/spec.md:9`, `kasane/decisions/maui/0001-call-scoped-bridge.md:18`

**問題点**: 共通契約は「ラッパー形態を含むすべての入口が同じレジストリを共有する」と規定しています。一方、accepted の maui/ADR-0001 は MAUI レジストリを C# 層、Native レジストリを別層とし、利用者の VM 型を Native レジストリへ登録しないと明示しています。したがって、MAUI で登録した VM を Native 直接入口から解決する TS-CO-06 相当の保証は実装不能です。

**推奨修正**: 次のどちらかを明示的に決定してください。

- 推奨: 「レジストリ共有は各形態内の singleton／DI インスタンス間」と限定し、MAUI と Native のレジストリは別であることを MAUI Requirement に明記する。TS-CO-06 も形態内共有と KMP–Native 共有に分割する。
- Native–MAUI 間でも本当に共有するなら、maui/ADR-0001 を置き換える新 ADR と、VM 型同一性・factory 輸送・ライフタイムを含む新 bridge 設計を先に確定する。

### [🟠 Major] 多重 Toast の配置が承認モックと器の設計で矛盾している

**該当箇所**: `design.md:79`, `specs/dialog-contract/spec.md:65`, `specs/samples/spec.md:27`, `ui/mock/default-pill.html:30`, `ui/mock/default-pill.html:43`, `kasane/decisions/core/0030-toast-container-implementation-form.md:22`

**問題点**: ADR／spec は同じ placement に1 Toast 1器を追加し、z-order だけを起動順にすると読めます。この場合、同サイズの Toast は同じ座標で完全に重なり、手前の1枚しか見えません。一方、承認モックは bottom 144 / 200 / 266 と縦方向に積み、TS-SA-03 も3枚すべてを観察できる前提です。Coordinator の表示リストだけでは、間隔、異なる高さ、消滅後の詰め直し、custom placement の扱いが決まりません。

**推奨修正**: 「同一位置で重ねる」か「可視スタックとしてずらす」かを決定してください。可視スタックなら、積む方向・間隔・高さの異なる View・途中消滅時の再配置・画面外への溢れ・明示 placement の混在を Requirement と共通ケース表で定義し、ADR-0030 の「自前スタック管理なし」も整合させてください。

### [🟠 Major] factory／提示処理の遅延失敗にエラーモデルがない

**該当箇所**: `specs/dialog-contract/spec.md:15`, `design.md:45`, `design.md:81`, `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingCoordinator.kt:365`, `maui/KsDialogs.Maui/Platforms/iOS/PlatformLoadingGateway.cs:104`

**問題点**: spec は message／inline 経路を「失敗しない」としていますが、factory の例外、MAUI View の platform view 化失敗、提示先消失、Window 取り付け失敗は起こり得ます。既存 Loading も、提示先が後から現れた時点で factory が失敗する経路を明示的に処理しています。Toast は戻り値なしなので、show が返った後の失敗を呼び出し元へ返せません。失敗時に表示リストやタイマーを残すとリークや再試行ループになります。

**推奨修正**: 解決・View 生成・取り付け・演出の各段階について、同期 throw、警告して破棄、再取り付け待ちのどれにするかを表で規定してください。遅延失敗時に timer、container、factory、VM を必ず解放する Scenario も追加してください。

### [🟠 Major] duration の開始点と演出を含む寿命が未定義

**該当箇所**: `specs/dialog-contract/spec.md:13`, `specs/dialog-contract/spec.md:19`, `specs/dialog-contract/spec.md:67`, `specs/dialog-contract/spec.md:121`, `design.md:57`, `design.md:81`, `kasane/concepts/core/api/transition-semantics.md:194`

**問題点**: duration を show の受理、器の取り付け、presentation 完了のどこから測るかが決まっていません。また、duration より長い presentation、終了しない dismissal、提示先不在、アプリの background 中に期限を迎えた場合も未定義です。「duration 後に消える」と「フック完了まで器を撤去しない」はそのままでは両立しません。さらに ToastStyle の既定 duration 自体が0以下の場合や、各言語の整数幅を超える値の扱いもありません。

**推奨修正**: 単調時計に基づく時系列を明記してください。最低限、計時開始点、duration 到達時は dismissal 開始か撤去完了か、提示先不在中も時間を消費するか、background 復帰時の扱い、フック未完了時の扱い、有効値域と style 値の検証規則が必要です。時計注入による決定的な Scenario を追加してください。

### [🟠 Major] 形態別 API 宣言と bridge の値対応が固定されていない

**該当箇所**: `design.md:12`, `design.md:20`, `specs/maui-binding/spec.md:9`, `specs/kmp-facade/spec.md:9`, `kasane/lessons/spec-review.md:8`

**問題点**: 完全な宣言があるのは Swift の例だけです。「各形態へ写す」だけでは、MAUI の `Toast.Instance`／純 static、`Show` の例外型、KMP の非 suspend 形、duration の整数幅、色表現、nullable placement、factory と managed View の保持期間が一意に決まりません。ToastStyle の全フィールドや per-call duration が bridge のどの口を通るかも列挙されていません。

**推奨修正**: iOS / Android / MAUI / KMP ごとの完全シグネチャ表と、公開値→ObjC/Java bridge DTO→Native 値の1対1対応表を design に追加してください。fire-and-forget 後も factory／View／transition callback を duration 終了まで保持する経路を明記し、最小コンパイルプローブで成立を確認してください。

### [🟠 Major] 禁止 API を保証する負の形状検査がタスク化されていない

**該当箇所**: `design.md:120`, `proposal.md:21`, `proposal.md:25`, `tasks.md:15`, `tasks.md:24`, `tasks.md:31`, `tasks.md:37`

**問題点**: design は正・負の公開 API 形状検査を要求していますが、spec と tasks にあるのは正の compile 検査だけです。これでは `hide`、await／結果型、options、show の style 引数、デフォルト View の transition 指定、KMP commonMain の ToastStyle など、Non-Goals の API が誤って公開されても検出できません。

**推奨修正**: 各形態に負の compile Scenario を追加し、禁止形状ごとに独立したフラグと期待診断を tasks に列挙してください。

### [🟠 Major] アクセシビリティ契約が欠落している

**該当箇所**: `ui/brief.md:5`, `specs/dialog-contract/spec.md:49`, `specs/dialog-contract/spec.md:119`

**問題点**: デフォルト Toast は視覚表示しか規定されておらず、VoiceOver／TalkBack への通知、accessibility label、フォーカスを奪わない保証がありません。完全非対話かつ短時間で消えるため、読み上げ通知がなければ支援技術利用者には機能自体が伝わりません。Android の非フォーカス Window と iOS の key window 直貼りでは実装差も生じやすい箇所です。

**推奨修正**: デフォルト View の読み上げ、フォーカス非移動、同時 Toast の通知順、custom View の責任境界を Requirement と Scenario に追加してください。Native 2実装で accessibility tree／event を検査するタスクも必要です。

### [🟠 Major] Toast Overlap の受け入れ条件が自己矛盾し、再現手順も決定的でない

**該当箇所**: `specs/samples/spec.md:45`, `specs/samples/spec.md:50`, `tasks.md:25`, `tasks.md:43`

**問題点**: Requirement は Loading 終了後に Toast が「残っていれば」と条件付きですが、Scenario は「Toast が残る」を必須にしています。Dialog の閉じる時刻は操作者依存なので、duration の具体値と自動進行がなければ結果が毎回変わります。また、Loading 再前面化の瞬間的なちらつき、タッチ素通し、IME／システムジェスチャ干渉は終端スクリーンショットだけでは判定できません。

**推奨修正**: Toast／Loading の具体 duration、Dialog の閉鎖方法と時刻、両起動順を固定してください。前面順はフレーム抽出、タッチ素通しは背後要素の状態変化、IME／ジェスチャは操作前後の状態で判定する手順を tasks に記載してください。

### [🟡 Minor] ToastStyle のどの項目が custom Toast に効くか不明確

**該当箇所**: `specs/dialog-contract/spec.md:98`, `specs/dialog-contract/spec.md:100`, `specs/dialog-contract/spec.md:109`

**問題点**: 優先順位からは defaultPlacement と defaultDuration が custom Toast にも効くように読めますが、Scenario はデフォルト View の style 適用しか確認していません。背景色・文字色・角丸は custom View に適用すべきではないものの、その境界も明記されていません。

**推奨修正**: 「duration／placement は全 Toast、視覚項目はデフォルト View のみ」と項目別に定義し、登録・インライン custom の nil duration／placement Scenario を追加してください。

### [🟡 Minor] 空文字と極端な長文の表示結果が未定義

**該当箇所**: `specs/dialog-contract/spec.md:19`, `ui/brief.md:44`, `kasane/concepts/core/api/layout-semantics.md:122`

**問題点**: 最大幅はありますが最大高さ・最大行数・省略方法がありません。レイアウト規則は内容を可視領域へクランプするため、極端な長文は非対話でスクロールもできず、文字がクリップする可能性があります。空文字も空のピルを出すか、何もしないか不明です。

**推奨修正**: 空文字、改行のみ、可視領域を超える長文について、表示・折り返し・切り詰めの契約と境界 Scenario を追加してください。

### [🟡 Minor] 証跡の保存先が現行 Kasane 規約と異なる

**該当箇所**: `tasks.md:43`

**問題点**: tasks は証跡を `verification/` に置く指定ですが、現行 ksn-core は `evidence/` を保存先としています。

**推奨修正**: 保存先を `evidence/` に統一し、動画を使う場合も保存するのは必要な静止フレームと説明だけにしてください。

## アクションプラン

1. MAUI レジストリ境界と、多重 Toast を可視スタックにするかをオーナー判断で確定する。
2. duration、遅延失敗、形態別 API、bridge 輸送表を design で凍結する。
3. 負の API 検査、アクセシビリティ、custom style、境界入力の Scenario を追加する。
4. Sample の時系列と実機判定手順、証跡保存先を修正する。

依頼どおり静的レビューのみ実施し、ビルド／テストおよびファイル変更は行っていません。


## 突き合わせ結果

ホスト側自己レビュー (指摘なし) との突き合わせ。相方のみの指摘につき根拠で採否判定:

- **[Critical] MAUI レジストリ共有の ADR 矛盾** — **採用** (maui/ADR-0001 accepted と実物照合し確定。ホスト側の上位層照合の見逃し)。→ dialog-contract の状態共有を「Native 内 + KMP 共有 / MAUI は C# 層で別」に修正、TS-CO-06 を形態内共有に限定、maui-binding に互換面専用 VM 型の構造 (maui/0001 踏襲) を明記
- **[Major] 多重 Toast の配置とモックの矛盾** — **採用 → オーナー確定 (2026-08-27)**: 同一位置で重ねる (z-order のみ、自動ずらしなし)。spec に明文化し、Sample の Toast Stack は視認用に placement をずらして表示、モックに「縦ずらしは図示」の注記を追加
- **[Major] 遅延失敗のエラーモデル欠落** — **採用**。→ 失敗モデルを段階別 (解決 = 同期 fail-fast / 受理後 = 警告 + 破棄と資源解放 / 提示環境不在 = 出現待ち・計時は消費) に規定、TS-CO-07 追加
- **[Major] duration の時間モデル未定義** — **採用**。→ 「受理時点から単調時計・実時間消費・到達で出の演出開始・撤去はフック完了後・style 既定の検証」を規定、TS-CO-08 追加
- **[Major] 形態別 API と bridge 対応の未固定** — **部分採用**。→ design Decision 1 に形態別入口の表とレジストリ共有境界・参照保持を追記。公開値→DTO の完全対応表は実装フェーズの詳細として見送り (Loading 先例で bridge 面は確立済み)
- **[Major] 負の形状検査の欠落** — **採用**。→ 4形態に負の compile 検査 Scenario (TS-IO-03 / TS-AN-04 / TS-MA-03 / TS-KM-03) と tasks を追加
- **[Major] アクセシビリティ契約の欠落** — **採用 (最小形)**。→ Requirement「支援技術への通知」(デフォルト View の announce・フォーカス非移動・カスタムはアプリ責務) と TS-AC-01・tasks 2.7 / 3.8 を追加
- **[Major] Toast Overlap の自己矛盾・非決定性** — **採用**。→ 固定時系列 (Toast 10000ms / Dialog 2000ms 自動閉じ / Loading 2000ms) の自動進行に修正し、判定手順 (フレーム・背後要素の状態変化) を tasks 6.3 / 3.7 に明記
- **[Minor] ToastStyle の適用範囲の曖昧さ** — **採用**。→ 視覚項目はデフォルト View のみ / 既定値項目は全 Toast、を明記
- **[Minor] 空文字・極端な長文** — **部分採用**。→ 「空文字はそのまま表示・長文は折り返し・レイアウト規則のクランプに従いスクロール/省略なし」を明記。境界 Scenario の追加は見送り (レイアウト共通ケース表の既存規則で判定可能)
- **[Minor] 証跡保存先 verification/ → evidence/** — **降格 (事実誤認)**。現行規約の保存先は verification/ (config.yaml の lint 除外定義・add-loading の実績と一致)。ksn-core に evidence/ の規定は存在しない

採用 8 / 部分採用 2 / 降格 1 / オーナー判断待ち 1 (多重 Toast の配置)
