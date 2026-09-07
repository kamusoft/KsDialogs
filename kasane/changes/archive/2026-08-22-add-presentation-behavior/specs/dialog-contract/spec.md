# dialog-contract デルタ (add-presentation-behavior)

Scenario ID は `PB-<領域>-<NN>` (design Decision 7)。両 Native のテスト名に ID を含めて対応付ける。

## ADDED Requirements

### Requirement: トランジションの添付

コンテンツ定義には、提示演出 (presentation) と退出演出 (dismissal) の2フックを持つトランジションを添付できる (SHALL)。添付は DialogOptions / DialogPlacement と同じ添付機構の第3スロットとし、採用時点も同じ (初回レイアウトパス完了時点のスナップショット。以後の添付変更は当該ダイアログに影響しない)。フックはそれぞれ省略可能で、未指定側には既定のトランジションが適用される。フックが指定された側では既定のトランジションは実行されない (置き換え)。フックはコンテンツのホスト View (宣言的 UI では中身を包むプラットフォーム View) を受け取り、UI スレッドで開始される。オーバーレイの出現・消滅はコンテンツとは別レイヤでライブラリが常に扱い、フックの対象にならない。オーバーレイのフェード時間はトランジションの `overlayDuration` に従い、プリセットでは自身の duration と同じ値になる (カスタムフックで省略時は既定値)。show の引数ではトランジションを受け取らない。

#### Scenario: [PB-TR-01] presentation フックが表示時にちょうど1回、ホスト View を引数に UI スレッドで呼ばれる
- **GIVEN** presentation フックを添付したコンテンツ
- **WHEN** show で表示する
- **THEN** フックがコンテンツのホスト View を引数に UI スレッドでちょうど1回呼ばれ、呼ばれた時点でホスト View はウィンドウ上にありレイアウト済みで、ダイアログは表示状態になる

#### Scenario: [PB-TR-02] 片側だけの添付では未指定側に既定が適用される
- **GIVEN** dismissal フックのみ添付したコンテンツ
- **WHEN** show で表示し、結果を報告して閉じる
- **THEN** 表示は既定のトランジション経路で行われ (presentation フックは呼ばれない)、閉鎖時は添付した dismissal フックが呼ばれる

#### Scenario: [PB-TR-03] 添付なしでは両フックとも呼ばれず既定のトランジションで表示・閉鎖される
- **GIVEN** トランジション未添付のコンテンツ
- **WHEN** show で表示し、閉じる
- **THEN** 表示と閉鎖は既定のトランジション経路で完了し、結果は従来どおりの値で配送される

#### Scenario: [PB-TR-04] 表示後の添付変更は退出に影響しない
- **GIVEN** トランジション A を添付して表示中
- **WHEN** 表示後に添付をトランジション B へ書き換え、結果を報告して閉じる
- **THEN** 退出には A の dismissal フックが使われ、B のフックは呼ばれない

### Requirement: 退出の開始条件と直列化

ライブラリ発の閉鎖信号 — DialogNotifier の報告 (completed / cancelled)・外側タップ・Android 戻るボタン・呼び出し元のキャンセル — が退出を開始する (SHALL)。提示 (presentation) の完了条件は presentation フックの完了とオーバーレイの出現完了の両方である。提示開始前 (ホスト View が表示される前) に閉鎖信号が来た場合は、両フックとも実行せず演出なしで撤去・配送する。presentation の実行中に報告・外側タップ・戻るの閉鎖信号が来た場合は presentation を完走させてから退出を開始する。呼び出し元のキャンセルだけは状態により扱いが異なる (脱出口): presentation 中なら presentation フックをキャンセルして退出へ進み、表示中なら通常どおり退出を開始して dismissal フックを完遂し (Kotlin は NonCancellable 相当)、既に退出中なら実行中の dismissal フックをキャンセルして撤去・配送する。退出中の入力 (外側タップ・戻る・コンテンツ操作) は無視される。複数の閉鎖信号が来ても dismissal フックは高々1回しか実行されない。OS 発の器消失 (画面破棄・提示関係の外部からの解除) では dismissal フックを実行せず、未ラッチなら cancelled をラッチし、ラッチ済みなら既存の outcome を維持して配送する (PB-TR-09 が未ラッチ、PB-TR-12 がラッチ済みの例)。

