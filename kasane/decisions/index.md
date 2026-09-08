# decisions 一覧 (ドメイン地図)

decisions はドメイン別に分割して管理する。ドメイン定義の正は [config.yaml](../config.yaml) の `domains`、導出規則は [concepts/rules.md](../concepts/rules.md) を参照。

| ドメイン | 内容 |
|---|---|
| [core](core/index.md) | 全 platform が共有するダイアログ契約・共通 architecture (ADR 35件) |
| ios | iOS 系統 (まだ ADR なし) |
| [android](android/index.md) | Android 系統 (ADR 1件) |
| [maui](maui/index.md) | .NET MAUI 系統 (ADR 5件) |
| [kmp](kmp/index.md) | Kotlin Multiplatform 系統 (ADR 6件) |
| [cross](cross/index.md) | リポジトリ横断のメタ事項 (ADR 20件) |

ドメインディレクトリと各ドメインの index は最初の ADR 書き込み時に作成する。
