# レビュー結果: add-page-layout-area (004 回目)

**日付**: 2026-09-27
**判定**: APPROVED

> **範囲**: 今回のレビューは、オーナー指示による小さな変更だけを対象にする。変更は、4 ルートの Layout Dialog パネルが出すダイアログの dialogMargin を全辺 0 にするもので、根拠は deviation.md の最終項。見たのは、Sample の登録 5 ファイルのうち余白を変えた部分と、差し替えた証跡 24 枚 (`ui/verification/` の 6 形態 × 4 状態)。change 全体は review-003 (APPROVED) で確認済みで、今回はやり直していない。

## サマリー

余白 0 は 5 つの登録ファイル (KMP は iOS / Android の 2 つのホスト) で、どれも `LayoutDialogViewModel` の登録の中だけに入っている。この ViewModel を作るのは各ルートの属性調整パネルだけなので、他のデモ項目には効かない。4 ルートとも「全辺 0」という同じ意味で書かれている。差し替えた 24 枚では、カードの端が基準領域の端 (タブバーの上端 / 可視領域の下端 / ナビゲーションバーの下端 / ステータスバーの下端) に接して見え、samples の Scenario を満たしている。deviation.md の最終項と実装は 1 対 1 で対応する。

指摘は Minor 1 件と Suggestion 1 件。Minor は、deviation.md の最終項が Scenario「既存のパネル操作は変わらない」(従来と同じ位置に出る) に触れていないこと。余白 0 にしたので、Start / End 寄せでは従来 (24) と位置が変わる。verify-001 はこの Scenario の証跡として `*-visible-area-end-end.png` を挙げているが、この画像は今回差し替えられている。

## 照合した規約

- comment-policy.md (always): `scripts/comment-policy-lint.py` で禁止 0 件 (検査対象 1132 ファイル)。足したコメント 1 行 (「余白は全辺 0 にして、カードが基準領域の端に接するかで置かれた領域を見分けられるようにする」) は、5 ファイルとも同じ文面の日本語。外部文書の ID に頼らず、理由だけで読める
- sample-parity.md (`samples/**` を触るとき): 「一致の単位はデモ項目」「専用の画面を伴う場合は、その画面の構成・文言・初期値も一致の対象」の節と照らした。4 ルートとも余白は全辺 0 で同じ。文言・色トークン・初期値・タブ構成は変わっていない。「利用者と同じ側から使う」の節とも照らした。どのルートも公開 API (iOS `DialogOptions(dialogMargin:)` + `DialogEdgeInsets.zero` / Android `DialogEdgeInsets.ZERO` / MAUI `Dialog.SetDialogMargin`) だけを使い、ルート間でコードを共有していない
- lint: `local-path-lint.py` と `identity-lint.py` を、変更した 5 ファイルと deviation.md に `--paths` で掛けた。どちらも違反なし (終了コード 0)
- lessons/code-review.md: 今回の範囲に当たる「指摘しないこと」の型は無い

## 確認した内容

### 1. 余白 0 が効く範囲 (波及の有無)

| ルート | 箇所 | 確認 |
|---|---|---|
| iOS Native | `samples/ios/KsDialogsSample/SampleDialogRegistration.swift:26-29` | `LayoutDialogViewModel` の登録の中で `DialogOptions(layoutArea:, dialogMargin: .zero)`。Basic / Declarative / Transition / Model / TextInput の登録は変わっていない |
| KMP (iOS) | `samples/kmp/iosApp/KsDialogsSampleKmp/SampleDialogRegistration.swift:33-36` | 同上 |
| Android Native | `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleDialogRegistration.kt:36-39` | `LayoutDialogViewModel` の登録の中で `dialogMargin = DialogEdgeInsets.ZERO`。使わなくなった `DialogLayoutArea` の import は外され、`DialogEdgeInsets` の import に替わっている |
| KMP (Android) | `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/SampleDialogRegistration.kt:44-47` | 同上 |
| MAUI | `samples/maui/KsDialogs.Sample.Maui/SampleDialogRegistration.cs:35-36` | `LayoutDialogViewModel` の登録の中で `Dialog.SetDialogMargin(view, new Thickness(0))` |

`LayoutDialogViewModel` を作っている箇所を grep した結果は、各ルートのパネル (`SampleLayoutPanelModel.swift:29` / `MainActivity.kt:366` / `SamplePresenter.kt:199` / `SampleLayoutPanelPage.xaml.cs:151`) だけだった。どの呼び出しも、show に渡すのは placement だけで、余白を上書きする経路は無い。したがって、余白 0 はパネル (Panel タブと Info タブの `Show`) のダイアログにだけ効く。0 は単位 (pt / dp / MAUI の論理単位) によらず同じ意味なので、4 ルートの意味は揃っている。

### 2. 証跡の目視 (24 枚)

6 形態を並べた縮小図で全体を見た。そのうえで、ios / maui-ios / android / kmp-android / maui-android の下端は、切り出しと画素の走査で確かめた。

