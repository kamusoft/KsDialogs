# 実装レビュー 011 — MAUI Skill

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

`skills/{en,ja}/ksdialogs-maui/` は、Setup、主要 API のコード例、DI、非同期 configure、カスタム Loading の進捗配送、英日ロックステップ、および閉世界性については現行の concepts・公開実装・テスト・Sample と整合している。機械検査も全項目で違反 0 件だった。

一方、予定 manifest のルートファイルに実際の依存 concept が 5 本欠け、公開 API の掲載規約に対して未掲載・未分類の名前が残る。また、MAUI 利用者に必要な DialogNotifier のラッチと下段を先に閉じた場合の OS 差が具体化されていない。初期生成の完了条件であるファイル単位の源泉完全性と「簡潔でも網羅」を満たさないため、修正後の再レビューが必要である。

## 照合した規約

- `kasane/changes/rollout-user-docs/specs/user-skills/spec.md` — 生成内容、翻訳ロックステップ、manifest 初期版、レビュー 4 層
- `kasane/handbook/cross/user-skill-api-listing.md` — `skills/**` の公開 API 掲載基準、3e の exact 分類、コード例コメント
- `kasane/handbook/cross/comment-policy.md` — always
- `kasane/lessons/inbox/fresh-eyes-review-longlived-docs.md` — 長命文書の用語と自己完結性
- `kasane/lessons/inbox/review-check-public-api-surface.md` — 配布成果物の公開 API 面
- proposal の合意済み例外 (`proposal.md:43`) — 製品コード・テスト非変更のため全ビルドルート実行を省略

## 指摘事項

### 🟠 Major: ルート SKILL.md の source map が実際の依存を 5 本落としている

**該当箇所**: `/private/tmp/docs-refresh-ksdialogs-manifest-planned.json:66`, `skills/en/ksdialogs-maui/SKILL.md:18`, `skills/en/ksdialogs-maui/SKILL.md:49`
**問題点**: 予定 manifest は `ksdialogs-maui/SKILL.md` の源泉を Loading、登録・表示、Toast、MAUI DI の 4 本だけとしている。しかし本文自身が capability map と recipe routing で `Notifier` / ViewModel factory、`DialogOptions` / `DialogPlacement`、`DialogTransition`、typed result、stacked dialogs を説明している (`SKILL.md:18-24`, `49-55`)。したがって、このファイルは少なくとも `core/api/model-binding-semantics.md`、`core/api/layout-semantics.md`、`core/api/transition-semantics.md`、`core/api/result-notification-semantics.md`、`core/api/multi-display-semantics.md` にも依拠する。全体 concepts 網羅検査は「どこかの target に出るか」しか見ないため通過するが、仕様が独立レビューに要求するファイル単位の源泉完全性は満たさない。
**推奨修正**: 予定 manifest の当該 target に上記 5 本を追加する。最終 manifest 書き出し時にも同じ集合を保持し、manifest を入力に 6-①〜6-⑧と 3e を再実行する。

### 🟠 Major: 公開添付 API と既定値 API に未掲載・未分類の名前が残る