#### Scenario: [PB-TR-05] presentation 中の閉鎖信号は presentation 完走後に退出する
- **GIVEN** 完了までの間を制御できる presentation フックを添付して表示を開始した直後
- **WHEN** presentation フック完了前に結果を報告する
- **THEN** presentation フックは中断されず完了し、その後に dismissal フックが1回呼ばれ、結果が配送される

#### Scenario: [PB-TR-06] 複数の閉鎖信号でも dismissal フックは1回
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中
- **WHEN** 結果を報告した直後に外側をタップする
- **THEN** dismissal フックはちょうど1回呼ばれ、配送される結果は最初の報告の値になる

#### Scenario: [PB-TR-07] cancelled 経路 (外側タップ) でも dismissal フックが実行される
- **GIVEN** dismissal フックを添付し、外側タップキャンセル有効で表示中
- **WHEN** 外側をタップする
- **THEN** dismissal フックが実行され、完了後に cancelled が配送される

#### Scenario: [PB-TR-08] 呼び出し元キャンセルでも dismissal フックが実行され、形態ごとの規約で観察される
- **GIVEN** dismissal フックを添付して表示中
- **WHEN** show を待つ Task / コルーチンをキャンセルする
- **THEN** dismissal フックが実行されて当該ダイアログは閉じ、呼び出し元は Swift では `.cancelled` の戻り値、Kotlin (Android Native・KMP) では `CancellationException` を観察する (MAUI はこの経路を持たない)

#### Scenario: [PB-TR-09] OS 発の器消失ではフックを実行せず即 cancelled
- **GIVEN** dismissal フックを添付して表示中
- **WHEN** ダイアログを載せていた画面が OS 都合で破棄される
- **THEN** フックは呼ばれず、show は cancelled で確定し、ホスト View (宣言的 UI のホストを含む) は解放される

#### Scenario: [PB-TR-19] 提示開始前の報告は演出なしで配送される
- **GIVEN** 両フックを添付したコンテンツで、View factory の中で直ちに completed を報告する
- **WHEN** show で表示する
- **THEN** presentation / dismissal フックとも呼ばれず、ダイアログは表示されないか即座に撤去され、completed が配送される

#### Scenario: [PB-TR-20] 提示開始前の呼び出し元キャンセルは演出なしで閉じる
- **GIVEN** 両フックを添付したコンテンツで show を開始した直後 (提示開始前)
- **WHEN** show を待つ Task / コルーチンをキャンセルする
- **THEN** 両フックとも呼ばれず、ダイアログは表示されないか即座に撤去され、呼び出し元は形態ごとの規約 (PB-TR-08) でキャンセルを観察する

#### Scenario: [PB-TR-21] none 直後の閉鎖はオーバーレイの出現完了を待ってから退出する
- **GIVEN** none プリセットを添付して表示を開始した直後
- **WHEN** オーバーレイの出現中に結果を報告する
- **THEN** オーバーレイの出現が完了してから退出が始まり、結果が配送される

#### Scenario: [PB-TR-28] presentation 中の呼び出し元キャンセルは presentation をキャンセルして退出する
- **GIVEN** 完了までの間を制御できる presentation フックを添付して表示を開始した直後
- **WHEN** presentation フック完了前に show を待つ Task / コルーチンをキャンセルする
- **THEN** presentation フックはキャンセルされ (完走を待たない)、dismissal フックが1回実行されて器が撤去され、呼び出し元は形態ごとの規約 (PB-TR-08) でキャンセルを観察する

