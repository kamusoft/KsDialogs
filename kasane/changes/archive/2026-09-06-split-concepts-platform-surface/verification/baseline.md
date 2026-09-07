# baseline (着手時点の採取)

## 着手時点

| 項目 | 値 |
|---|---|
| コミット SHA | `218d5c09bc7aa9c6bb57c6243ac8e9b2be959745` |
| 採取日 | 2026-09-05 |
| 対象 | `kasane/concepts/core/api/*.md` 8 本 |

採取対象の 8 本 (この SHA 時点の内容が台帳・差分一覧の出典):

| concept | 主な内容 |
|---|---|
| `kasane/concepts/core/api/registration-show-semantics.md` | 登録と show、真偽値の省略形、技術別の呼び分け、宣言的 UI での添付、インライン show |
| `kasane/concepts/core/api/result-notification-semantics.md` | 結果型の宣言、報告口、呼び出し元キャンセルの観察、形態ごとの形 |
| `kasane/concepts/core/api/multi-display-semantics.md` | 多段表示の重なりと閉鎖の順序 |
| `kasane/concepts/core/api/model-binding-semantics.md` | notifier の VM 供給、参照型限定、VM factory による解決、KMP での見え方 |
| `kasane/concepts/core/api/layout-semantics.md` | 器のメタ属性、属性の渡し方と優先順位、基準領域、最終 rect の決め方、共通ケース表と OS 差の統制 |
| `kasane/concepts/core/api/transition-semantics.md` | 演出の添付、フックの形、既定の演出とオーバーレイ、プリセット、呼び出し元キャンセルの観察 |
| `kasane/concepts/core/api/loading-semantics.md` | 公開面、合流のルール、器の性質、既定ローディングと styling、カスタム View 版 |
| `kasane/concepts/core/api/toast-semantics.md` | 公開面、表示の重なりと duration、配置と ToastStyle、デフォルト View とカスタム View |

## この採取で作ったもの

| ファイル | 役割 |
|---|---|
| `identifier-landing.py` | 台帳の生成 (`collect`) と、再構成後の突き合わせ (`check`) の両モードを持つ検査スクリプト |
| `identifier-ledger.md` | 識別子 108 件 × 出典 × 期待する移動先 × 判定の着地台帳 |
| `platform-differences.md` | core に書かれている platform 差分の挙動の一覧 (出典つき) |
| `forbidden-tokens.json` | Skill 範囲ごとの禁止トークン集合 (他 platform の識別子に対する負の検査の fixture) |

## 識別子の抽出規則

API 名網羅検査 (`.agents/skills/docs-refresh/scripts/api-coverage-check.py`) と同じ正規表現
`` `([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)` `` でバッククォート括りのトークンを拾い、同じ STOP 語だけを除く。
数値始まりのトークンは正規表現の時点で外れる。網羅検査が持つ絞り込み (`looks_api`: 3 文字未満・
大文字を含まない名前を落とす) は**掛けない** — `show` / `hide` / `cancelled` / `leading` / `trailing` の
ような小文字の公開名を台帳から落とさないため。この違いにより、台帳には `C` / `R` / `AA` のような
API 名ではないトークンも入る (それぞれ備考で性格を書いてある)。

台帳の見方と判定の埋め方は `identifier-ledger.md` の冒頭にある。

## 禁止トークン集合 (`forbidden-tokens.json`) の構造と足し方

JSON にコメントを書けないため、構造の説明はここに置く。ファイルは Skill 範囲名
(`ios` / `android` / `maui` / `kmp` / `aiforms-migration`) をキーに、値が
`{ "source": <集合の由来>, "tokens": { <トークン>: <その Skill 範囲で禁止する理由> },
"concept-exempt": [<トークン>] }` という形のオブジェクトになっている。`source` の値は集合の由来を
表す区分で、初期値はすべて `current-exclusion-list` (再構成前の
`kasane/handbook/cross/user-skill-api-listing.md` の現行除外リストのうち、基準が
「対象 Skill 外・機械検査由来」の 71 行から拾った名前) である。初期値の件数は
ios 41 / android 32 / maui 35 / kmp 29 / aiforms-migration 32。後の巡で
「新設した他 platform の surface concept にあって自 platform の公開面には無い識別子」を足すときは、
同じ Skill 範囲の `tokens` にトークンと理由を追記し、`source` を
`current-exclusion-list + new-surface` に変える (由来が混ざったことが読めるようにする)。トークンを
消してよいのは、その名前が実は自 platform の公開面にあると実装コードで確認できた場合だけで、
そのときは削除の根拠を実装報告に書く。検査は 2 面 — 自 platform の concept (`<p>/api/*.md`) と、
再生成した Skill (en / ja) の両方に、その Skill 範囲の禁止トークンが出現しないことを見る
(`aiforms-migration` の Skill 範囲は自前の concept を持たず、源泉が `maui/api/` なので、
concept 側の検査は `maui/api/*.md` に対して行う)。

