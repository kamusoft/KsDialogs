---
type: concept
title: Toast のルール
description: Toast (fire-and-forget の非対話通知表示) の core 契約 — 呼び出し面の構成 (メッセージ入口・登録経路・インライン経路・型指定経路)・duration の時間モデル・失敗モデル・非モーダルと多重表示・機能間の前後関係・配置と一括設定・デフォルト View とカスタム View・持たない機能
tags: [toast, api, contract]
timestamp: 2026-09-07
---

# Toast のルール

この文書は、全形態 (iOS Native / Android Native / MAUI / KMP) 共通の「画面の操作を妨げずに短時間だけメッセージを重ね表示する機能」のルールを定める。読むと、呼び出し面がどんな経路で構成されているか、表示が消えるまでの時間の扱い、失敗したときに何が起こるか、複数同時に出したときや Loading / Dialog と同時に出したときの重なり、見た目のカスタマイズの2経路 (一括設定とカスタム View) で何が保証されるかが分かる。ダイアログ側の [登録と表示の呼び出し面のルール](registration-show-semantics.md) と [レイアウトのルール](layout-semantics.md) を先に読むと分かりやすい。

- **この文書が正であり、実装はここに合わせる**。core は「全形態が共有する契約」の層 (層の区分は [concepts 配置ルール](../../rules.md))
- 公開名の綴り・署名・コード例・framework 固有の注意は各形態の公開面が持つ (末尾の「形態別の公開面」)

用語 (ダイアログ側の概念文書と共通のものを含む):

| 語 | 意味 |
|---|---|
| **器** | コンテンツを画面に載せる提示コンテナ。配置・出入りの演出を担い、中身の View とは分離されている。Toast の器は覆い (背景を暗くする層) を持たない |
| **表示** | 1回の show で生まれる Toast 1枚ぶんの実体 (器 + 中身 + タイマー)。多重起動では表示が複数並存する |
| **添付** | コンテンツ定義 (View / ViewModel) に属性オブジェクトを付けて、配置などの器メタ属性 (器が解釈する属性) を供給する方法 (詳細は [レイアウトのルール](layout-semantics.md)) |
| **VM** | ViewModel。表示に使う状態の運び手 |
| **レジストリ** | VM 型キーごとに View factory と VM factory の 2 スロットを持つ対応表。共有コード (KMP の共有コード・MAUI の VM 層) が UI 型に触れずにカスタム View を呼ぶための間接層 |
| **fire-and-forget** | 呼び出しが結果も完了通知も返さない様式。show は投げたら終わりで、表示の行方を呼び出し元は観察できない |
| **移植元** | 本ライブラリの前身 AiForms.Maui.Dialogs。その Toast は廃止予定 (Obsolete) だったため、KsDialogs の Toast は互換 shim を持たない再設計の新実装 |

## 公開面の構成

公開 API は Dialog / Loading と同じ流儀 — 契約 interface と既定シングルトンの2入口 (interface 経由の DI 注入でも、登録なしの既定エントリでも使える — core/ADR-0002) — で、独立した API として提供する。契約の型は全形態で `KsToast` である (MAUI だけ interface の接頭辞が付く)。既定シングルトンの綴りは形態ごとに違い、各形態の公開面が定める。

呼び出しは4経路で、意味は全形態で同じである:

| 呼び出し | 意味 |
|---|---|
| メッセージ入口 (message と duration・配置) | デフォルト View (角丸ピル) でメッセージを表示する。最頻ユースケースの1引数入口 (core/ADR-0028) |
| 登録経路 (VM のインスタンスと duration・配置) | レジストリ登録済みのカスタム Toast View を表示する (core/ADR-0029) |
| インライン経路 (VM と duration・配置と factory) | 登録せずその場の factory で表示する。UI 層の中で完結する単発表示向けの近道で、レジストリの状態は変えない (core/ADR-0013 のインライン経路) |
| 型指定経路 (VM の型と duration・配置・同期の configure) | レジストリ登録済みの VM factory で VM を生成し configure を適用してから、登録済みのカスタム Toast View を表示する (core/ADR-0035。Dialog / Loading の型指定 show と同型 — [ViewModel 主導の呼び出しのルール](model-binding-semantics.md))。configure は同期のみ — show が fire-and-forget の同期呼び出しのため |

