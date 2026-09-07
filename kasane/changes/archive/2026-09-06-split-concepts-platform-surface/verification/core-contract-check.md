# 共通概念名の確認 (core に残せる識別子)

design.md Decision 2 の規則 —「4 つの Skill 範囲 (iOS / Android / MAUI / KMP) すべての公開面に同綴り
(MAUI の interface 接頭辞 `I` は同綴り扱い、大文字小文字の違いは別綴り) で存在する」— を実装コードで
確認した結果を記録する。確定 (全巡を通した最終一覧) は tasks 4.2 で行い、本ファイルは巡ごとに追記する。

判定に使った公開面の範囲:

| Skill 範囲 | 公開面として読んだ場所 |
|---|---|
| iOS | `ios/Sources/KsDialogs/` の public 宣言 |
| Android | `android/ksdialogs/src/main/` と `android/ksdialogs-compose/src/main/` の public 宣言 |
| MAUI | `maui/KsDialogs.Maui/` の public 宣言 |
| KMP | `kmp/ksdialogs-kmp/src/commonMain/` の public 宣言と、KMP の Swift 向け公開面 `ios/Sources/KsDialogs/Kmp/`・Android ホスト側 (`androidMain` の typealias が指す Android Native の型) |

KMP の範囲に host 側を含めるのは、KMP Skill の源泉が commonMain の concept だけでなく
`kmp/api/ios-host-integration.md` と `android/api/` を含むため (design.md Decision 6 の `targets`)。
API 名網羅検査は Skill 単位で本文を突き合わせるので、KMP 利用者が host 側で書く名前も
「その Skill に載る名前」として扱う。

## 暫定一覧 (dialog 巡)

