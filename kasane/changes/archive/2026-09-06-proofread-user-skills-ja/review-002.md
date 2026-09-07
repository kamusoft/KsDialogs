# レビュー結果: proofread-user-skills-ja (2 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

review-001 が挙げた 14 件 (Major 4 / Minor 10) はすべて解消されており、修正は指摘した箇所だけでなく ja / en の対を揃えて入っている。Major 3 件 (KMP のレジストリ表・iOS Toast の `viewFactoryTypeMismatch`・`DialogAlignment` の物理方向) は実装 (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogAlignment.kt`、`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift`、`ios/Sources/KsDialogs/Kmp/*.swift`) に照合して正しくなったことを確認した。オーナー判断へ送られた Suggestion (KMP Skill への `DialogError` 掲載) は本 change のスコープ外として扱い、判定には含めていない。

機械検査 6 本 + `api-coverage-check.py` はすべて再走して差分ゼロ (api-coverage の報告 19 行は review-001 時点と同一)。加えてコードブロック内のコメント有無 (0 件) と表の列数整合 (0 件の破れ) を独自に検査し、修正で新たな構造の崩れが入っていないことを確認した。

新規指摘は Minor 2 件・Suggestion 1 件で、いずれも利用者の API 利用判断を誤らせる性質ではない。Critical / Major が無いため APPROVED とする。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` (適用のきっかけ: `skills/**` を更新するとき) — 「簡潔でも網羅」方針、現行除外リスト、コード例のノーコメント規約、ja / en コードブロックの byte 一致。修正後も 4 節すべてに適合
- `kasane/handbook/cross/comment-policy.md` (always) — `kasane/config.yaml` の `lint.comment-policy.exclude` に `skills` があり対象外。コード例のコメント 0 件は上記規約側で確認
- `kasane/handbook/cross/test-execution.md` — 本 change はソース・テストを変更しない (`git status` 上の変更は `skills/` のみ、`kasane/decisions/` と他 change の差分は別セッション由来で対象外)。代わりに docs-refresh の検査群と標準 lint を実行
- `kasane/concepts/core/api/layout-semantics.md`、`kasane/concepts/kmp/api/{loading,toast}-surface.md` — 修正内容の裏取り
- `kasane/lessons/code-review.md` は存在しないため、重点観点・指摘しないことの参照なし

## 実行した検査 (すべて成功)

| 検査 | 結果 |
|---|---|
| `.agents/skills/docs-refresh/scripts/code-block-parity-check.py` | code blocks byte-identical |
| `.agents/skills/docs-refresh/scripts/heading-parity-check.py` | en/ja heading structure OK |
| `.agents/skills/docs-refresh/scripts/link-resolution-check.py` (`DOCS_REFRESH_TARGETS` = `find skills -name '*.md'` の 68 件) | All internal links resolve |
| `.agents/skills/docs-refresh/scripts/frontmatter-check.py` | frontmatter OK |
| `.agents/skills/docs-refresh/scripts/api-coverage-check.py` | exit 0。報告 19 行は review-001 と同一 (`DialogError` 1 件のみ除外リスト外 = オーナー判断待ち) |
| `scripts/local-path-lint.py --paths skills` / `scripts/identity-lint.py --paths skills` | exit 0 |
| コードブロック内コメント検査 (本レビューで実施) | 変更 65 ファイルで 0 件 |
| Markdown 表の列数整合検査 (本レビューで実施) | 変更 65 ファイルで破れ 0 件 |

## review-001 の指摘の解消状況

| # | 重要度 | 指摘 | 状況 | 確認内容 |
|---|---|---|---|---|
| 1 | 🟠 Major | KMP の登録表が共有面に無い `Loading.instance.registry` を指す | **解消** | `skills/ja/ksdialogs-kmp/references/view-models.md:74-79` が列を「Android の登録 (Android Native)」/「iOS の登録」に分け、直前の地の文で「登録の API は host 側のもので、共有コードには無い」と明示。`Loading.instance.registry` は Android Native に実在 (`android/.../Loading.kt:24`、`Toast.kt:21`)、`registerCompose` は `android/ksdialogs-compose/.../Compose*Registration.kt` の extension、iOS の `Dialog.shared.kmp.register` は `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift:13` で確認。`loading.md:203` の地の文との矛盾も解消。en 同行も追随 |
| 2 | 🟠 Major | iOS Toast の構成ミス表に catch できない case が混在 | **解消** | `skills/ja/ksdialogs-ios/references/toast.md:236-238` は `viewFactoryNotRegistered` 1 行のみになり、`:234` に「型消去をまたぐ受け渡し (KMP の入口) での不整合 (`viewFactoryTypeMismatch`) もこの経路になり、警告ログが出るだけで `show` からは catch できない」を統合。en `:234-238` も同形 |
| 3 | 🟠 Major | `DialogAlignment.Start` / `End` の物理方向が 1 platform にしか無い | **解消** | maui `:17` / ios `:19` / android `:33` / kmp `:23` の 4 platform ja + en すべてに同文が入り、いずれも配置表の直後に置かれている。実装 (`DialogAlignment.kt:6`) と `concepts/core/api/layout-semantics.md:168` に一致。対になる `DialogTransitionEdge` が逆規則であること (`DialogTransitionEdge.kt:6`) も確認済み |
| 4 | 🟠 Major | 「ダイアログ」→「Dialog」が MAUI ja で未徹底 | **解消** | `grep -rn ダイアログ skills/ja/` の残存 6 件はすべてバッククォート内の実装リテラル引用 (`ダイアログを提示できる画面がありません。` ほか)。地の文・見出しの残存は 0 |
| 5 | 🟡 Minor | 却下語「scrim」が ja に 3 箇所残存 | **解消** | `grep -rn scrim skills/` で 0 件 |
| 6 | 🟡 Minor | 「器」/「host」の併存 (MAUI) | **解消** | MAUI ja の提示コンテナは 6 箇所すべて「器」に統一 (`dialogs.md:5`、`:97`、`loading.md:9`、`:178`、`toast.md:13`、`:237`、`transitions.md:139`)。en は "container"。`di-registration.md:125` / en `:125` の "host" は DI ホストを指す別語で、この統一の対象外 |
| 7 | 🟡 Minor | ja SKILL.md の見出しが platform 間で不揃い | **解消** | 4 platform ja がすべて `## 能力マップ` / `## セットアップ` / `## 最小例` / `## レシピを選ぶ`。en は `Capability map` / `Setup` / `Minimal example` / `Choose a recipe` で対応 |
| 8 | 🟡 Minor | iOS `configure` サンプルの暗黙 `self` | **解消** | `skills/{ja,en}/ksdialogs-ios/references/view-models.md:132` が `try await self.loadCurrentName()`。code-block-parity 再走で byte 一致を確認 |
| 9 | 🟡 Minor | iOS 選択表が factory の `throws` を落としている | **解消** | `skills/ja/ksdialogs-ios/references/dialogs.md:26` に「表では省いているが、登録した factory もインライン factory も `throws` にできる。factory が投げた失敗はそのまま `show` の失敗になり、Dialog は提示されない (Loading も同じで、Toast だけは呼び出し元へ返らない)」を追加。実装で裏取り済み — Dialog は `ios/Sources/KsDialogs/Presentation/DialogPresenter.swift:60` の `try factory.makeContent(...)`、Loading は `LoadingCoordinator.swift:123` の `try makeContent(...)` が伝播、Toast は受理後の破棄。en `:26` も同旨 |
| 10 | 🟡 Minor | Android の記述漏れ 4 件 | **解消 (4/4)** | (a) `toast.md:24` に 3 段の duration fallback と `BUILTIN_DEFAULT_DURATION` (1500) を追記 — `ToastCoordinator.kt:323-334` に一致 (ただし後述の新規 Minor あり)。(b) 同行に契約既定配置「可視領域の下部中央から上方向へ論理単位 80」を追記 — `ToastPlacementDefault.kt` の `BOTTOM_BAR_CLEARANCE = 80.0` に一致。(c) `dialogs.md:275` を「対象の ViewModel 型名」に補正。(d) `loading.md:236` / `toast.md:180` の `ValueClassViewModel` に「この検査は表示の時点だけでなく `register` / `registerCompose` の時点でも走る」を追記。en 各同行も追随 |
| 11 | 🟡 Minor | en の意味ずれ 3 件 (KMP) | **解消 (3/3)** | `SKILL.md:40` が "The current artifact is built with…" (記述に修正)、`ios-host.md:93` が "so a type mismatch leaves this button unable to report"、`transitions.md:124` が "The routes that run the exit transition — every closing route…"。いずれも推奨修正どおり |
| 12 | 🟡 Minor | 宣言のない型がサンプルに登場 | **解消** | maui `dialogs.md:29` に「以降の節に現れる `NoticeDialogViewModel` / `NoticeDialogView` のように、同じ形で利用者が書く型もある」、android `dialogs.md:173` に「`ItemEditContent` は `ConfirmContent` と同じ形で利用者が書く composable である」。en 同行も追随 |
| 13 | 🟡 Minor | 多段表示の記述から契約側の限定が落ちている | **解消** | android `dialogs.md:256` / maui `dialogs.md:261` に「アプリ側が下の報告口を保持して先に報告した場合にだけ起きる — ユーザー操作 (完了・キャンセル・外側タップ・戻るボタン) は常に手前の 1 枚にしか届かない」。`concepts/core/api/multi-display-semantics.md` の PB-MD-04 に一致。en 同行も追随 |
| 14 | 🟡 Minor | KMP loading.md の iOS 追加メッセージ欠落 | **解消** | `skills/{ja,en}/ksdialogs-kmp/references/loading.md:151` に `ローディングの表示に失敗しました。` を追記。`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosLoadingGateway.kt:141` のリテラルと逐語一致 |
| S1 | 🔵 Suggestion | ios-host.md だけ です・ます調 | **解消** | `skills/ja/ksdialogs-kmp/references/ios-host.md` の です / ます は 0 件。ja 32 ファイルで残る「ます。/ ません。」はすべて例外メッセージのリテラル引用 |
| S2 | 🔵 Suggestion | MAUI の `var` / `UseMauiApp` の揺れ | **解消** | `di-registration.md:75-76` が `var builder = MauiApp.CreateBuilder();` + `builder.UseMauiApp<App>();`、`:141` が `var viewType = …`。ja / en 同時修正で byte 一致維持 |
| S3 | 🔵 Suggestion | KMP Skill への `DialogError` 掲載 | **スコープ外** | オーナー判断へ送付済み。`api-coverage-check.py` は引き続き `ksdialogs-kmp <- kmp/api/ios-host-integration.md: DialogError` を報告するが、本 change の判定には含めない |

解消 14 / 14 (Major 4、Minor 10)、Suggestion 2 / 3 解消 + 1 件スコープ外。

## 新規の指摘事項

### [🟡 Minor] Android toast.md の警告ログの条件が実装より広い

**該当箇所**: `skills/ja/ksdialogs-android/references/toast.md:24`、`skills/en/ksdialogs-android/references/toast.md:24`

**問題点**: ja は「省略するか 0 以下を渡すと `ToastStyle.defaultDuration` を使い、それも 0 以下なら `ToastStyle.BUILTIN_DEFAULT_DURATION` (1500) へ丸める (いずれの丸めも警告ログを残す)」、en は "…Both fallbacks leave a warning in the log." と書く。実装 (`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastCoordinator.kt:323-334`) の `effectiveDuration` は、`duration` が `null` (= 省略) の場合は何も記録せずに `style.defaultDuration` へ落ち、警告を出すのは (a) `duration != null` かつ `duration <= 0`、(b) `style.defaultDuration <= 0` の 2 経路だけである。「省略」を fallback の一種として並べたうえで「いずれの丸めも警告」と締めているため、`durationMs` を省略しただけで警告ログが出ると読める。en の "Both fallbacks" は ja より一段強く同じ誤解を生む。

review-001 が比較対象とした `skills/ja/ksdialogs-maui/references/toast.md:9` の 3 段記述と揃えるための追記なので、方向は正しく、条件だけがずれている。

**推奨修正**: 「0 以下を渡した場合と、`defaultDuration` 自体が 0 以下だった場合に警告ログを残す (省略しただけなら記録しない)」の形にする。en も "A value of 0 or less, and a `defaultDuration` of 0 or less, each leave a warning in the log." のように条件を明示する。

### [🟡 Minor] summary.md の「触ったファイル」が実際の diff と食い違う

**該当箇所**: `summary.md`「触ったファイル」節 (change 相対)

**問題点**: summary は「`skills/ja/` と `skills/en/` の各 32 ファイル (計 64)」と書き、「触っていないもの」に「kmp の android-host.md」を挙げている。しかし実際の `git diff --stat -- skills` は 65 ファイルで、`skills/ja/ksdialogs-kmp/references/android-host.md:147` が「ダイアログは退出の演出と撤去を最後まで完遂する」→「Dialog は退出の演出と撤去を最後まで完遂する」に変わっている。

変更内容そのものは本 change の決定事項 (「プロダクト機能名は Dialog と表記する」) の正しい適用であり、指摘 #4 の修正の一環として妥当。問題は、合意済みスコープを記述する summary が更新されず、明示的に「触っていない」と書いたファイルを触っている点で、蒸留・アーカイブ時にそのまま記録として残る。

**推奨修正**: summary の「触ったファイル」を 65 ファイルに直し、`ksdialogs-kmp/references/android-host.md` (ja のみ) を触ったファイル側へ移す。あわせて「検査」節に `api-coverage-check.py` を実行済み検査として加えると、review-001 の指摘 (未実行) の記録とも整合する。

### [🔵 Suggestion] ja android-host.md の表記統一が en に及んでいない

**該当箇所**: `skills/ja/ksdialogs-kmp/references/android-host.md:147` と `skills/en/ksdialogs-kmp/references/android-host.md:147`

**問題点**: ja は「Dialog は退出の演出と撤去を最後まで完遂する」に直ったが、en は "the dialog still finishes its exit animation and removal" のまま小文字の一般名詞で残っている。en 側には他にも小文字 `dialog` が複数あり (`skills/en/ksdialogs-aiforms-migration/references/api-mapping.md`、`skills/en/ksdialogs-kmp/SKILL.md:77`、同 `android-host.md:133`)、summary の en 同期方針も英語側の大文字化までは決めていないため、本 change が作った不整合ではない。ただし ja だけを直した結果、同じ 1 文で ja / en の扱いが分かれた形にはなっている。

**推奨修正**: en の表記方針 (プロダクト機能名を大文字にするか一般名詞のままにするか) を決めて別 change で一括処理する。本 change での対応は不要。

## アクションプラン

1. 新規 Minor 1 件 (`skills/{ja,en}/ksdialogs-android/references/toast.md:24` の警告ログ条件) を直す。ja / en を同時に直し、`code-block-parity-check.py` と `heading-parity-check.py` を再走する — コードブロック外の地の文なので影響は無い見込み
2. `summary.md` の「触ったファイル」を 65 ファイルへ更新し、`api-coverage-check.py` を検査リストへ追加する
3. Suggestion の en 表記方針は別 change / オーナー判断へ送る。`DialogError` の掲載可否 (review-001 の Suggestion) も引き続きオーナー判断待ちとして蒸留時に引き継ぐ

1 と 2 は APPROVED の妨げにならない。実施せずに蒸留へ進んでも、指摘は本ファイルに記録として残る。
