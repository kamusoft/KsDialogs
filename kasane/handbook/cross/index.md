# handbook: cross

リポジトリ横断の規約。作業を始める前に、`always` の文書と、担当範囲 (触るファイル・行う作業) に当たる文書を本文まで読む。

| 文書 | 適用のきっかけ | 種別 |
|---|---|---|
| [comment-policy.md](comment-policy.md) | **常時** — コメント構文を持つ全ファイル (`*.cs` `*.kt` `*.swift` / XAML / ビルドスクリプト、テストコードを含む) を書くとき | rule |
| [test-execution.md](test-execution.md) | テストを実行するとき・テスト結果を報告するとき・変更の完了を判定するとき (MAUI は `dotnet test` と Android / iOS 互換面の 3 実行) | rule |
| [verification-ci.md](verification-ci.md) | CI が回す範囲と手元に残る範囲を確認するとき・CI の失敗 (とくに ios job の提示待ちの時間切れ) を切り分けるとき・`.github/workflows/` を変えるとき | rule |
| [sample-parity.md](sample-parity.md) | `samples/` を触るとき (デモ項目の追加・変更、文言・色トークンの変更、OS 操作 (戻る・回転) への反応の変更、撮影支援の起動引数の変更) | rule |
| [runtime-behavior-verification.md](runtime-behavior-verification.md) | 実行時挙動 (表示 / dismiss の演出・多段表示のタイミング・IME・OS 提示機構) が絡む不具合を調査・修正し、完了を判定するとき | rule |
| [local-development-setup.md](local-development-setup.md) | 環境構築・git worktree での作業開始・Gradle ルートのビルドやテストを始めるとき・4 形態の Sample をビルドまたは起動するとき・`dotnet` が拾う SDK と workload set を確認するとき・MAUI iOS で Xcode 版数の食い違いを解消するとき | guide |
| [user-skill-api-listing.md](user-skill-api-listing.md) | `skills/**` を生成・更新するとき・docs-refresh の API 名網羅検査を仕分けるとき | rule |
| [docs-refresh-timing.md](docs-refresh-timing.md) | docs-refresh を走らせるとき・変更を蒸留するとき (`skills/.manifest.json` の concepts スナップショットを書く時点) | rule |
| [user-skill-writing-style.md](user-skill-writing-style.md) | `skills/**` の references を新設・改稿するとき・ja から en へ同期するとき | rule |
| [diagnostic-message-language.md](diagnostic-message-language.md) | ライブラリ本体 (4 形態) に失敗型の case・例外文言・警告ログを足すか変えるとき・Skills の診断表で実装文言を引用するとき | rule |
| [aiforms-origin-reference.md](aiforms-origin-reference.md) | 未移植の Dialog / Loading 機能を実装するとき・Dialog / Loading の不具合や挙動差を調査するとき (移植完了で廃止する時限規約) | rule |

外部参考リポジトリのローカルパスは規範ではなく記述のため、concepts 側の [参考リポジトリの在り処](../../concepts/cross/reference/reference-repositories.md) が持つ。comment-policy.md (配布物) が許容する「他リポジトリのコード識別子」の参照先 (移植元 AiForms.Maui.Dialogs・先例 KsSettingsView / KsAppKMP) の在り処も同文書が唯一の情報源。
