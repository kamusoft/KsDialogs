# セカンドオピニオン: add-sample-capture-automation (code-001)
**相方**: codex / **label**: so-code-add-sample-capture-automation / **日付**: 2026-08-27 / **対象**: worktree の未コミット変更全体 (samples 4ルートのデモ駆動モード + ドキュメント + lint 設定)
---
# レビュー結果: add-sample-capture-automation

**判定**: CHANGES_REQUESTED
**件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0

ホスト側のビルド・テスト・lint 成功結果を前提に静的レビューしました。CA-SA-01〜07への実装構造は概ね整合しており、Android の重複 extra 後勝ちは合意済み乖離として除外しています。一方、証跡の配置と CA-SA-07 の検証成立性に修正必須の問題があります。

## 指摘事項

### [🟠 Major] スクリーンショットが媒体ファイルの許可された置き場にない

**該当箇所**: `verification/four-route-walkthrough/notes.md:51`

**問題点**: 20件の実機・シミュレータ画像が `verification/` 配下に保存されています。`ksn-core` の媒体ホワイトリストでは、動作証跡の静止画は `evidence/` 配下に限定されています。現状では archive 時の媒体削除規律から外れ、画像内個人情報を機械検査できない媒体が意図せず長期保存される可能性があります。

**推奨修正**: PNGとその索引を `evidence/four-route-walkthrough/` 等へ移し、`notes.md`・`parity-check.md` の参照を更新してください。移動後の全画像について、個人要素がないことも再確認してください。

### [🟠 Major] CA-SA-07 の証跡が複数アプリで画面・Activity再生成を実行していない

**該当箇所**: `verification/four-route-walkthrough/notes.md:98`
**関連箇所**: `samples/maui/KsDialogs.Sample.Maui/Platforms/Android/MainActivity.cs:7`

**問題点**: iOS側の検証はパネルを開いて戻っただけですが、SwiftUIの `fullScreenCover` は背後の `SampleMenuScreen` を破棄・再生成せず、通常は `.task` も再実行しません。またMAUI Androidは `ConfigurationChanges.Orientation` を処理するため、記録された回転ではActivityが再生成されません。したがって6アプリ中複数で、CA-SA-07の「画面・Activityが再生成されても再発火しない」を実際には検証できていません。`tasks.md` 5.3を完了扱いにする根拠として不足しています。

**推奨修正**: 同一プロセス内でルート画面・Page・Activityを実際に作り直す検証、または自動再生入口を2回起動して2回目にディスパッチされないことを保証する決定的なライフサイクルテストを各対象経路に追加してください。その結果に合わせて証跡とタスク状態を更新してください。

### [🟡 Minor] MAUIの自動再生Taskが観測されず、失敗が黙って失われる

**該当箇所**: `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:45`

**問題点**: `Dispatcher.Dispatch(() => _ = PlayAsync(demo))` は返されたTaskを破棄します。`ShowAsync`やナビゲーションが非同期に失敗した場合、例外が観測されないまま自動再生だけ消費済みとなり、契約した初期状態に到達できません。

**推奨修正**: ディスパッチ後のTaskを明示的に待機・観測するラッパーを設け、例外をassertionまたはログへ確実に報告してください。

## アクションプラン

1. 動作証跡を `evidence/` 配下へ移す。
2. CA-SA-07を実際の再生成経路または決定的なライフサイクルテストで再検証する。
3. MAUI自動再生の非同期例外を観測する。
4. 証跡とタスク状態を更新後、再レビューする。

## 突き合わせ結果 (2026-08-27, ホスト側 review-001.md と照合)

| 指摘 | 出典 | 採否 | 扱い |
|---|---|---|---|
| 証跡 PNG が verification/ 配下 (媒体規約は evidence/ 限定) | 双方一致 | **確定 (Major)** | evidence/ へ移設・参照更新 |
| MAUI 自動再生 Task の破棄 (失敗が無言で消える) | 双方一致 | **確定 (Minor)** | Task 観測の修正 |
| ADR cross/0010 と実装の矛盾・申し送り記録なし | ホストのみ | **確定 (Major)** | deviation / proposal に申し送り記録 (ADR 改訂は蒸留時) |
| CA-SA-07 証跡が iOS / maui-android で実際の再生成を起こせていない | 相方のみ | **採用 (Major)** | 根拠強 (fullScreenCover の非破棄・ConfigurationChanges) — 証跡の再構成 + 可能な範囲の決定的検証 |
| design.md の取り下げ前提記述の残存 | ホストのみ | 確定 (Minor) | 改訂注記の追加 |
| kmp-android play() の else 網羅性 / コメント2件 | ホストのみ (Suggestion) | 裁量対応 | 修正バッチに同梱 |
| deviation.md 1件目「オーナー確認待ち」のまま | ホストのみ (Suggestion) | 保留 | 完了報告でオーナー判断を仰ぐ |

未解決 (両者矛盾) : なし
