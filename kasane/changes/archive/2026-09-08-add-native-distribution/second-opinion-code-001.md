# セカンドオピニオン: add-native-distribution (code-001)
**相方**: codex / **label**: so-code-add-native-distribution / **日付**: 2026-09-08 / **対象**: 作業ツリーの HEAD (aa71409) からの全差分 (tasks 7.1〜7.3 を除く)
---
# レビュー結果: add-native-distribution

**日付**: 2026-09-08  
**判定**: **CHANGES_REQUESTED**

## サマリー

Android のモジュール改名・発行設定・消費者追随と SwiftPM 同期処理は、概ねデルタスペックに沿っています。提示されたビルド／テスト結果も、既知の対象外を除けば十分です。

一方、検証用 tag の後始末が失敗時に偽成功し得る問題と、API 36 エミュレータ検証を実機で代替した事実が未記録のまま完了扱いになっている問題があります。

件数: **Critical 0 / Major 2 / Minor 1 / Suggestion 0**

## 照合した規約

- `comment-policy.md` — always
- `test-execution.md` — テスト結果の報告・完了判定
- `verification-ci.md` — workflow 変更・iOS 並列飢餓の判定
- `sample-parity.md` — `samples/**` の依存経路変更
- `local-development-setup.md` — Gradle composite build と Sample 参照方式
- `diagnostic-message-language.md` — 改名後の検査対象パス
- `kasane/lessons/code-review.md` は存在しないため、追加の「指摘しないこと」はなし

tasks.md 7.1〜7.3と、対応する未検証 Scenario は指定どおり判定対象から除外しました。ただし `verify-https-resolution.sh` の静的内容は対象に含めています。

## 指摘事項

### [🟠 Major] tag 削除失敗を検出できず、remote tag が残っても成功扱いになり得る

**該当箇所**: `scripts/spm-snapshot/verify-https-resolution.sh:136`、`scripts/spm-snapshot/verify-https-resolution.sh:178`

**問題点**: `cleanup()` は `set +e` の後、remote tag の削除と `ls-remote` の終了状態を検査していません。実行前に remote tag がゼロ件だった場合、削除と事後照会がともにネットワークエラーになると、`tags_remote_after` は空文字になり、実行前の空文字と一致して成功と報告されます。実際には prerelease tag が remote に残っている可能性があります。

また、`tag_pushed_remote="yes"` は push が戻った後に設定されます。remote が tag を受領した後で接続が切れて push が非ゼロになった場合や、push 成功直後に中断された場合、cleanup は remote 削除を試みません。local tag 作成にも同じ更新窓があります。

これは「成功・失敗を問わず検証用 tag を削除する」という Scenario の中核を破ります。

**推奨修正**: 事前に同名 tag が存在しないことを確認済みなので、操作開始の意図を mutation より前に記録し、tag 作成開始後は remote 削除を常に試みてください。削除と事後 `ls-remote` の終了コードを個別に検査し、remote を照会できない場合や対象 tag の不在を確認できない場合は必ず非ゼロ終了させます。削除失敗・事後照会失敗・push の結果が不明な場合を再現するテストも追加してください。

### [🟠 Major] エミュレータ検証を実機で代替した乖離が未記録のまま完了扱いになっている

**該当箇所**: `kasane/changes/add-native-distribution/tasks.md:6`

**問題点**: task 1.2 と `specs/android-maven-distribution/spec.md:21` は API 36 エミュレータでの実行を要求していますが、提示された結果は API 36 実機によるものです。`deviation.md` にこの差はなく、task はエミュレータ実行まで完了した形でチェックされています。

実機での成功自体は有力な証拠ですが、検証 CI と同じエミュレータイメージを通した証拠とは同一ではありません。未実施内容を完了済みと読める状態は、tasks.md の真正性を損ないます。

**推奨修正**: task 1.2 を未完了へ戻し、API 36 エミュレータで実行してから再度完了にしてください。実機を代替として採用する場合は、オーナー合意を得て `deviation.md` に環境差・理由・保証できない範囲を記録し、task の記述も実際の証跡と一致させてください。

### [🟡 Minor] ソースコメントが proposed ADR または改訂前 ADRを確定済み根拠として参照している

**該当箇所**: `android/build.gradle.kts:27`、`android/ksdialogs-core/build.gradle.kts:16`、`android/ksdialogs/build.gradle.kts:16`

**問題点**: `cross/ADR-0009` は現在 `proposed` です。また、`cross/ADR-0005` の accepted 本文は素の `ksdialogs` を Android Native 本体へ割り当てており、今回の `ksdialogs-core`／`ksdialogs` 写像はまだ反映されていません。コメント規約が許容する ADR 参照は確定した判断への参照であり、現在のコメントは根拠の状態を誤って伝えます。

**推奨修正**: 蒸留前はコメントを自己完結した現在形にし、proposed ADR への参照を外してください。`cross/ADR-0019` と lockstep の決定が accepted になった後、必要であれば確定した ID に差し替えます。

## アクションプラン

1. `verify-https-resolution.sh` の cleanup を失敗側に倒し、曖昧な push 結果・削除失敗・照会失敗をテストする。
2. API 36 実機への代替を未完了へ戻すか、オーナー合意済み deviation として正確に記録する。
3. proposed／改訂前 ADR を参照するソースコメントを自己完結した説明へ直す。
4. 修正後に静的再レビューを行う。


## 突き合わせ結果 (ホスト側 review-001.md との照合、2026-09-08)

| 指摘 | ホスト側 | 相方 | 採否 |
|---|---|---|---|
| `verify-https-resolution.sh` の後始末が失敗側に倒れない (tag 削除 / 事後照会の終了状態を検査しない、`tag_pushed_remote` の更新窓で remote 削除を試みない経路がある) | なし | Major | **採用 (Major)** — 該当箇所と実害シナリオが具体的で、spec「成否を問わず削除」の中核に触れる。ホスト側の Minor「remote 到達の事前検証なし」と同じファイルの同じ修正サイクルで対処 |
| instrumented test をエミュレータでなく API 36 実機で実行した乖離が deviation.md / evidence に未記録 | Minor | Major | **確定 (Major)** — 相方が高い方を主張。実装修正 (エミュレータでの再実行) か deviation 合意かはオーナー判断へ |
| ビルドスクリプトのコメントが proposed の cross/ADR-0009 と改訂前の cross/ADR-0005 を確定根拠として参照 | なし | Minor | **採用 (Minor)** — comment-policy.md「ADR 参照は確定した設計判断への参照」の条文で裏付けあり。蒸留前は自己完結の現在形へ書き換える |

未解決 (矛盾) の指摘: なし。降格: なし。
