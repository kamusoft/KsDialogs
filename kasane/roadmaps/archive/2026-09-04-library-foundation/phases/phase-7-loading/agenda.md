# Loading

既定ローディングとカスタム View 版 Loading を実装する change フェーズ。原典では既定ローディングは Dialog 機構と別経路のため、Dialog 完全化と独立して着手可能。

## 論点

議論の土台: [原典 Loading 仕様サーベイ](artifacts/origin-loading-survey.md) (2026-08-25 調査)

(全論点解消 — 決定事項へ移動済み)

## 決定事項

- **既定ローディングの実現方式と styling の受け口** (2026-08-25、旧論点1): 既定ローディングはライブラリ同梱のローディングコンテンツ (スピナー + メッセージ) をカスタム View 版と同じ共有部品に載せて実現する (内蔵コンテンツ方式)。原典のような native 直組みの別経路は作らない。styling は内蔵コンテンツ固有の項目 (インジケータ色・メッセージのフォント/色・既定メッセージ・進捗フォーマット) を1つのスタイル値オブジェクトにまとめ、アプリ側で一括設定する口のみとする — show / StartAsync 相当にスタイル引数は設けない。各表示の開始時に読み、設定変更は次の表示から効く。色を含むため KMP 共有コードからは設定不可 (DialogOptions と同じ非対称)。オフセットは DialogPlacement、覆いの色は DialogOptions (overlayColor) が既にカバーし、重複属性は作らない。→ core/ADR-0023 (proposed) に起票 (styling 受け口を含めて追記済み)
- **カスタム View 版と Dialog 機構の共有範囲** (2026-08-25、旧論点2): 器 (画面への載せ方) は Loading 専用のオーバーレイとし、OS の提示スタック (present / DialogFragment の積み重ね) には載せない。コンテンツの載せ方・レイアウト規則 (中央 + オフセット)・演出の添付は Dialog の core 仕様を再利用する。器メタ属性・演出のうち Loading に効かせる範囲は論点1・6で個別に詰める。→ core/ADR-0022 (proposed) に起票

- **出入りの演出はカスタム View 版のみ添付可・既定は固定** (2026-08-25、旧論点6): カスタム Loading は Dialog と同じ第3スロット (`DialogTransition`) 添付で演出を差し替えられる。既定ローディングはライブラリ既定のクロスフェード固定 (スタイル値オブジェクトに演出は入れない — 添付との二重供給を避ける)。core/ADR-0017 のうち Loading に効くのは両フック (ホスト View 統一形・完了通知つき) と既定クロスフェード・覆いの別レイヤ常時フェードで、「結果のラッチと配送」は結果を持たない Loading には対象外。原典もカスタムのみ差し替え可・既定固定で同じ構造。→ core/ADR-0022 (proposed) に適用範囲として追記
- **器メタ属性は全面適用 + 外側タップキャンセルのみ常に無効** (2026-08-25、旧論点8、論点2決定の残り確認): レイアウト系の器メタ属性 (layoutArea・dialogMargin・比率・overlayColor・placement) は Dialog と同じ意味・同じ供給経路 (添付 + 表示 API の引数は placement のみ — core/ADR-0015 踏襲) で Loading にも効く。既定ローディングは利用者の添付面を持たないため、配置・オフセット (原典 LoadingConfig の OffsetX/Y 相当) は表示 API の placement 引数で指定する。isCanceledOnTouchOutside は Loading では常に無効 (添付されても効かない) — Loading はユーザー操作では閉じない (原典踏襲)。→ core/ADR-0022 (proposed) に適用範囲として追記
- **多重利用は合流モデル** (2026-08-25、旧論点4): 重なった利用は1つの表示に合流し、表示は最初の開始から最後の終了まで継続する。渡された処理 (StartAsync 相当の action) は必ず実行される — 原典 iOS の「黙って無視・action 未実行」は継承しない。メッセージ・進捗は最新の報告が勝つ (後勝ち)。明示の hide は合流数によらず即閉じる。→ core/ADR-0024 (proposed) に起票
- **進捗通知はスコープ形 + 形態別イディオム** (2026-08-25、旧論点3): 処理ブロックを渡す形 (原典 StartAsync 相当) を主経路とし、処理には進捗報告口が渡る。C# は IProgress&lt;double&gt;、Swift はクロージャ ((Double) -> Void)、Kotlin / KMP は関数型 (suspend ブロック内で呼べる (Double) -> Unit)。値は 0〜1、表示は後勝ち (合流モデルの決定に従う)。スコープ形は開始・終了が対になり合流カウントと噛み合う。ADR 化はせず (確立済みの写像流儀 cross/ADR-0005 の適用)、Loading 契約の concepts に明記する
- **回転ロックは導入しない** (2026-08-25、旧論点7、phase-5-3 申し送りの決着): 表示中の回転抑止は持たない。表示中の回転は既存の再配置規則 (layout-semantics の PB-WN-01) に従う。調査で原典 Sample の AutoRotateForIOS 指定は Loading では事実上ノーオペ (Dialog 側しか読んでいない) と判明し、継承すべき実挙動が存在しなかった。phase-5-3 の Dialog 側決定 (移植しない) と整合。必要になれば器メタ属性 (core/ADR-0014) として非破壊追加できる。ADR 化はせず、Loading 契約の concepts に「持たない」と明記する
- **表示スコープは移植しない** (2026-08-25、旧論点5): Loading は常にウィンドウ全体を覆う。原典 isCurrentScope 相当のスコープ指定 (引数・属性) は設けない — 原典で iOS のみ有効・Android は無視の半端な機能で、オーナーも使った記憶がないと証言。将来必要になれば非破壊で追加できる。ADR 化はせず (可逆な判断)、Loading 契約の concepts に「持たない」と明記する

## TODO

- [x] 論点の解消 (2026-08-25 全8論点解消)
- [x] Sample 通し: 既定・カスタム両方のデモをパリティ準拠で追加 (完了条件 — add-loading tasks 6.x で4ルート実施、verification/sample-walkthrough に証跡)
- [x] ksn-propose で変更提案を起こす (add-loading)

## 実装結果 (2026-08-26 反映)

change [add-loading](../../../../changes/archive/2026-08-26-add-loading/proposal.md) として実装完了 (review-003 APPROVED / verify-001 VALID / 全6ルート 0 failures)。決定事項はすべて実装され、ADR は core/0022〜0024 の accepted 昇格に加え、実装設計判断から core/0025 (専用レジストリ + 進捗受け口)・core/0026 (器の実装形)・core/0027 (プロセス内 coordinator) を起票・accepted。契約は concepts [core/api/loading-semantics.md](../../../../concepts/core/api/loading-semantics.md) に蒸留済み。

スペックからの乖離は deviation.md に 14 項目 (要点: LD-CO-11 の世代遮断は進捗のみ — setMessage は世代に紐づかないグローバル操作 / 提示先不在の遅延 View 生成失敗は表示なしで完走 / MAUI の `DialogOptions` を public 化)。

申し送りのルーティング:

- **Android の最終進捗順序の回帰ガード** (review-003 Minor — 保証がディスパッチャ選択の1行依存でテストなし) → 簡易起票 [add-android-loading-progress-order-guard](../../../../changes/add-android-loading-progress-order-guard/exploration.md) へ
- **Sample 寸法トークン候補** (ui/brief.md — カスタムデモカードの寸法・内蔵コンテンツの固定値) → 見送り (2026-08-26 オーナー承認)。デザイントークンの概念ファイルが未確立で、現状は各ルートの View 側の生値 + brief の記録で足りる。必要が生じたら docs フェーズ以降で再考