| 識別子 | iOS | Android | MAUI | KMP | 判定 |
|---|---|---|---|---|---|
| `KsDialogs` | `ios/Sources/KsDialogs/Presentation/KsDialogs.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialogs.kt` | `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs` (`IKsDialogs`) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt` | 共通概念名 |
| `DialogViewModel` | `ios/Sources/KsDialogs/Contract/DialogViewModel.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewModel.kt` | `maui/KsDialogs.Maui/Contract/DialogViewModel.cs` (`IDialogViewModel`) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewModel.kt` (expect interface) | 共通概念名 |
| `DialogViewRegistry` | `ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogViewRegistry.kt` | `maui/KsDialogs.Maui/Registry/DialogViewRegistry.cs` | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewRegistry.kt` | 共通概念名 |
| `DialogResult` | `ios/Sources/KsDialogs/Contract/DialogResult.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogResult.kt` | `maui/KsDialogs.Maui/Contract/DialogResult.cs` | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogResult.kt` | 共通概念名 |
| `DialogPlacement` | `ios/Sources/KsDialogs/Contract/DialogPlacement.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPlacement.kt` | `maui/KsDialogs.Maui/Contract/DialogPlacement.cs` | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogPlacement.kt` | 共通概念名 |
| `DialogNotifier` | `ios/Sources/KsDialogs/Contract/DialogNotifier.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogNotifier.kt` | `maui/KsDialogs.Maui/Contract/DialogNotifier.cs` | commonMain には無い。KMP の Swift 向け公開面 `ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` の factory 引数・`notifier(for:result:)` の戻り値、および Android ホスト側 (Android Native の同名型) に同綴りで現れる | 共通概念名 (下記の注記つき) |

### 条件を満たさなかったもの (dialog 巡)

| 識別子 | 不成立の理由 | core での書き方 |
|---|---|---|
| `DialogException` | iOS は `DialogError` (`ios/Sources/KsDialogs/Contract/DialogError.swift`) で別綴り。Android / MAUI / KMP commonMain は `DialogException` | 「構成ミスの失敗」と散文で書き、綴りは各 platform の公開面へ |
| `DialogOptions` | KMP commonMain が公開しない (`kmp/ksdialogs-kmp/src/commonMain/` に宣言なし。MAUI も `maui/KsDialogs.Maui/Internals/DialogOptions.cs` で内部) | 「器の静的な属性」と散文で書く (layout 巡で扱う) |
| `DialogTransition` | KMP に宣言が無い (`kmp/ksdialogs-kmp/src/` に該当なし) | dialog 巡の core からは外し「出入りの演出」と散文で書く。最終判断は transition 巡 |

### `DialogNotifier` についての注記

commonMain には結果報告口の型が無い (共有コードの中身は各 OS 側で組み立てるため、報告口も Native 側の型)。
そのため「4 形態の**モジュール**の公開 API に同綴りで存在する」を厳格に取ると条件を満たさない。
本巡では上表の「公開面の範囲」の定義 (KMP は commonMain + host 2 側) を採り、共通概念名として core に残した。
KMP Skill の源泉には `kmp/api/ios-host-integration.md` と `android/api/dialog-surface.md` が入るため、
API 名網羅検査で KMP Skill の候補に上がり続けることはない見込み。
この見込みが外れた場合 (tasks 6.1 の検査で KMP に `DialogNotifier` が報告された場合) は、
core を「結果報告口 (notifier)」の散文へ落として platform 側に綴りを置く方向で見直す。

### 非 API のラベル (バッククォートを外したもの)

design.md Decision 2 は「識別子」を対象とする規則であり、API 名ではない見本ラベルは対象外。
下記は意味を変えずに表記だけ平文へ変えた (台帳では「意図して落とした (非 API のラベル、表記を変更)」に当たる)。

| ラベル | 出典 | 性格 |
|---|---|---|
| R | `result-notification-semantics.md` / ルール1 | 結果型の型引数の見本 |
| AA | `transition-semantics.md` (transition 巡で扱う) | Scenario ID の接頭辞 |

## 暫定一覧 (layout 巡)

layout 巡で core に残す候補を、上表と同じ「公開面として読んだ場所」の範囲で確認した結果。
`DialogPlacement` は dialog 巡で確定済みのため再掲しない。

| 識別子 | iOS | Android | MAUI | KMP | 判定 |
|---|---|---|---|---|---|
| `DialogAlignment` | `ios/Sources/KsDialogs/Contract/DialogAlignment.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogAlignment.kt` | `maui/KsDialogs.Maui/Contract/DialogAlignment.cs` | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogAlignment.kt` | 共通概念名 |

`DialogAlignment` は KMP commonMain にも同綴りで存在するため、dialog 巡の `DialogNotifier` のような
host 側への読み替えを要しない (4 モジュールの公開 API に同綴りで存在する厳格条件を満たす)。
列挙の値の綴りは形態で違う (Swift は小文字・Kotlin / KMP は大文字・MAUI は PascalCase) ため、
値そのものは core では散文で書き、綴りは各 platform の layout-surface に置いた。

### 条件を満たさなかったもの (layout 巡)

| 識別子 | 不成立の理由 | core での書き方 |
|---|---|---|
| `DialogLayoutArea` | KMP commonMain が公開しない (`kmp/ksdialogs-kmp/src/commonMain/` に宣言なし。共有コードから渡せるのは置き場所だけ) | 「基準領域」と散文で書き、選択肢は表で示す |
| `DialogEdgeInsets` | 同上 (KMP commonMain に宣言なし) | 「4 辺の余白」「dialogMargin」と散文で書く |
| `DialogOptions` | dialog 巡の判定どおり KMP commonMain が公開しない。ただし dialog 巡の表にある「MAUI も内部」は誤りで、MAUI では `maui/KsDialogs.Maui/Internals/DialogOptions.cs` の `public sealed record` である (既定ローディングの供給経路があるため公開型。core/ADR-0023)。不成立の理由は KMP commonMain の不在だけである | 「静的メタ属性」と散文で書く |
| `LayoutArea` | どの形態にも存在しない綴り (実装は `DialogLayoutArea`)。乖離として deviation.md に記録した | 型名を書かず「基準領域」と散文で書く |

