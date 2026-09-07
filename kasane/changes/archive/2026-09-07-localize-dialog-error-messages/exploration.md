# Exploration: localize-dialog-error-messages

## 課題 / 動機

`DialogError` の `errorDescription` が返すメッセージが日本語で固定されている
(`ios/Sources/KsDialogs/Contract/DialogError.swift`)。一方、利用者向け Agent Skill の
英語版 (`skills/en/ksdialogs-ios/references/{dialogs,loading,toast}.md`) は
`localizedDescription` 列を持つ診断表を掲載しており、英語利用者がその列に日本語の
メッセージを見ることになる前提が en Skill に明示されていない。

発見の文脈: 2026-09-06 の docs-refresh (add-loading-toast-typed-show の蒸留に伴う
skills 追従更新) で、KMP Skill へ `DialogError` を掲載する作業中にサブエージェントが
検出した。当該作業では KMP 側の新設節にメッセージ列を置かないことで回避しており、
iOS Skill 側の扱いは未着手のまま残っている。

### 探索で確認した現状 (2026-09-07、コードとテストで実測)

- 失敗型のメッセージは 4 形態すべて日本語のハードコード。ローカライズ機構
  (`.strings` / strings.xml / `.resx`) の利用は 0 件
  - iOS `DialogError` 7 case (`ios/Sources/KsDialogs/Contract/DialogError.swift`)、
    iOS KMP 入口 `KsDialogsKmpError` 2 case (`ios/Sources/KsDialogs/Kmp/KsDialogsKmpError.swift`)
  - Android `DialogException` 5 種 (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogException.kt`)
  - MAUI `DialogException` 6 種 (`maui/KsDialogs.Maui/Contract/DialogException.cs`)
  - KMP 共有 `DialogException` は文言を持たず Native の message を素通し。ただし iOS gateway
    に日本語定数 3 本 (`kmp/ksdialogs-kmp/src/iosMain/.../IosDialogGateway.kt`、`IosLoadingGateway.kt`)
  - Loading / Toast は専用の失敗型を持たず上記を共用
- ライブラリ本体 (samples / テスト除く) の日本語文字列リテラルは全 61 件 (コメントは除く):
  失敗型 20 / throw 箇所で直接渡す文言 12 (iOS の storyboard 非対応 `fatalError` 6、
  MAUI の gateway フォールバック 4、MAUI の「中身は Native が持つ」2) / 警告ログ 19
  (iOS `os.Logger` 12、Android `Log.w` 7、MAUI `Trace` 2。iOS と Android は対になる文言) /
  文言の部品・定数 10 (KMP iOS gateway 3、MAUI iOS ブリッジ内部 Error 2、MAUI 効果句 3)。
  UI に出る文字列 (ボタン文言・アクセシビリティラベル等) は 0 件
- 文言に依存するテストは KMP の 2 ファイル 4 assertion だけ
  (`kmp/ksdialogs-kmp/src/androidHostTest/.../AndroidDialogGatewayContractTests.kt` 完全一致 1、
  `kmp/ksdialogs-kmp/src/iosTest/.../InteropBridgeContractTests.kt` 部分一致 3)。
  iOS / Android / MAUI のテストは case・例外型の同一性で検証しており文言に依存しない
- samples は文言を表示・依存していない
- Skills は en / ja とも 4 形態 × {dialogs,loading,toast}.md = 12 ファイルにメッセージ列を持つ
  診断表があり、en 版も日本語リテラルをそのまま引用している (handbook
  `kasane/handbook/cross/user-skill-writing-style.md` の「実装が日本語リテラルを持つ限り
  en でも日本語のまま引用する」規約による)
- concepts / ADR に言語方針を定めた記述は無かった

## 検討した選択肢 (却下案と理由を含む)

言語方針 (cross/ADR-0015 の Alternatives Considered と同じ):

- 英語固定に統一 — **採用**
- 日本語固定のまま en Skill に前提を注記 — 公開ライブラリとして英語圏の利用者がログで原因を読めない。却下
- 多言語化 (en + ja) — 開発者向け診断文のために 4 形態へリソース基盤を新設し保守が重い。
  端末言語で変わる文言を Skills の表に固定できない。却下
- 文言を持たず case だけ公開 — ログに出る情報が減る。却下

スコープ:

- 診断文言 61 件全部 — **採用** (規約が 1 行で済み、次に case やログを足す人が迷わない。
  件数が手で追い切れ、テスト依存も 4 件と判明している)
- 例外に届く文言だけ (42 件) / 失敗型だけ (20 件) — 「例外は英語・ログは日本語」の 2 段になる。却下

## 決定事項

- ライブラリ本体が実行時に外へ出す診断文言 (失敗型のメッセージ・throw 箇所の文言・警告ログ・
  その部品の定数) は英語固定、ローカライズしない。4 形態同じ方針。ソースコメントは対象外
  (cross/ADR-0015、accepted 2026-09-07)
- 対象は棚卸しした 61 件全部 (到達不能な init の `fatalError` 文言も含む)
- iOS と Android で対になる警告ログは同じ英語文言で揃える
- 文言に依存する KMP のテスト 4 assertion は英語文言へ追随する
- Skills の診断表 12 ファイル (en / ja) は英語文言を引用する形へ同じ change で追随する。
  handbook `cross/user-skill-writing-style.md` の「日本語リテラルをそのまま引用する」規約
  (L34・L47・「してはいけないこと」節) は撤回し、「実装の英語文言をそのまま引用する」に改める
- 公開契約 (case 名・例外型・引数) は変えない。変わるのは文言だけ

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: cross/ADR-0015 (accepted 2026-09-07) — 診断文言は英語固定・ローカライズしない

## 未決の論点

- (解決済み) 変更級は M、ロードマップ package-distribution とは独立の change として進める
- 英語文言の具体的な訳文 (実装フェーズで決める。デルタスペックで ja → en の対応表を持つと
  レビュー・verify と Skills の追随が機械的に突き合わせられる)

## UI 素材 (ui/references/ の一覧と注釈)

(なし)

## 変更級の推奨: S / M / L (理由)

**M で確定** (2026-09-07 オーナー確定。迷ったら 1 段上)。ロードマップとは独立の change として進める。

- 公開 API (case 名・型・署名) は変えず可逆だが、触る範囲が 4 形態 × 約 25 ファイル +
  Skills 12 ファイル + handbook 1 本 + KMP テスト 2 ファイルと局所的ではない
- 例外メッセージは Skills の診断表に載る観察可能な公開面であり、文言の ja → en 対応表を
  デルタスペックに持てば、レビュー・verify・docs-refresh の突き合わせが機械的にできる
- UI は触らない (ui/ 不要)

ロードマップとの関係: package-distribution の非ゴールに「ライブラリの機能追加」があり、
本 change は機能追加ではなく公開面を固める前の文言修正。phase-6 (MAUI パッケージング) が
同種の小修正 (`DialogException` 合流) を同梱する予定だが、本 change は 4 形態横断で
phase-6 の範囲を超えるため独立 change として進め、Skills の英語文言が公開履歴に載るよう
**phase-3 (public 化) より前に完了させる**順序だけを守る。
