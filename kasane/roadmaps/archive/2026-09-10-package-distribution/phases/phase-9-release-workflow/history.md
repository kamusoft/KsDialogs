# phase-9-release-workflow 議論履歴

## 2026-09-10: R1 KMP の Maven artifact を作る場所と publish 段の順序

phase-7 C3 の申し送り (SPM tag の先行) を起点に議論。事実の裏取り: KMP 発行物の SwiftPM 連携メタデータに Swift 参照 URL と `packageName` が焼き込まれる / iOS publication の発行は同版 tag の実在が必須 / 発行には Xcode が要る (add-kmp-maven-distribution の deviation・evidence)。翻案元 KsSettingsView `release.yml` の publish step 順と冪等化を scout で確認。

- 案 A (採用): publish 段の中で Android upload validated → SPM tag push → KMP を https + exact で発行 → upload validated → NuGet push → Maven release ×2 → monorepo tag。publish job は macOS 1 本。package 段に KMP の job は置かない。放棄時の tag 削除手順を handbook へ
- 案 B (却下): package 段でも `file://` で KMP を発行 — 消費者が居ず同一性検査も掛けられない
- 案 C (却下): SPM tag を publish 段の先頭 — 認証・署名の失敗でも tag が残る
- publish job の 2 本割り (却下): drop と deployment ID の引き継ぎが job をまたぐ
- 理由: C3 の却下前提 (package 段で KMP を先に作れる) が崩れた。ADR-0008 / 0009 は順序を定めておらず、踏襲元 ADR-0020 の「取り消せる順」は tag が削除可能なので保てる

## 2026-09-10: R2 Maven Central の deployment 2 件の回し方と反映待ち・smoke の 4 本化

- 案 A (採用): deployment を Android / KMP の 2 枠で持ち、ID の保存・状態分岐・release・drop を枠ごとに繰り返す。`central-portal.sh` は不変。反映待ちは Maven 3 座標の POM、smoke は `verify-consumer-kmp.yml` を `mode: smoke` で呼ぶ
- 案 B (却下): 1 つの bundle にまとめて Portal API へ手で upload (upload の新設と bundle 組み立てが新たな失敗点。C3 が退けた形)
- 理由: 翻案元のステップを 2 回書くだけで済み、「両方 validated まで release しない」は R1 の順序で既に満たされる。ADR 級ではない (ADR-0024 の順序決定の実装詳細)

## 2026-09-10: R3 初回リリースの version と prerelease 扱い

- 案 A (採用): `0.1.0-beta.1`、GitHub Release は prerelease。翻案元と同じ理由 (試用版と API 変更余地が伝わる・Issue テンプレートとカタログに揃う)
- 案 B (却下): `0.1.0` 正式版 / 案 C (却下): `1.0.0-beta.1`

## 2026-09-10: R4 README / Skill の version 置換の位置

- 当初の提示は踏襲案 A (リリース PR 内で手で実行 + validate の `--check`)。オーナーから「KsSettingsView でもここだけ手動なのは不便、改善したい」
- 案 A' (採用): release workflow が package-maui の pack 前に作業木で置換 (nupkg 向け)、publish 成功後に `develop` へ置換 commit を push (リポジトリ向け)。`--check` は廃止。`develop` は PR 必須・必須 check が無く `contents: write` で push 可。`GITHUB_TOKEN` の push は他 workflow を起動しない
- 案 B (却下): リリース準備 workflow を別 dispatch して PR を自動生成 (dispatch 2 回、失敗時に README が未公開版を指す)
- `main` へ commit する形は翻案元 ADR-0020 の却下のまま。KsSettingsView への逆流は別途起票

## 2026-09-10: R5 docs-refresh のタイミングと内容

- 案 A (採用): 提案化の前 (出典 = 現行 concepts + phase-6 / 7 の申し送り表を Input に添付) と、初回リリース後の蒸留の後の 2 回。README に KMP ホスト側の例は載せない (Skill references に任せ、lint 表も不変)
- 案 B (却下): release change 実装後・リリース PR 直前 / 案 C (却下): 配布構成 concepts 化を ksn-concept で前倒しして 1 回
- 根拠: handbook「docs-refresh を走らせる時点」の例外規定、manifest は 2026-09-07 のまま、旧座標は skills 12 ファイル + README 2 枚に残存

## 2026-09-10: R6 MAUI の `0.0.0-dev` 発行ガードと XML ドキュメント

- ガード (採用): workflow 側で nupkg 名 `<ID>.<version>.nupkg` の存在検査を package 後と push 直前の 2 回。MSBuild で pack を止める形は却下 (push は MSBuild の外、手元 pack と消費者検証を壊す)
- XML doc: 案 A (採用) facade の 3 TFM で同梱・binding は生成しない、props に明示 / 案 B (却下) pack から外す / 案 C (却下) 英訳
- 材料: facade の doc コメント 431 か所すべて日本語、binding は 0。翻案元は facade で true。cross/ADR-0015 は doc コメントを英語化の対象外

## 2026-09-10: R7 MAUI の依存警告の負ケース

- 案 A (採用): 見送り。必要になったら簡易起票 / 案 B (却下): release の change に足す / 案 C (却下): 今すぐ簡易起票
- 根拠: 守る経路は docs 側で塞ぎ済み、NU1605 は SDK 既定で error、release の change の主題を保つ
- これで R1〜R7 がすべて決定。残る TODO は docs-refresh 1 回目 → ksn-propose
