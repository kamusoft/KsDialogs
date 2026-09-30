# セカンドオピニオン: wait-for-host-appearance (spec-001)
**相方**: codex / **label**: so-spec-wait-for-host-appearance / **日付**: 2026-09-27 / **対象**: 提案一式 (kasane/changes/wait-for-host-appearance/ の proposal.md・design.md・specs/ 8 能力・tasks.md)
---
# 提案レビュー: wait-for-host-appearance

**判定: NEEDS_DISCUSSION**（Major 5件、Minor 1件）

提示先を待つ方針は明確ですが、既存の accepted ADR との衝突と、実装前に決めるべき検証方法が残っています。静的レビューのみ行い、ビルド・テストは実行していません。照合した規約は `test-execution.md`、`runtime-behavior-verification.md`、`sample-parity.md` です。

### 🟠 Major — Loading の失敗契約が accepted ADR と衝突する

**該当箇所:** `specs/loading-contract/spec.md:19`、`design.md:76`  
**問題点:** 提示先を待った後の View factory 失敗を「警告を残して表示だけ諦める」としています。一方、accepted の `kasane/decisions/core/0033-user-factory-failure-boundary.md:23-25` は Loading の factory 失敗を開始の失敗として呼び出し元へ届けると定めています。現提案はその例外を明示せず、蒸留候補にも ADR-0033 の改訂がありません。  
**推奨修正:** この失敗を開始時に検出するのか、ADR-0033 の契約を改訂するのかを提案段階で決め、spec・ADR 候補・移行上の影響を揃えてください。

### 🟠 Major — MAUI の画面文脈が Native の提示先と同時に用意される保証がない

**該当箇所:** `design.md:82`、`design.md:168-178`、`specs/maui-binding/spec.md:64-78`  
**問題点:** design は Native が提示先を確保すれば MAUI の画面文脈も「必ずある」と推論しています。しかし既存の `ResolvePresentationTarget()` は MAUI Window の `MauiContext` が無ければ null を返します。Native の提示先が先に現れる起動順では、待ちを解いた直後に中身の生成が失敗し得ます。PB-MH-02 はこの経路をレビューだけの除外 Scenario にしており、全形態での起動直後の改善を判定できません。  
**推奨修正:** MAUI 文脈の準備が遅れた場合に待ち続けるか、失敗として許容するかを定めてください。その条件を再現できるテストまたは MAUI Sample の実測を受け入れ基準に加えてください。

### 🟠 Major — 複数 Dialog の表示順を実現経路から保証できない

**該当箇所:** `design.md:115-120`、`specs/dialog-contract/spec.md:44`  
**問題点:** 購読順に通知して continuation を再開することから、再開後の各 show が必ず同順に `present` へ進むとは導けません。特に iOS は MainActor 上の再開順を保証する仕組みが design にありません。PB-HW-06 が要求する「A、B の順で、B が手前」を実装時のスケジューリングに委ねています。  
**推奨修正:** 待機順を保持して提示開始を直列化する仕組みと、実 UIKit で二枚が重なるまでの条件を design に定めてください。

### 🟠 Major — KMP の既存 Scenario の代替検査が未決定

**該当箇所:** `design.md:210-212`、`tasks.md:49`  
**問題点:** TS-KM-02、LD-KM-03、PB-KT-09 の iosTest は、提示先不在時に View factory が呼ばれなくなるため既存の判定材料を失います。候補の「未登録の即時失敗との差」は、登録済みの View factory が正しく解決されたことまでは証明しません。design は検査方法を実装時の判断に残す一方、Open Questions は「なし」です。  
**推奨修正:** factory 解決を観測できる差し替え口を使うか、KMP では輸送だけを検査し Native 側で解決を検査するかを先に決め、各 Scenario の判定材料を書いてください。

### 🟠 Major — iOS の完了テスト手順では UIKit のテストが走らない

