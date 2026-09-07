# kmp ADR 一覧

Kotlin Multiplatform 系統の決定記録。

| ID | タイトル | status | date |
|---|---|---|---|
| [0001](0001-swift-interop-plain-suspend.md) | KMP 公開 API の Swift 変換は素の suspend 直接公開とし、第三者依存を導入しない | accepted | 2026-08-14 |
| [0002](0002-thin-facade-native-registry.md) | KMP は契約のみ commonMain に置き、レジストリ実体は各 Native lib へ全委譲する (一部改訂: 0006 — VM factory レジストリは commonMain) | accepted | 2026-08-14 |
| [0003](0003-swift-facing-registration-in-swift-package.md) | Swift 向け KMP 登録 API は Swift パッケージ側に置き、klib に公開面を持たない | accepted | 2026-08-17 |
| [0004](0004-swift-facing-typed-generic-facade.md) | Swift 向け KMP 面は型付きジェネリック糖衣で構成し、show も Swift パッケージ側・型不一致は型付きエラーとする | accepted | 2026-08-17 |
| [0005](0005-swift-show-caller-cancellation-via-show-handle.md) | Swift 向け show の呼び出し元キャンセルは、機械面の show ハンドル経由で当該ダイアログだけを閉鎖する | accepted | 2026-08-17 |
| [0006](0006-common-vm-factory-typed-show.md) | 共有コードの型指定 show は commonMain の VM factory レジストリで解決し、View レジストリの Native 委譲は維持する (amends 0002) | accepted | 2026-09-06 |