**該当箇所**: `skills/en/ksdialogs-maui/references/layout.md:27`, `skills/en/ksdialogs-maui/references/loading.md:155`, `skills/en/ksdialogs-maui/references/toast.md:1`, `maui/KsDialogs.Maui/Presentation/DialogAttachedProperties.cs:28`, `maui/KsDialogs.Maui/Contract/LoadingStyle.cs:51`, `maui/KsDialogs.Maui/Contract/ToastStyle.cs:31`
**問題点**: handbook は、利用者が使う公開プロパティ名・機能名を Skill のどこかに最低 1 回載せ、載せない名前は現行除外リストでオーナー判断済みにするよう要求している (`user-skill-api-listing.md:16-20`, `32-36`, `149-153`)。現状の layout recipe は 10 個の添付属性のうち `ProportionalHeight` と `OffsetX` を一度も示さず、公開されている `GetX` / `SetX` / `XProperty` の対応規則も説明していない。よって `GetProportionalHeight`、`SetProportionalHeight`、`ProportionalHeightProperty`、`GetOffsetX`、`SetOffsetX`、`OffsetXProperty` を含む公開面を、handbook の「機械的に導出できる名前」としても扱えない。さらに `LoadingStyle.DefaultProgressFormat`、`ToastStyle.BuiltinDefaultDuration`、`ToastStyle.BuiltinBackgroundColor` も未掲載で、MAUI の現行除外リストに無い。3e スクリプトは concept のバッククォート token のみを入力にするため `API-name coverage OK` になったが、公開実装からしか拾えないこれらの名前は報告集合へ入っていない。
**推奨修正**: layout reference に全 10 属性名と「各属性は `GetX` / `SetX` / `XProperty` を持つ」という導出規則を短く追加する。Loading / Toast の公開既定値 API は到達可能な reference に用途とともに載せるか、除外基準に該当すると判断するものだけを根拠付きでオーナーへ提示し、承認後に現行除外リストへ追加する。エージェント判断だけで除外しない。

### 🟠 Major: Dialog の結果ラッチと下段先閉じの OS 差が利用者向け契約から落ちている

**該当箇所**: `skills/en/ksdialogs-maui/references/dialogs.md:3`, `skills/en/ksdialogs-maui/references/dialogs.md:62`, `kasane/concepts/core/api/result-notification-semantics.md:35`, `kasane/concepts/core/api/multi-display-semantics.md:35`, `maui/KsDialogs.Maui/Contract/DialogException.cs:96`
**問題点**: Dialog recipe は notifier の `Complete` / `Cancel` を示すが、「最初の報告だけが結果をラッチし、以後は no-op」という二重報告時の公開契約を説明していない。また stacked dialogs を「下段を先に閉じると host OS behavior に従う」とだけ書いており、MAUI 利用者が必要とする結果をこの文書から判断できない。正本では、iOS は上下とも消えて上段が cancelled、Android は下段だけ閉じて上段は操作可能なまま、と明記されている (`multi-display-semantics.md:35-44`)。さらに「提示先画面なしは cancelled ではなく失敗」という契約に対応する公開 `DialogException.PresentationHostUnavailable` が Skill 全体に現れず、他の 5 種類の構成例外だけが列挙された状態である。これは、クロスプラットフォーム MAUI の結果処理を誤らせる実質的な欠落である。
**推奨修正**: dialogs reference に、(1) 最初の notifier 報告だけが有効、(2) `ShowAsync` の結果配送は退出と器の撤去後、(3) 提示先不在は `PresentationHostUnavailable` で fault、(4) 下段先閉じ時の iOS / Android の具体的な見え方と上段結果、を簡潔に追記する。ja 側を同じ見出し構造で更新し、コードブロックは byte 一致を維持する。

### 🟡 Minor: custom transition の失敗と待機上限の契約が読み取れない

**該当箇所**: `skills/en/ksdialogs-maui/references/transitions.md:35`, `kasane/concepts/core/api/transition-semantics.md:185`, `kasane/concepts/core/api/transition-semantics.md:194`, `maui/KsDialogs.Maui/Internals/DialogTransitionRunner.cs:13`
**問題点**: reference は「有限時間で完了すべき」とだけ述べる。実際には hook の fault / cancellation はログへ吸収されて show の失敗にならず、ライブラリは hook にタイムアウトを設けない。これは custom hook のエラー処理と「閉じない」障害を診断する際の重要な挙動で、現在の説明だけでは利用者が反対の前提を置ける。
**推奨修正**: custom hook の lead に「fault / cancellation は結果へ伝播しない」「完了まで上限なく待つため、完了しない Task は撤去と結果配送を止める」を 1〜2 文で追加する。

### 🟡 Minor: Loading の合流と世代の説明が手動 hide の一文だけでは不足する