### 非 API のラベル (layout 巡でバッククォートを外したもの)

| ラベル | 出典 | 性格 |
|---|---|---|
| A.min / A.max | `layout-semantics.md` / 最終 rect の決め方 | rect 決定手順の数式断片。fenced code block へ移した (design Decision 3) |
| C05 / C19 / C07 / C10 / C22 | `layout-semantics.md` / 共通ケース表と OS 差の統制・例1・例2 | 共通ケース表のケース ID。`core/architecture/layout-case-table.md` へ移した |

## 暫定一覧 (transition 巡)

transition 巡で core に残す候補を、上表と同じ「公開面として読んだ場所」の範囲で確認した結果。

| 識別子 | iOS | Android | MAUI | KMP | 判定 |
|---|---|---|---|---|---|
| `DialogTransition` | `ios/Sources/KsDialogs/Contract/DialogTransition.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransition.kt` | `maui/KsDialogs.Maui/Contract/DialogTransition.cs` | commonMain には無い。KMP 利用者は演出を各 OS のホスト側で添付するため、同綴りの型を iOS / Android の上記ファイルで書く | 共通概念名 (下記の注記つき) |
| `DialogTransitionEdge` | `ios/Sources/KsDialogs/Contract/DialogTransitionEdge.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogTransitionEdge.kt` | `maui/KsDialogs.Maui/Contract/DialogTransitionEdge.cs` | 同上 (commonMain には無く、各 OS 側の型を書く) | 共通概念名 (型名のみ。core 本文では使っていない) |

`DialogTransitionEdge` は型名としては 3 形態に同綴りで存在するが、**列挙の値の綴りは形態で割れる** (iOS が `top` / `bottom` / `leading` / `trailing`、Android が `TOP` / `BOTTOM` / `START` / `END`、MAUI が `Top` / `Bottom` / `Start` / `End`)。core は辺を「上 / 下 / 行の始まり側 / 行の終わり側」と散文で書き、値の綴りは各 platform の transition-surface に置いた。型名自体も core 本文では使っていない (baseline の core にも出現しない識別子であり、書き直しで新たに増やす必要がなかった)。

### `DialogTransition` / `DialogTransitionEdge` についての注記

commonMain には演出の型が無い (共有コードは演出を運ばず、添付の面も持たない)。そのため「4 形態の**モジュール**の公開 API に同綴りで存在する」を厳格に取ると条件を満たさない。dialog 巡の `DialogNotifier` と同じく、本ファイル冒頭の「公開面として読んだ場所」の定義 (KMP は commonMain + host 2 側) を採り、共通概念名として core に残した。

KMP Skill の `references/transitions.md` の源泉には `ios/api/transition-surface.md` と `android/api/transition-surface.md` が入る (design.md Decision 6 の `targets`) ため、API 名網羅検査で KMP Skill の候補に上がり続けることはない見込みである。この見込みが外れた場合 (tasks 6.1 の検査で KMP に `DialogTransition` が報告された場合) は、core を「出入りの演出」の散文へ落として platform 側に綴りを置く方向で見直す。

### 条件を満たさなかったもの (transition 巡)

