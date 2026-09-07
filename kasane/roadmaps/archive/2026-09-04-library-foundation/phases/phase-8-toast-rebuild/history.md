# phase-8-toast-rebuild 議論履歴

## 2026-08-27: フェーズ開始・基本要件の提示

オーナーから Toast の基本要件が提示され、決定事項として記録した:

- ページをまたげる (常に Window 最上位)
- ページの要素を妨げない (非モーダル・表示中も操作可能)
- 多重起動可 (起動順に重なるだけ)
- アニメーションは Dialog / Loading 踏襲
- 配置は Dialog 踏襲

あわせて論点「デフォルト View を用意するか否か」が提示され、これを最初の議論対象とする。

## 2026-08-27: デフォルト View を用意するか否か

scout 調査の結果を土台に議論:

- 原典の Toast は完全にカスタム View 前提 (`IToast.Show<TView>()` のみ)。message だけ渡す API はなく、一言出すにも View 自作が必須だった
- 原典の Obsolete 宣言の理由は README・コードのどこにも書かれていない (「廃止予定」の宣言のみ)。状況証拠として Android 実装が OS Toast のカスタム View 機構 (API 30 で非推奨化) に依存していた点はあるが、明記された根拠はない
- KsDialogs の Loading には既定 View の型紙がある (core/ADR-0023: 内蔵コンテンツ + LoadingStyle + show(message:) 入口)。Dialog は既定 View なし

選択肢: A. デフォルト View + カスタム View 併設 (Loading 同型) / B. カスタム View のみ (原典踏襲) / C. デフォルト View のみ。

**採用: A (両対応)**。理由: 最頻ユースケース「メッセージを一言出す」が1引数で済む / Loading の実装型紙 (ADR-0023) が再利用できて API の対称性も取れる / Android の素の OS Toast より手数が多いと選ばれない。→ core/ADR-0028 (proposed) 起票。

派生論点として「デフォルト View の見た目と styling の受け口」を論点リストに追加。

## 2026-08-27: カスタム View の供給経路

選択肢: A. インライン factory のみ / B. インライン factory + 型指定レジストリ (Dialog / Loading 同型)。

当初の推奨は A (Toast は結果を返さないためレジストリの仕掛けが不要・API 表面を小さく) だったが、オーナーから「レジストリは共有層から呼べるという役割があるはず。インライン factory のみだと UI 層が漏れる」と指摘。レジストリの本質は再利用機能ではなく、共有層 (KMP commonMain・MAUI の VM 層) が UI 型に触れずに表示を呼ぶための間接層であり、A では KMP commonMain からカスタム Toast を呼べず、既定 View (message 入口) だけ共有層から呼べる非対称が生じる。当初の比較表には「共有層から呼べるか」という判断軸そのものが欠けていた。

**採用: B (型指定レジストリ + インライン factory の両対応)**。レジストリは notifier / 結果型を落とした軽い形、インライン factory は UI 層内の単発表示向け。→ core/ADR-0029 (proposed) 起票。

## 2026-08-27: 器の実装形 (Dialog / Loading 機構との共有範囲)

選択肢: A. Loading の器 (core/ADR-0026) の非モーダル派生・1 Toast 1器 / B. OS ネイティブ Toast へ委譲 (Android) / C. 単一共有器に複数 Toast を積む。

**採用: A**。理由: 提示スタック不参加の器は「ページをまたぐ・常に最前面」の実現形として Loading で実証済みで、レイアウト・演出の共有部品にそのまま乗れる / 非モーダル化は器の一点変更 (iOS: ヒットテスト透過、Android: 非フォーカス + タッチ素通し) で済む / 1 Toast 1器なら重なり順が追加順で自然に成立。B は API 30 のカスタム View 非推奨・3.5 秒クランプ・SetGravity 制約・OS キュー直列化で要件を満たせず却下。C は共有部品の「器 = 1 コンテンツ」前提を崩すため却下。

あわせてオーナーから「Loading vs Toast は Loading が最上位としておいた方が良い」と追加指示。Loading はモーダルで操作を止める覆いであり、その上に通知が乗ると覆いの意味が崩れる。追加順だけでは Loading 表示中に出た Toast が上に来てしまうため、明示的な順序規則 (Loading が常に前面) として決定に含めた。Dialog との前後関係は core/ADR-0006 (保証しない) の線を維持。→ core/ADR-0030 (proposed) 起票。

この決定により論点「Dialog / Loading 機構との共有範囲」は解消 (共有部品はそのまま再利用・器は非モーダル派生)。

## 2026-08-27: Toast との対話と消え方

選択肢: A. 完全非対話 (面もタッチ素通し・時間経過でのみ消滅・show は void・duration は show 引数で既定 1500ms) / B. 面はタッチ受け (タップ即消し・カスタム View 内ボタン可)。

**採用: A**。理由: Android OS Toast の意味論と一致し「ページの要素を妨げない」に最も忠実 (真下の要素も操作可) / 対話が欲しいケースは Dialog が受け皿 / タッチ受けの opt-in 後付けは互換だが逆方向は破壊的。オーナーからの補強: Toast はページをまたいで生き残るため、View に対話要素を置いても「ページが変わったらどうするんだ」となる — 非対話はページ横断要件からの必然。原典 Android の 3.5 秒クランプは継承しない。→ core/ADR-0031 (proposed) 起票。

