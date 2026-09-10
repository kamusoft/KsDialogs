# concepts 目次 (ドメイン地図)

concepts はドメイン別に分割して管理する。カテゴリ定義・配置基準・ドメイン導出規則は [rules.md](rules.md) を参照。

ここに置くのは**記述** (今どうなっているか) だけ。コードが従うべき**規範** (規約・ガイド) は [handbook](../handbook/index.md) にある。

| ドメイン | 内容 |
|---|---|
| [core](core/index.md) | 全 platform が共有するダイアログ契約 — 結果通知・多段表示・レイアウト・登録と表示の呼び出し面・トランジション (出入りの演出)・Loading・Toast のルール (規約が正) と、レイアウト検証機構 (共通ケース表) の記述 |
| [ios](ios/index.md) | iOS Native (Swift) の公開面 — Dialog・レイアウト・トランジション・Loading・Toast |
| [android](android/index.md) | Android Native (Kotlin) の公開面 — Dialog・レイアウト・トランジション・Loading・Toast |
| [maui](maui/index.md) | .NET MAUI 固有の知識 (binding 構成・DI 連携と登録糖衣) と MAUI (C#) の公開面 — Dialog・レイアウト・トランジション・Loading・Toast |
| [kmp](kmp/index.md) | KMP 固有の知識 (expect/actual 境界・iOS host 統合) と共有コード (commonMain) の公開面 — Dialog・Loading・Toast |
| [cross](cross/index.md) | リポジトリ横断のメタ事項 — 消費者検証 (`verification/`) の構成と参照先の切り替え、外部参考リポジトリの在り処 (横断的な開発規約は handbook/cross へ移送) |

同じ機能でも、挙動の契約は core、公開名・署名・コード例・framework 固有の注意は `<platform>/api/` に分かれる ([rules.md の「配置判断」](rules.md)・cross/ADR-0014)。core の各 concept は末尾の「形態別の公開面」節から、各公開面 concept は冒頭から、互いにリンクする。

ドメインディレクトリと各ドメインの index は最初の概念書き込み時に作成する。
[log.md](log.md) は全ドメイン共通の append-only 履歴。
