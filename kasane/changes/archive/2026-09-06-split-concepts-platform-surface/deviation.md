# Deviation: split-concepts-platform-surface

実装フェーズ中に発生した合意済みの差分・付随修正・契約と実装の乖離の記録 (spec 本体は書き換えない)。

## 付随修正

- [付随修正] `core/api/multi-display-semantics.md` / `core/api/result-notification-semantics.md`: 調査記録へのリンクが archive 前のパス `roadmaps/library-foundation/...` を指して切れていたため、archive 後の `kasane/roadmaps/archive/2026-09-04-library-foundation/...` へ付け替えた (ksn-core paths.md の archive 解決規則。本文の主張は不変) (2026-09-05)

- [付随修正] `core/api/layout-semantics.md`: 出典リンクが archive 前のパス `roadmaps/library-foundation/...` を指して切れていたため、`kasane/roadmaps/archive/2026-09-04-library-foundation/...` へ付け替えた (上と同じ修正。本文の主張は不変) (2026-09-05)
- [付随修正] `{ios,android,maui}/api/dialog-surface.md` / `kmp/api/dialog-surface.md`: 巡の進行に伴い「レイアウト公開面は未作成」の注記を削除・縮小 (リンク解決の事実追随のみ) (2026-09-05)

- [付随修正] `{ios,android,maui}/api/{dialog,layout}-surface.md` / `kmp/api/dialog-surface.md`: transition-surface 新設に伴い「トランジションの公開面は未作成」の注記を削除 (リンク解決の事実追随のみ) (2026-09-05)

- [付随修正] `maui/api/di-registration.md`: 「関連」節に新設 surface へのリンク 5 本を足すと構造 lint の項目数上限に触れるため、節を h3 二つ (MAUI の公開面 / core の契約と決定記録) に割った。既存項目の内容は不変 (2026-09-05)

- [付随修正] `core/api/registration-show-semantics.md`: 3.4 の付け替え (共通ケース表の帰属を `core/architecture/layout-case-table.md` へ) で箇条書きが構造 lint の字数上限に触れたため、同じ文意のまま短縮 (2026-09-05)
- [付随修正] `core/api/transition-semantics.md` (slide の辺の項目) / `core/api/layout-semantics.md` (出典行): 書き直し・archive パス化で構造 lint の字数上限に触れた項目を、リンクの段落分離・リンクラベルの短縮で是正 (主張・URL は不変) (2026-09-05)

## 契約と実装の乖離 (書き直し中に発見。蒸留送り)

- core/api/layout-semantics.md:基準領域 (LayoutArea) / 器が持つメタ属性: 記述 `LayoutArea` (節見出しと静的メタ表の型欄) → 実装は 4 形態とも `DialogLayoutArea` (`ios/Sources/KsDialogs/Contract/DialogLayoutArea.swift`・`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayoutArea.kt`・`maui/KsDialogs.Maui/Contract/DialogLayoutArea.cs`。KMP commonMain には無い)。core からは型名を外して散文にし、綴りは各 platform の layout-surface に置いた (2026-09-05)
- core/api/layout-semantics.md:器が持つメタ属性 (DialogOptions 静的メタ表): 記述 「overlayColor の型は Swift = UIColor / Kotlin = ColorInt / MAUI = Maui.Color」 → 実装は Kotlin が `@ColorInt` 注釈つきの `Int` (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogOptions.kt`)、MAUI が `Microsoft.Maui.Graphics.Color`(`maui/KsDialogs.Maui/Internals/DialogOptions.cs`) で、`ColorInt` / `Maui.Color` はどちらも型名そのものではない。core からは型欄を外し、各 platform の layout-surface に実装の綴りで書いた (2026-09-05)

- core/api/transition-semantics.md:プリセット: 記述 「`leading` / `trailing` (Android・MAUI は `START` / `END`)」 → 実装は Android が `START` / `END` (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionEdge.kt`)、MAUI は `Start` / `End` の PascalCase (`maui/KsDialogs.Maui/Contract/DialogTransitionEdge.cs`) で、MAUI が Android と同綴りだという記述は誤り。core からは値の綴りを外して「上 / 下 / 行の始まり側 / 行の終わり側」の散文にし、綴りは各 platform の transition-surface に実装どおり置いた (2026-09-05)

- core/api/loading-semantics.md:公開面 (操作表のスコープ形): 記述 `start(message:placement:action:)` → 実装 (Swift) は `start(message:placement:_:)` で処理の引数は無ラベルである (`ios/Sources/KsDialogs/Presentation/KsLoading.swift`)。他形態はそもそも別綴り (Kotlin / KMP は `start(message, placement, action)`、MAUI は `StartAsync(action, message, placement)` で引数順も違う)。core からは署名を外して「スコープ形 (start)」の散文にし、綴りは各 platform の loading-surface に実装どおり置いた (2026-09-05)
- core/api/toast-semantics.md:公開面 / duration の時間モデル: 記述 `show(message, duration?, placement?)` (引数名 duration) → 実装は Android (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsToast.kt`)・MAUI (`maui/KsDialogs.Maui/Presentation/IKsToast.cs`)・KMP commonMain (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt`) が `durationMs`、iOS だけ `duration` (`ios/Sources/KsDialogs/Presentation/KsToast.swift`)。core からは引数名を外して散文にし、綴りは各 platform の toast-surface に実装どおり置いた (2026-09-05)