この決定により論点「API 設計 (duration・消え方・多重表示の挙動)」は解消 (多重は基本要件 + ADR-0030 の追加順で既決)。

## 2026-08-27: 出入りの演出の適用形 (phase-5-3 申し送りの解消)

「載せる」こと自体は基本要件 (アニメーションは Dialog / Loading 踏襲) で既決。残りの「添付の面」と「既定の演出」を議論。

選択肢: A. Loading と完全同型 (カスタム Toast のみ DialogTransition 添付可・デフォルト View はクロスフェード固定) / B. デフォルト View にも演出選択の口 (ToastStyle にプリセット指定)。

**採用: A**。理由: 「カスタムは添付可・既定は器の標準演出」の規則が Dialog / Loading / Toast で1本化される / 既定 View への口は後から ToastStyle に互換で足せるが、開けた口を閉じるのは破壊的 / クロスフェードは OS Toast の出入りの見え方とも近い。Toast は結果を持たない (core/ADR-0031) ため、core/ADR-0017 のうち結果のラッチと配送は対象外でフック機構のみが乗る。

既存機構の適用であり単独の ADR は起こさない (propose 時に toast-semantics の concepts へ落とす)。

## 2026-08-27: 「新 Toast のコンセプト」「OS ネイティブ機構との関係」の解消

両論点はここまでの決定 (ADR-0028〜0031) の積み上げで実質的に答えが出たため、オーナー確認の上で解消扱いとした:

- Obsolete 理由の調査: 一次情報なし (README・コードとも「廃止予定」宣言のみ)。状況証拠は Android 実装の OS Toast カスタム View 依存 (API 30 で非推奨化)
- 再設計の方向: OS Toast 依存を捨てた自前の器 (ADR-0030) により、原典の死因と思われるものと制約 (3.5 秒クランプ・SetGravity 配置・多重不可) を構造的に解消
- 提供価値: カスタム View (OS では API 30 以降不可)・Dialog 踏襲の配置と演出・多重の重なり・クランプなし duration・3形態統一 API。iOS には OS 標準 Toast がないため統一 API での提供自体が価値
- 意味論は OS Toast (非対話・時間で消える) を踏襲。対話が欲しいケース (Snackbar 的) は Dialog が受け皿

## 2026-08-27: デフォルト View の見た目と styling の受け口

選択肢: A. OS 慣習寄せピル + ToastStyle / B. Material Snackbar 風 / C. styling 受け口なし。見た目は A の方向で進める中で、オーナーから「既定位置はタブバーと被らないようにする必要がある。Toast の座標系はスクリーン座標なので下中央そのままだと被りそう」と指摘。

layout-semantics.md:111 を確認: 配置基準の既定は visibleArea (window − insets) で、OS のシステムバーは避けるが、アプリのタブバー / ボトムナビは visibleArea 内側のアプリ側 UI のため避けられない。器はページ構造を知らない (ADR-0030) ため自動検知も原理的に不可。

対応の選択肢: A. 既定オフセットで慣習的に避ける + ToastStyle にアプリ既定配置 / B. タブバー自動検知 / C. 既定を中央に。

**採用: A**。既定配置は visibleArea 下部中央 + ボトムバー1本ぶんの上方向オフセット (具体値は propose で確定、iOS 49pt / Material 80dp 目安)。ToastStyle に「アプリ既定の配置」を追加し、アプリが自分のクロームに合わせて一括調整できるようにする (LoadingStyle にない Toast 固有項目)。見た目は半透明ダークグレー + 白文字の角丸ピル (Toast は覆いがないため既定 View が自分で背景を描く)。ToastStyle の規律は ADR-0023 同型。Dialog (中央) との既定配置の乖離は ADR-0008 の線で明示。→ core/ADR-0032 (proposed) 起票。

## 2026-08-27: Sample のデモ項目 (論点「Sample 通し」の解消)

規約は確立済み (cross/ADR-0007 パリティ・cross/ADR-0010 デモ駆動撮影) のため、デモ項目のラインナップだけ確定して論点を閉じた:

1. メッセージ Toast (show(message) 一発)
2. カスタム View Toast (レジストリ + インライン factory)
3. 多重起動 (起動順に重なる)
4. 配置・duration の変更
5. 機能間多重起動 (オーナー提案): Dialog / Loading / Toast を同時に出し、重なり規則 (Loading 常に前面 = ADR-0030、Dialog と Toast は保証なし = ADR-0006) と非モーダル併存を検証する項目

これで論点はすべて解消。次は ksn-propose での提案化。

## 2026-08-27: 提案化 (add-toast) と多重の重なりの明確化

ksn-propose で change [add-toast] を作成 (L 級)。mock は案A (OS Toast 準拠ピル、固定角丸 22) をオーナー承認、長文の複数行折り返し検証をオーナー指示で Sample に追加。codex セカンドオピニオン (second-opinion-spec-001.md) で Critical 1 件 (MAUI レジストリ共有が maui/ADR-0001 と矛盾) を含む 11 指摘 → 採用 8 / 部分採用 2 / 降格 1。

指摘から派生した設計判断をオーナー確定: **多重 Toast の重なりは z-order のみ (同一配置は同座標に重なる。自動で位置をずらす可視スタックはしない)** — 基本要件「起動順に重なるだけ」の素直な解釈で、ADR-0030 の「自前のスタック管理はしない」と整合。Sample の Toast Stack は視認用に placement をずらして表示する。