`concept-exempt` は、**Skill 本文にだけ禁止を効かせ、自 platform の concept では許すトークン**である。
初期値は `maui` の 3 件 (`SetIocConfig` / `ShowResultAsync` / `UseCurrentPageLocation`) だけで、
これは移植元 (AiForms) の API 名であって「他 platform の識別子」ではないため、MAUI の公開面 concept が
現行 API との対比として書くのは正当だと判断したことによる。task 4.3 で実物を確認し、3 件とも実際に
対比として使われていた — `maui/api/di-registration.md` の `SetIocConfig` (static 一括設定 API の粗を
構造ごと消した説明)、`maui/api/dialog-surface.md` の `ShowResultAsync` (動詞分裂の解消)、
`maui/api/layout-surface.md` の `UseCurrentPageLocation` (基準領域の真偽値プロパティからの置き換え)。
いずれも区分の趣旨どおりなので `concept-exempt` はそのまま維持した (整理・削除なし)。一方、利用者向け
MAUI Skill にこれらを載せないというオーナーの判断は現行除外リストのとおりなので、Skill 側の禁止は残す。
`aiforms-migration` の Skill 範囲は旧 AiForms の名前を対応表の左側として載せるため、これらを
そもそも禁止トークンに入れていない。

トークンの突き合わせは**完全一致**で行う (部分一致にしない)。`Register` のように短く、
別の識別子や英文の一部として現れうる名前が集合に入っているため、部分一致にすると
`RegisterForDialog` のような自 platform の正当な名前まで拾ってしまう。

非 API トークン (`A.min` / `C05` / `C19` / `approvedBy` / `approvedDiff`) も初期値に残してある。
これらは検証機構の記述として `core/architecture/layout-case-table.md` へ移り Skill の源泉から外れるので、
再構成後は出現しないはずのもの — 出現したら移動漏れを示す回帰検出になる。

## 追記: 新設 surface 由来の禁止トークンの追加 (task 4.3、2026-09-05)

分割で新設した公開面 concept の識別子から、上の足し方に従って各 Skill 範囲の `tokens` を増やし、
`source` をすべて `current-exclusion-list + new-surface` に変えた。足す対象の決め方は次のとおり:

| Skill 範囲 | 「自 platform の公開面」として扱った concept | 「他 platform の surface」として拾った concept |
|---|---|---|
| ios | `ios/api/*.md` | `android/api/` `maui/api/` `kmp/api/` |
| android | `android/api/*.md` | `ios/api/` `maui/api/` `kmp/api/` |
| maui | `maui/api/*.md` | `ios/api/` `android/api/` `kmp/api/` |
| kmp | `kmp/api/*.md` + `android/api/*.md` + `ios/api/layout-surface.md` + `ios/api/transition-surface.md` + **KMP 専用 Swift 入口から到達可能な名前** (下の再分類節に一覧) | `maui/api/` + `ios/api/` の残り (dialog / loading / toast-surface) |
| aiforms-migration | `maui/api/*.md` | `ios/api/` `android/api/` `kmp/api/` |

kmp の「自 platform の公開面」に android / ios を含めるのは、KMP Skill の源泉が
`references/android-host.md` で `android/api/` 5 本を、`references/layout.md` / `transitions.md` で
`ios/api/layout-surface.md` / `ios/api/transition-surface.md` を含むため (design.md Decision 6 の
`targets`)。そこを禁止すると Skill 側の検査が正当な記述を落としてしまう。**kmp の許可面は
「targets の和集合」+「KMP 専用 Swift 入口 (`ios/Sources/KsDialogs/Kmp/`) から到達可能な名前」の
2 本立てで定める** (後者は実装で 1 件ずつ確認し、下の再分類節に一覧を残す)。concept の
ディレクトリ単位では前者だけしか表せないが、KMP Skill の `references/ios-host.md` は
「KMP 利用者が Swift ホスト側で直接見る面」を書くので、その面の署名・戻り値・失敗型に現れる名前は
concept の置き場所にかかわらず正当な記述になる。逆に、`targets` が参照せず Swift 入口からも
到達しない `ios/api/dialog-surface.md` / `loading-surface.md` / `toast-surface.md` の識別子
(`Dialog.shared.registry` / `Loading.shared.registry` / `UIHostingController` など) まで許可面に
含めると、KMP Skill へ iOS Native 固有名が混入しても負の検査で検出できなくなる。