#### Scenario: [PB-TR-29] 退出中の呼び出し元キャンセルは実行中の dismissal フックをキャンセルして脱出する
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中、completed を報告済み (退出中)
- **WHEN** show を待つ Task / コルーチンをキャンセルする
- **THEN** 実行中の dismissal フックはキャンセルされ、器は撤去され、呼び出し元は形態ごとの規約でキャンセルを観察する (ラッチ済みの completed は呼び出し元には届かない — Swift は `.cancelled`、Kotlin は `CancellationException`)

#### Scenario: [PB-TR-22] 退出中の入力は無視される
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中、結果を報告済み (退出中)
- **WHEN** 外側をタップし、コンテンツ内のボタンを操作する
- **THEN** どちらの操作も無視され、配送される結果は最初の報告の値のままである

### Requirement: 結果のラッチと配送

結果 (outcome) は最初の報告で不可逆に確定 (ラッチ) し、以後の報告は no-op である (SHALL)。show の呼び出し元への配送は、dismissal フックの完了・オーバーレイの消滅・器の撤去の後に行う。退出中の二重報告や OS 発の器消失が起きても、配送される値はラッチ済みの outcome である。フックにタイムアウトは設けない。

#### Scenario: [PB-TR-10] show は dismissal フック完了と器の撤去より先に返らない
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中
- **WHEN** 結果を completed で報告する
- **THEN** フックが完了するまで show は返らず、フック完了と器の撤去の後に completed が配送される

#### Scenario: [PB-TR-11] 退出中の二重報告はラッチ済みの値を配送する
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中
- **WHEN** completed(A) を報告し、dismissal フック実行中に completed(B) を報告する
- **THEN** 配送される結果は completed(A) である

#### Scenario: [PB-TR-12] 退出中の OS 発器消失ではフックをキャンセルして即配送する
- **GIVEN** 完了までの間を制御できる dismissal フックを添付して表示中、completed を報告済み
- **WHEN** dismissal フック実行中に画面が OS 都合で破棄される
- **THEN** フックはキャンセルされ (Swift / Kotlin)、フック完了を待たずに器とホスト View が解放され、show は completed で配送される (cancelled には変わらない)

#### Scenario: [PB-TR-23] 提示中の OS 発器消失ではフックをキャンセルして cancelled を配送する
- **GIVEN** 完了までの間を制御できる presentation フックを添付して表示を開始した直後
- **WHEN** presentation フック実行中に画面が OS 都合で破棄される
- **THEN** フックはキャンセルされ、器とホスト View が解放され、show は cancelled で配送される

#### Scenario: [PB-TR-13] 添付なしの既定トランジションでも配送は撤去後
- **GIVEN** トランジション未添付のコンテンツを表示中
- **WHEN** 結果を報告する
- **THEN** 既定の退出処理が完了し器が撤去された後に結果が配送される

### Requirement: フックの失敗

フックの例外・失敗 (Swift の throw / Kotlin の例外 / MAUI の fault) は演出の失敗であり結果の失敗ではない (SHALL)。presentation フックが失敗しても状態機械は表示状態へ進んで報告を受け付け (フックが途中まで変えた見た目はライブラリが復元しない)、dismissal フックが失敗しても器の撤去は続行され、ラッチ済みの outcome が配送される。show は失敗しない。

#### Scenario: [PB-TR-14] presentation フックの失敗でも表示は継続する
- **GIVEN** 例外を投げる presentation フックを添付したコンテンツ
- **WHEN** show で表示する
- **THEN** 状態機械は表示状態へ進み、結果を報告すれば通常どおり配送される (show は失敗しない)

#### Scenario: [PB-TR-15] dismissal フックの失敗でも撤去と配送は完了する
- **GIVEN** 例外を投げる dismissal フックを添付して表示中
- **WHEN** 結果を completed で報告する
- **THEN** 器は撤去され、show は completed で配送される

