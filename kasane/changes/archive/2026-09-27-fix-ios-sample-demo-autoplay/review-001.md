# レビュー結果: fix-ios-sample-demo-autoplay (001 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

`samples/ios` の自動再生の開始点を、最初の画面の `.task` + `Task.yield()` から「`scenePhase` が `.active` になった時点で独立した `Task` を起こす」形へ置き換えた変更。合意スコープ (exploration.md「決定事項」: 範囲は Sample の自動再生だけ・待ち方はシーンが前面でアクティブになってから・プロセス起動につき 1 回だけ・崩れていた `samples/ios` だけ直す・理由コメントの置き換え) を過不足なく満たし、ビルドは通り、証跡は修正前後の A/B で主張を裏付けている。変更そのものに Critical / Major は無い。ただし証跡の実測が、ライブラリ iOS の Toast が core 契約 (提示環境の不在では提示先の出現を待って表示する) を満たしていない疑いを示しており、これはこの change の外 (ライブラリ本体) の所見として起票判断を求める。

## 照合した規約

- cross/comment-policy.md (always) — 追加コメント 5 行を禁止参照・禁止記述類型・デルタスペック構文キーワードの節ごとに照合。`scripts/comment-policy-lint.py --advisory` の検出も 0 件
- cross/sample-parity.md (`samples/**` を触る・撮影支援の起動引数の変更) — 「例外枠」「撮影支援の起動引数」「してはいけないこと」節を照合
- cross/runtime-behavior-verification.md (実行時挙動が絡む不具合修正の完了判定) — 規約 1〜3 (修正前の再現・同一手順での解消確認・evidence/ の証跡) を照合
- cross/local-development-setup.md (Sample のビルド) — ビルド手順の確認にのみ使用
- lessons: code-review.md (重点観点 L-001)、process.md (L-001〜L-003)

## 確認した観点と結果

### ビルド・静的検査

| 項目 | 結果 |
|---|---|
| `xcodebuild -project samples/ios/KsDialogsSample.xcodeproj -scheme KsDialogsSample -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` (DerivedData はレビュー用の一時領域) | BUILD SUCCEEDED。`SampleMenuScreen.swift` に警告なし (Swift 6.0 / 配備先 iOS 17.0) |
| `scripts/identity-lint.py --paths` (change ディレクトリ・変更ファイル) | 検出 0 件 |
| `scripts/local-path-lint.py --paths` (change ディレクトリ) | 検出 0 件 |
| Sample の自動テスト | 存在しない (sample-parity「Sample を自動テストの代わりに使う」禁止の裏返しで、Sample は実測で確かめる対象)。実測は evidence/ で確認 |

### 観点ごとの判定

| 観点 | 判定 | 根拠 |
|---|---|---|
| 自動再生が「シーンが前面でアクティブになってから」始まるか | 適合 | `samples/ios/KsDialogsSample/SampleMenuScreen.swift:93-101` の `.onChange(of: scenePhase, initial: true)` が `.active` 以外を捨てる。`initial: true` なので最初から `.active` で現れた場合も取りこぼさない。`onChange(of:initial:_:)` の 2 引数版は iOS 17 からで、配備先 17.0 と整合 |
| プロセス起動につき 1 回だけか | 適合 | 回数の保証は `SampleCaptureAutoPlay.consumeDemo()` (static の消費済みフラグ、`@MainActor`) が持ち、画面側は `.active` のたびに呼んでも 2 回目以降は nil で抜ける。背面から戻したときに再生しないことは evidence の「1 回限り」の実測 (設定アプリを前面にして戻す) で確認済み |
| 引数なし起動の挙動を変えていないか | 適合 | `consumeDemo()` が nil を返して即 return。画面構成・文言には手を入れていない。evidence の「引数なしの起動」実測でも通常メニュー |
| `Task.yield()` を外したことがパネル系デモに影響しないか | 適合 | 構造的にも、`onChange` の中で起こす `Task { }` は onChange の処理を終えた後の別の MainActor ジョブで走るため、旧実装の「初回表示と同じターンで画面状態を変えない」配慮 (ターンを 1 回譲る) を包含する。実測でも修正後の `transition-dialog` 2/2・`layout-dialog` 2/2 で全画面表示が出ている |
| 画面の `task` ではなく独立 `Task` にした判断 | 適合 | シーンの状態変化で再生中のデモ (Loading の長い処理・Dialog の結果待ち) が打ち切られないための選択で、コメントにも理由がある。`Task { }` は MainActor 文脈を継承し、`autoPlay()` の MainActor 分離と整合 (Swift 6 でビルド警告なし) |
| 合意スコープ外の変更 | なし | コード変更は `SampleMenuScreen.swift` 1 ファイル。handbook・`kasane/config.yaml` は不変 (決定事項どおり)。`samples/kmp/iosApp`・`samples/maui` は実測で崩れていないため不変 (決定事項「崩れているものだけ直す」どおり) |
| sample-parity 撮影支援機構の一致要件 | 適合 | 外から見える契約 (引数キー名・安定デモ ID・不正値の倒れ方・1 回限り) は不変。待ち方は置き場・型の作りに当たり、ルートごとに idiomatic でよい範囲 (「例外枠」節) |
| 観測用の一時改変の戻し | 適合 | evidence は `scenePhase` 到着時の key window 有無を一時的な診断出力で観測したと書くが、作業ツリーの diff は意図した変更だけで、診断出力は残っていない |
| コメント規約 | 適合 | 追加コメントは現在形の設計説明で、作業文書・change 名・通番への参照や履歴記述を含まない。旧コメントの「9 デモの通し撮影で安定を確認済み」(実測の履歴) は削除され、新しい待ち方の理由に置き換わった (決定事項どおり) |
| 実行時挙動の検証規約 (規約 1〜3) | 適合 | 修正前に同一環境で症状を再現 (basic-dialog 0/3・default-loading 1/3・custom-loading 0/3・default-toast 0/5)、修正後に同一手順 (terminate → 1.5 秒 → `--demo` 付き launch → 連続撮影) で 14 デモ全件を確認、判定を evidence 本文にテキストで残している。画像 3 枚は archive で消えるが、何が写っているかを表で文章化済み |
| 証跡の弁別力 (lessons code-review L-001) | 適合 | 観測 (指定デモの起動直後の状態が出るか) は修正の効き (提示先が得られる時点まで再生を遅らせる) で結果が分かれ、修正前ビルドで実際に失敗している。パネル系の「yield を外しても壊れない」命題も、壊れていれば修正後の観測が失敗する形で弁別できる。原因仮説「`.active` 到着時に key window がある」も診断出力で 3 回観測しており、コード読解だけでの断定になっていない |
| 証跡の識別値・個人情報 | 問題なし | ローカル絶対パス・UDID・ユーザー名なし (起動例は `<bundle-id>` のプレースホルダ)。画像 3 枚はステータスバーが時刻・電波・電池のみで、キャリア名・通知・個人データの写り込みなし |