`verification/core-contract-check.md` で確定した**共通概念名 14 種は、どの Skill 範囲にも足していない**
— core/api を源泉に持つ以上すべての Skill 本文に現れる名前であり、禁止集合に入れると Skill 側の検査が
必ず落ちるため。

追加後の件数: ios 200 (+159) / android 178 (+146) / maui 154 (+119) / kmp 152 (+123) /
aiforms-migration 151 (+119)。concept 側の負の検査の結果は
`verification/forbidden-tokens-concepts.txt` にある (5 Skill 範囲すべて 0 件)。
**kmp の 152 件は修正サイクル 2 の再分類で 136 件になった** (下の再分類節。ほかの 4 範囲は変えていない)。

kmp の 152 件のうち 19 件は、修正サイクル 1 で許可面を `targets` の和集合へ絞り直したときに
足した iOS Native 固有の識別子である (`Dialog.shared` / `Dialog.shared.registry` /
`Loading.shared` / `Loading.shared.registry` / `Loading.shared.style` / `Toast.shared` /
`Toast.shared.registry` / `Toast.shared.style` / `DialogError` とその case 4 種
(`presentationHostUnavailable` / `viewFactoryNotRegistered` / `viewModelAlreadyShowing` /
`viewModelFactoryNotRegistered`) / `cancelled` / `LoadingStyle.ProgressFormat` /
`LoadingStyle.defaultProgressFormat` / `ToastStyle.builtinBackgroundColor` /
`ToastStyle.builtinDefaultDuration` / `UIHostingController`)。`ios/api/dialog-surface.md` /
`loading-surface.md` / `toast-surface.md` にしか無い識別子は機械的には 22 件だが、そのうち
Swift の言語・標準ライブラリの型 `AnyObject` / `Sendable` / `Result` の 3 件は足していない —
KMP の Swift 向け公開面の署名そのものに現れる制約 (`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift`
の `register<ViewModel: AnyObject, Result: Sendable>` など) で、禁止すると KMP Skill の
`references/ios-host.md` の正当な記述を落としてしまうためである。`Dialog.shared.kmp` のような
KMP 側の入口は別トークンなので、完全一致の突き合わせでは巻き添えにならない。

## 追記: kmp 禁止集合の再分類 (修正サイクル 2、2026-09-05)

修正サイクル 1 は kmp の許可面を design.md Decision 6 の `targets` の和集合 (= concept のパス) だけで
決めた。しかし KMP Skill の `references/ios-host.md` が書く面は「KMP 利用者が Swift ホスト側で直接見る
公開契約」であり、その名前は `kmp/api/ios-host-integration.md` に全部が載っているとは限らない。
そこで**許可面の定義に「KMP 専用 Swift 入口 (`ios/Sources/KsDialogs/Kmp/`) から到達可能な名前」を
加え**、152 件を 1 件ずつ実装で確認して再分類した。

到達可能と数えた基準は次の 4 つ。いずれも `ios/Sources/KsDialogs/Kmp/` の **public 宣言**を出典に取る。

1. 公開署名に現れる型・添付 API 名
2. 戻り値の型とその case
3. その入口が throw する失敗型とその case (内部の失敗を写し替えず素通しするものを含む)
4. 公開契約の doc comment が「KMP 利用者はここで設定・終了する」と案内している Native 入口の名前

### 禁止集合から外したトークン (到達可能。14 件)

