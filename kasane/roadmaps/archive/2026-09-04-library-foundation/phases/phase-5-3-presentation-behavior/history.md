# phase-5-3-presentation-behavior 議論履歴

## 2026-08-17: phase-5-dialog-completion から分割 (論点13超の膨張のため3テーマに分割)

phase-5-1 (レイアウト共通仕様テストの器) の完了が先行条件。

## 2026-08-21: 多段表示 — OS 間挙動差の記録方式

現状確認から: 委譲の実装 (UIKitDialogPresentationSurface / ActivityDialogPresentationSurface) と器消失検知は phase-4 で配置済み (core/ADR-0006 現行照合 2026-08-15)、OS 間挙動差の実測も multi-display-semantics.md に記録済み。残りは「挙動差を共通仕様テストとしてどう自動化するか」の方式決めだった。

選択肢:
- A. Scenario テスト方式 — Native 2実装への同名 Scenario テスト、期待値の正は multi-display-semantics.md の差分表 (採用)
- B. 挙動ケース表の新設 — core/behavior-spec 等に系列スキーマ + approvedDiff 流用 (却下: 4〜6本の系列のためにスキーマ新設は過剰)
- C. ドキュメント記録のみ (却下: 退行検知できず ADR-0007 の自覚済み弱点「テストがなければ絵に描いた餅」へ逆行)

採用理由: ADR-0009 が手書きミラーを却下したのはレイアウトのマトリクス性ゆえで、同 ADR 自身が「マトリクスでない検証は少数の Scenario テストが担う」と切り分け済み。多段表示の挙動は系列でケース数 4〜6本程度。挙動差の統制は、オーナー確認済みの multi-display-semantics.md 差分表を正としてテストが参照する形で保つ。Android は instrumented test の「ローカル実行 + 証跡記録」運用 (ADR-0009 と同じ)。

## 2026-08-21: MD-d (キーボード表示中の Android 戻るボタン) の期待値確定

選択肢:
- A. 契約化しない — OS の既定処理 (IME が戻るを先に消費) に委ねる挙動として「保証しないこと」に確定 (採用)
- B. 契約化する — 「キーボード表示中の1回目の戻るは cancelled にしない」をルール化し instrumented Scenario で守る (却下)

採用理由: この挙動はライブラリのコードでなく OS の戻るイベント配送の産物。契約化すると OS 変化 (predictive back・IME の消費変更) 時に移植元方式の明示ガード実装を負う義務が生まれる。ADR-0006 の哲学「契約は自分が制御できる観察可能な意味論のみ」に照らして委譲側と判断。保証しなくても「結果はちょうど1回」の意味論は崩れず、実害は端末差の可能性開示に収まる。移植元との観察結果の同一性 (実測 2026-08-15) は現状のまま維持される。

処理: result-notification-semantics.md「まだ決めていないこと」からの削除と「保証しない」側への移記は ksn-propose のスコープへ。ADR は起こさない (ADR-0006 の哲学の適用にすぎず、覆すコストも低い文書上の確定のため)。

## 2026-08-21: キーボード・回転追随の継承範囲

ksn-scout の原典調査で前提が変わった: 原典に「追随」機能は実質存在しない。AutoRotateForIOS は ShouldAutorotate 差し替えの回転抑止スイッチのみ (位置追随は presentation controller の毎パス再計算が担い、サイズは提示時固定)。Android KeyboardListener はフラグを立てるだけで位置調整もリサイズもせず、唯一の実効果 (キーボード中の戻る無視) は MD-d 決定で継承しないと確定済み。KsDialogs 側は両 OS とも毎レイアウトパスでウィンドウ寸法・インセットを再読して再計算する構造で、回転追随は機構的に成立見込み (実機裏取り未)。

選択肢:
- 推奨案 (採用): キーボードは「動かさない・リサイズしない」を継承 (リスナー非移植) / 回転は既存再計算機構による追随を仕様化し実機裏取り + Scenario テスト / AutoRotateForIOS は移植せず phase-7 (Loading の回転ロック用途) へ申し送り
- B. 原典完全踏襲 (AutoRotateForIOS も移植) (却下: deprecated 系譜 API・iOS 専用属性が契約に入る負担・用途がニッチ)
- C. キーボード回避 (位置調整/リサイズ) の新規追加 (却下: 原典に無い機能追加でロードマップの範囲外)