## 検証 fixture の解釈差分 (task 6.1、2026-09-06)

- user-skills-manifest「Skill 本文への負の検査」/「検査候補への負の検査」: spec では禁止集合の初期値を「分割前の除外リストで基準が『対象 Skill 外・機械検査由来』の行の名前」+「他 platform の surface concept の識別子のうち自 platform の公開面に無いもの」と定める → 実装 (task 4.3) は後者の判定を concept のバッククォート span だけで行ったため、自 platform の実在名・見本名・文字列リテラルの語が ios / android / maui / aiforms-migration の 4 集合に混入し、全文突き合わせ (baseline の定め) で 44 件の偽陽性が出た。オーケストレーターの裁定で、kmp 集合に修正サイクル 2〜3 で確定した規則 (実装の到達可能性で外す / handbook のオーナー確定行は優先) を 4 集合にも適用して是正した (ios 200→192 / android 178→171 / maui 154→142 / aiforms 151→144。一覧と出典は `verification/baseline.md` 末尾)。理由: Requirement 本文「自 platform の公開面に無いもの」に照らすと混入は実装誤りで、spec の逸脱ではない。是正後の負の検査は 5 範囲とも 0 件
- 同 Requirement (禁止集合の初期値): spec では初期値に旧除外リスト由来の行を含める → 指示により maui 集合から移植元 AiForms.Maui.Dialogs の旧 API 名 3 件 (`SetIocConfig` / `ShowResultAsync` / `UseCurrentPageLocation`) を外した。理由: 他 platform 名ではなく移植元名で、`maui/api/*.md` の「移植元との対応」節に載る限り api-coverage-check の候補に出続けるため「検査候補への負の検査: 一致 0 件」と両立しない。掲載しない判断自体は書き直し後の除外リストに「非 API token」として引き継いだ
- 同 (fixture の判断案件): ios 集合の `Show` / `Register` (MAUI の公開名) は、iOS Skill のコード例の文字列リテラル (`"Show toast"` 等) の語に当たったため ios 集合から外した。複合形 (`ShowAsync` / `RegisterForDialog` 等) は残る。代替案 (2 件を戻して抽出から文字列リテラルを除外) は `verification/baseline.md` 末尾に併記。最終レビューの判断材料

## 契約と実装の乖離 (Skill 再生成 (task 5.2) のワーカー報告から。蒸留送り、concepts は直していない)