| トークン | 到達の経路 | 確認先 (`ios/Sources/KsDialogs/Kmp/`) |
|---|---|---|
| `Dialog.shared` | KMP 入口 `Dialog.shared.kmp` の取り出し元 (`Dialog.shared` は `public static let`、`kmp` は `public let`) | `KsDialogsKmp.swift:13`・`:16`・`:22`・`:145-146` (`ios/Sources/KsDialogs/Presentation/Dialog.swift:10`・`:16`) |
| `Loading.shared` | 同上 (`Loading.shared.kmp`)。終了の `Loading.shared.hide()` も KMP の show と対になる公開契約 | `KsLoadingKmp.swift:12`・`:15`・`:23`・`:64` (`ios/Sources/KsDialogs/Presentation/Loading.swift:13`・`:20`) |
| `Toast.shared` | 同上 (`Toast.shared.kmp`) | `KsToastKmp.swift:12`・`:15`・`:22` (`ios/Sources/KsDialogs/Presentation/Toast.swift:12`・`:19`) |
| `Loading.shared.style` | KMP の Loading 入口の doc が、見た目のスタイルの設定先として案内する | `KsLoadingKmp.swift:17` |
| `Loading.shared.options` | 同上 (既定ローディングの器メタ属性の設定先) | `KsLoadingKmp.swift:17-18` |
| `Toast.shared.style` | KMP の Toast 入口の doc が、スタイルとアプリ既定配置の設定先として案内する | `KsToastKmp.swift:17` |
| `DialogError` | KMP 向け show が throw する失敗型。Dialog は「KMP 固有でない失敗はこの型のまま」、Loading / Toast は未登録をこの型で投げる | `KsDialogsKmp.swift:185`、`KsDialogsKmpError.swift:9`・`:34`、`KsLoadingKmp.swift:67`、`KsToastKmp.swift:64` |
| `cancelled` | KMP 向け show の戻り値 `DialogResult<Result>` の case。待機の打ち切りでこれが返る | `KsDialogsKmp.swift:197`・`:214-215` |
| `presentationHostUnavailable` | KMP 向け show が素通しする `DialogError` の case (理由が欠けた結果の既定でもある) | `KsDialogsKmpError.swift:30`・`:34` |
| `viewFactoryNotRegistered` | KMP 向け Loading / Toast の入口が実際に throw する `DialogError` の case | `KmpLoadingViewModel.swift:33`・`:42`、`KmpToastViewModel.swift:26`・`:30`、`KsDialogsKmpError.swift:26` |
| `viewModelAlreadyShowing` | KMP 向け show が素通しする `DialogError` の case | `KsDialogsKmpError.swift:32` |
| `viewModelFactoryNotRegistered` | 同上 | `KsDialogsKmpError.swift:31` |
| `ksDialogOptions` | KMP の SwiftUI 登録で器の属性を供給する添付 API。doc が明示している | `KsDialogsKmp.swift:62`、`KsLoadingKmp.swift:49` |
| `ksDialogPlacement` | 同上 (Toast は `ksDialogPlacement` / `dialogTransition`) | `KsDialogsKmp.swift:62`、`KsLoadingKmp.swift:49`、`KsToastKmp.swift:48` |

`ksDialogOptions` / `ksDialogPlacement` は許可面 concept (`android/api/layout-surface.md` /
`ios/api/layout-surface.md`) にも出現するため、次節の理由でも外れる。

### 禁止集合から外したトークン (targets の和集合の中にあった。2 件)

`source` が `current-exclusion-list` の初期値のまま残っていた 4 件のうち、`ksDialogOptions` /
`ksDialogPlacement` を除く 2 件。理由欄は再構成前の旧構成での判断 (「この対象 Skill の正規公開面には
存在しない」) であり、`targets` の和集合を許可面とする現行の定義と食い違っていた。

| トークン | 出現する許可面 concept | `targets` 上の経路 |
|---|---|---|
| `KsDialogAttributes` | `android/api/dialog-surface.md`・`layout-surface.md`・`transition-surface.md` | `ksdialogs-kmp/references/android-host.md` (`android/api/` 5 本)・`layout.md`・`transitions.md` |
| `SimpleDialogViewModel` | `android/api/dialog-surface.md` | 同上 (android-host) |

### 外さなかった境界事例

- `Dialog.shared.registry` / `Loading.shared.registry` / `Toast.shared.registry` — Loading / Toast の
  KMP 入口の doc はこれらを**対比**として挙げているだけで (「Native の登録面 (`Loading.shared.registry`)
  では受けられない」`KsLoadingKmp.swift:8`、`KsToastKmp.swift:8`)、KMP 利用者が通る経路ではない。
  KMP Skill が共有 VM の登録先としてこれらを案内したらそれ自体が誤りなので、検出価値のほうが大きい。
  task 6.1 でこの 3 件が出た場合は、「対比としての言及」か「登録先としての案内」かを本文で見分ける
- `LoadingStyle.ProgressFormat` / `LoadingStyle.defaultProgressFormat` /
  `ToastStyle.builtinBackgroundColor` / `ToastStyle.builtinDefaultDuration` — KMP 入口の doc が指すのは
  `Loading.shared.style` / `Toast.shared.style` という**設定先**までで、型のメンバ名は現れない
  (`ToastStyle` は `KsToastKmp.swift:67` に出るが、これはもともと禁止集合に入っていない)