- show は **fire-and-forget** (core/ADR-0031): 同期・戻り値なしで、表示終了を待つ手段を契約に設けない。閉じる操作・メッセージ更新・処理ブロックを渡すスコープ形・進捗の報告口 ([Loading の公開面の構成](loading-semantics.md) にある操作) は持たない
- 表示の開始処理は受理順に UI スレッド上で順に行う (スレッド安全性のための順序化であって、表示を1枚ずつ順番待ちさせる意味ではない — 多重表示は並存する)。呼び出しは任意スレッドからできる
- 型指定経路の実行順序は「VM 生成 → configure 完了 → View 生成 → 提示」で固定され、VM factory と configure は View factory と同じく UI スレッドで受理順に実行される。解決は呼び出し時点のレジストリのスナップショットで、生成した VM は器が撤去まで保持する
- カスタム View の factory は従来 View 系 + 宣言的 UI 系の技術別オーバーロード (Dialog と同じ呼び分け — core/ADR-0010・0011)。factory が失敗を表明できるかどうかは形態の言語事情で違い、綴りと理由は各形態の公開面が定める
- レジストリと一括設定は 1 OS プロセス内で単一で、Native のすべての入口が同じ状態を共有する。KMP は View factory のレジストリを Native へ委譲して共有し、VM factory の表だけを共有コード側に持つ (kmp/ADR-0002・0006)、MAUI のレジストリは C# 層にあり Native とは層が別 (maui/ADR-0001)

KMP の共有コードの型指定経路だけは、VM factory と configure を呼び出しスレッドで済ませてから受理へ渡す (UI スレッドでの実行にならない — [ViewModel 主導の呼び出しのルール](model-binding-semantics.md) の「KMP での見え方」)。

## duration の時間モデル

duration はミリ秒の整数で show 引数に指定する (KMP の共有コードから呼べるよう、プラットフォーム固有の時間型は使わない)。

- 省略時は一括設定の既定 duration (初期値 1500)。0 以下の show 引数は無効値として一括設定の既定へ、その既定自体が 0 以下なら内蔵既定 1500 へ丸め、いずれも警告ログを出す — 値の妥当性の問題は例外にしない (fire-and-forget の同期 show に、引数値起因の例外経路を作らない)
- 上限クランプは無い (移植元の Android 実装は OS の Toast API に載っていたため実質 3.5 秒に切り詰められていた。自前の器ではこの制約自体が存在しない)
- 計時は show の**受理時点**から単調時計で開始し、実時間で消費する (アプリが背面にある間も進む)。duration の到達で出の演出を開始し (入りの演出の途中でも出へ移る)、器の撤去は出のフックの完了通知後 ([トランジションのルール](transition-semantics.md) と同じ)

## 失敗モデル (3 段階)

| 段階 | 何が起きるか |
|---|---|
| **解決の失敗** (登録経路で View factory 未登録の VM 型・型指定経路で VM factory と View factory のどちらかが未登録の VM 型 — 種類は区別する) | show の呼び出し時点で構成ミスとして同期に失敗する (形態のイディオムの例外)。表示は行われない (Dialog / Loading と同じ)。メッセージ入口とインライン経路にこの失敗はない。Swift から KMP の共有コードを直接呼ぶときの届き方は [KMP の Toast 公開面](../../kmp/api/toast-surface.md) が定める |
| **受理後の失敗** (View factory の例外・型指定経路の VM factory と configure の例外・View の実体化失敗・器の取り付け失敗) | show は既に戻っているため呼び出し元へ返せない。警告ログを出してその表示 **1 枚だけ**を破棄し、器・タイマー・factory 参照などの資源を解放する。他の表示・後続の show には影響しない (core/ADR-0033) |
| **提示環境の不在** (取り付け先ウィンドウ / resumed Activity が無い) | 呼び出しは失敗せず、提示先の出現を待って表示する。計時は受理時点から消費しているため、提示先が現れないまま duration が満了した表示は**表示されずに破棄**される (エラーではなく通常の満了として扱う)。中身の生成をいつ行うかは OS で違う (承認済みの差): 提示先の確保後に生成する Android では、この破棄で型指定経路の VM factory と configure が一度も呼ばれない。受理時点で生成する iOS では、呼ばれたあとに破棄される |