**該当箇所:** `tasks.md:65`  
**問題点:** 全形態のテストとして `swift test` を指定していますが、`kasane/handbook/cross/test-execution.md:41-49` は、macOS 上の `swift test` では `#if canImport(UIKit)` 下のテストが実行対象にならず、完了判定には Simulator の `xcodebuild test` を使うと定めています。新設する iOS の待ちと提示順のテストが空振りします。  
**推奨修正:** iOS の全件実行を Simulator の `xcodebuild test` に替え、実行件数と新規 Scenario ID の出現を証跡で確認してください。

### 🟡 Minor — MAUI でハンドル生成前に打ち切る競合が検証されない

**該当箇所:** `design.md:149-153`、`specs/maui-binding/spec.md:32-45`  
**問題点:** 既存 gateway は UI スレッドへの移動を `await` してからブリッジのハンドルを得ます。呼び出し開始後、その `await` 中に token が打ち切られた場合の保持・中継順序が未指定です。PB-MC-04 の「呼び出し時点で打ち切り済み」では、この競合を検査できません。  
**推奨修正:** ハンドル取得前の打ち切りを保持して取得時に適用する手順と、その時点を制御するテストを追加してください。



## 突き合わせ結果

突き合わせの相手は、ホスト側の自己レビュー (ksn-propose Step 8 の 2 周。相方の指摘と重なるものは無かった)。相方の指摘 6 件はすべて相方のみで、根拠 (該当箇所・実害の筋書き) をコードと規約で確かめた。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| Major 1 | Loading の失敗契約が accepted の core/ADR-0033 と衝突する | **採用・未解決 (オーナー判断待ち)** | ADR-0033 の Decision「Loading = 開始そのものの失敗」と、design Decision 2 の「提示先が無いまま始まった表示の生成失敗は表示だけを諦める」が衝突することを確認した。Android はすでに後者の挙動で、中身の生成に提示先 (Activity) が要るため開始時点では作れない。accepted ADR との衝突なので、従う案と改訂案をオーナーに提示する |
| Major 2 | MAUI の画面の文脈が Native の提示先と同時に用意される保証がない | **採用** | design Decision 2 の「必ずある」を見込みに弱め、根拠と、外れた場合は Decision 7 の失敗になることを明記した。tasks 9.3 に MAUI の Sample (iOS・Android) の起動直後の実測を足した |
| Major 3 | 複数 Dialog の表示順を実現経路から保証できない | **採用** | design Decision 4 を「Presenter が呼んだ順の列で持ち、前の 1 枚の提示の完了後に次を明ける」に書き直した。dialog-contract に追い越しの禁止と PB-HW-08 を足した。UIKit の連続提示のリスクも設計の側で解消した |
| Major 4 | KMP の既存 Scenario の代替検査が未決定 | **採用** | design Decision 9 で、Toast は互換面の show の同期の結果、Loading は開始の成否と表示前の進捗の到達で判定すると決めた。factory の呼び出しは Native のテストと KMP の iOS Sample の実測で補う (Risks に検査範囲の縮小を記載)。tasks 7.4・9.3 を具体化した |
| Major 5 | iOS の完了テスト手順では UIKit のテストが走らない | **採用** | `kasane/handbook/cross/test-execution.md:44-47` で確認。tasks 9.2 を Simulator の `xcodebuild test` に替え、件数と新しい Scenario ID の出現の確認を足した |
| Minor | MAUI でハンドル生成前に打ち切る競合が検証されない | **採用** (根拠強のため降格しない) | design Decision 6・maui-binding デルタに取りこぼさない手順を足し、PB-MC-08 と tasks 8.2・8.6 を足した |

Major 1 の決着: オーナーが改訂を選んだ (2026-09-27)。core/ADR-0042 (proposed、core/ADR-0033 の amends) を起票し、design Decision 2 と ADR 候補、proposal に反映した。design とスペックの挙動 (LD-HW-04) は変えていない。

突き合わせの途中でホスト側が見つけて直したもの: Loading の中身の生成を遅らせても、View factory の登録の解決 (未登録の失敗) は開始時点で行うことをスペックに明記した (loading-contract の要件本文と LD-HW-07)。