| 識別子 | 不成立の理由 | core での書き方 |
|---|---|---|
| `presentation` / `dismissal` / `overlayDuration` | iOS / Android は小文字始まり、MAUI は `Presentation` / `Dismissal` / `OverlayDuration` で別綴り。commonMain には演出型そのものが無い | 項目名として平文 (バッククォートなし) で書き、綴りは各 platform の transition-surface に置く |
| `none` (プリセット) | iOS / Android は `none`、MAUI は `None` で別綴り | 「無演出のプリセット」と散文で書く |
| フックの型 | 形態ごとにまったく別 (Swift は `DialogTransition.Hook` の typealias、Kotlin と C# は型名を持たない関数型・デリゲート型) | 「中身のホスト View を受け取り、演出が終わったら戻る非同期の関数」と散文で書く |
| duration / easing の型 | 形態ごとのネイティブ表現で共通の綴りが無い | 「形態のネイティブ表現に従う」と散文で書く |

### 非 API のラベル (transition 巡でバッククォートを外したもの)

| ラベル | 出典 | 性格 |
|---|---|---|
| TR / IA / AA / MA / KC | `transition-semantics.md` / 冒頭の読み方 | Scenario ID の接頭辞。`PB-TR-04` のようなハイフンを含む完全な ID はバッククォートのままでよい (識別子として抽出されない) |

## 暫定一覧 (loading / toast 巡)

loading / toast 巡で core に残す候補を、本ファイル冒頭と同じ「公開面として読んだ場所」の範囲で確認した結果。`DialogPlacement` (dialog 巡) と `DialogTransition` (transition 巡) は確定済みのため再掲しない。

| 識別子 | iOS | Android | MAUI | KMP | 判定 |
|---|---|---|---|---|---|
| `KsLoading` | `ios/Sources/KsDialogs/Presentation/KsLoading.swift` (protocol) | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt` (interface) | `maui/KsDialogs.Maui/Presentation/IKsLoading.cs` (`IKsLoading`) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsLoading.kt` (interface) | 共通概念名 |
| `KsToast` | `ios/Sources/KsDialogs/Presentation/KsToast.swift` (protocol) | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsToast.kt` (interface) | `maui/KsDialogs.Maui/Presentation/IKsToast.cs` (`IKsToast`) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsToast.kt` (interface) | 共通概念名 |
| `LoadingProgressReceiver` | `ios/Sources/KsDialogs/Contract/LoadingProgressReceiver.swift` | `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingProgressReceiver.kt` | `maui/KsDialogs.Maui/Contract/LoadingProgressReceiver.cs` (`ILoadingProgressReceiver`) | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingProgressReceiver.kt` (expect interface) | 共通概念名 |

`KsLoading` / `KsToast` / `LoadingProgressReceiver` はいずれも KMP commonMain に同綴りで存在するため、dialog 巡の `DialogNotifier` のような host 側への読み替えを要しない (4 モジュールの公開 API に同綴りで存在する厳格条件を満たす)。ただし**メンバ名は条件を満たさない** — 進捗受け口のメソッドは iOS / Android / KMP が `onProgress`、MAUI が `OnProgress` (`maui/KsDialogs.Maui/Contract/LoadingProgressReceiver.cs`) で別綴りのため、core では「進捗値を受け取る 1 メソッドだけの面」と散文で書き、綴りは各 platform の loading-surface に置いた。

`LoadingViewModel` / `ToastViewModel` も同条件を満たす (iOS / Android / KMP commonMain が同綴り、MAUI が `ILoadingViewModel` / `IToastViewModel`) が、書き直した core 本文では使っていない — baseline の core にも出現せず、新たに増やす必要がなかったため (transition 巡の `DialogTransitionEdge` と同じ扱い)。

### 条件を満たさなかったもの (loading / toast 巡)

| 識別子 | 不成立の理由 | core での書き方 |
|---|---|---|
| `LoadingStyle` | KMP commonMain が公開しない (`kmp/ksdialogs-kmp/src/commonMain/` に宣言なし。負のコンパイル検査 `kmp/api-surface-check/src/negativeCheckLoadingStyleType/` が不在を固定している) | 「styling」「styling の値オブジェクト」と散文で書く |
| `ToastStyle` | 同上 (`kmp/api-surface-check/src/negativeCheckToastStyleType/`)。iOS / Android / MAUI は `ToastStyle` で同綴り | 「一括設定」「一括設定の値オブジェクト」と散文で書く |
| `LoadingViewRegistry` / `ToastViewRegistry` | KMP commonMain が公開しない (登録は各 OS 側のため、共有コードにレジストリのハンドルが無い) | 「Loading 専用レジストリ」「Toast 専用レジストリ」と散文で書く |
| 既定シングルトン (`Loading.shared` / `Loading.instance` / `Loading.Instance`、`Toast.shared` / `Toast.instance` / `Toast.Instance`) | 綴りが形態で 3 種に割れる (design.md Decision 2 代替案 B の却下どおり) | 「既定シングルトン」と散文で書き、綴りは各 platform の surface へ |
| 操作名 (`hide` / `setMessage` / `start` / `show`) | MAUI だけ非同期メソッドの命名慣習で `HideAsync` / `SetMessage` / `StartAsync` / `ShowAsync` (`maui/KsDialogs.Maui/Presentation/IKsLoading.cs`) と別綴り | 「閉鎖 (hide)」のように平文の項目名で書き、署名は各 platform の surface へ |
| duration の引数名 | Android / MAUI / KMP commonMain は `durationMs`、iOS だけ `duration` で別綴り | 「duration」と平文で書き、引数名は各 platform の toast-surface へ |
| `LoadingCoordinator` | 内部層の名前で公開面ではない (iOS / Android にそれぞれ 1 つ) | core からは外し「プロセス内に 1 つの内部 coordinator」と散文で書く。綴りは iOS / Android の loading-surface に「公開面ではない」と分かる形で残した |

### 非 API のラベル (loading / toast 巡でバッククォートを外したもの)

| ラベル | 出典 | 性格 |
|---|---|---|
| LD | `loading-semantics.md` / 関連 | Scenario ID 接頭辞 (Loading)。AA と同じ扱い |
| TS | `toast-semantics.md` / 関連 | Scenario ID 接頭辞 (Toast)。AA と同じ扱い |

---

# 確定一覧と検査 (task 4.2)

## 確定した共通概念名

全 5 巡の暫定一覧を design.md Decision 2 の規則でまとめた**確定一覧**。core/api の本文で
バッククォート表記してよいのはこの一覧の名前と、そのメンバー名のうち同じ条件 (4 つの Skill 範囲
すべての公開面に同綴り) を満たすものだけである。

| 識別子 | 巡 | 4 形態の充足 | core 本文での使用 |
|---|---|---|---|
| `KsDialogs` | dialog | 4 モジュール (MAUI は `IKsDialogs`) | あり (3 か所) |
| `DialogViewModel` | dialog | 4 モジュール (MAUI は `IDialogViewModel`) | あり (2 か所) |
| `DialogViewRegistry` | dialog | 4 モジュール | あり (1 か所) |
| `DialogResult` | dialog | 4 モジュール | あり (3 か所) |
| `DialogPlacement` | dialog / layout | 4 モジュール | あり (6 か所) |
| `DialogNotifier` | dialog | commonMain には無い。KMP は Swift 向け公開面と Android ホスト側で同綴り (注記あり) | あり (17 か所) |
| `DialogAlignment` | layout | 4 モジュール | あり (2 か所) |
| `DialogTransition` | transition | commonMain には無い。KMP は各 OS ホスト側で同綴り (注記あり) | あり (9 か所) |
| `DialogTransitionEdge` | transition | 同上 (注記あり) | なし (型名のみ確認) |
| `KsLoading` | loading | 4 モジュール (MAUI は `IKsLoading`) | あり (1 か所) |
| `KsToast` | toast | 4 モジュール (MAUI は `IKsToast`) | あり (1 か所) |
| `LoadingProgressReceiver` | loading | 4 モジュール (MAUI は `ILoadingProgressReceiver`) | あり (1 か所) |
| `LoadingViewModel` / `ToastViewModel` | loading / toast | 4 モジュール (MAUI は `I` 接頭辞) | なし |

`DialogNotifier` / `DialogTransition` / `DialogTransitionEdge` の 3 つは「KMP の公開面 = commonMain +
Android ホスト側 + Swift 向け公開面の 3 側」という読み方 (本ファイル冒頭の「公開面として読んだ場所」)
を採って一覧に入れている。各巡の注記 (dialog 巡・transition 巡) をそのまま維持する — tasks 6.1 の
API 名網羅検査で KMP Skill の候補にこれらが上がった場合は、core を散文へ落とす方向で見直す。

**メンバー名は 1 つも条件を満たさなかった** (進捗受け口の `onProgress` / `OnProgress`、Loading の操作名、
演出の項目名、辺の値、既定シングルトンの綴りがいずれも MAUI または iOS で別綴り)。したがって
確定一覧は上記の型名だけで閉じている。

## 検査 1: 共通概念名以外の識別子の不在 (spec Scenario「共通概念名以外の識別子の不在」)

`core/api/*.md` のバッククォート識別子を API 名網羅検査と同じ抽出規則 (STOP 語と数値を除く) で
すべて列挙し、確定一覧と突き合わせた。

```sh
python3 - <<'PY'
import re, glob, os
TOKEN = re.compile(r"`([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)`")
STOP = {"true","false","null","nil","None","self","this","var","val","let","public","internal",
        "private","open","static","enum","class","struct","interface","protocol","data","case",
        "import","async","await"}
ALLOWED = {"KsDialogs","DialogViewModel","DialogViewRegistry","DialogResult","DialogPlacement",
           "DialogNotifier","DialogAlignment","DialogTransition","DialogTransitionEdge",
           "KsLoading","KsToast","LoadingProgressReceiver","LoadingViewModel","ToastViewModel"}
out = 0
for f in sorted(glob.glob('kasane/concepts/core/api/*.md')):
    for i, line in enumerate(open(f, encoding='utf-8'), 1):
        for t in TOKEN.findall(line):
            if t.rstrip('()') in STOP or t.rstrip('()') in ALLOWED:
                continue
            out += 1
            print(f"{os.path.basename(f)}:{i} {t}")
print(f"一覧外の識別子 {out} 件")
PY
```

結果 (2026-09-05):

```
一覧外の識別子 0 件
```

出現した識別子は 11 種 — `DialogAlignment` (2) / `DialogNotifier` (17) / `DialogPlacement` (6) /
`DialogResult` (3) / `DialogTransition` (9) / `DialogViewModel` (2) / `DialogViewRegistry` (1) /
`KsDialogs` (3) / `KsLoading` (1) / `KsToast` (1) / `LoadingProgressReceiver` (1) — で、すべて確定一覧の中にある。
メンバー名の出現は 0 件だった。

## 検査 2: 識別子を含む形態別テーブルの不在 (spec Scenario「識別子を含む形態別テーブルの不在」)

先頭セルが「形態」「platform」または形態名 (iOS / Android / MAUI / KMP) で始まる表の行を集め、
各行のバッククォート識別子を数えた。

```sh
python3 - <<'PY'
import re, glob, os
TOKEN = re.compile(r"`([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)`")
STOP = {"true","false","null","nil","None","self","this","var","val","let","public","internal",
        "private","open","static","enum","class","struct","interface","protocol","data","case",
        "import","async","await"}
HEAD = re.compile(r'^(形態|platform|iOS|Android|MAUI|KMP)')
rows = hits = 0
for f in sorted(glob.glob('kasane/concepts/core/api/*.md')):
    for i, line in enumerate(open(f, encoding='utf-8'), 1):
        s = line.strip()
        if not s.startswith('|'):
            continue
        cell = s.strip('|').split('|')[0].strip().lstrip('*').strip()
        if not HEAD.match(cell):
            continue
        rows += 1
        toks = [t for t in TOKEN.findall(s) if t.rstrip('()') not in STOP]
        if toks:
            hits += 1
            print(f"{os.path.basename(f)}:{i} {cell} -> {toks}")
print(f"形態名で始まる表の行 {rows} 件 / うち識別子を含む行 {hits} 件")
PY
```

結果 (2026-09-05):

```
形態名で始まる表の行 15 件 / うち識別子を含む行 0 件
```

15 行は「呼び出し元のキャンセルの観察」(2 表) と「下を先に閉じたときの見え方」など、
承認済みの platform 差分の挙動を識別子なしで書いた表であり、spec が明示的に残してよいとしている
「識別子を含まない差分の表」に当たる (残存の確認は `platform-differences.md` の「確認結果」節)。

## 検査 3: 数式の表記 (spec Scenario「数式の表記」)

`layout-semantics.md` の「最終 rect の決め方」節を切り出し、同じ抽出規則で識別子を列挙した。

```sh
python3 - <<'PY'
import re
TOKEN = re.compile(r"`([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)`")
lines = open('kasane/concepts/core/api/layout-semantics.md', encoding='utf-8').read().splitlines()
start = None
end = len(lines)
for i, l in enumerate(lines):
    if l.startswith('#') and '最終 rect の決め方' in l:
        start, lvl = i, len(l) - len(l.lstrip('#'))
    elif start is not None and l.startswith('#') and (len(l) - len(l.lstrip('#'))) <= lvl:
        end = i
        break
toks = sorted({t for l in lines[start:end] for t in TOKEN.findall(l)})
print(f"節 {lines[start]} ({start + 1}-{end} 行) の識別子候補: {toks}")
PY
```

結果 (2026-09-05):

```
節 ## 最終 rect の決め方 (軸ごとの手順) (114-190 行) の識別子候補: []
```

`A.min` / `A.max` は fenced code block と平文の数式として書かれており (`:131` `:133` `:139`〜`:145`)、
インライン code は 1 つも無い。抽出候補は 0 件。

## 検査 4: ケース表の記述の移動 (spec Scenario「ケース表の記述の移動」)

ケース ID (`C` + 2 桁) と `approvedDiff` / `approvedBy` の出現箇所を検索した。

```sh
grep -cE '`C[0-9]{2}`|`approvedDiff`|`approvedBy`' kasane/concepts/core/api/layout-semantics.md
grep -cE '`C[0-9]{2}`|`approvedDiff`|`approvedBy`' kasane/concepts/core/architecture/layout-case-table.md
```

結果 (2026-09-05):

```
0      (layout-semantics.md)
10     (layout-case-table.md)
```

`layout-semantics.md` にはバッククォート表記も平文表記も出現しない (`grep -nE 'C[0-9]{2}|approvedDiff|approvedBy'` も 0 件)。
`layout-case-table.md` には `C19` `C05` `C10` `C07` `C22`・`approvedDiff` (4) ・`approvedBy` (3) が出現する。

# 修正サイクル 1 後の再実行 (2026-09-05)

review-001 / second-opinion-code-001 の指摘を直したあと、検査 1〜4 を同じスクリプトで再実行した。
書き直したのは core/api では `model-binding-semantics.md` (1引数 factory の段落を登録経路へ限定)、
`transition-semantics.md` (前提の箇条書き 2 件を段落へ・duration の項目を段落へ・脱出口を表へ)、
`layout-semantics.md` (前提の箇条書き 2 件を段落へ・統制の禁止事項をリンクへ) の 3 本で、
公開名は 1 つも動かしていない。

| 検査 | 結果 | baseline (task 4.2) との差 |
|---|---|---|
| 1: 共通概念名以外の識別子の不在 | 一覧外の識別子 0 件 (出現 11 種・内訳も同一) | 差なし |
| 2: 識別子を含む形態別テーブルの不在 | 形態名で始まる表の行 15 件 / うち識別子を含む行 0 件 | 差なし (脱出口の表は先頭セルが形態名でないため対象外) |
| 3: 数式の表記 | 「最終 rect の決め方」節の識別子候補 0 件 | 差なし |
| 4: ケース表の記述の移動 | layout-semantics.md 0 件 / layout-case-table.md 10 件 (平文表記も 0 件) | 差なし |