「値の妥当性の問題は丸め (duration)、構成の誤りは同期の失敗 (未登録 VM)、受理後の問題は破棄」という書き分けになっている。型指定経路は VM factory と View factory の両方を呼び出し時点で解決するので、どちらが欠けていても同期に失敗する (判定は VM factory が先で、両方欠けているときは VM factory 未登録として返る)。型指定経路の VM factory / configure の例外が「受理後の問題」側なのは、show が同期に戻ったあと UI スレッドで実行されるため呼び出し元へ返せないからである (Dialog / Loading の型指定 show では呼び出し元へ伝播する — 3 機能の違いは [ViewModel 主導の呼び出しのルール](model-binding-semantics.md))。KMP の共有コードの型指定経路は例外で、生成と configure を呼び出しスレッドで済ませてから受理へ渡すため、VM factory 未登録・型不一致・VM factory / configure の例外はいずれも show から同期に呼び出し元へ伝播する。

## 完全非対話と非モーダル

Toast は表示中、いかなる入力も奪わない (core/ADR-0031):

- Toast の器の領域へのタッチは背後のページ要素へ素通しされ、ページ要素は Toast の真下でも通常どおり操作できる
- Toast はタッチで消えず、カスタム View 内に対話可能な部品を置いても反応しない。消滅の契機は duration の経過のみ
- 「触れる通知」(タップ消し・ボタン付き) が欲しいケースは Dialog が受け皿 (配置属性で非モーダル的な配置も可能)。ページをまたいで生き残る Toast に対話要素を置くと、押された時点のページ文脈が壊れるため設けていない

## 多重表示と表示の継続

- 多重起動可: 同時に複数の show はすべて表示され、重なり順は起動順 (= show の受理順。後に受理されたものが手前)。移植元の Android 実装のように OS のキューで1枚ずつ順番待ちになることはない
- 重なりは z-order のみで、同一の実効配置の Toast は同じ位置に重なる — 器が自動で位置をずらす可視スタックは行わない (core/ADR-0030)
- 各表示は自分の duration で独立に消える。合流 ([Loading の合流](loading-semantics.md) のような1表示へのまとめ)・置き換え・キューイングは行わない
- 表示はページ遷移をまたいで継続する。器は表示 1 枚ごとに専用のオーバーレイ (iOS = key window 直貼りの View / Android = 全画面透過 Window) を持ち、ページの提示機構 (OS の提示スタック) に参加しないため、ページが変わっても器は残る (core/ADR-0030)
- 回転を含むウィンドウ寸法・insets の変化では Dialog と同じ規則 (凍結済み実効値での再配置 — [レイアウトのルール](layout-semantics.md)) で継続し、残り duration は巻き戻らない (Android では器の作り直しをまたいで)

### 機能間の前後関係

- Loading と Toast が同時に表示される場合、起動順によらず**常に Loading が前面** (core/ADR-0030)。Loading は操作を止める覆いであり、その上に通知が乗ると「操作できない状態の提示」という覆いの意味が崩れるため
- Dialog と Toast の前後関係は契約で保証しない ([多段表示のルール](multi-display-semantics.md) の線)

## 配置と一括設定

配置は `DialogPlacement` (画面上の位置を指定する器メタ属性の値オブジェクト — [レイアウトのルール](layout-semantics.md)) を Dialog と同じ意味で適用する。実効値の優先順は:

> show の配置引数 > (カスタム View の) 添付 > 一括設定のアプリ既定配置 > Toast の契約既定値