補足: 原典 iOS の「提示後サイズ固定 (動的リサイズ非対応)」は継承せず、毎パス再計算方式で上書きする意図的乖離 (core/ADR-0008 の枠)。ADR は起こさない (原典踏襲 + 既存 ADR (0008/0014/0015/0016) の適用で、独立の決定を含まないため)。concepts への明文化 (キーボードで動かさない・回転追随) は ksn-propose のスコープへ。

## 2026-08-21: immersive 時のシステムバー表示状態の引き継ぎ

現状確認: 原典は immersive 未対応 (システムバー関連は insets 取得のみ)。KsDialogs は DialogWindowSystemBars が edge-to-edge・透明化・アイコン明暗の引き継ぎまで実装済みだが、表示/非表示状態は未引き継ぎ。Android 11 未満経路は systemUiVisibility 丸ごとコピーで暗黙に引き継がれる一方、11+ 経路は appearance のみコピーする非対称があった。

選択肢:
- A. 表示状態も引き継ぐ (採用) — R+ で rootWindowInsets の可視判定 + hide(systemBars) + systemBarsBehavior コピー
- B. 引き継がない (アプリ責務として文書化) (却下: クラスの宣言済み目的「見えを変えない」と矛盾が残り、全画面アプリでバーが再出現する)

採用理由: 新機能ではなく既存クラスの宣言済み目的の完成であり、新旧経路の非対称解消でもある。一般公開ライブラリとして全画面アプリ (ゲーム・動画・キオスク) は無視できない利用者層。原典未対応のため意図的乖離 (ADR-0008 の枠)。ADR は起こさない (Android 実装内部の品質完成で、境界を越えず覆すコストも低いため)。検証は実機確認 + 証跡記録、iOS 側の確認1点 (提示機構の継承で足りるか) を提案スコープへ。

## 2026-08-21: Kotlin IosDialogGateway のキャンセル追随

現状確認: 機械面 showViewModel は既に KSDInteropDialogShowHandle を返す形 (ios/Sources/KsDialogs/Interop/KsDialogsInteropBridge.swift) だが、Kotlin gateway は戻り値を捨てており「キャンセルでも表示が残る」契約不適合が KMP Kotlin 経路だけに残っていた。

選択肢:
- A. 本フェーズで追随 (採用) — invokeOnCancellation からハンドルの取り消しを引く。doc コメント訂正も同時
- B. doc 訂正のみで挙動は現状維持 (却下: ADR-0005 で却下済みの状態の追認)
- C. phase-6 へ先送り (却下: phase-6 は model-binding-di がテーマで筋違い。蒸留の申し送りも本フェーズ宛て)

採用理由: kmp/ADR-0005 が「Kotlin 側 suspend 経路の契約追随にも流用できる」と敷いたレールの実行で、新しい設計判断を含まない (新規 ADR 不要)。4形態の結果通知契約適合が揃う。

## 2026-08-21: アニメーションフック (presentation / dismissal)

原典調査 (ksn-scout): フックは基底ビューの引数なし void 仮想メソッドで、OS 側 250ms トランジションとの時間による暗黙同期 (README 自認の脆さ)。既定はライブラリ側クロスフェード 250ms。KsDialogs 現状は両 OS とも OS 既定トランジション任せでフック未実装。KsDialogs の登録モデル (View factory + 宣言的 UI) には継承する基底がなく、運び方の再設計が必要。

選択肢:
- A. dismissal フックのみ完了通知つき (ホスト推奨案 — presentation は onAppear 等の標準手段で表現可能、ADR-0014 適用) (却下: オーナー判断)
- B. 両フック導入・完了通知つき (採用 — オーナー決定)
- C. 移植しない (却下: cancelled 経路の退出演出が不可能になる)

オーナー決定と理由: 片側だけの対応は非対称で使いにくい (公開ライブラリの API 一貫性を優先)。あわせて既定トランジションは OS 既定を維持せずライブラリ実装のクロスフェードとする — iOS の crossDissolve は演出が見えるが、Android の OS 既定は実質トランジション無しのため。供給は DialogOptions / DialogPlacement と同じコンテンツ添付方式で行う方向 (クロージャは添付機構で技術的に運搬可能)。

