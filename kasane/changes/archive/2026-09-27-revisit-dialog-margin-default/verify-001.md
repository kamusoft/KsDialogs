# 検証結果: revisit-dialog-margin-default (001 回目)

**日付**: 2026-09-27
**判定**: VALID

対象はデルタスペック 2 本 (`specs/dialog-contract/spec.md` MODIFIED 2 Requirement / `specs/samples/spec.md` ADDED 1 Requirement) と、作業ツリーの未コミット変更 (基準 HEAD 525fb32)。deviation.md の 2 件は合意済みの差分として扱う。

## 対応表

### dialog-contract / Requirement: メタ属性セットと既定値 (MODIFIED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定の options の余白は全辺 0 | `ios/Sources/KsDialogs/Contract/DialogOptions.swift:38`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt:29`、`maui/KsDialogs.Maui/Contract/DialogOptions.cs:31`、MAUI Android 橋渡し `maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogAttributes.kt:51-60`、MAUI iOS 橋渡し `maui/macios/native/KsDialogsMauiBridge/MauiDialogAttributes.swift:39-48` (契約既定値から引く)、添付プロパティ `maui/KsDialogs.Maui/Presentation/DialogAttachedProperties.cs:39` (同) | `ios/Tests/KsDialogsTests/DialogLayoutAttributeDefaultsTests.swift:16,61`、`android/ksdialogs-core/src/test/kotlin/jp/kamusoft/ksdialogs/DialogAttributeDefaultsTests.kt:17,26,42`、`maui/KsDialogs.Maui.Tests/DialogLayoutPassthroughTests.cs:120`、`maui/android/native/ksdialogs-maui-bridge/src/test/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogLayoutPassthroughTests.kt:81` (橋渡しの既定 = 契約の既定) | ✅ 一致 |
| 属性を指定しない呼び出しはコンパイル互換を保つ | 公開 API のシグネチャは変わらず既定値だけを差し替え。`core/layout-spec/cases.json` C19 (期待値不変、note を改訂) | ケース表 C19 を読む `ios/Tests/KsDialogsTests/DialogLayoutCaseTableTests.swift` / `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogLayoutCaseTableTests.kt` と、既存の呼び出し側 (Sample 4 ルート・api-surface-check) のビルド成功 | ✅ 一致 |
| 余白を指定しない末尾寄せは基準領域の端に接する | `core/layout-spec/cases.json` C24 (120, 530)・C26 (120, 590)、`defaults` を `dialogMargin=0全辺` に改訂 | ケース表 C24/C26 を読む両 Native の CurrentPage 系テスト (`ios/Tests/KsDialogsTests/Support/DialogCurrentPageStage.swift:16` の margin 0、`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogCurrentPageTests.kt`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/compose/ComposeCurrentPageTests.kt`) | ✅ 一致 |
| Loading とカスタム Toast も余白の既定値 0 で置かれる | 契約既定値の差し替えに追従 (既定ローディングの一括設定の既定は `DialogOptions()`) | 既定ローディング: `ios/Tests/KsDialogsTests/LoadingAttributeTests.swift:47`、`android/.../LoadingAttributeTests.kt:120` (カスタム Loading も同じテスト内)。カスタム Loading (iOS): `LoadingAttributeTests.swift:64` の引数 End/End で下端 = 可視領域の下端。カスタム Toast: `android/.../ToastAttributeTests.kt:238`、iOS `ios/Tests/KsDialogsTests/ToastAttributeTests.swift:58` (TS-AT-02 の引数 End で下端 = 可視領域の下端) | ✅ 一致 |
| 無効値の正規化 | `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayout.kt:49` (`DialogOptions().dialogMargin` から引く形に変更)、`ios/Sources/KsDialogs/Layout/DialogLayout.swift:75` (従来から `DialogOptions()` から引く) | `android/.../DialogAttributeDefaultsTests.kt:85` (負と非有限値が 0)、`android/.../DialogAttributeSupplyTests.kt:136` (x=60)、`ios/Tests/KsDialogsTests/DialogAttributeSupplyTests.swift:260` (x=60) | ✅ 一致 |

### dialog-contract / Requirement: 配置属性と ToastStyle (MODIFIED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| (Requirement 本文) デフォルト View は余白全辺 24 を添付して持つ | `ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift:21,56`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDefaultContentView.kt:48,138` (View 自身の init で添付) | TS-AT-04〜06 (下記) | ⚠️ deviation 記録済み (添付箇所を tasks の Builtin の分岐から View の init に移した。結果は同じ) |
| [TS-AT-01] placement 引数で配置が変わる | 既存 | `ios/.../ToastAttributeTests.swift:30` (期待値をデフォルト View の余白 24 に合わせて改訂)、`android/.../ToastAttributeTests.kt:47` | ✅ 一致 |
| [TS-AT-02] 優先順は show 引数 > 添付 > style 既定 > 契約既定 | 既存 | `ios/.../ToastAttributeTests.swift:58` (カスタム View の余白 0 に合わせて改訂)、`android/.../ToastAttributeTests.kt:63` | ✅ 一致 |
| [TS-AT-03] style の変更は次の表示から効く | 既存 | `ios/.../ToastAttributeTests.swift:120`、`android/.../ToastAttributeTests.kt:100` | ✅ 一致 |
| [TS-AT-04] デフォルト View は契約既定配置で余白 24 の内側に置かれる | 同上 (デフォルト View の添付) | `ios/.../ToastAttributeTests.swift:141`、`android/.../ToastAttributeTests.kt:126` | ✅ 一致 |
| [TS-AT-05] 長いメッセージでもデフォルト View の左右に余白 24 が残る | 同上 | `ios/.../ToastAttributeTests.swift:158` (許容差「1 字幅の半分 + 1」)、`android/.../ToastAttributeTests.kt:147` (測定ハーネスで作った `ToastDefaultContentView`、ちょうど 24 ± 1dp) | ⚠️ deviation 記録済み (iOS は読み替えの許容差で判定。Android は show(message) ではなく測定ハーネス経由で、添付は本番と同じ View の init で行われる — deviation の 1 件目と tasks 4.5 の指示どおり) |
| [TS-AT-06] 配置を渡してもデフォルト View の余白は保たれる | 同上 | `ios/.../ToastAttributeTests.swift:190`、`android/.../ToastAttributeTests.kt:180` (show 引数と ToastStyle のアプリ既定配置の両方) | ✅ 一致 |
| [TS-AT-07] 何も添付しないカスタム View は余白 0 で契約既定配置に置かれる | 契約既定値の差し替え + `core/layout-spec/cases.json` C23 (60, 510) | `ios/.../ToastAttributeTests.swift:221`、`android/.../ToastAttributeTests.kt:216`、ケース表 C23 の Toast 器テスト (`ToastLayoutCaseTableTests` 両 Native) | ✅ 一致 |

### samples / Requirement: Layout Dialog のパネルで余白を選べる (ADDED)

| Scenario | 実装 | テスト (Sample のため照合記録) | 状態 |
|---|---|---|---|
| 初期値のまま出すと基準領域の端に接する | 4 ルートの `SampleMarginChoice` (初期 `.zero` / `ZERO`)・`SampleLayoutPanelModel` / `SampleLayoutPanelState`・`LayoutDialogViewModel.dialogMargin`・`SampleDialogRegistration` の添付。MAUI は `SampleMarginSegmentsView` と `SampleLayoutPanelPage.xaml.cs:151-154` | `ui/verification/*-current-page-end-end-margin-0.png` 6 枚と `ui/brief.md` 照合結果の実測 (iOS 下端 791pt = タブバー上端 / Android 809dp) | ✅ 一致 |
| 余白を変えると次の表示から効く | 同上 (セグメントの選択が次の `Show` で ViewModel に載る) | `ui/verification/*-current-page-end-end-margin-24.png` 6 枚と brief の実測 (右・下とも 24 内側) | ✅ 一致 |
| タブを切り替えても余白は保たれる | Panel / Info の `Show` が同じ表示関数を通り、パネルの状態を共有する (Android `MainActivity.kt:363-369`、MAUI `SampleLayoutPanelPage.xaml.cs:146-154`、iOS / KMP iOS は `SampleLayoutPanelModel`、KMP Android は `MainActivity.kt:305`) | `ui/brief.md` 照合結果の実測 (Info タブの Show で左 24・上端はステータスバー下から 24) と `ui/verification/accessibility-*.txt` の back-to-panel 後の選択状態 | ✅ 一致 |
| (Requirement 本文) 文言・選択肢・読み上げの 4 ルート一致 | 各ルートの `SampleText` (`Margin` / `0` / `24` / `48`)、読み上げ名 `Margin <値>` | `ui/verification/accessibility-*.txt` 6 本 (選択肢の読み上げ名・Button の役割・OS 標準の選択状態) | ✅ 一致 |

## 追加検査

- [x] **tasks.md**: 5.4 以外はチェック済み。5.4 はオーナーの最終承認待ちで、撮影と照合の記録は `ui/verification/` と brief.md にある (呼び出し元の指示により指摘しない)。チェック済みのタスクは、すべて上の対応表の実装・テストと突き合わせられた。虚偽のチェックはない (2.1 / 2.2 は添付箇所を deviation で差し替えたうえで満たしている)
- [x] **逆流検査**: `git diff HEAD` で proposal.md / exploration.md / specs/ に差分がない。change 配下の変更は tasks.md のチェックと ui/brief.md の照合結果の追記、未追跡の deviation.md と ui/verification/ だけ
- [x] **未記録乖離**: なし。diff のうち Scenario に直接対応しない変更 (PlacementHost の余白定数を 0 に、currentPage 系テストの `DEFAULT_MARGIN_DP`、テスト支援の `visibleArea` / `createContentView` の追加) は tasks 4.3 / 4.5 に明記された作業の範囲内
- [x] **UI 変更**: brief.md に承認モック (mock-a、approved.png、2026-09-27) の記録と照合結果がある。合意済み妥協は 0 件。裁量の範囲の差 2 件 (セグメントの書体、未実行時の結果欄を出さないこと) が記録されている
- [x] **テストの実行** (レビュー側で実行):
  - ios: 321 tests。3 件は負荷時の時間切れで、差分外の Toast スイート。単独の再実行では 23/23 成功。変更したテストはすべて成功
  - android unit: 75 / 0 failures
  - android instrumented (API 35): ksdialogs-core 356 / 0 failures / 1 skipped。ksdialogs 52 件の 1 件目が起動直後の時間切れで失敗したが、同クラスの単独再実行で 7/7 成功
  - kmp: 85 + 81 / 0 failures
  - maui dotnet: 197 / 0 failures
  - maui bridge Android: 38 / 0 failures
  - maui bridge iOS: 9 / 0 failures
  - `scenario-id-coverage.py` (`--require-mirror` 含む): 未網羅なし

変更に起因する失敗はない。実行の詳細は review-001.md の表にある。

## 判定

全 Scenario が「✅ 一致」または「⚠️ deviation 記録済み」(TS-AT-05 と、デフォルト View の添付箇所)。虚偽のチェックと逆流はなく、テストも成功している。よって **VALID**。
