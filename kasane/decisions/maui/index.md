# maui ADR 一覧

.NET MAUI 系統の決定記録。

| ID | タイトル | status | date |
|---|---|---|---|
| [0001](0001-call-scoped-bridge.md) | MAUI Bridge は Store 非搭載の呼び出しスコープとし、MAUI レジストリは C# 層に持つ | accepted | 2026-08-14 |
| [0002](0002-net10-tfm-gateway-seam.md) | MAUI facade は素の net10.0 TFM を持ち、Bridge 呼び出しは internal gateway 越しにする | accepted | 2026-08-14 |
| [0003](0003-build-wiring-xcodeproject-and-gradle-exec.md) | MAUI のビルド連携は iOS が標準 XcodeProject アイテム、Android は gradlew Exec + AndroidLibrary 束縛 | accepted | 2026-08-15 |
| [0004](0004-nuget-three-package-structure.md) | MAUI NuGet は facade + 輸送層 binding 2件の3パッケージ構成とし、SDK 標準の pack 経路で native 成果物を同梱する — MAUI 本体の下限は workload 同梱版、最低 OS 版は facade 同梱の buildTransitive ガード `KSDLG0001`、自 assembly 用 aar の除去だけが自作 MSBuild の意図的な例外 | accepted | 2026-09-08 |
| [0005](0005-fallback-resolver-sugar.md) | 一括解決糖衣は static 関数ペアではなくレジストリの fallback resolver とし、解決順序を仕様で規定する | accepted | 2026-08-24 |
