# Live Session: proofread-user-skills-ja
対象: skills/ja/ 配下の利用者向け Agent Skills (SKILL.md + references/) の文章校正。オーナーの都度指示で箇所ごとに直す
開始: 2026-09-06

## 試行ログ (append-only)
- ワーカー起動 (ksn-implementer, host) → 準備完了。適用規約: handbook/cross/user-skill-api-listing.md (skills/** 該当) → 継続
- 継続送信 (SendMessage) がこの環境で使えず、都度起動フォールバックに切替 → 継続
- SKILL.md 冒頭 2 段落を KsSettingsView 流に書き直し、「ダイアログ」→「Dialog」統一、「型付き」削除、「レシピを選ぶ」を表化 → skills/ja/ksdialogs-{maui,ios,android,kmp}/SKILL.md (maui:12,14,20,49- / ios:12,18,44- / android:12,76- / kmp:12,22,97-)。aiforms-migration は該当なしで無変更。references/ の一般語「ダイアログ」は未編集 → 確認待ち
- di-registration.md 冒頭 3 段落を 4 節 (Dialog を登録する / Loading・Toast の custom content を登録する / facade の注入と AddKsDialogs / MauiProgram での登録例) に分割・短文化。後続 :89, :120 の長段落も同方針で分割。コードブロックは無変更 (en と byte 一致確認済) → skills/ja/ksdialogs-maui/references/di-registration.md → 確認待ち
- di-registration.md :25-29 「facade / 契約 / 消費側」を平易化: 見出し「facade の注入と AddKsDialogs」→「DI で受け取る入口と AddKsDialogs」、本文を「Dialog.Instance を IKsDialogs として singleton で登録し…」の具体形に。「省ける」は実装 (maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs) と concepts/maui/api/di-registration.md で裏取りし「fallback を使わないアプリは AddKsDialogs を別途呼ばなくてよい」と条件付きに → 確認待ち
- maui dialogs.md を全面改稿: 見出し「型付きダイアログ」→「型付き Dialog」(ios も同様。android/kmp は元から該当なし)、サンプルを ViewModel / XAML View + code-behind / MauiProgram 登録 / 呼び出し元 Page の分割に (static helper 廃止)、明示型→var、bool 以外の結果型 (record ItemEdit) の節を新設、構成ミス節にメッセージ列・対処列を追加 (6 例外、実装 Contract/DialogException.cs で裏取り) → skills/ja/ksdialogs-maui/references/dialogs.md, skills/ja/ksdialogs-ios/references/dialogs.md:1 → 確認待ち
- maui dialogs.md 「構成ミスの失敗を扱う」の表の直後にコード例 2 ブロック (登録を落とした MauiProgram の失敗構成 / 呼び出し元で DialogException.ViewFactoryNotRegistered を catch し ViewModelTypeName をログして再 throw) と基底型 catch の 1 文を追記 (:231-269)。例外階層は Contract/DialogException.cs で裏取り → 確認待ち
- maui dialogs.md :10 に「ShowAsync を選ぶ」節を新設: IKsDialogs の 7 overload (Presentation/IKsDialogs.cs で裏取り、拡張メソッドなし) を表 (シグネチャ | 何をする | いつ選ぶ | 必要な登録) で列挙し、既存サンプルとの対応を 1 文で示す → 確認待ち
- maui loading.md (128→349 行) / toast.md (152→271 行) を dialogs.md の型に展開: 「表示メソッドを選ぶ」(IKsLoading 11 メソッド) / 「Show を選ぶ」(IKsToast 3 overload) の選択表を新設、static helper を Page + 注入に置換、custom content を ViewModel / XAML / code-behind / MauiProgram / 呼び出し元に分割、Style 設定を MauiProgram 内に、明示型→var、「構成ミスの失敗を扱う」を新設 (loading 4 例外 / toast 2 例外 + 受理後失敗は破棄)、長文を節に分割 → 確認待ち
- android dialogs.md (136→314 行) / loading.md (108→249 行) / toast.md (123→209 行) を同じ型に展開: 「show を選ぶ」「show と start を選ぶ」の選択表 (KsDialogs 4 / KsLoading 10 / KsToast 4)、混載ブロックを ViewModel / コンテンツ (Compose と View) / Application.onCreate 登録 / Activity または ViewModel 呼び出しに分割、Boolean 以外の結果型 (ItemEdit) 節を新設、構成ミス表 (dialogs 5 / loading 2 / toast 2 例外) + 失敗構成と catch の例、intro 節の新設と長文の分解 → 確認待ち
- kmp dialogs.md (63→198 行) / loading.md (88→181 行) / toast.md (57→146 行) を同じ型に展開: 選択表 (show 1 / Loading 6 / Toast 2)、top-level suspend fun (static helper 型) を class + コンストラクタ注入に置換、「真偽値以外の結果型」(String) 節を新設、構成ミス表 (dialogs 5 状況 / loading 1 / toast 1) + 失敗構成と catch の例、「型付き」除去、var の型注釈除去。loading.md:52 の setMessage の記述を実装 (KsLoading.kt:53-58) に合わせ「内蔵 Loading 表示中だけ効く」に補正 → 確認待ち
- ios dialogs.md (127→300 行) / loading.md (107→222 行) / toast.md (96→206 行) を同じ型に展開: 選択表 (show 8 行 / Loading 14 行 / Toast 6 行)、free function を @Observable な画面 model + init(dialogs: any KsDialogs = Dialog.shared) の注入形に、登録と style 設定は @main App の init に、「Bool 以外の結果型」(struct ItemEdit) 節を新設、構成ミス表 (DialogError 4 case + 型消去 3 case は注記 / loading 2 / toast 1) + 失敗構成と case 別 catch、「ダイアログ」→「Dialog」統一 → 確認待ち
- maui transitions.md を整理: 添付の説明を箇条書き 4 点に、preset の引数・辺を表に、既定挙動と細則を箇条書き 5 点に、サンプルを ViewModel / XAML / code-behind (Dialog.SetTransition(this, ...)) / 登録 / 呼び出し元に分割、custom hook と組み合わせの節に「定義した DialogTransition を code-behind で添付して ShowAsync で表示する」使い方を追記、明示型→var。Contract/DialogTransition.cs / Presentation/DialogAttachedProperties.cs で裏取り → 確認待ち
- 表記統一: 「ピル / pill」→「組み込みのメッセージ Toast」(kmp は「組み込みの非対話メッセージ Toast」) を android / ios / maui / kmp の toast.md と aiforms-migration/api-mapping.md の 16 箇所で置換 (android の見出しと参照文も追随)。Dialog の接頭語「型付き」を maui dialogs.md:1 見出し、各 SKILL.md 能力マップ (maui/ios/android/kmp)、aiforms-migration SKILL.md:12 の 6 箇所から除去。「型付き結果」等の結果・型修飾は残置 → 確認待ち
- android: 未例示 overload の 1 行例を loading.md に 3 つ追加 (show(viewModel) :173 / show(viewModel, factory) :218 / showCompose :224、対応文 :26 更新)。dialogs.md / toast.md は表の全行が既存例でカバー済みで追加なし → 確認待ち
- maui: 未例示 overload の最小例を「残りの overload の最小例」節として dialogs.md:27 (4 例: 2 引数 factory / 非同期 configure ×2 / bool 省略形) と loading.md:33 (4 例: ShowAsync(ILoadingViewModel) / StartAsync 戻り値なし / StartAsync<T>(ILoadingViewModel) / StartAsync<TViewModel,T>(factory)) に新設、対応文を更新。toast.md は全 overload カバー済みで無変更。非同期 configure 例は利用者側の補助メソッド (LoadDefaultNameAsync / PrepareAsync) を仮置き → 確認待ち
- kmp: 「各メソッドの最小例」節を 3 ファイルに新設。dialogs.md:142 (placement 付き show)、loading.md:123 (show(viewModel) / show(message, placement) / start(viewModel, placement))、toast.md:80 (show(message) のみ / show(viewModel, placement))。overload 自体は全てカバー済みだったため、未例示だった引数の形 (placement 省略・指定) を補う粒度 → 確認待ち
- ios: 「各 overload の最小例」節を dialogs.md:28 (7 例)、loading.md:32 (15 例)、toast.md:26 (7 例) に新設。SwiftUI / UIKit の factory 版は両方を例示。toast.md:24 の対応文は既存例が実際には show(_:duration:) だったため訂正 → 確認待ち
- maui view-models.md の説明文を平易化 (コードブロックは md5 一致で無変更): 冒頭 2 段落を「Notifier を読む」(表 4 行) / 「ViewModel は class にする」/ 「同じ instance を重ねて表示しない」の 3 節に分割、型指定 show の節に「必要な登録」(2 slot の表) と「生成から表示までの順序」(番号付き 4 段) の小節を新設、各コード例の直前に 1 文の説明を追加、「ダイアログ」→「Dialog」 → 確認待ち
- maui layout.md (97→145 行) を同じ方向性で整理: 前文の地の文 3 段落を「添付できる項目」「サイズと位置の細則」(表 5 行)「実効値が決まる時点」(表 4 行) に分解、code-behind の static helper 例を ConfirmCardView の constructor で添付する例と show の placement 引数で渡す例に分割、「値オブジェクトにまとめる」を表化、「契約の既定値」→「ライブラリの既定値」、「ダイアログ」→「Dialog」。XAML ブロックは無変更 → 確認待ち
- kmp layout.md (103→133 行) / transitions.md (93→142 行) / view-models.md (53→87 行) を同じ方向性で整理: layout は属性 2 種の表 + DialogPlacement のプロパティ表 + 「添付が効く時点」の箇条書き、transitions は preset / 引数 / 辺 / 添付先の表と「添付後に show がどう動くか」の番号付き 5 段、view-models は報告口の表 + 「必要な登録」(3 レジストリ × Android / iOS の表) + 「生成から結果までの順序」6 段。top-level suspend fun と composition root 関数を class + コンストラクタ注入 (ItemListViewModel) に置換、AnimatedDialogViewModel → NoticeViewModel に改名。host のコード例と options 表はスキップ (既に同水準) → 確認待ち
- kmp ios-host.md 「Swift boundary に @Throws を付ける」節にコード例 3 ブロックを追加 (:111 exported wrapper に @Throws を付ける / message 専用 Toast には付けない対比、:141 Swift の try await + catch、:162 iosMain の override に @Throws を書かない例)。既存の文・表は無変更。samples/kmp の SamplePresenter.kt と SampleMenuModel.swift、ADR kmp/0001 で裏取り → 確認待ち
- ios layout.md / transitions.md / view-models.md を全面改稿: layout は属性表 (DialogOptions / DialogPlacement) + 添付面の表 + 「値の決まり方」9 項、transitions は添付面 / preset / 引数 / 辺の表 + 細則 8 項 + content → App.init 登録 → 呼び出し元の 3 例、view-models は notifier の表 + 「必要な登録」2 slot の表 + 「生成から表示までの手順」5 段。free function を ItemScreenModel の extension に、ダミー型を dialogs.md の ConfirmViewModel に統合。旧 ja に無かった core 契約の事実 (余白の丸め・比率が fill に勝つ・クランプ・未添付時 0.25 秒クロスフェード等) を concepts から追加。「不正な ViewModel lifetime を避ける」のコード例は削除して規則 3 項に凝縮 (重ね表示の例は dialogs.md にあり) → 確認待ち
- android layout.md (92→192 行) / transitions.md (85→213 行) / view-models.md (72→163 行) を全面改稿: layout は DialogOptions / DialogPlacement のプロパティ表 + 「値の決まり方」「いつの値が使われるか」+ Compose / View 添付 + SampleApplication 登録 + ItemActivity の placement 上書き、transitions は preset / 引数 / 辺の表 + 細則 + View 添付と登録の節を新設、view-models は notifier の表 + 2 スロットの表 + 手順 5 段。static helper 的な関数を ItemActivity / ProfileScreenViewModel に置換。旧「ViewModel の同一性を守る」のコード例は削除して dialogs.md へのリンクに → 確認待ち
- en 同期を開始 (4 ワーカー並列: maui + aiforms-migration / ios / android / kmp)。方針: 節構成と見出しを ja に揃える、コードブロックは ja と byte 一致で複製、地の文は英訳、例外メッセージは日本語リテラルのまま引用、Dialog の接頭語 typed は外す。検査は docs-refresh の code-block-parity / heading-parity / link-resolution スクリプト → 進行中
- en 同期 ios (7 ファイル) 完了: 節構成・見出し・表を ja に揃え、コードブロックは byte 複製 (dialogs 3→19、loading 3→23、toast 4→15 等)。code-block-parity / heading-parity / link-resolution / lint すべて差分ゼロ。訳語: 組み込み・内蔵 → built-in、覆い → scrim (overlayColor 行は除く)、合流 → merge → 確認待ち
- en 同期 android (7 ファイル) 完了: 節構成・見出し・表を ja に揃え、コードブロックは byte 複製 (dialogs 5→14、loading 4→14、toast 5→10 等)。parity / link / lint / frontmatter / api-coverage すべて差分ゼロ。訳語: 器 → container、覆い → overlay、レシピリンクは [Dialog] 単数に統一 → 確認待ち
- en 同期 maui (8 ファイル) + aiforms-migration (2 ファイル) 完了: 節構成・見出し・表を ja に揃え、コードブロックは byte 複製 (dialogs 4→16、loading 5→16、toast 4→11、transitions 3→7 等)。di-registration の「AddKsDialogs は fallback を使わないアプリでは不要」の条件付き化に追随。parity / link / lint すべて差分ゼロ → 確認待ち
- en 同期 kmp (8 ファイル) 完了: 節構成・見出し・表を ja に揃え、コードブロックは byte 複製 (dialogs 2→8、loading 3→9、toast 2→7、ios-host 3→6)。loading.md の setMessage の誤記述を ja に追随して補正。parity / link / frontmatter / lint すべて差分ゼロ → 確認待ち
- en 訳語統一: scrim → overlay (12 箇所 / 5 ファイル)、[Dialogs]( → [Dialog]( (18 箇所 / 11 ファイル)。ja 小修正: ios toast.md 最小例の Toast.shared.show → toasts.show (2 箇所)、maui dialogs.md:9 の「以下は 3 つに分けた最小構成」文を「ViewModel を宣言する」直前へ移動。en は byte 複製 / 同位置移動で追随。全検査 (parity / heading / link / lint / grep) 差分ゼロ → 確認待ち
- 確定 (オーナー宣言)。summary.md を書き起こし、全検査 (parity / heading / link / frontmatter / lint / api-coverage) 差分ゼロを確認 → review-001 へ
- review-001: CHANGES_REQUESTED (Major 4 / Minor 9 / Suggestion 3)。事実誤り (kmp view-models の Loading/Toast registry、ios toast の viewFactoryTypeMismatch) と表記統一の取りこぼし (ja maui「ダイアログ」6 箇所、scrim 残存) が中心。見た目・内容の合意を覆す指摘なしと判断し修正へ → 継続
- review-001 の指摘を修正 (Major 4 / Minor 9 / Suggestion 2 対応、Suggestion 1 件「KMP の DialogError 掲載」は新規面の追加になるためオーナー判断へ据え置き)。ja/en 同期、全検査差分ゼロ → review-002 へ
- review-002: APPROVED (review-001 の 14 件解消、新規 Minor 2 / Suggestion 1)。Minor 2 件を対応: android toast.md:24 の警告ログ条件を実装 (ToastCoordinator.kt:323-334) に合わせて ja/en 修正、summary.md の触ったファイルを 65 件 (ja android-host.md を含む) に訂正 → 完了
## 決定事項
- en の「覆い」は overlay に統一 (API 名 overlayColor / OverlayDuration に寄せる)。レシピリンクは [Dialog] 単数
- en は ja に同期する (構成・見出し・表は揃える、コードは byte 一致、例外メッセージは実装の日本語をそのまま引用)
- 「Dialog にはある型指定 show が Loading / Toast には無い」注記は別対処 (この change では扱わない)
- Dialog の接頭語「型付き」は全部外す。「型付き結果」のような結果・型の修飾は残す
- 「ピル / pill」は「組み込みのメッセージ Toast」に統一
- ja をベースに校正する (en への反映は確定時に扱いを決める)
## エスカレーション・スコープ外の発見
- review-001 Suggestion: KMP Skill に `DialogError` (Swift 側の表示入口の失敗型) が未掲載 (本 change 以前からの欠落)。掲載先の Swift 表示入口自体が未掲載のため、追記は新規面の追加になる。別対処候補
- en リンクラベルの単複が platform で不揃い (android は [Dialog] 単数に統一、kmp は既存の [Dialogs] [View models] を維持)。en 側の訳語 (覆い = scrim / overlay) も ios と android で異なる。確定前に揃えるか要判断
- ja maui dialogs.md:9「以下は ViewModel・View・呼び出し元の 3 つに分けた最小構成である」の直後に「ShowAsync を選ぶ」「残りの overload の最小例」が挟まり、3 分割は 2 節先から始まる (en 同期ワーカーの指摘。文の移動は要判断)
- ja ios toast.md「各 overload の最小例」で最初の 2 例だけ Toast.shared.show、残り 5 例は toasts.show と混在 (loading.md は loading. で統一)。en 同期ワーカーの指摘。ja 側の直しは要判断
- 裏取り (ksn-scout): 型指定 show (ViewModel factory 経由) は Dialog にだけ存在し、Loading / Toast には全 platform で無い (レジストリに VM factory slot も無い)。concepts / ADR に「Loading / Toast には出さない」という明示の決定は無く、Dialog 対象の ADR 群がそのまま実装に落ちた形。skills/ja の選択表は実装と一致。「Dialog にはある型指定 show が Loading / Toast には無い」の注記は未記載 (追記はオーナー判断)
- ios / android の view-models.md 旧「lifetime / 同一性」節のコード例 (ChoiceViewModel + showChoicesSeparately) をワーカーが削除して箇条書きに凝縮。オーナーの「コード例は良い」評価は maui 版に対するものだったが、ios 版でのコード削除は要確認
- ja/en の heading-parity は改稿前から全 Skill で乖離 (heading-parity-check.py)。en 追従時に見出し対応も取り直す
- kmp ios-host: Swift 側で Kotlin 由来 NSError の message を読む実例が samples / テストに無く、例は `\(error)` 補間に留めた (localizedDescription の読み方は未記述)
- 全 platform: コード例はコンパイル未検証 (署名は実装読みでの裏取り)。確定時にサンプルの主要ブロックをビルドで検証するか判断
- ios: 起動時に style / options を設定する実例が samples に無く、App の init 配置は推定
- 各 SKILL.md 能力マップの「型付き Dialog を登録して表示する」に「型付き」が残存 (references は除去済)。統一するか判断待ち
- kmp: 真偽値以外の結果型で String 以外 (自前 data class) が iOS の型消去輸送を往復できるかは未確認 (samples / テストは Boolean と String のみ)。dialogs.md の例は String に留めた
- kmp loading.md 旧「setMessage は常に現在の表示に効く」は実装と異なっていた (校正中に補正)。en 版と concepts に同じ記述が無いか drift 確認候補
- android の Loading / Toast は提示先 (resumed Activity) 不在でも例外にならない実装のため、PresentationHostUnavailable は Dialog の表のみに掲載。不在時の挙動は本文未記述 (追記はオーナー判断)
- maui loading の ServiceProviderUnavailable / PresentationHostUnavailable 経路は直接検証するテストが無く、表の記述はコード読解ベース (ワーカー報告)
- ShowAsync/show の overload 一覧の欠落は ios (型渡し show が dialogs.md から辿れない) と android (一覧表なし、KClass 版は view-models.md のみ) にもある。kmp は 1 形のみで欠落なし。未編集 (オーナー判断待ち)
- maui dialogs.md のコードブロックを ja で全面差し替えたため en との byte 一致が崩れている (10 ブロック + xml 新規 + 構成ミス表の列追加)。確定時に en 追従を必須項目とする
- 例外メッセージは実装が日本語ハードコード (ローカライズ機構なし)。en 版でも日本語のまま引用する必要がある
- en の di-registration.md には「AddKsDialogs は無条件に省ける」旧記述が残る (ja は条件付きに修正済)。en 反映の扱いは確定時に判断