- `UIHostingController` — KMP の SwiftUI 登録は内部で `DialogSwiftUIHost` に閉じており、この型名は
  公開署名にも doc にも現れない (`KsDialogsKmp.swift:64`・`:108`、`KsLoadingKmp.swift:50` の `Content: View`)
- `AnyObject` / `Sendable` / `Result` — 修正サイクル 1 の判断 (そもそも足していない) をそのまま維持。
  今回の基準 1 に当たるので判断は変わらない

### 再分類後の件数

kmp 152 → **136 件** (削除 16 件 = 到達可能 14 + targets 由来 2)。ほかの 4 範囲は変えていない
(ios 200 / android 178 / maui 154 / aiforms-migration 151)。`source` は
`current-exclusion-list + new-surface + kmp-targets-narrowing + kmp-entry-reachability` に変えた。

136 件の内訳は、iOS Native 固有 8 件 (上の境界事例) + MAUI 固有 123 件 + 非 API トークン 5 件
(`A.min` / `C05` / `C19` / `approvedBy` / `approvedDiff`) で、**他 platform 固有の名前は残っている**。
許可面 (`kmp/api/*` + `android/api/*` + `ios/api/layout-surface.md` + `ios/api/transition-surface.md`)
の識別子との積集合は 0 件で、host Minor 1 が求めた不変条件を満たす。件数と内訳は
`verification/forbidden-tokens-concepts.txt` の末尾にも残した。

## 追記: kmp 禁止集合への復帰 (修正サイクル 3、2026-09-05)

修正サイクル 2 の再分類 (152 → 136) に対する独立レビュー (review-003 / second-opinion-code-003) の指摘で、
外し過ぎと判定された 6 件を禁止集合に戻した。kmp は **136 → 142 件**。ほかの 4 範囲は変えていない。

| トークン | 戻す理由 |
|---|---|
| `Dialog.shared` / `Loading.shared` / `Toast.shared` | 突き合わせは完全一致で、KMP の入口 `*.shared.kmp` (と `Loading.shared.hide()` / `Loading.shared.style` 等) は別トークン。裸のシングルトン名を許可する必要は無く、外すと KMP Skill が iOS Native の入口を案内しても検出できない |
| `viewModelFactoryNotRegistered` | `DialogError` の網羅的な変換 switch に列挙されているだけで、KMP 入口 (`KsDialogsInteropBridge.swift:94-99` が `DialogPresenter.present` を直接呼ぶ) からは到達しない。発生するのは iOS Native の型指定 show が VM factory を解決するとき (`ios/Sources/KsDialogs/Presentation/Dialog.swift:68-82`) のみ |
| `viewModelAlreadyShowing` / `Loading.shared.options` | `kasane/handbook/cross/user-skill-api-listing.md` のオーナー確定行 (前者は「内部層・interop 層」、後者は「対象 Skill 外」) を優先する。到達可能性 (素通しの case・doc comment の設定先案内) はオーナーの層判断を覆す根拠にしない。どちらも KMP の源泉 concept (`kmp/api/*.md`・許可面) に出現しないため、禁止に戻しても正当な記述は落ちない |

到達可能性の基準 (上の 4 つ) は維持するが、次の 2 点を補う: (a) **完全一致のトークン単位で判定する** —
複合名 (`Dialog.shared.kmp`) が正当でも、その構成要素 (`Dialog.shared`) を許可する理由にはならない。
(b) **handbook のオーナー確定行は到達可能性より優先する** — 除外行の基準 (内部層 / 対象 Skill 外) は層の判断で、
実装の到達経路が答える問いではない。

### Skill 側の負の検査 (task 6.1) の突き合わせ単位

spec (user-skills-manifest「Skill 本文への負の検査」) は「各 Skill 範囲のファイル全文を当該集合と突き合わせる」と
定める。ここでの「全文」は、本文のバッククォート span と fenced code block の両方から、api-coverage-check.py と
同じ識別子規則 (`[A-Za-z_][A-Za-z0-9_.]*` に末尾 `()` を許す、単語境界で切る) でトークンを抽出し、禁止集合と
**完全一致**で突き合わせる、と読む (concept 側の検査がバッククォートだけを見るのと違い、コード例も対象にする)。
一致が出た場合の扱いは task 6.2 の仕分けに従う: 源泉 concept のコード例が写されたもの (例: KMP Skill の
`layout.md` / `transitions.md` の源泉 `ios/api/layout-surface.md:72` / `transition-surface.md:71` にある
`Dialog.shared.registry` の登録例) は「Skill を修正 (KMP の登録入口 `Dialog.shared.kmp` に書き換える、
または iOS Native 側の例と明示する)」に倒し、判断が割れるものはオーナーに提示する。禁止集合から外して
通すことはしない。