- ios/api/dialog-surface.md:DialogError 表: 記述 4 case (`viewFactoryNotRegistered` / `presentationHostUnavailable` / `viewModelFactoryNotRegistered` / `viewModelAlreadyShowing`) → 実装 `ios/Sources/KsDialogs/Contract/DialogError.swift` は public 7 case (`viewFactoryTypeMismatch` / `resultTypeMismatch` / `viewModelFactoryTypeMismatch` が未記載)。意図的な非掲載か記載漏れかは concept 側の判断 (2026-09-06)
- ios/api/loading-surface.md / toast-surface.md:未登録 ViewModel の失敗: 記述は「構成ミスとして失敗」「解決の失敗」で型名なし → 実装は両方 `DialogError.viewFactoryNotRegistered` を throw (`Presentation/LoadingCoordinator.swift:301`・`ToastCoordinator.swift:296`)。Skill には実装の事実を書いた (2026-09-06)
- core/api/loading-semantics.md:styling 既定「白 14pt bold」: bold は iOS / Android とも内蔵 View 固定で `LoadingStyle` に weight の項目は無い (`ios/Sources/KsDialogs/Presentation/LoadingDefaultContentView.swift:40`)。core の記述が利用者の設定項目と読める軽微な齟齬 (2026-09-06)
- android/api/{dialog,loading,toast}-surface.md:registerCompose: `@Composable` 関数への関数参照 (`::ConfirmContent`) は Compose コンパイラが拒否するためラムダ形式のみ成立 (実利用 `android/` 配下は全件ラムダ形式)。契約に注意が未記載 (契約の穴)。旧 Skill の関数参照 3 箇所はラムダ形式へ修正した (2026-09-06)
- maui/api/transition-surface.md:58 コード例: 記述 `await view.FadeTo(1d, 200u)` → 実装 (`maui/KsDialogs.Maui/Contract/DialogTransition.cs`・ApiSurfaceCheck) は Microsoft.Maui.Controls 10.0.1 の `FadeToAsync` 系。Skill は `FadeToAsync` で書いた (2026-09-06)
- maui/api/layout-surface.md:添付プロパティ表 `Dialog.OverlayColor` の型欄: 記述 `Color` → 実装は `Color?` (`maui/KsDialogs.Maui/Presentation/DialogAttachedProperties.cs`・`Internals/DialogOptions.cs`)。同 concept の散文は null 依存なので型欄が古い側 (2026-09-06)
- maui/KsDialogs.Maui/Internals/DialogOptions.cs: public 契約型 `DialogOptions` (layout-surface / loading-surface が公開型として扱う) が `Internals/` 配下に置かれている (他の public 契約型は `Contract/`)。可視性は正しくファイル配置だけの齟齬 (2026-09-06)
- kmp/api/ios-host-integration.md:Swift 側で登録するもの: 表は Loading / Toast を「登録するもの」だけで `show` は Dialog のみ → 実装は `KsLoadingKmp.show(_:placement:)` (`ios/Sources/KsDialogs/Kmp/KsLoadingKmp.swift:71`) と `KsToastKmp.show(_:duration:placement:)` (`KsToastKmp.swift:69`) が public。未規定 (2026-09-06)
- kmp/api/toast-surface.md / ios-host-integration.md:Toast の duration 引数名: commonMain `durationMs` に対し Swift の KMP 入口は `duration:` で、境界での対応が未記載 (2026-09-06)
- kmp/api/ios-host-integration.md:KMP 利用者の iOS 側 styling への導線: `kmp/api/{loading,toast}-surface.md` は `ios/api/` に委譲するが、ios-host-integration.md から `Loading.shared.style` / `Toast.shared.style` の入口を辿れない。KMP Skill の iOS 側 styling レシピは Android 側より薄いまま (2026-09-06)
- kmp/api/dialog-surface.md:DialogException: commonMain の `DialogException` は internal constructor (`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogException.kt`) で利用者は catch できるが構築できない (テストダブルを書けない)。未記載 (2026-09-06)
- core/api/loading-semantics.md (MAUI Skill 由来の 2 主張): 「`Loading.ShowAsync` は入力遮断が有効になった時点で戻り入場演出を待たない」「合流の非最終参加者は撤去を待たずに戻る」は contract に無い (矛盾もしない)。旧 Skill の記述を残した。契約の明確化か Skill からの除去の候補 (2026-09-06)

## 最終レビュー・verify で確定した差分 (2026-09-06)

- tasks 7.1 (cross/ADR-0014 の Decision 節の「確認」): 確認にとどめず本文を直接改訂した — design Decision 1 の「core 契約への冒頭宣言と相互リンク」、Decision 2 の候補列挙 (`KsDialogs` 追加、`DialogException` / `LayoutArea` を確定一覧に合わせて除き理由を併記)、KMP の公開面 = 3 側の定義 (rules.md と同じ言い方)、Decision 3 の実配置ファイル名と manifest `excluded` の記載、Context のパスをリポジトリ相対に。理由: 引き継ぎメモの申し送り (3 側の定義が ADR にも要るか確認) と、proposed の ADR は本文を直接改訂するオーナー規約。status は proposed のまま
- [付随修正] `.agents/skills/docs-refresh/SKILL.md`「移行 Skill の源泉規則」: 旧記述「`core/api/*` + `maui/api/di-registration.md` だけ」が新 manifest (`maui/api/*-surface.md` 6 本を源泉に持つ) と食い違っていたため、MAUI 公開面 concept を含む記述へ追随 (verify-001 の観察事項)
- [付随修正] `second-opinion-code-001/002/003/005.md`: 相方 CLI 応答の逐語記録に含まれていた行末空白を落とした (`git diff --check` を通すため。tasks 7.2)。本文の語句は不変
- tasks 7.2 の doc-structure lint: 引き継ぎメモの「違反なし」は zsh の未クォート変数展開 (単語分割されず 1 引数になる) による偽陽性だった。`find -print0 | xargs -0` で全ファイルを渡した正しい結果は、concepts 配下で既存本文由来の残存 11 件 (`maui/api/di-registration.md` 10・`cross/reference/reference-repositories.md` 1、review-002 / log.md に記録済み、本 change では触らない) のみで、本 change が新設・書き直したファイルの違反は 0 件 (`core/api/layout-semantics.md` / `transition-semantics.md` は baseline の計 16 件 → 0 件)。ファイル散文字数の注意 (違反ではない) が同 2 本に残る
- task 6.2 の仕分け (暫定確定 4 名): 実装側は `Center` / `Fill` / `Top` / `ToastStyle.BuiltinBackgroundColor` (AiForms migration) を「機械的に導出できる名前」として除外 → 指示により掲載に倒した (`ksdialogs-aiforms-migration/references/api-mapping.md` の散文に en/ja 同一構成で追記、除外リスト 22 行 → 20 行)。理由: 列挙子の一部 (`End` / `Bottom`) と兄弟の `BuiltinDefaultDuration` が既に載っており、揃えたほうが利用者に読める (2026-09-06)