配置はオブジェクトまるごと置換で採用する。Toast の契約既定値は**可視領域 (ウィンドウから OS のバー等の insets を除いた領域) 基準の下部中央 + 上方向オフセット 80 (論理単位 — iOS は pt、Android は dp)**。80 は両 OS の標準的なボトムバー1本ぶん (iOS タブバー 49pt / Material ボトムナビ 80dp) をどちらも上回るよう選んだ単一値で、OS 別の値は持たない。Dialog の既定 (中央) からの意図的乖離であり、値の正はレイアウト共通ケース表 ([レイアウト共通ケース表と OS 差の統制](../architecture/layout-case-table.md)。core/ADR-0032)。

Toast の一括設定は値オブジェクトで、規律は Loading の styling と同型 — 表示 API にスタイル引数は無く、器は各表示の開始時に読み、変更は次の表示から効く。色を含むため KMP の共有コードからは設定できず、各 OS 側で設定する。項目の適用範囲は2種:

| 項目 | 効く範囲 |
|---|---|
| 視覚項目 (背景色・文字色・フォントサイズ・角丸) | デフォルト View のみ。カスタム View には効かない |
| 既定値項目 (既定 duration・**アプリ既定配置**) | デフォルト・カスタムを問わず全 Toast (該当引数・添付の省略時の既定として) |

「アプリ既定配置」は Loading の styling に無い Toast 固有の項目。タブバー等のアプリ側 UI は可視領域の内側にあるため契約既定値では避けられず、器はページ構造を知らない設計なのでライブラリ側の自動検知もしない (各 OS・各 UI 系のボトムバー検知を網羅するのは脆い — core/ADR-0032 で却下)。アプリが一度、自分の画面構成に合わせた既定を設定すれば以後の全 Toast に効く、という逃げ道になっている。

Toast は器メタ属性のうち覆い・外側タップに関わるもの (覆いの色・外側タップで閉じる指定 — [レイアウトのルール](layout-semantics.md) の静的メタ属性が運ぶもの) を持たない。覆いも対話も存在しないため、静的メタ属性に相当する受け口ごと無い (負のコンパイル検査で固定)。

## デフォルト View とカスタム View・演出

- デフォルト View (メッセージ入口が使うライブラリ同梱の見た目) は半透明ダークグレー背景 + 白文字の角丸ピル。テキストは中央寄せで、空文字はそのまま (内容が空のまま) 表示され、長文は複数行に折り返す — メッセージの内容に制限は無い。寸法は Dialog と同じレイアウト規則 (可視領域へのクランプ) に従い、スクロール・省略表示は行わない
- Toast は覆いを持たないため、デフォルト View が自分で背景を描く (core/ADR-0032)。実現は Loading の既定ローディングと同じ作り — カスタム View と同じ提示部品の上に載る、ライブラリ同梱のコンテンツ (core/ADR-0023 と同型)
- デフォルト View は表示時にメッセージを OS の支援技術 (VoiceOver / TalkBack) へ通知 (announce) し、accessibility フォーカスは移動させない。カスタム View の読み上げは View を供給するアプリの責務で、器は通知もフォーカス移動もしない
- カスタム Toast View は Toast 専用レジストリに登録する。Toast は結果を返さないため、Dialog のレジストリが持つ結果報告口・Loading の進捗受け口に相当する仕掛けを落とした軽い形 (core/ADR-0029)
- 演出はカスタム View のみ `DialogTransition` 添付で差し替えられる ([トランジションのルール](transition-semantics.md) の出入りフックがそのまま効く。同ルールの「結果のラッチと配送」は、結果を返すダイアログ向けの仕組みなので Toast では出番がない)
- 添付を省略したカスタム View とデフォルト View は器の既定クロスフェード固定で、デフォルト View に演出を選択する口は設けない

レジストリのエントリは VM 型キーごとに View factory と VM factory の 2 スロットで、再登録はスロット単位の後勝ち (該当スロットだけを置換し他方は保持する)、show 時は呼び出し時点のスナップショットで解決する (core/ADR-0035)。Dialog / Loading のレジストリとは独立しており、同じ VM 型を別々に登録できる。

## 持たない機能