残論点: 運び方の入れ物 (DialogOptions のクロージャプロパティ vs 別添付スロット) と、宣言的 UI 系でのフック形状 (View 参照が無い) は次ターン以降・propose の design で確定。

## 2026-08-21: アニメーションフックの運び方 — 別添付スロット DialogTransition

選択肢:
- A. 別添付スロット DialogTransition (採用) — DialogOptions / DialogPlacement と同じ添付機構の第3スロット
- B. DialogOptions にクロージャプロパティ内蔵 (却下: ADR-0015 の値オブジェクト契約 (等値比較・スナップショット凍結) に振る舞いが混入し ADR 改訂が必要になる)

採用理由: クロージャは等値比較不能で値オブジェクトの性格を壊す。フックは提示/退出のライフサイクル点で呼ばれるだけで凍結契約に載せる必要もない。別スロットなら ADR-0015 は無傷で、添付 DSL に置き場が1つ増えるだけ。4形態の書き味サンプル (Android View 拡張プロパティ / Compose KsDialogAttributes 引数 / MAUI code-behind 添付プロパティ) で確認済み。

残論点: 宣言的 UI 系 (Compose / SwiftUI) のフック形状はオーナー指示によりこのフェーズ内で調査して確定する (propose 送りにしない)。

## 2026-08-21: 宣言的 UI 系のフック形状 — 統一形 (ホスト View) + プリセット

調査 (オーナー指示でフェーズ内実施): Compose のコンテンツは DialogComposeContentView (AbstractComposeView 派生、doc に「器から見ればただの View」と明記)、SwiftUI は DialogSwiftUIHost が UIHostingController に載せる構造で、宣言的 UI のコンテンツも器の中では必ずプラットフォーム View に包まれている (ADR-0011 の単一系統)。「View 参照が無い」問題は器レベルでは存在しないと判明。

選択肢:
- A. 統一形 — フックは常にホスト View を受け取る `suspend (View) -> Unit` / `(UIView) async -> Void` の1型 (採用)
- B. 宣言的 UI は慣用形 — phase 通知 (CompositionLocal / Environment) + 中身が完了報告 (却下: 完了報告義務の呼び忘れ→器の閉鎖待ち続けにタイムアウト救済設計が必須、入場の自己管理、Compose の完了検知イディオムが玄人向け。両技術系の View 定義スタイルのサンプル比較で確認)

オーナー決定: A で確定。ただし B は「もっと綺麗にできる余地がある」感触つきで将来の非破壊追加候補として記録 (添付スロットと退場待ち機構は共通のため後から足せる)。追加要件: fade / slide 等の定番アニメは DialogTransition のプリセット (factory) として一発呼び出しできるようにする。プリセットの具体一覧は propose で確定。

## 2026-08-21: プリセットの引数化 (追記)

オーナー追加要件: プリセットは時間・easing を引数で指定できる形とする。easing は形態ごとのネイティブ表現 (Android = Interpolator / iOS = UIKit タイミング指定 / MAUI = Easing) をそのまま受け、形態横断の easing 抽象は作らない。プリセット一覧と引数既定値は propose で確定。

## 2026-08-21: フェーズ議論完了

全6論点 + 派生2論点 (フックの運び方・宣言的 UI のフック形状) を解消。ADR 起票2件 — core/ADR-0016 (挙動系 Scenario テスト)・core/ADR-0017 (アニメーションフック一式)、いずれも proposed。phase-7 へ申し送り1件 (回転ロックの要否)。ksn-propose (フェーズ由来入力) へ移行。

## 2026-08-21: 提案化完了 (add-presentation-behavior)

ksn-propose で L 級の提案一式を作成。design Decision 10件、specs 6 capability / 17 Requirement / 61 Scenario (PB-xx-NN ID)、tasks 8グループ 36タスク、ui/ (案B 承認 + プリセット動的モック承認)。相方 (codex) スペックレビュー4ラウンド (採用 31 / 降格 0) を経て APPROVED。議論で決めた「タイムアウトなし」は相方指摘 (終了しないフックの宙吊り) を受けて「前提条件 + 脱出口 + デバッグ警告 (案C)」に精緻化 (オーナー決定)。ADR-0017 を2箇所訂正 (ライブラリ発の閉鎖経路に限定、Swift フックの throws 化)。次は ksn-orchestrator へ。