| 状態 | 見えたこと | Scenario |
|---|---|---|
| current-page-end-end | iOS 系 3 形態: カードの下端は浮いたタブバーの上端に接し、右端は画面の右端に接する。android: タブバー上罫線 (y=1070) の 1px 上 (y=1069) がカードの白の最後の行で、ぴったり接している。kmp-android も同じ位置。maui-android: カードの下端は y=1119。MAUI のタブバーは地も罫線も白で、境界は画素では取れない。タブのアイコンの位置 (y≈1146) と BottomNavigationView の高さから見ると、タブバーの上端に接していると見てよい | 「タブバーの上に、重ならずに」を満たす |
| visible-area-end-end | 6 形態とも、カードがタブバーに重なり、下端は可視領域の下端 (ホームインジケータ / ジェスチャーバーの上) に接する。current-page のときよりタブバーの高さぶん下に出る | 「タブバーに重なる」を満たす |
| current-page-start-start | 6 形態とも、カードの上端はナビゲーションバーの下端、左端は画面の左端に接し、バーとは重ならない | 「ナビゲーションバーの下に出る」を満たす (dialogMargin 0 ぶん内側) |
| info-current-page-start-start | 6 形態とも、カードの上端はステータスバーの下端に接し、パネルのタブのときより上に出る | 「Info タブでは画面の上端に出る」を満たす |

`*-panel-initial.png` と `*-info-tab.png` はダイアログを出していない状態なので、余白の影響は受けない。差し替えなかったのは妥当である。maui-android のステータスバーだけ地の色が違うのは、今回の変更の前からあることで、範囲外。

### 3. deviation.md の最終項との照合

| deviation の記述 | 実装・証跡 |
|---|---|
| 4 ルートの Layout Dialog パネルが出すダイアログ | 5 つの登録ファイルの `LayoutDialogViewModel` の登録だけ (上の表 1) |
| dialogMargin を全辺 0 | iOS `.zero` / Android `DialogEdgeInsets.ZERO` / MAUI `Thickness(0)`。どれも全辺 0 |
| カードが基準領域の端 (タブバーの上端・ナビゲーションバーの下端・ステータスバーの下端) に接する | 証跡 3 状態で確認 (上の表 2)。visible-area の下端の接し方も同じように確認した |
| Scenario の「dialogMargin 分内側」は 0 で満たす | current-page-start-start と info-current-page-start-start で確認 |

記述と実装は 1 対 1 で対応し、記述に無い変更は含まれていない。

### 4. ビルド

指示により端末は使っていないので、ビルドとテストは自分では回していない。差し替えた 24 枚は変更後の日時 (2026-09-27 00:13) に 6 形態すべてで撮られていて、どれも余白 0 の配置になっている。つまり 6 形態とも変更後のコードでビルドと起動が通っている。使っている API は、どれも公開 API として存在することを確かめた (`ios/Sources/KsDialogs/Contract/DialogEdgeInsets.swift:25` `static let zero` / `android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/DialogEdgeInsets.kt:22` `ZERO` / `maui/KsDialogs.Maui/Presentation/DialogAttachedProperties.cs:133` `SetDialogMargin(BindableObject, Thickness)`)。変更は Sample だけで、本体のテストに当たる面は無い。

## 指摘事項

### 🟡 Minor 1. deviation.md の最終項が Scenario「既存のパネル操作は変わらない」に触れていない

**該当箇所**: `deviation.md` (最終項) / `specs/samples/spec.md:34-37` / `verify-001.md:93`
**問題点**: この Scenario は、基準領域が Visible area の初期状態で配置・移動量を変えて `Show` すると、「従来 (トグル ON) と同じ位置にダイアログが出る」ことを求めている。余白を 24 から 0 にしたので、Start / End に寄せたときの位置は従来から 24 ずれる (Center / Center でも、最大の寸法の計算が変わる)。deviation.md の最終項は「dialogMargin 分内側」を前提にした 2 つの Scenario しか挙げておらず、この Scenario の食い違いは合意済みの差分として書かれていない。さらに verify-001 は、この Scenario の証跡に `*-visible-area-end-end.png` を挙げているが、この画像は今回差し替えられ、余白 0 の位置になっている。オーナー指示の趣旨 (パネルのダイアログはすべて余白 0) からすれば、この食い違いも指示に含まれているのは明らかである。ただ、記録が欠けていると、次の verify や蒸留で無断の逸脱と読まれるおそれがある。
**推奨修正**: deviation.md の最終項に、「既存のパネル操作は変わらない」の「従来と同じ位置」は、余白 (24 → 0) の違いを除いて従来と同じ、と読み替える旨を 1 文足す。例: 「Scenario『既存のパネル操作は変わらない』の『従来と同じ位置』は、余白が 0 になったぶんを除いて同じ (配置・移動量・基準領域の効き方は変わらない)」。verify を回し直すときは、この項を参照して当該行を判定する。

### 🔵 Suggestion 1. sample-parity.md にパネルのダイアログの余白を書いておく

**該当箇所**: `kasane/handbook/cross/sample-parity.md` (「Layout Dialog の属性調整パネル」の「画面の構成」表)
**問題点**: 余白 0 は 4 ルートで揃えるべき Sample の構成 (見た目で基準領域を判定するための装置の性質) だが、パリティ規約の表には無い。後から 1 ルートだけ既定値 (24) に戻っても、規約からは検出できない。
**推奨修正**: 蒸留のときに、「画面の構成」表へ「パネルが出すダイアログ | dialogMargin は全辺 0 (カードが基準領域の端に接するかで、置かれた領域を見分けるため)」の 1 行を足す。handbook の更新は蒸留の責務なので、今回の実装で直す必要はない。

## アクションプラン

1. (Minor 1) deviation.md の最終項に、Scenario「既存のパネル操作は変わらない」の読み替えを 1 文足す。そのあと verify を回し直すときに、当該行をその項に沿って判定する
2. (Suggestion 1) 蒸留のときに、sample-parity.md の「画面の構成」表へパネルのダイアログの余白 0 を足す
