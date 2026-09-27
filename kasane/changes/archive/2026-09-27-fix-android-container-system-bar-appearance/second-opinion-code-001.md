# セカンドオピニオン: fix-android-container-system-bar-appearance (code-001)
**相方**: codex / **label**: so-code-fix-android-container-system-bar-appearance / **日付**: 2026-09-27 / **対象**: HEAD (d1396b0) に対する未コミットの作業ツリー (android/ksdialogs-core・ios/Tests/KsDialogsTests・scripts/scenario-id-coverage.py)
---
## 指摘事項

### 🟠 Major — Toast の表示期限が観測の待機上限より短い

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemBarsTests.kt:221`、`ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:18`

**問題点**: Android の Toast は 8 秒で消えますが、表示待ちだけで最大 10 秒、さらにバーの観測に 1 秒を使います。iOS も表示期限が 5 秒で、取り付け待ちと安定待ちがそれぞれ最大 5 秒です。負荷で取り付けが遅れると、観測中に正常な Toast が消えてテストが失敗し得ます。成功時の結果だけでは、この時間競合は除外できません。

**推奨修正**: 各待機上限と観測時間の合計を超える表示期限を設定し、判定時にも器が表示中であることを確認してください。これにより、期限切れとシステムバーの退行を区別できます。

提示された実行結果を前提に静的レビューを行い、ビルド・テストは再実行していません。上記以外に、差分と合意済み `deviation.md` に対する Critical／Major の指摘はありません。

**判定: CHANGES_REQUESTED**


## 突き合わせ結果

ホスト側レビュー: review-001.md (NEEDS_DISCUSSION / 一致検証 VALID)

| 指摘 | 出典 | 採否 | 理由 |
|---|---|---|---|
| Toast の表示期限 (Android 8 秒 / iOS 5 秒) がテストの待機上限 + 観測時間より短く、負荷で正常な Toast が観測中に消えて落ち得る (Major) | 相方のみ | **採用** | 該当箇所と実害シナリオが特定されている。手元でもホスト高負荷時に iOS の既存 Toast テストが時間切れになっており、負荷で待ちが伸びる環境は現実にある |
| 旧来のフラグの OR 読みが、テーマの明るい地向け + WindowInsetsController での暗い地向けの明示が食い違う提示先で退行を入れる (Major) | ホストのみ | 確定 (要判断) | 相方は指摘していないが矛盾ではない。設計判断を要するためオーナーに諮る |
| 作法 0 のコメントが Android 11 について不正確 (Minor) | ホストのみ | 確定 | 修正サイクルで直す |
| PB_SB_07 の最初のアサーションが OS の返す値と比べている / iOS の失敗し得ない `#expect` (Suggestion) | ホストのみ | 確定 (任意だが修正サイクルに同梱) | テストの判定力に関わり、修正が小さい |
| test-execution.md の API 30 境界の記述 (Suggestion) | ホストのみ | 蒸留へ申し送り | handbook は実装タスクで書き換えない |
| 引き継ぎの入口が 2 通り (Suggestion) | ホストのみ | 見送り | 挙動に問題なし。統一は過剰 |

件数: 確定 4 (うち要判断 1) / 採用 1 / 降格 0 / 見送り・申し送り 2 / 未解決 0
