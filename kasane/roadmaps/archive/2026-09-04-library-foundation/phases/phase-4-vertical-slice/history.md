# phase-4-vertical-slice 議論履歴

## 2026-08-14: 縦串の題材選定

「シンプルな Dialog 1本」の最小仕様を議論。選択肢は A. 固定コンテンツ・結果通知なし / B. 固定コンテンツ・結果通知あり / C. 最小カスタム View・結果通知あり の3案。

**採用: C (最小カスタム View + 型付き結果通知あり)**。カスタム View = ラベル1個 + OK/Cancel、VM = メッセージ文字列1個、結果 = completed(単純値) / cancelled。

理由: phase-4 の目的は全形態のアーキテクチャリスク潰しであり、最大リスク2箇所 — (1) VM 型キー → View factory レジストリのキー同一性 (core/ADR-0004、phase-1 最優先申し送りの KMP commonMain クラスの ObjC 可視性・キー同一性を含む) (2) Swift async → completion handler → Kotlin suspend の結果経路 (core/ADR-0003) — は固定コンテンツでは通らない。A/B はレジストリを迂回してしまい縦串の検証価値が薄い。C は phase-5 以降の Sample 題材としてもそのまま土台になる。レイアウト規則 (ADR-0007)・多段表示 (ADR-0006) の本格検証は共通仕様テストの器 (別論点) の責務とし、題材は上記の最小形に絞る。

ADR 化はなし (フェーズ内の題材選定 = 足場の決定で、選別3基準に非該当。契約自体は core/ADR-0003・0004 が既に保持)。

## 2026-08-14: 全形態貫通の受け入れ条件

選択肢は A. ビルド + 手動確認のみ / B. 手動確認 + 結果経路のみ自動テスト / C. UI 自動テスト完備 の3案。

**採用: B**。条件は (1) 4ルート (iOS Native / Android Native / MAUI / KMP) の Sample から show → 表示 → OK で completed(値)・Cancel/外タップで cancelled を手動確認、スクショを artifacts に記録 (phase-3 申し送りの ui.screenshot 実測を兼ねる) (2) KMP は iOS / Android 両ターゲットで確認 (3) 結果経路 (特に Swift async → Kotlin suspend 変換) は正しい型・値で返ることを自動テストで検証 (4) BuildProbe 削除後もビルドが通る。

理由: 縦串で最も怖い故障は表示不能ではなく境界 (Swift ↔ Kotlin) での結果の型・値の崩れで、目視では見逃しやすいため、そこだけコード検証する。UI 自動テストの4形態分のインフラ整備は縦串の細さに反するため phase-5 以降へ。ADR 化はなし (フェーズの受け入れ条件 = 足場の決定)。

## 2026-08-14: KMP 公開 API の形 (a) Swift async 変換の手段

論点4を (a) Swift async 変換の手段 / (b) expect/actual 境界の置き方 に分割し、(a) を先に議論。選択肢は A. 素の suspend 直接公開 (ObjC 自動変換の Swift async に任せる) / B. KMP-NativeCoroutines 採用 (KsAppKMP ADR-0003 踏襲) / C. 自前 @objc completion handler ラッパー の3案。

**採用: A (素の suspend 直接公開)。疎通確認で自動変換の粗が受け入れ条件 (正しい型・値で返る) を割ったら C にフォールバック。B は不採用。**

理由: KsDialogs は一般公開予定のライブラリで、KMP-NativeCoroutines は公開 API 面に第三者依存が漏れ、消費者の Swift 側にも SPM 依存を強いる。ダイアログの show は「単発呼び出し → 結果1個」で Flow 購読・キャンセル伝播が本質的に不要なため、KsAppKMP が採用した状況 (Flow 多用の共有 VM 層) とはワークロードが異なる — 判断型だけ借りて結論は変えた。疎通確認で実測する粗: 戻り型の nullable 化・型付き結果 (sealed DialogResult) の Swift からの見え方・エラーチャネルの形。

ADR: kmp/ADR-0001 として proposed 起票 (公開 API の依存方針 = 覆すコスト高 + 消費者を拘束)。

## 2026-08-14: KMP 公開 API の形 (b) expect/actual 境界の置き方

選択肢は A. commonMain は純粋な契約のみ + expect は既定 singleton エントリだけ + 実体 (レジストリ含む) は各 Native lib へ全委譲 / B. KMP 層にレジストリ実体を持つ / C. API 全体を expect class 化 の3案。