## 指摘事項

### 🟡 Minor ライブラリ iOS の Toast が「提示環境の不在」の契約を満たしていない疑い — 起票の判断が要る

**該当箇所**: `evidence/autoplay-measurement.md` (修正前の表の `default-toast` 列)、`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:222-237`、`ios/Sources/KsDialogs/Presentation/ApplicationKeyWindowProvider.swift:24-28`
**問題点**: core の toast-semantics (「提示環境の不在」の行) は、取り付け先ウィンドウが無いとき Toast は失敗せず**提示先の出現を待って表示する**と定める。ところが修正前の実測では `default-toast` が 0/5 (0.3 秒刻みの撮影でも一度も写らない) で、この契約どおりなら出るはずの Toast が出ていない。exploration.md の原因表は Toast を「影響なしの見込み (未実測)」としていたが、実測はそれを否定しており、決定事項の「Toast = 出現を待つ、を現状のまま契約とする」の前提も崩れている。コードから立つ仮説 (未検証) は次のとおり: iOS の Toast は `UIWindow.didBecomeKeyNotification` だけを再取り付けの契機にしている一方、提示先の選択は「前面でアクティブなシーンの key window」に絞っている。起動直後にシーンが `inactive` のまま window が key になると、通知の時点では提示先が nil で、その後シーンが `.active` になっても key window の変化が無いので通知が再び来ず、duration の満了で破棄される。evidence が「起動直後は `inactive` で始まり、続いて `active` が届く」と観測している点とも合う。
この change の実装 (Sample) 側の不備ではなく、Sample を直したことで症状は消えている。ただし利用者アプリが起動直後に Toast を出す場合に同じく表示されない可能性があり、ライブラリ本体 (別の能力) の契約乖離なので、ksn-core の付随修正の振り分けでは「起票」に当たる。
**推奨修正**: この change では何も変えない。オーケストレーターがオーナーへ、ライブラリ iOS の Toast の提示先待ちが `.foregroundActive` への遷移 (`UIScene.didActivateNotification` 等) を契機に含んでいない疑いとして、ksn-explore の簡易起票を提案する。起票時は、上の仮説を実環境で確かめること (コード読解だけで断定しない) と、Android の同状況 (resumed Activity 待ち) の照合を含める。あわせて、exploration.md の原因表の Toast 行と決定事項の Toast の記述が実測と食い違っていることを、蒸留時に concepts へ書き足す候補 (exploration「未決の論点」の Loading の件) と一緒に扱うのがよい。

### 🔵 Suggestion basic-dialog の修正前の失敗を署名で残す

**該当箇所**: `evidence/autoplay-measurement.md` (修正前の表の `basic-dialog` 列)
**問題点**: 修正前の `basic-dialog` は「3 回とも起動直後に落ちる」とだけあり、落ちた箇所 (Sample の `assertionFailure` か、提示先不在の `presentationHostUnavailable` か) を示すテキストが無い。原因の特定は exploration のコード読解に依っている。画像は archive で消えるため、archive 後に読めるのはこの記述だけになる。
**推奨修正**: 必須ではない。撮り直しの機会があれば、クラッシュログやコンソール出力のうち失敗の署名 (メッセージ 1 行) を `scripts/log-sanitize.py` に通して抜粋し、表の注記に添える。

## アクションプラン

1. (この change の外) Toast の契約乖離の疑いをオーナーに諮り、簡易起票するかを決める — 上の Minor
2. (任意) basic-dialog の修正前の失敗署名を証跡に足す — 上の Suggestion
3. 変更そのものは現状のままでよい。APPROVED