### rules.md の「KMP の公開面」との関係

`kasane/concepts/rules.md` の共通概念名の判定に使う「KMP の公開面 (Swift 向け公開面 = `kmp/api/ios-host-integration.md`
が扱う面)」と、本 fixture の許可面 (「`ios/Sources/KsDialogs/Kmp/` から到達可能な名前」) は目的が違う別の定義である。
前者は core にバッククォートで残せる名前を絞る条件 (concept の記述範囲が基準)、後者は Skill 側の負の検査で正当な
記述を落とさないための許可 (実装の到達可能性が基準)。fixture の許可面のほうが広いのは意図的で、差分の名前
(concept に出ないが到達可能な `DialogError` / `cancelled` 等) が Skill に現れるかは task 6.1 の API 名網羅検査側で
別に判定される。

## 追記: ios / android / maui / aiforms-migration 禁止集合の是正 (task 6.1、2026-09-06)

task 6.1 の Skill 側の負の検査を初めて回したところ **44 件** (ios 22 / android 10 / maui 8 /
aiforms-migration 4 / kmp 0) と、検査候補側で maui 3 件が一致した。全件を実装と源泉 concept で
照合した結果、**Skill に他 platform 名が写り込んだものは 1 件も無く**、すべて禁止集合側の誤りに
由来する偽陽性だった。ここでは原因・適用した規則・外したトークンを記録する。

### 原因: task 4.3 と task 6.1 の突き合わせ単位の非対称

4 集合は task 1.3 (旧除外リストの「対象 Skill 外・機械検査由来」) と task 4.3 (新設 surface concept の
識別子のうち「自 platform の公開面に無いもの」) から作られた。task 4.3 の「自 platform の公開面に無い」
判定は **concept のバッククォート span だけ**を見ている。一方 task 6.1 の Skill 側の検査は本追記の前節
「Skill 側の負の検査 (task 6.1) の突き合わせ単位」の定めにより **コード例も対象にする**。この非対称のため、

- 自 platform の concept ではコード例か複合 span (`` `register(...)` `` など) にしか現れない自分の公開名
- 見本型名・ラムダ引数名・文字列リテラルの語など、そもそも API 名ではないトークン

が「自 platform の公開面に無い」と誤判定されて禁止集合に入っていた。spec Requirement
(`specs/user-skills-manifest/spec.md:47`) は「他 platform の surface concept の識別子のうち**自 platform の
公開面に無いもの**を加える」と定めるので、自 platform の実在名・見本名の混入は Requirement に対する
実装誤りであり、その是正は spec からの逸脱ではない (オーケストレーターの裁定)。

### 適用した規則 (修正サイクル 2〜3 で kmp に確定したものを 4 集合へ適用)

- **(a) 到達可能性で外す** — 自 platform の実装で public な名前 (宣言を 1 件ずつ実物で確認し出典を残す)、
  または自 platform の源泉 concept (manifest `targets` の当該 Skill の和集合) の**全文** (span + コード例) に
  現れる名前 (見本名・ラムダ引数名・文字列リテラルの語を含む) は外す。**完全一致のトークン単位で判定する**
- **(b) handbook のオーナー確定行は到達可能性より優先する** — 分割前の除外リスト
  (`git show HEAD:kasane/handbook/cross/user-skill-api-listing.md`) で基準が「内部層・interop 層」
  「低頻度の細部 API」「可視性引き下げ候補」の行に載る名前は外さない。基準が「対象 Skill 外・機械検査由来」
  「機械的に導出できる名前」の行は層の判断ではないので (a) に従う
- **(c) 移植元名は禁止集合の目的の外** — `maui/api/*.md` の「移植元との対応」節が旧 AiForms の API 名として
  言及する名前は、他 platform 名の混入検出という目的に当たらないので maui 集合から外す

### 外したトークンと出典

**ios: 200 → 192 件**