### Requirement: フックの完了は利用者の責務 (前提条件と脱出口)

フックが有限時間で完了することは利用者の責務であり、ライブラリはタイムアウトを設けない (SHALL)。完了しないフックは契約違反で、その間ダイアログは撤去されず結果も配送されない。ただし、呼び出し元のキャンセル (Swift / Kotlin) と OS 発の器消失では、ライブラリは実行中のフックをキャンセルして (MAUI は参照を切って待つのをやめて) 撤去と配送を行う。デバッグビルドでは一定時間を超えるフックに警告ログを出す (打ち切りはしない)。

#### Scenario: [PB-TR-24] 終了しないフックは呼び出し元キャンセルで脱出できる
- **GIVEN** 決して完了しない dismissal フックを添付して表示中、結果を報告済み
- **WHEN** show を待つ Task / コルーチンをキャンセルする
- **THEN** フックはキャンセルされ、器は撤去され、呼び出し元は形態ごとの規約 (PB-TR-08) でキャンセルを観察する

#### Scenario: [PB-TR-25] 終了しないフックは OS 発の器消失で脱出できる
- **GIVEN** 決して完了しない dismissal フックを添付して表示中、completed を報告済み
- **WHEN** 画面が OS 都合で破棄される
- **THEN** フックはキャンセルされ (MAUI は放置され)、器は撤去され、show は completed で配送される

### Requirement: トランジションのプリセット

トランジションは fade / slide (4方向: top / bottom / leading(start) / trailing(end)、leading/trailing はレイアウト方向に追随) / zoom / none のプリセット factory として一発生成できる (SHALL)。none 以外は時間 (duration) とイージング (easing) を引数で受け取り、省略時は既定値を用いる。easing の型は形態ごとのネイティブ表現とする。1つのプリセットは presentation / dismissal の対称ペアを構成する。none はコンテンツ側の両フックが即完了する (オーバーレイは既定どおり扱われる)。duration が有効範囲外のとき — 0・負値・NaN・無限大、および形態のアニメーション API のミリ秒表現に変換できない大きさ (MAUI の `TimeSpan` は総ミリ秒が `uint.MaxValue` を超える値、`TimeSpan.MaxValue` を含む) — そのフックは例外にもオーバーフローにもならず即完了する。overlayDuration も同じ規則。

#### Scenario: [PB-TR-16] プリセット指定で追加のフック記述なしにトランジションが差し替わり、覆いの時間も揃う
- **GIVEN** duration を指定した slide プリセットを添付したコンテンツ
- **WHEN** show で表示し、閉じる
- **THEN** 表示・閉鎖ともプリセットのフック経路で完了し、オーバーレイのフェード時間は指定した duration と同じで、結果は従来どおり配送される

#### Scenario: [PB-TR-17] none プリセットではコンテンツ側の待ちなしで表示・閉鎖される
- **GIVEN** none プリセットを添付したコンテンツ
- **WHEN** show で表示し、結果を報告する
- **THEN** コンテンツのフックは即完了し、閉鎖の完了待ちはオーバーレイの既定処理のみで、結果が配送される

#### Scenario: [PB-TR-18] duration 0 のプリセットは即完了する
- **GIVEN** duration に 0 を指定した fade プリセットを添付したコンテンツ
- **WHEN** show で表示し、結果を報告する
- **THEN** 例外にならず、コンテンツのフックは即完了して結果が配送される

#### Scenario: [PB-TR-26] 負の duration のプリセットは即完了する
- **GIVEN** duration に負値を指定した slide プリセットを添付したコンテンツ
- **WHEN** show で表示し、結果を報告する
- **THEN** 例外にならず、コンテンツのフックは即完了して結果が配送される

