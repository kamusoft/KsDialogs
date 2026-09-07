# Live Summary: proofread-user-skills-ja

## 最終状態 (何がどうなったか)

利用者向け Agent Skills (`skills/ja/` と `skills/en/`) の 5 スキルのうち、platform 4 スキル (maui / ios / android / kmp) の SKILL.md と references 一式、および aiforms-migration の 2 ファイルを校正した。ja を正として書き直し、en を ja に同期した (64 ファイル)。

各 references (dialogs / loading / toast / layout / transitions / view-models、maui は di-registration、kmp は ios-host) に共通して施した型:

- **API 選択表**: 公開 show / start / hide 系の全 overload を「シグネチャ | 何をする | いつ選ぶ | 必要な登録」の表で列挙し、既存サンプルとの対応を示す。サンプルに現れない overload には最小例の節を設ける
- **現実的なサンプル**: ViewModel / content (MAUI は XAML + code-behind、iOS は SwiftUI と UIKit、Android は Compose と View) / 起動時の登録 (MauiProgram / App.init / Application.onCreate) / 呼び出し元 (Page・画面 model・ViewModel からの既定入口または注入) に分割。static helper・free function・top-level suspend fun の形は廃止。明示型は型推論に寄せる
- **bool 以外の結果型**の節を各 dialogs.md に新設 (maui: record ItemEdit、ios: struct ItemEdit、android: data class ItemEdit、kmp: String)
- **構成ミスの失敗**: 例外 (case) ごとに「メッセージ | 原因と対処」の表を実装で裏取りして掲載し、失敗する構成の例と catch して扱う例を添える
- **地の文の整理**: 長い段落を `##` 節に分け、列挙・優先順位・細則は表と箇条書きに。「facade」「契約」「消費側」「配線」「器」「ピル / pill」など分かりにくい語を平易化。各コード例の直前に「何を示す例か」の 1 文
- **表記統一**: プロダクト機能名は「Dialog」(「ダイアログ」を置換)、Dialog の接頭語「型付き」は除去 (「型付き結果」等の結果・型修飾は残置)、「ピル / pill」は「組み込みのメッセージ Toast」(kmp は「組み込みの非対話メッセージ Toast」)
- **SKILL.md**: 冒頭 2 段落を「ライブラリの一文 → 使い方の像 → この Skill が扱う版」の順に書き直し、「レシピを選ぶ」を表化

en 同期の方針: 節構成・見出し・表の列構成を ja に揃える、コードブロックは ja と byte 一致で複製、地の文は英訳、例外メッセージは実装が日本語リテラルのため en でも日本語のまま引用、"typed dialog" の接頭語は除去。訳語は「覆い」= overlay、「組み込み / 内蔵」= built-in、レシピリンクは [Dialog] 単数に統一。

## 採用値と根拠 (却下試行の要点)

- **AddKsDialogs の省略条件** (maui di-registration): 旧「RegisterForDialog が startup の捕捉を冪等に行うため、AddKsDialogs の呼び出し自体は省ける」は無条件に読めたが、実装 (`maui/KsDialogs.Maui/Hosting/KsDialogsServiceCollectionExtensions.cs`) と concepts (`kasane/concepts/maui/api/di-registration.md`) では fallback を使わないアプリに限られるため、条件付きに補正
- **kmp loading の setMessage**: 旧「世代に紐づかず、常に現在の表示に効く」は実装 (`kmp/.../KsLoading.kt` の doc) と異なり、「内蔵 Loading を表示している間だけ効く」に補正。en も追随
- **例外メッセージの掲載**: 実装が日本語ハードコード (ローカライズ機構なし) のため、en でも日本語文字列をそのまま引用 (訳すと実物と食い違う)
- **PresentationHostUnavailable の掲載範囲** (android): Loading / Toast は提示先不在でも例外にならない実装のため、Dialog の表のみに掲載
- **kmp の非真偽値の結果型の例**: 自前 data class が iOS の型消去輸送を往復できるかは samples / テストに実例が無く未確認のため、例は String に留めた
- **「型付き」**: 当初 maui dialogs.md の見出しを「型付き Dialog」に直したが、後に「Dialog の接頭語としての型付きは全部外す」で統一 (却下 → 再修正)
- **en の訳語**: ios ワーカーは「覆い」を scrim、android は overlay と訳して不揃いになったため、API 名 (`overlayColor` / `OverlayDuration`) に寄せて overlay に統一
- **view-models のコード例削除** (ios / android): 旧「lifetime / 同一性」節の重ね表示の例は dialogs.md に同水準の例があるため箇条書きとリンクに凝縮 (オーナー確認済み。異議なし)

## 触ったファイル

`skills/ja/` 33 ファイルと `skills/en/` 32 ファイル (計 65):

- ksdialogs-maui: SKILL.md, references/{di-registration, dialogs, layout, loading, toast, transitions, view-models}.md
- ksdialogs-ios: SKILL.md, references/{dialogs, layout, loading, toast, transitions, view-models}.md
- ksdialogs-android: SKILL.md, references/{dialogs, layout, loading, toast, transitions, view-models}.md
- ksdialogs-kmp: SKILL.md, references/{dialogs, ios-host, layout, loading, toast, transitions, view-models}.md。ja のみ references/android-host.md (「ダイアログ」→「Dialog」の表記統一 1 箇所。en は元から小文字 dialog が複数あり、en の表記方針は別対処)
- ksdialogs-aiforms-migration: SKILL.md, references/api-mapping.md

触っていないもの: README.md / README_ja.md / `.manifest.json`、ソース・テスト・concepts・decisions。

検査 (docs-refresh の scripts と標準 lint): ja/en コードブロック byte 一致・見出し構造対応・内部リンク解決 (70 ファイル)・frontmatter・local-path / identity lint、すべて差分ゼロ。コード例のコンパイル検証は行っていない (シグネチャ・既定値・例外は実装ファイルの読解で裏取り)。

## 決定事項 / ADR 候補

決定事項 (skills/ の記述規約として):

- プロダクト機能名は「Dialog」「Loading」「Toast」で表記し、Dialog の接頭語「型付き」は付けない。「組み込みのメッセージ Toast」で「ピル / pill」を置き換える
- en は ja に同期する: 構成・見出し・表は揃え、コードブロックは byte 一致、例外メッセージは実装の日本語リテラルをそのまま引用。「覆い」は overlay、レシピリンクは [Dialog] 単数
- 各 references は「API 選択表 → 現実的なサンプル分割 → 構成ミスの表とコード例」の型を持つ

ADR 候補: なし (公開 API・設計判断に触れていない)。

別対処に送った事項 (この change では扱わない):

- 「Dialog にはある型指定 show が Loading / Toast には無い」ことの注記 (裏取り済み: 全 platform で Loading / Toast にはレジストリの ViewModel factory slot 自体が無い。concepts / ADR に明示の決定は無い)
- android の Loading / Toast で提示先不在時の挙動の記述
- コード例のビルド検証