| トークン | 規則 | 出典 |
|---|---|---|
| `View` | (a) 実装 + concept | `ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:50` (`public extension View`) / `kasane/concepts/ios/api/dialog-surface.md:51` (コード例の注記) |
| `ConfirmContent` | (a) concept | `kasane/concepts/ios/api/dialog-surface.md:51` (見本の SwiftUI View 名) |
| `setMessage` | (a) 実装 | `ios/Sources/KsDialogs/Presentation/Loading.swift:90` (`public func setMessage`) |
| `options` | (a) 実装 | `ios/Sources/KsDialogs/Presentation/Loading.swift:41` (`public var options`) |
| `ProgressFormat` | (a) 実装 | `ios/Sources/KsDialogs/Contract/LoadingStyle.swift:14` (`public typealias ProgressFormat`) |
| `shared` | (a) 実装 | `ios/Sources/KsDialogs/Presentation/Dialog.swift:10`・`Loading.swift:13`・`Toast.swift:12` (`public static let shared`) |
| `Show` | 判断 (下の「規則で判断が割れたもの」) | `skills/{en,ja}/ksdialogs-ios/SKILL.md:37` の `Button("Show toast")` — 文字列リテラル内の英単語 |
| `Register` | 同上 | `skills/{en,ja}/ksdialogs-ios/references/dialogs.md:121` の `assertionFailure("Register content for ...")` — 同上 |

**android: 178 → 171 件**

