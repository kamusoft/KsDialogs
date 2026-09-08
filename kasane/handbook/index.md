# handbook 一覧 (ドメイン地図)

handbook は**規範層** — コードがこれに従う側であり、文書とコードが食い違ったらコードを直す。「今どうなっているか」の記述は [concepts](../concepts/index.md) にある。

ドメイン別に分割して管理する。ドメイン定義の正は [config.yaml](../config.yaml) の `domains`、導出規則は [concepts/rules.md](../concepts/rules.md) を参照。

| ドメイン | 内容 |
|---|---|
| core | 全 platform が共有するダイアログ契約 (まだ規約なし) |
| ios | iOS 系統 (まだ規約なし) |
| android | Android 系統 (まだ規約なし) |
| maui | .NET MAUI 系統 (まだ規約なし) |
| kmp | Kotlin Multiplatform 系統 (まだ規約なし) |
| [cross](cross/index.md) | リポジトリ横断の規約 (12件 — コメント・テスト実行・検証 CI の範囲と実行条件・実行時挙動の検証・Sample パリティ・移植元参照・ローカル開発環境の準備・利用者向け Skill の API 掲載基準・利用者向け Skill の記述スタイル・docs-refresh を走らせる時点・診断文言の言語・CI で動くスクリプト内の削除) |

ドメインディレクトリと各ドメインの index は最初の規約書き込み時に作成する。
更新履歴は concepts と共通の [concepts/log.md](../concepts/log.md) に記録する (append-only)。
