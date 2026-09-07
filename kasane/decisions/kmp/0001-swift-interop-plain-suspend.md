---
id: 0001
title: KMP 公開 API の Swift 変換は素の suspend 直接公開とし、第三者依存を導入しない
status: accepted
date: 2026-08-14
---

## Context

KsDialogs は一般公開予定のライブラリで、KMP 形態は Native 2実装を土台とする薄い expect/actual ファサード (core/ADR-0001)。結果通知は async 単発 + 型付き結果 (core/ADR-0003) で、KMP の show は Kotlin suspend 関数として公開される。Swift 消費者へは `@objc` 互換面経由で届く (Swift Export 見送りは cross/ADR-0002)。

Kotlin suspend の Swift への変換手段には、参考リポジトリ KsAppKMP が KMP-NativeCoroutines を採用した先例 (KsAppKMP ADR-0003) がある。ただし KsAppKMP の状況は Flow 多用の共有 VM 層であり、ダイアログの show (単発呼び出し → 結果1個、キャンセル伝播・Flow 購読が本質的に不要) とはワークロードが異なる。

## Decision

素の suspend 関数を直接公開し、ObjC export の自動変換が生成する Swift async に任せる。第三者依存 (KMP-NativeCoroutines) は導入しない。

受け入れ条件は「completed / cancelled が正しい型・値で Swift 側に届くこと」とし、これを割る場合は自前の `@objc` completion handler ラッパー (手書き) にフォールバックする。フォールバック時も第三者依存は導入しない。

## Alternatives Considered

- **KMP-NativeCoroutines 採用 (KsAppKMP ADR-0003 踏襲)**: 却下。公開 API 面に第三者依存が漏れ、消費者の Swift 側にも SPM 依存を強いる。show 単発ワークロードにはキャンセル伝播等が過剰装備で、変換をライブラリが隠すため縦串で自動変換の粗を実測する検証価値も失われる
- **自前 `@objc` completion handler ラッパーを最初から全面採用**: 不採用 (フォールバック控えとして保持)。API ごとの手書きコストがかかるため、自動変換で受け入れ条件を満たせるならば不要

## Consequences

- 正: 公開 API の依存ゼロを維持でき、一般公開ライブラリとして消費者に依存を強いない
- 正: 変換をライブラリが隠さないため、ObjC 境界で何が失われるかが公開 API 面にそのまま現れ、消費者から見て挙動を追跡できる
- 負: ObjC 自動変換の書き味の粗がそのまま Swift 消費者に見える — sealed な `DialogResult` の網羅分岐は失われ (protocol + 具象クラスになるため `as?` 判別が要る)、結果値のジェネリクスも消える (`AnyObject?`)。これらは ObjC 表現の制約であり、自前 completion handler ラッパー方式へ切り替えても解消しない
- 負: Swift Task のキャンセルは Kotlin 側 coroutine に伝播しない (show 単発では影響軽微と判断)
- 負: フォールバック発動時は公開 API の形が変わり、以降ラッパーの手書き維持コストが発生する

出典: kasane/roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md (2026-08-14: KMP 公開 API の形 (a))

現行照合: 2026-08-15 確認。kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt の `suspend fun show` が素の suspend のまま `@Throws` つきで公開され、kmp/ksdialogs-kmp/build.gradle.kts に KMP-NativeCoroutines 等の第三者依存はない。判定: 維持

現行照合: 2026-09-02 確認。公開面の `@Throws` は維持。fix-kmp-ios-unhandled-exception-crash で、利用者側 (Sample) が show を包んだ suspend 関数に `@Throws` を欠くと未処理例外で abort することが実測で確定 — Consequences の「ObjC 境界で何が失われるかが公開 API 面にそのまま現れる」の具体例。利用者向けの注意は concepts (result-notification-semantics) と phase-9-docs の論点に置いた。判定: 維持

現行照合: 2026-09-02 確認 (add-kmp-loading-toast-throws)。素の suspend 直接公開は維持。ライブラリ公開面の `@Throws` を `KsDialogs.show` だけでなく Loading / Toast の VM 経路 (`KsLoading.show(viewModel)` / `start(viewModel)`・非 suspend の `KsToast.show(viewModel)`) にも徹底し、message 経路には宣言しない (投げないため)。非 suspend + 宣言なしは例外を一切伝播せず abort することを A/B で実測 (kasane/changes/archive/2026-09-02-add-kmp-loading-toast-throws/evidence/)。判定: 維持

現行照合: 2026-09-05 確認 (fix-kmp-iosmain-throws-metadata)。公開面の `@Throws` 宣言 (commonMain の `KsDialogs` / `KsLoading` / `KsToast`) は維持。iosMain の実装 override (`IosLoadingGateway` / `IosToastGateway`) は `@Throws` を書かず interface の宣言を継承する — Kotlin 2.4.10 の metadata compile が同一 filter の override を誤検出する既知バグ (https://youtrack.jetbrains.com/issue/KT-88548、2.5.0 で修正) の回避で、2.5.0 採用後は書き戻してもよい (どちらでも契約は同じ)。生成 ObjC ヘッダで VM 経路 4 本が引き続き `DialogException` (Loading は `CancellationException` も) を NSError 化することを確認 (kasane/changes/archive/2026-09-05-fix-kmp-iosmain-throws-metadata/evidence/metadata-compile-recovery.txt)。判定: 維持

現行照合: 2026-09-05 確認 (generalize-kmp-iosmain-throws-note)。iosMain の override に `@Throws` を書かない回避は `KsLoading` / `KsToast` に限らず、`KsDialogs.show` (ジェネリック suspend) の override でも同じ metadata compile エラーになることを再現 (commonMain の override は対象外)。公開面の宣言と NSError 化の契約は不変。判定: 維持 (kasane/changes/archive/2026-09-05-generalize-kmp-iosmain-throws-note/exploration.md)