| 持たないもの | 理由 |
|---|---|
| 閉じる操作・タップ消し・スワイプ消し | 消滅の契機は duration の経過のみ (core/ADR-0031)。対話的な消し方が要るなら Dialog |
| 結果通知・表示終了の await | fire-and-forget に存在しない概念。将来必要になれば既存 API を壊さない opt-in の追加で入れられる |
| duration の上限クランプ | 移植元の OS Toast API 由来の制約で、自前の器では制約自体が消滅 |
| タブバー等のアプリ側 UI の自動検知・回避 | 器はページ構造を知らない設計に反し、検知の網羅は脆い。逃げ道は一括設定のアプリ既定配置 (core/ADR-0032) |
| OS ネイティブ Toast への委譲 (Android) | カスタム View が API 30 で非推奨・実質 3.5 秒クランプ・重力指定による配置・多重の順番待ち、という移植元が行き詰まった制約を持ち込まない (core/ADR-0030) |

## 保証すること

- show が受理された表示は、次のいずれかの結末に必ず到達し、表示リスト・タイマー・factory 参照が残留しない: (1) 表示され duration の経過で消える (2) 受理後の失敗として警告ログとともに破棄される (3) 提示先が現れないまま満了し、表示されずに破棄される
- Toast の表示中も、真下を含むページ要素は通常どおり操作できる
- Loading と同時に表示されたとき、Loading が常に前面にある

## してはいけないこと

- **カスタム Toast View に対話部品 (ボタン・入力欄) を置かない**: 完全非対話のため置いても反応せず、利用者には壊れた UI に見える。対話が要る通知は Dialog で作る
- **show の後に表示を前提とした処理を続けない**: 表示は非同期に開始され、失敗しても呼び出し元には何も返らない。表示の成立に依存するフローを Toast で組まない
- **高頻度・大量の多重表示をしない**: 表示 1 枚ごとに器 (Android は Window) を生成するため、常識的な枚数 (数枚) を超える同時表示は重い。契約上の上限は無いが想定利用の範囲で使う

## 形態別の公開面

契約と既定シングルトンの綴り・show の署名と引数名・一括設定の型と項目の綴り・カスタム View の登録の書き方・コード例・framework 固有の注意は、各形態の公開面が持つ:

- [iOS の Toast 公開面](../../ios/api/toast-surface.md)
- [Android の Toast 公開面](../../android/api/toast-surface.md)
- [MAUI の Toast 公開面](../../maui/api/toast-surface.md)
- [KMP の Toast 公開面](../../kmp/api/toast-surface.md)

## 関連

- 挙動の網羅的な契約は TS- 系 Scenario テスト (TS = Toast の ID 接頭辞。安定 ID つき同名テストの運用は core/ADR-0016) が固定する。テストの所在と回し方は [テスト実行規約](../../../handbook/cross/test-execution.md)

隣接する概念文書: [登録と表示の呼び出し面のルール](registration-show-semantics.md) / [レイアウトのルール](layout-semantics.md) / [トランジションのルール](transition-semantics.md) / [多段表示のルール](multi-display-semantics.md) / [Loading のルール](loading-semantics.md)

決定記録 (`kasane/decisions/core/`)。本文中の ADR 参照 (core/ADR-0002 等の流儀の出典) も同ディレクトリの index から辿れる:

| ADR | 内容 |
|---|---|
| [ADR-0028](../../../decisions/core/0028-toast-default-and-custom-view.md) | 既定 View とカスタム View の両対応 |
| [ADR-0029](../../../decisions/core/0029-toast-registry-and-inline-factory.md) | レジストリとインライン factory |
| [ADR-0030](../../../decisions/core/0030-toast-container-implementation-form.md) | 器の実装形と前後規則 |
| [ADR-0031](../../../decisions/core/0031-toast-non-interactive-fire-and-forget.md) | 完全非対話と fire-and-forget |
| [ADR-0032](../../../decisions/core/0032-toast-default-view-and-placement.md) | 既定 View と配置・ToastStyle |
| [ADR-0033](../../../decisions/core/0033-user-factory-failure-boundary.md) | factory 失敗の境界 |
| [ADR-0035](../../../decisions/core/0035-loading-toast-typed-show-vm-factory.md) | VM factory スロットと型指定経路 |