#### Scenario: [PB-TR-27] 有効範囲外の duration (NaN / 無限大 / 変換不能な大きさ) のプリセットは即完了する
- **GIVEN** duration に形態の有効範囲外の値 (iOS `.nan` / `.infinity`、Android `Duration.INFINITE`、MAUI `TimeSpan.MaxValue`) を指定した zoom プリセットを添付したコンテンツ
- **WHEN** show で表示し、結果を報告する
- **THEN** 例外にもオーバーフローにもならず、コンテンツのフックとオーバーレイ (overlayDuration に同値が設定される) の両方が即完了して結果が配送される

### Requirement: 多段表示の系列挙動の固定

多段表示の観察挙動 (multi-display-semantics.md の保証4点と OS 差分表) は、Native 2実装への同名 Scenario テストで検証される (SHALL)。OS 間で期待値が割れる挙動は同文書の差分表を正とする。

#### Scenario: [PB-MD-01] 結果確定で自分のダイアログだけが閉じる
- **GIVEN** ダイアログ A の上に B を重ねて表示中
- **WHEN** B に結果を報告する
- **THEN** B だけが閉じて B の show が結果を受け取り、A は表示されたまま結果未確定で残る

#### Scenario: [PB-MD-02] 2枚重ねて上から順に閉じる
- **GIVEN** ダイアログ A の上に B を重ねて表示中
- **WHEN** B → A の順に完了報告する
- **THEN** B の show と A の show がそれぞれ自分の結果をちょうど1回受け取る

#### Scenario: [PB-MD-03] 重ね出し中の外側タップは手前のみに届く
- **GIVEN** ダイアログ A の上に B を重ねて表示中 (外側タップキャンセル有効)
- **WHEN** 外側をタップする
- **THEN** B だけが cancelled になり、A は表示されたまま結果未確定で残る

#### Scenario: [PB-MD-04] 下の段を先に閉じたときの挙動 (OS 差分表に従う)
- **GIVEN** ダイアログ A の上に B を重ねて表示中
- **WHEN** A (下の段) に先に結果を報告する
- **THEN** iOS では B の show が cancelled で確定して返る (OS 発の消失のため B の dismissal フックは呼ばれない)。Android では B が表示されたまま残り、その後の報告で通常どおり結果が返る (期待値の正は multi-display-semantics.md の差分表)

#### Scenario: [PB-MD-05] 器消失時の cancelled 確定
- **GIVEN** ダイアログを表示中
- **WHEN** 報告を経ずに器が画面から外れる (画面破棄)
- **THEN** show は cancelled でちょうど1回確定する

### Requirement: 表示中のウィンドウ寸法変化への追随

表示中にウィンドウ寸法・可視領域インセット (システムバー由来) が変化した場合 (回転を含む)、ダイアログは凍結済みの実効属性のまま新しい寸法・インセットで再配置される (SHALL)。属性の実効値は変化しない (スナップショット凍結の維持)。IME (ソフトキーボード) によるインセット変化は本 Requirement の対象外 (キーボードではダイアログを動かさない)。

#### Scenario: [PB-WN-01] 回転後も配置規則が新しい寸法で成立する
- **GIVEN** 属性を添付したダイアログを表示中
- **WHEN** 画面を回転する
- **THEN** ダイアログは凍結済み属性と新しいウィンドウ寸法・インセットから導かれる位置・サイズに再配置される

#### Scenario: [PB-WN-02] ウィンドウ寸法のみの変化に追随する
- **GIVEN** 属性を添付したダイアログを表示中
- **WHEN** インセットは変わらずウィンドウ寸法だけが変わる (マルチウィンドウのリサイズ等)
- **THEN** ダイアログは新しい寸法から導かれる位置・サイズに再配置される

#### Scenario: [PB-WN-03] 可視領域インセットのみの変化に追随する
- **GIVEN** 基準領域 visibleArea の属性を添付したダイアログを表示中
- **WHEN** ウィンドウ寸法は変わらずシステムバーのインセットだけが変わる
- **THEN** ダイアログは新しいインセットから導かれる位置に再配置される
