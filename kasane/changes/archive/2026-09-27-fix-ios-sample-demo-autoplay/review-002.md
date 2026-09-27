# レビュー結果: fix-ios-sample-demo-autoplay (002 回目)

**日付**: 2026-09-27
**判定**: APPROVED

## サマリー

001 回目で APPROVED 済みの `samples/ios` に続き、deviation.md の合意 (KMP iOS Sample の待ち方の同梱) に沿って、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift` の自動再生の開始点を置き換えた差分と、その実測の証跡追記をレビューした。提示先のポーリング (前面アクティブのシーンの key window を 50ms 刻み・上限 2 秒で確かめる待ち) と `Task.yield()` が取り除かれ、`samples/ios` と同じ「`scenePhase` が `.active` になった時点で独立した `Task` から再生する」形になっている。開始点のブロックは `samples/ios` と一字一句同じで、共有コード (`samples/kmp/shared`) には差分が無く、ビルドは通る。証跡は修正後の 14 デモ全件・1 回限り・引数なし起動を確かめている。Critical / Major / Minor は無い。

## 照合した規約

- cross/comment-policy.md (always) — 追加コメント 5 行・削除コメントを、許容参照・禁止参照・禁止記述類型・デルタスペック構文キーワードの節ごとに照合。`scripts/comment-policy-lint.py --advisory` の対象ファイルの検出 0 件
- cross/sample-parity.md (`samples/**` を触る・撮影支援の起動引数の変更) — 「例外枠」「撮影支援の起動引数」「してはいけないこと」節を照合
- cross/runtime-behavior-verification.md (実行時挙動が絡む不具合修正の完了判定) — 規約 1〜3 と「証跡の残し方」を照合
- cross/local-development-setup.md (Sample のビルド) — KMP iOS のビルド手順の確認にのみ使用
- lessons: code-review.md (重点観点 L-001)、process.md (L-001〜L-003)

## 確認した観点と結果

### ビルド・静的検査

| 項目 | 結果 |
|---|---|
| `xcodebuild -project samples/kmp/iosApp/KsDialogsSampleKmp.xcodeproj -scheme KsDialogsSampleKmp -destination 'generic/platform=iOS Simulator' ARCHS=arm64 ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO build` (DerivedData はレビュー用の一時領域) | BUILD SUCCEEDED。`SampleMenuScreen.swift` に警告・エラーなし (Swift 6.0 / 配備先 iOS 17.0)。`ARCHS` を付けない generic 指定では x86_64 のリンクで失敗するが、これはリンク対象の KMP framework が arm64 の Simulator 用だけであることによるもので、今回の差分とは無関係 (handbook の手順は機種名指定で arm64 になる) |
| `scripts/identity-lint.py --paths` (change ディレクトリ・変更ファイル) | 検出 0 件 |
| `scripts/local-path-lint.py --paths` (同上) | 検出 0 件 |
| Sample の自動テスト | 存在しない (Sample は実測で確かめる対象)。実測は evidence/ で確認 |

### 観点ごとの判定

| 観点 | 判定 | 根拠 |
|---|---|---|
| deviation の項と実装の 1 対 1 照合 (process L-003) | 適合 | deviation の「提示先を確かめる待ちを iOS Sample と同じ『シーンが前面でアクティブになってから再生』に揃える」に対し、`waitUntilPresentationHostIsReady()`・`hasPresentationHost`・待ちの刻みと上限の定数 2 つ・それだけが使っていた `import UIKit` が消え、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:94-102` に `.onChange(of: scenePhase, initial: true)` が入っている。`Task.yield()` の除去も `samples/ios` と同じ (下の Suggestion を参照) |
| `samples/ios` との対称性 | 適合 | `onChange(of: scenePhase` から閉じ括弧までのブロックを 2 ファイルで比べ、コメントを含めて差分なし。`autoPlay()` の冒頭から待ちが消え、取り出し直後に `switch` へ進む形も同じ。違いは取り出しの API (`SampleCaptureAutoPlay.shared.consumeDemo(options:)`) と、インライン・パネルをこの画面が受け持つ分岐だけで、これは変更前からのルート間の差 |
| シーンが前面でアクティブになってから再生するか | 適合 | `.active` 以外を捨てる。`initial: true` で、最初から `.active` で現れた場合も取りこぼさない。`onChange(of:initial:_:)` は iOS 17 からで、配備先 17.0 と整合 |
| one-shot の維持 | 適合 | 回数の保証は共有コードの `SampleCaptureAutoPlay.consumeDemo(options:)` (`samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SampleCaptureAutoPlay.kt:17-23`、消費済みフラグ) が持ち、画面側は `.active` のたびに呼んでも 2 回目以降は null で抜ける。evidence の kmp 節で「設定アプリを前面にして戻しても Toast は再び出ない」を確認済み |
| 引数なし起動の挙動 | 適合 | `consumeDemo` が null を返して即 return。メニュー構成・文言には手が入っていない。evidence の kmp 節でも通常メニュー |
| 共有コード (`samples/kmp/shared`) に変更が無いか | 適合 | `git status` で `samples/kmp` 配下の変更は `iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift` の 1 件だけ |
| 待ちを外したことで失われるもの | 問題なし | 旧実装の待ちは上限 2 秒で「間に合わなければそのまま進む」作りで、上限超過時の失敗を結果表示側に任せていた。新実装は条件そのもの (シーンの `.active`) を上限なしで待つので、コールド起動が遅い KMP iOS でも待ちが先に尽きることがなく、むしろ堅くなる。旧実装が持っていた「画面の Task のキャンセルで待ちを打ち切る」性質は、待ち自体が無くなったので不要 |
| Swift 6 の分離 | 適合 | `Task { }` は `onChange` のクロージャ (MainActor) の文脈を継承し、`autoPlay()` と `model` の MainActor 分離と整合。ビルド警告なし |
| sample-parity 撮影支援機構の一致要件 | 適合 | 外から見える契約 (引数のキー名・安定デモ ID・不正値の倒れ方・1 回限り) は不変。待ち方は置き場・型の作りに当たる。今回で ios と kmp (iOS) の待ち方が揃い、maui (iOS) は `OnAppearing` 起点のまま (決定事項「崩れているものだけ直す」どおりで、ルートごとに idiomatic でよい範囲) |
| 観測用の一時改変の戻し | 適合 | 作業ツリーの diff は意図した変更だけで、診断出力は残っていない |
| コメント規約 | 適合 | 追加コメントは現在形の設計説明で、作業文書・change 名・通番への参照、履歴記述、デルタスペック構文キーワードを含まない。削除されたコメントのうち「9 デモの通し撮影で安定を確認済み」は実測の履歴で、消えるのが規約に沿う |
| 実行時挙動の検証規約 | 適合 | kmp (iOS) は修正前から崩れていなかった (修正前の表で 4 デモとも 2/2) ため、規約 1 の「修正前の再現」の対象ではなく、置き換えで壊していないことの確認が要点。修正後は `samples/ios` と同じ手順 (terminate → 1.5 秒 → `--demo` 付き launch → 連続撮影) で 14 デモ全件を確かめ、判定を本文にテキストで残している。画像 2 枚は archive で消えるが、何が写っているかを表で文章化済み |
| 証跡の弁別力 (code-review L-001) | 適合 | kmp の修正後の観測 (起動直後の状態が出るか) は、「`.active` を待つだけで足りる」という命題が偽なら失敗する形になっている — 待ちを持たない ios の修正前ビルドが同じ環境で basic-dialog 0/3・default-loading 1/3 と実際に失敗しているため、待ちが足りなければ kmp の basic-dialog / default-loading も失敗するはずで、それが 3/3 で出ている。旧実装のポーリングの有無を操作しても、観測はライブラリの提示可否そのもの (ダイアログ・Loading が出るか) で取っており、操作と観測が同じ述語に寄っていない |
| 証跡の識別値・個人情報 | 問題なし | 追記した本文にローカル絶対パス・UDID・ユーザー名なし (起動例は `<bundle-id>` のプレースホルダ)。画像 2 枚 (`kmp-basic-dialog-after.png`・`kmp-default-loading-after.png`) はステータスバーが時刻・電波・電池のみで、キャリア名・通知・個人データの写り込みなし。画面は Sample のメニューとダイアログ / Loading だけ |
| 画像の内容と本文の一致 | 適合 | `kmp-basic-dialog-after.png` はメニューの上に `こんにちは、KsDialogs!` と `キャンセル` / `OK` のダイアログ、`kmp-default-loading-after.png` はインジケータと `Loading...` の Loading と結果表示 `結果: 処理中` が写っており、evidence の画像表の記述どおり |

## 指摘事項

### 🔵 Suggestion deviation の項に `Task.yield()` の除去も書き添える

**該当箇所**: `deviation.md` (唯一の項)、`samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift:109-115`
**問題点**: deviation の項は「提示先を確かめる待ち」の置き換えだけを書いているが、実装はその待ちに続く `Task.yield()` (初回表示と同じターンで画面状態を変えないための配慮) と、その理由コメントも取り除いている。`samples/ios` と「揃える」という記述から読み取れ、`onChange` の中で起こす `Task` が別の MainActor ジョブで走るため機能上も包含されている (001 回目で確認済みの理屈と同じ) ので実害は無い。ただし archive 後に deviation だけを読んだ人が、パネル系デモの配慮まで外したことを実装と 1 対 1 で照合するには一言あるほうがよい。
**推奨修正**: 必須ではない。deviation の項の末尾に「待ちに続く `Task.yield()` も iOS Sample と同じく取り除く」と一文添える。

## アクションプラン

1. (任意) deviation の項に `Task.yield()` の除去を書き添える — 上の Suggestion
2. 変更そのものは現状のままでよい。APPROVED
3. 001 回目の Minor (ライブラリ iOS の Toast が提示環境の不在の契約を満たしていない疑いの起票判断) は、この差分とは独立に引き続きオーケストレーターの判断待ち