**採用: A**。commonMain = `interface KsDialogs` + `sealed DialogResult` + レジストリ契約 (純 Kotlin、FakeDialogs によるテスト差し替えはここで完結)。expect は既定 singleton エントリの取得のみ (core/ADR-0002 の singleton 側)。androidMain actual は Android Native lib へ直接委譲、iosMain actual は iOS Native lib (Swift) の @objc 互換面へ cinterop 経由で委譲。

理由: core/ADR-0001「KMP は薄いラッパー」を最薄で貫き、レジストリ実体を OS ごとに Native lib 側1個に置くことで純 Native 利用者と KMP 利用者が同一レジストリを共有でき、core/ADR-0004 の「singleton / DI はレジストリ共有」が自然に成立する。B はレジストリ二重化で登録漏れ・キー不一致の温床。C は expect 面積が広くモック困難。iosMain の cinterop 委譲は phase-1 申し送りのキー同一性疎通確認そのものを踏む形になり、崩れたら (b) 見直しだが、それこそ縦串で発見したい故障。

ADR: kmp/ADR-0002 として proposed 起票 (KMP ↔ Native の境界を跨ぎ将来を拘束する構造判断)。

## 2026-08-14: 共通仕様テストの器の初版

選択肢は A. シナリオ表 + 同名テスト規約 (最軽量) / B. 共有テストベクター + 各 platform ランナー (データ駆動) / C. KMP commonTest に共通テスト集約 の3案。

**採用: A**。共通仕様シナリオ表 (シナリオ ID + 前提 + 操作 + 期待される観察可能な結果 + OS 差の記録欄) を core の仕様文書として置き、各 platform はシナリオ ID を冠した同名テストで実装。UI 操作が要るものは手動確認 + artifacts 記録可 (受け入れ条件 B と同じ精神)。初版シナリオは phase-3 申し送りの4項目 — (a) 順閉じ = 期待値あり (ADR-0006 から導出) (b) 下を先に閉じる = 実挙動記録枠 (c) 外タップ = 期待値あり (d) Android 戻るボタン = 判断待ち枠。

理由: レイアウト属性セットの取捨が未確定 (phase-4/5 仕様化) の段階でデータ駆動の器を作ると属性確定のたびに作り直しになる。縦串では「シナリオ ID で対応付けを追跡できる規約」だけを確立し、B への進化余地 (ID が安定キー) を残す。C は MAUI・純 Native を覆えず不成立。

ADR 化はなし (器の初版 = フェーズ足場の決定)。縦串で器が実証されたら蒸留時に concepts 規約化を検討する。

## 2026-08-14: MAUI binding 構成

ksn-scout で KsSettingsView の maui ADR 群を要約調査 ([artifacts/scout-kssettingsview-precedents.md](artifacts/scout-kssettingsview-precedents.md)) し、3 subパートをまとめて議論。選択肢は「推奨案 (使い捨て Bridge + 標準アイテム優先 + gateway 踏襲)」「先例そのまま移植 (Store/Host 常駐)」「全部 Exec 方式 (先例の旧形)」。

**採用: 推奨案の3点セット。**

(1) **Bridge の形 = Store 非搭載の呼び出しスコープ Bridge**。KsSettingsView maui/0001 (内部所有 Store)・0005 (Host 所有)・0007 (releaseHost) は常駐 View の状態同期のための判断で、KsDialogs の「show → 結果1個 → 消滅」(core/ADR-0005 使い捨てモデル) には寿命モデルが合わないため構造は踏襲しない。借りる原則: 操作 1:1 (0002)・通知集約の縮退形 = show ごとの completion 1本 (0003)・MAUI 慣例型公開 + interop DTO 非公開 (0004)。MAUI レジストリは C# 層 (VM 型 → MAUI View factory) に持ち、show 時に MAUI View を platform view へ実体化して Native lib に渡す。Native 側レジストリ (kmp/ADR-0002 の共有レジストリ) とは層が別で、MAUI 経由の show は Native レジストリを使わない。

