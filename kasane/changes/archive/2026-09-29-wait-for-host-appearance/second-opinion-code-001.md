# セカンドオピニオン: wait-for-host-appearance (code-001)
**相方**: codex / **label**: so-code-wait-for-host-appearance / **日付**: 2026-09-28 / **対象**: コミット e2d4ac9 から作業ツリーまでの未コミットの変更すべて (android/ksdialogs-core・ios/・kmp/ksdialogs-kmp・maui/・samples/ios・samples/kmp・scripts/scenario-id-coverage.py・kasane/changes/wait-for-host-appearance/)
---
## 独立コードレビュー結果

ホスト側のテスト・lint 成功を前提に、変更アーティファクト、`deviation.md`、実装差分と新規ファイルを静的に確認しました。合意済みの乖離は指摘に含めていません。以下の2件は、成功済みテストが通る条件の外で起きる問題です。

### 🟠 Major — 提示直前に提示先が消えると Dialog の show が終わらない

**該当箇所**: `ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift:36`  
**問題点**: Presenter が提示先を確認した後、中身の factory やレイアウト処理を経て、提示面は提示先を再取得します。この時点で取得できない分岐は提示完了コールバックだけを呼びます。器は一度も提示されず、結果も確定・配送されないため、`DialogPresenter.swift:117` の結果待ちが残ります。  
**推奨修正**: この分岐で器を「提示先を失った」結末まで進め、show の結果待ちを必ず解放してください。factory の実行中に提示先が失われるテストを追加してください。

### 🟠 Major — 保留中の登録経路 Toast が後から登録された factory を使う

**該当箇所**: `ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:94`、`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:274`  
**問題点**: show の受理時には factory の存在だけを確認し、提示先が現れた時点で引き直しています。受理後に同じ VM 型の factory を再登録すると、保留中の Toast は後の factory で表示されます。呼び出し時のスナップショットで解決する既存の決定と、受理時に factory を保持する Android 実装から外れます。  
**推奨修正**: 受理時に解決した factory を表示要求へ保持し、取り付け時はそれを使用してください。提示先の待機中に再登録するテストで固定してください。

**照合した主な規約**: ソースコメント、診断文言、Sample パリティ、状態遷移テスト、実行時挙動の検証、および accepted の core/ADR-0035。

**判定: CHANGES_REQUESTED**

## 突き合わせ結果

ホスト側: review-001.md (APPROVED、Minor 2 / Suggestion 2)。相方: 上記 (CHANGES_REQUESTED、Major 2)。

- **確定 (Major)**: iOS の提示面が器を載せられなかったときに show が終わらない。相方の Major 1 (提示先を再取得できない分岐が完了通知だけを呼び、結果が確定しない) と、ホストの Minor 1 (UIKit が提示を拒否すると完了通知が呼ばれず、共有の列の番が返らない) は、同じ箇所 (`ios/Sources/KsDialogs/Presentation/UIKitDialogPresentationSurface.swift` の提示できない分岐) への指摘。相方が高い重要度を主張するので Major とする。Android は同じ分岐で `IllegalStateException` を投げており、姉妹面の振る舞いも揃っていない
- **採用 (Major)**: 待っている登録経路の Toast が、提示先が現れた時点で factory を引き直すので、受理後に再登録された factory で表示される (相方の Major 2)。該当箇所と実害のシナリオが具体的で、受理時点で factory を持つ Android と食い違う。Loading (tasks 4.1) の「View factory の登録の解決は開始時点」とも揃わない
- **確定 (Minor)**: Android の公開 KDoc に ADR ID (ホストの Minor 2)
- **降格なし**
- **未解決なし**
- ホスト側の Suggestion 2 件は相方の指摘と重ならない。Suggestion 4 (MAUI の中身の供給で文脈の解決より先に利用者の factory が走る) は対応する。Suggestion 3 (MAUI の両ブリッジで中身の供給開始直後の閉じると打ち切りの競合) は、C# が受け取る結果は正しく閉鎖の種別だけの差のため、オーバーエンジニアリングを避けて見送る