| トークン | 規則 | 出典 |
|---|---|---|
| `register` | (a) 実装 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt:33` (`public fun ... register(`) |
| `top` | (a) 実装 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogEdgeInsets.kt:12` (`public val top`) |
| `bottom` | (a) 実装 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogEdgeInsets.kt:14` (`public val bottom`) |
| `setMessage` | (a) 実装 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:83` (`public suspend fun setMessage`) |
| `ViewFactoryNotRegistered` | (a) 実装 | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogException.kt:15` (`public class ViewFactoryNotRegistered`) |
| `ConfirmContent` | (a) concept | `kasane/concepts/android/api/dialog-surface.md:66` (見本の Composable 名) |
| `cancelled` | (a) concept | `kasane/concepts/android/api/layout-surface.md:70` (コード例のコメント「結果 (completed / cancelled)」) |

**maui: 154 → 142 件**

| トークン | 規則 | 出典 |
|---|---|---|
| `presentation` | (a) 実装 + concept | `maui/KsDialogs.Maui/Contract/DialogTransition.cs:45` (ctor パラメータ) / `kasane/concepts/maui/api/transition-surface.md:55` |
| `dismissal` | (a) 実装 | `maui/KsDialogs.Maui/Contract/DialogTransition.cs:46` |
| `overlayDuration` | (a) 実装 | `maui/KsDialogs.Maui/Contract/DialogTransition.cs:47` |
| `LoadingStyle.ProgressFormat` | (a) 実装 | `maui/KsDialogs.Maui/Contract/LoadingStyle.cs:20`・`:44` (`public sealed record LoadingStyle` の `public Func<string?, double?, string> ProgressFormat`)。`kasane/concepts/maui/api/loading-surface.md:75` に `ProgressFormat` として載る |
| `options` | (a) concept | `kasane/concepts/maui/api/di-registration.md:23` (`AddKsDialogs(o => ...)` の注記。Skill 側は `AddKsDialogs(options => ...)` のラムダ引数名) |
| `transition` | (a) concept | `kasane/concepts/maui/api/transition-surface.md:81` (`var transition = ...`) |
| `A.min` | (a) concept | `kasane/concepts/core/api/layout-semantics.md:131` (レイアウト数式の変数名) |
| `dialogMargin` | (a) concept | `kasane/concepts/core/api/layout-semantics.md:153` (図の注記) |
| `layoutArea` | (a) concept | `kasane/concepts/core/api/layout-semantics.md:150` (図の注記) |
| `SetIocConfig` | (c) | `kasane/concepts/maui/api/di-registration.md:48` (「移植元との対応」) |
| `ShowResultAsync` | (c) | `kasane/concepts/maui/api/dialog-surface.md:93` (同上) |
| `UseCurrentPageLocation` | (c) | `kasane/concepts/maui/api/layout-surface.md:69` (同上) |

`concept-exempt` の 3 件はこの (c) の 3 件そのものだったため、トークンごと外れて `[]` になった。

**aiforms-migration: 151 → 144 件**

| トークン | 規則 | 出典 |
|---|---|---|
| `LoadingStyle.ProgressFormat` | (a) 実装 | `maui/KsDialogs.Maui/Contract/LoadingStyle.cs:44`。`skills/{en,ja}/ksdialogs-aiforms-migration/references/api-mapping.md:182` は移行先として正しく書いている |
| `options` | (a) concept | `kasane/concepts/maui/api/di-registration.md:23` |
| `presentation` | (a) concept | `kasane/concepts/maui/api/transition-surface.md:55` |
| `transition` | (a) concept | `kasane/concepts/maui/api/transition-surface.md:81` |
| `A.min` | (a) concept | `kasane/concepts/core/api/layout-semantics.md:131` |
| `dialogMargin` | (a) concept | `kasane/concepts/core/api/layout-semantics.md:153` |
| `layoutArea` | (a) concept | `kasane/concepts/core/api/layout-semantics.md:150` |

### 外さなかったもの

- **(b) で残した行**: 今回の候補に (b) 該当 (「内部層・interop 層」「低頻度の細部 API」「可視性引き下げ候補」)
  は 1 件も無かった。`A.min` / `C05` / `C19` / `approvedBy` / `approvedDiff` は maui / aiforms-migration の
  除外行に載るが基準は「対象 Skill 外・機械検査由来」なので (b) の保護対象ではなく、そのうち
  実際に源泉 concept に現れる `A.min` だけを (a) で外した。`C05` / `C19` / `approvedBy` / `approvedDiff` は
  どの源泉 concept にも現れないので残した (architecture への移動漏れの回帰検出は維持される)
- **(a) に当たらないと判定して残したもの** (自 platform の public 宣言ではない、または源泉 concept に無い):
  `Color` (android — `ToastStyle.kt:32` の既定値に出る Android framework の型で、KsDialogs の public 宣言ではない)、
  `UIView` (maui — `maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogContent.cs:154` の引数型に出る Apple の型)、
  `Result` (maui / aiforms-migration — `maui/KsDialogs.Maui/Contract/DialogResultChannel.cs:18` の
  `Result` は **internal** クラスのメンバで公開面ではない)、`Dialog()` / `Loading()` / `Toast()` / `Double`
  (maui / aiforms-migration — トークンの形が MAUI の宣言と一致しない)
- **kmp 集合は触っていない** (142 件のまま)

### 件数の前後

| 集合 | 前 | 後 |
|---|---|---|
| ios | 200 | 192 |
| android | 178 | 171 |
| maui | 154 | 142 |
| kmp | 142 | 142 (変更なし) |
| aiforms-migration | 151 | 144 |

kmp 集合を触らなかった理由: kmp の許可面は修正サイクル 2 の定義 (`kmp/api/` + `android/api/` + `ios/api/` の layout / transition surface + Swift 入口の到達可能名。`core/api/` を含めない) のままであり、規則 (a) を kmp にも適用すると外れるのは `A.min` 1 件だけである。`A.min` は `core/api/layout-semantics.md` の fenced code block に残る rect 決定手順の数式で、KMP Skill への転記漏れの回帰検出として、`C05` / `C19` / `approvedBy` / `approvedDiff` の 4 件は architecture (`core/architecture/layout-case-table.md`) への移動漏れの回帰検出として、いずれも意図的に kmp 集合に残す。KMP Skill にこの数式は写っていないので検査結果 (一致 0 件) には影響しない。

`source` は ios / android / aiforms-migration が
`current-exclusion-list + new-surface + own-surface-reachability`、maui はこれに `+ porting-source-names`
を足した値に変えた。

### 規則で判断が割れたもの (`Show` / `Register`、ios)

この 2 件だけは (a) にも (c) にも当たらない。`Show` は `maui/api/toast-surface.md:28`、`Register` は
`maui/api/dialog-surface.md:33` などにバッククォート識別子として実在する **MAUI の公開名**であり、
ios の実装にも ios の源泉 concept にも (大文字始まりの形では) 存在しない。にもかかわらず一致したのは、
Skill 側の突き合わせ単位が fenced code block を語単位で切るため、**文字列リテラル内の英単語**
(`Button("Show toast")` / `assertionFailure("Register content for ...")`) を識別子として拾ったからである。

- 採った処置: 2 件とも ios 集合から外した。Skill 側は API 参照ではなく英文なので修正対象ではなく
  (かつ本ワーカーの担当範囲では `skills/**` を書き換えない)、突き合わせ単位を変えるのは修正サイクル 3 で
  確定した定めを覆すことになるため
- 残る検出力: MAUI の複合形 `ShowAsync` / `ShowResultAsync` / `RegisterForDialog` / `RegisterViewModel` は
  ios 集合に残っているので、iOS Skill が MAUI の登録・表示 API を案内すればそれらで検出される。
  失われるのは「裸の `Show` / `Register` だけが iOS Skill に現れる」場合の検出であり、
  この 2 語は英語の一般語でもあるため、そもそも識別子としての検出信号は弱い
- オーナー判断が要る場合は、この 2 行を戻したうえで Skill 側の文字列リテラルを検査対象から外す
  (突き合わせ単位の再定義) のが代替案になる