(2) **ビルド連携 = 標準アイテム優先 + 実測フォールバック**。iOS は標準 XcodeProject アイテム (先例が Exec から復帰した結論に従う)。Android は KsDialogs が単一モジュール (:ksdialogs) で、KsSettingsView maui/0006 の制約 (project 依存を持つ複数モジュール構成で SDK init script が衝突) に非該当の見込みのため、まず標準 AndroidGradleProject を実測。失敗時のみ gradlew Exec + AndroidLibrary 束縛にフォールバック。実測結果込みでビルド連携 ADR を実装時に起票 (TODO 化)。成立すれば KsSettingsView への知見還元にもなる。

(3) **テスト戦略 = net10.0 TFM + internal gateway 踏襲 (maui/0009 の判断型)**。受け入れ条件 (3) の結果経路自動テストを、シミュレータなしの素の dotnet test で fake gateway に completed / cancelled を返させて回す。seam の後付けは高くつくため初めから入れる。

ADR: maui/ADR-0001 (Bridge の形) と maui/ADR-0002 (gateway 抽象 + TFM 構成) を proposed 起票。ビルド連携は実測待ちのため未起票。

## 2026-08-14: Sample 構成の確定

選択肢は A. 集約 samples/ + KsSettingsView の consumer 境界踏襲 + KMP Sample 新設 / B. 各ビルドルート内に Sample / C. Sample 別リポジトリ の3案。

**採用: A**。samples/ios = Local Swift Package 参照、samples/android = composite build (includeBuild + dependencySubstitution 明示)、samples/maui = facade への ProjectReference 1本 (Binding 推移参照・TFM は platform のみ)、samples/kmp = shared + androidApp + iosApp の3点構成 (新設。shared の Presenter が KMP facade を消費し、iosApp の Swift 側 VM→View 登録でキー同一性の疎通確認を踏む)。「Sample を配布物・挙動契約の SSoT・自動テストの代替として扱わない」禁止事項も踏襲。

理由: KsSettingsView の consumer 境界は参照方式の落とし穴 (AGP が Maven publication を生成せず dependencySubstitution の明示が必要、等) まで実測済みで、同構成の KsDialogs が再発明する理由がない。C は配布基盤が必要でロードマップ非ゴールと正面衝突。B は本体ビルドと混ざり consumer 境界が曖昧になる。KMP Sample は先例なし (KsSettingsView は KMP 未導入) で KsDialogs が初の実物になる。

ADR: cross/ADR-0006 として proposed 起票 (リポジトリ境界を跨ぎ将来の全フェーズの Sample 運用を拘束)。

## 2026-08-14: sample-parity 規約の確定

選択肢は A. KsSettingsView 規約の翻案 + 一致単位を「デモ項目」に再定義 / B. 緩い parity (構成のみ・文言自由) / C. 規約なし (platform ごと idiomatic) の3案。

**採用: A**。規約の核 (Sample はパリティの検証装置 — Sample がばらつくと画面差が本体の仕様差か Sample の書き方の差か判別不能) はダイアログでも成立するため原則を全部借り、ダイアログに「ページ」がない構造差を一致単位の再定義で吸収する。一致単位 = 「デモ項目」: ルートメニューの起動項目文言 + 表示されるダイアログの内容 (タイトル・本文・ボタン文言・デモデータ) + 閉じた後の結果表示のワンセット。対応はメニュー項目文言で取る。4ルート (ios / android / maui / kmp) 全部で同一メニュー構成・同一文言。KMP Sample が登録する Native View は samples/ios・android の View と重複するが共有せず各自書く (samples 間コード共有は consumer 境界を濁す)。色は共通 SampleTheme の同一 RGBA (platform 固有 semantic color 禁止)、許容差異 (OS 標準 chrome・既定フォント・描画差・追跡付き片側先行)・例外枠 (platform 固有 API の固有区分・技術検証区分)・禁止事項 (追跡なし放置・片側だけの改善・製品契約との混同) は先例踏襲。

縦串の初版実物はメニュー1項目「Basic Dialog」で4ルート一致を通し、蒸留時にこの実物を根拠に cross/conventions/sample-parity.md として concepts 化する。

B は文言差と仕様差が混ざり検証装置として機能しない。C は先例で却下済みの型 (見本価値は同一サンプルでも果たせる)。

ADR: cross/ADR-0007 として proposed 起票 (全フェーズの完了条件「パリティ準拠の Sample 通し」を拘束)。

---

**フェーズ議論完了 (2026-08-14)**: 全7論点を決定事項へ昇格。ADR 起票6件 (kmp/0001・0002、maui/0001・0002、cross/0006・0007、いずれも proposed)。次は ksn-propose での変更提案化。