**該当箇所**: `skills/en/ksdialogs-maui/references/loading.md:1`, `skills/en/ksdialogs-maui/references/loading.md:25`, `kasane/concepts/core/api/loading-semantics.md:37`
**問題点**: `HideAsync` の説明から「複数 caller が joined し得る」ことは分かるが、並行利用がプロセス単位の 1 表示へ合流し、最初の開始から最後の終了まで続くこと、action の失敗も 1 件の終了として数えて呼び出し元へ伝播すること、`HideAsync` が現世代だけを終わらせて旧 action の完了・遅延 progress が新世代へ影響しないことは読み取れない。custom progress のコードと UI-thread 配送自体は正しいが、並行処理時の運用判断に必要な契約が欠ける。
**推奨修正**: 冒頭または手動操作節に、process-wide coalescing、最後の利用までの表示、hide 後の新世代、action failure の伝播を短く追記する。

### 🟡 Minor: Toast の duration 正規化と多重表示モデルが不明確

**該当箇所**: `skills/en/ksdialogs-maui/references/toast.md:1`, `skills/en/ksdialogs-maui/references/toast.md:47`, `kasane/concepts/core/api/toast-semantics.md:51`, `kasane/concepts/core/api/toast-semantics.md:77`
**問題点**: 「省略時は `DefaultDuration`」「各 Toast は独立して消える」は正しいが、0 以下の引数は正の style default、さらに内蔵 1500 ms へフォールバックすること、上限クランプが無いこと、同時 show はキューや置換ではなく同位置にも重なり得ることが説明されない。Toast の移植時に特に差が出る挙動であり、fire-and-forget の呼び出し面だけからは推測できない。
**推奨修正**: message route の lead に duration の正規化と上限なしを、custom route 付近に「各表示は独立タイマーで並存し、自動オフセットしない」を追記する。

## 確認済み事項

- planned manifest を入力に `concepts-coverage-check.py`、`heading-parity-check.py`、`code-block-parity-check.py`、`frontmatter-check.py`、`link-resolution-check.py`、`api-coverage-check.py` を実行し、それぞれ OK。3e の報告候補は 0 件で、現行 MAUI 除外表との exact 分類に未判断候補は無かった。ただし Major 指摘のとおり、3e の入力に現れない公開実装名は別途手動照合した。
- `skills/{en,ja}/ksdialogs-maui/` の 8 ファイルは見出し階層が一致し、対応する全コードブロックは byte 一致。frontmatter の `name` / `description` / `license` / `metadata.language` / `metadata.source` も仕様どおり。
- 内部リンクは全解決。ローカル絶対パス、`skills/`・`kasane/` 等の開発者向け内部経路、競合製品名、旧 identity の残留は 0 件。
- Setup の `KsDialogs.Maui` `0.1.0`、.NET 10、iOS 17、Android API 24 は `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` と一致する。
- `RegisterForDialog` / `RegisterForLoading` / `RegisterForToast`、`AddKsDialogs`、View / VM fallback の優先順位、`ServiceProviderUnavailable` を含む DI 説明は公開実装と一致する。型指定 show の同期・非同期 configure は「VM factory → configure 完了 → notifier → View factory → presentation」の順序に反していない。
- custom Loading の `ILoadingProgressReceiver.OnProgress` は、iOS / Android bridge とも MAUI ViewModel へ UI thread 上で配送する実装と一致する。添付 layout / transition の bridge 変換にも値落ちを確認しなかった。
- proposal の合意済み例外に従い、製品ビルド・テストは実行していない。ドキュメント用コード例のコンパイルもこのレビューでは要求しない。

## アクションプラン

1. planned manifest の `ksdialogs-maui/SKILL.md` source map に欠落 5 concepts を追加する。
2. layout / Loading / Toast の未掲載公開 API を Skill へ追加し、意図的に載せない候補だけをオーナー判断へ送る。
3. DialogNotifier のラッチ、提示先不在、下段先閉じの OS 差を dialogs reference に追記する。
4. transition、Loading、Toast の重要な失敗・並行・時間契約を簡潔に補う。
5. en / ja を同時更新し、manifest を入力に機械検査、3e、独立再レビューを再実行する。
