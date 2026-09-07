# セカンドオピニオン: split-concepts-platform-surface (code-003)
**相方**: codex / **label**: so-code-split-concepts-platform-surface (同一セッションの 3 ターン目) / **日付**: 2026-09-05 / **対象**: 修正サイクル 2 の確認 — kasane/changes/split-concepts-platform-surface/verification/ の KMP 禁止集合の再分類 (forbidden-tokens.json / baseline.md / forbidden-tokens-concepts.txt) と kasane/concepts/log.md の本 change エントリの訂正
---
# 再レビュー結果

**最終判定: CHANGES_REQUESTED**

前回挙げた4種は正しく禁止集合から外れました。ただし、同時に外した16件のうち4件は外し過ぎです。残る指摘はMajor 1件です。

## 前回の4種

| トークン | 判定 | 根拠 |
|---|---|---|
| `DialogError` | **解消** | KMP向けDialog/Loading/Toast入口が公開失敗型として使用 |
| `cancelled` | **解消** | `KsDialogsKmp.show` の戻り値case |
| `presentationHostUnavailable` | **解消** | KMP Dialog入口から到達可能な `DialogError` case |
| `viewFactoryNotRegistered` | **解消** | KMP Loading/Toast入口が実際にthrowする |

いずれもKMP自身のSwift公開面なので、禁止集合から外す修正は妥当です。

## その他の削除

以下も妥当です。

- `Loading.shared.style` / `Loading.shared.options` / `Toast.shared.style`
  - KMP入口の公開doc commentがKMP利用者の設定先として明示
- `viewModelAlreadyShowing`
  - KMP bridgeが共有VMを `DialogPresenter.present` へ渡し、同一インスタンスの再表示を `DialogPresenter.swift:48-54` で拒否するため到達可能
- `ksDialogOptions` / `ksDialogPlacement`
  - KMP SwiftUI factoryで使う公開添付API
- `KsDialogAttributes` / `SimpleDialogViewModel`
  - KMP SkillのAndroid host targetsに含まれるAndroid公開面

## 残る指摘

### 🟠 Major: 4件が到達可能性の基準を満たさないまま禁止集合から外されている

**該当箇所**:

- `verification/baseline.md:152-154`
- `verification/baseline.md:163`
- `verification/forbidden-tokens.json:556`

**問題点**:

次の4件はKMP向けの正当な完全一致トークンではありません。

- `Dialog.shared`
- `Loading.shared`
- `Toast.shared`
- `viewModelFactoryNotRegistered`

最初の3件について、正当なKMP入口はそれぞれ `Dialog.shared.kmp`、`Loading.shared.kmp`、`Toast.shared.kmp` です。検査は完全一致なので、複合名を許可するために裸のsingleton名まで外す必要はありません。裸の `Dialog.shared` などを外すと、KMP Skillが誤ってiOS Native入口を案内しても検出できなくなります。`Loading.shared.hide()`や各style名も別の完全一致トークンです。

`viewModelFactoryNotRegistered` は網羅的なエラー変換switchに列挙されていますが、現在のKMP入口からは到達しません。

- KMP入口は `KsDialogsInteropBridge.swift:94-99` で `DialogPresenter.present(viewModel:registry:...)` を直接呼ぶ
- この経路の失敗は `viewFactoryNotRegistered`、`presentationHostUnavailable`、`viewModelAlreadyShowing`など
- `viewModelFactoryNotRegistered` はiOS Nativeの型指定showがVM factoryを解決するときだけ発生する
  `ios/Sources/KsDialogs/Presentation/Dialog.swift:68-82`

switchにcaseがあることだけでは「KMP入口から到達可能」の根拠になりません。

**推奨修正**: 上記4件をKMP禁止集合へ戻し、baselineの削除表も修正してください。件数は136件から140件になる見込みです。

## 境界事例

`Dialog.shared.registry` / `Loading.shared.registry` / `Toast.shared.registry` を禁止に残した判断は**妥当**です。

これらはKMP入口のdoc commentで否定的な対比として現れるだけで、共有VMの正しい登録先ではありません。KMP Skillが登録先として案内した場合は誤りなので、負の検査を維持する価値があります。

## 機械確認

- JSON: 構文正常、現行KMP集合136件
- 現行許可面conceptと禁止集合の積集合: 0件
- 境界事例8件: すべて禁止集合に残存
- `forbidden-tokens-concepts.txt`: KMP 136件・一致0件へ更新済み
- `concepts/log.md`: 本changeの対象範囲では残存11件（`di-registration.md` 10件、`reference-repositories.md` 1件）へ訂正され、実測と一致
- `git diff --check`: 成功
- ファイル書き込み、ビルド、テスト: 未実施


## 突き合わせ結果 (2026-09-05、ホスト側 review-003 との照合)

| # | 相方の指摘 | ホスト側 (review-003) | 採否 | 根拠・反映 |
|---|---|---|---|---|
| 前回 Major | 解消 | 解消 | **確定 (解消)** | — |
| 新規 Major | `Dialog.shared` / `Loading.shared` / `Toast.shared` / `viewModelFactoryNotRegistered` の 4 件は外し過ぎ | 未検出 (別角度で Minor 2: `Loading.shared.options` / `viewModelAlreadyShowing` / `viewModelFactoryNotRegistered` はオーナー確定行を覆している) | **採用** | 完全一致の突き合わせで複合名 `*.shared.kmp` は別トークン。`viewModelFactoryNotRegistered` は KMP 入口から到達しない (`Dialog.swift:68-82` のみ)。修正サイクル 3 (オーケストレーター直接修正) で 4 件を復帰 |
| (ホスト Minor 2 との交差) `viewModelAlreadyShowing` の削除は妥当 | 妥当 | オーナー確定行 (内部層・interop 層) を覆すので戻すべき | **ホスト側を採用** | 除外行の基準は層判断で、素通し到達性が答える問いではない。KMP の源泉 concept に出現しないため戻しても正当な記述を落とさない。`Loading.shared.options` も同じ理由で復帰 (計 6 件、136 → 142) |
| 境界事例 3 件 (`*.shared.registry`) を禁止に残す判断 | 妥当 | 妥当 (ただし Skill 側を全文で突き合わせると許可面 concept のコード例経由で当たる) | **確定** | 禁止は維持。Skill 側の突き合わせ単位と task 6.2 での扱い (コード例の写しは Skill 修正へ倒す) を baseline.md に定義 (ホスト Minor 1) |

降格: なし。未解決: なし。ホスト側 Suggestion (rules.md の KMP 公開面と fixture の許可面の定義差) は baseline.md に「目的が違う別の定義」として注記した。
