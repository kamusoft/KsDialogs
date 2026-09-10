# 利用者向け文書の初期生成 (docs-rollout)

Skill 本文 (4〜5 本 × en / ja) の初期生成と、ルート README 英日 2 枚・貢献導線 (Issue Forms / CONTRIBUTING) の整備を行う change フェーズ。library-foundation phase-9-docs の論点を内容として吸収する。

## 論点

(なし — 2026-09-04 に 7 論点をすべて決定事項へ移した。元の 10 項目は同日に内容の近いもので 7 件に統合してから議論した)

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView cross/ADR-0023 (README はルート 2 枚)・cross/ADR-0024 (貢献は Issue、外部 PR なし) と同 phase-9 / phase-12 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-9-docs/agenda.md`・`phase-12-skills-rollout/agenda.md`)。

- README はルート英日 2 枚のみ。platform / Sample の README は利用者の入口にしない (開発者向け知識は concepts へ)。翻訳ロックステップ
- インストールは座標だけ README に置き、手順は Skills へ。最小コード例は Skills と逐語一致させ lint で検査する (`scripts/readme-example-lint.py` を踏襲)
- public 化〜初回リリースの間は README 冒頭に「配信準備中」の状態表記 1 行だけ (解除箇所を 1 箇所に限定)
- 貢献は Issue のみ。Pull requests は collaborators only (オーナー自身の PR と PR トリガー CI のため完全無効化はしない)。Issue Forms は英語、CONTRIBUTING は英日 2 枚。AI スロップ抑止は「実際に動かした証拠」の必須化で効かせる。Discussions は開かない
- Skill 生成は Skill 単位の fan-out。レビューは 4 層: 機械検査 → 独立レビュー → 初見レビュー (concepts もコードも読んでいないエージェントに Skill 本文だけ渡す) → オーナー目視検収
- KsSettingsView の落とし穴 (踏襲時に避ける): 移送対応表を「出典ファイル × 節名」で立てると網羅できない (内容クラスで読む) / オーナー検収で閉世界性・冒頭概念説明・導入節のパッケージ前提化が追加要求として出た

### 公開 API の両入口 (既定 singleton / DI 注入) の使い分けの置き場所 (2026-09-04)

README の最小コード例は既定エントリ (Swift `Dialog.shared` / Kotlin `Dialog.instance` / C# `Dialog.Instance` / KMP の既定インスタンス) だけを見せ、DI には触れない。使い分け (既定エントリで始め、テストや DI 構成では契約 interface を注入する) は各 platform Skill の冒頭の概念説明に 1 段落だけ書く。DI の具体レシピ (references) を持つのはライブラリ側に糖衣がある MAUI Skill (源泉 maui/api/di-registration.md) と KMP Skill (共有コードでの契約注入) だけ。iOS / Android Skill は「契約 (`KsDialogs` protocol / interface) を自分の DI に登録すれば既定エントリと同じ実体が注入できる」の 1 段落に留める。

理由: 源泉が concepts に揃う範囲がちょうどそこ (registration-show-semantics / toast-semantics の「2 入口」、model-binding-semantics の「DI 連携は VM factory の中身として表現」、maui/api/di-registration.md)。iOS / Android Native の DI は利用者の道具 (Koin 等) の側にあり源泉 concept も糖衣もないため、閉世界性の範囲で書けるのは 1 段落まで。README は踏襲した「座標 + 最小コード例だけ」の純度を保つ。

却下: 全 4 Skill に DI レシピ節 (iOS / Android で源泉なし・外部 DI ライブラリ名に触れる) / README にも使い分けを書く (README に手順が戻り始め、Skill との逐語一致も保てない)。

前提の更新: 「原典 README (711 行) を仕様の一次情報源とする」は library-foundation 期の文言で、phase-1 の決定 (知識の正は concepts とコード・テスト、移植元 README は移行 Skill の旧 API 側だけ) により不成立。Skill 生成の源泉は concepts (core/api 8 本 + maui/api 1 本) で固定する。

### README の対応プラットフォーム表は最小 OS + ビルドに使った toolchain の 3 列、利用側の下限は注記に分ける (2026-09-04)

翻案元 KsSettingsView の README と同じ「Platform / 最小 OS / ライブラリのビルドに使った toolchain」の 3 列を 4 形態 (iOS Native / Android Native / .NET MAUI / KMP) 分載せ、表の下に利用側の下限 (Kotlin の最小版・minSdk / compileSdk 等) と「表の版はビルドに使った版であって利用側の最小ではない」の注記を分けて置く。docs-refresh 3d の突合先はこの表 + 該当記載を持つ SKILL.md の導入節 (phase-1 の取得元 4 行をそのまま活かす)。KMP 行の Kotlin バージョン範囲は phase-7 の結論で埋める前提で、proposal では暫定値と印を付ける。現在の README の「Build roots」表 (ビルドコマンドつきの開発者向け) は落とし、最小 OS (iOS 17 / Android 7.0 API 24) だけ引き継ぐ。

理由: 3d は取得元 4 行を既に持つため載せても追従コストは増えず、載せないと突合先が Skill 側だけになって検査の半分が空振りする。4 形態あるライブラリでは「自分の環境で入るか」が訪問者の最初の関心で、OSS の README として表が無いのは期待を外す。

却下: 最小 OS だけ (toolchain 版は Skill の Setup 節へ — 訪問者が Skill を開かないと判断できない) / 表を置かない。

### README のスクリーンショットは iOS / Android × Dialog / Loading / Toast の 6 枚 (2026-09-04)

ルート README にスクリーンショットを載せる。iOS / Android × Dialog / Loading / Toast の 6 枚 (2 列 × 3 行) をルートに新設する `assets/` に置き、英日 README で画像を共有してキャプションだけ言語別にする。MAUI / KMP は「Native をラップするので同じ画面になる」の 1 行で補足し画像は増やさない。撮影は Sample のデモ駆動モード (samples/README.md「撮影のための起動引数」の安定デモ ID、`basic-dialog` / `default-loading` / `toast-stack` 等) でシミュレータ・エミュレータから行い、端末固有情報を写さない (identity lint の検査範囲)。採用する画面は実装時に候補を撮って選ぶ (mock 承認ゲートの変形として change の `ui/` に候補を置く)。画像参照は raw.githubusercontent の絶対 URL (nuget.org 等の README 表示でも画像が出るため) とし、URL 中のブランチ名は phase-3 のブランチモデルで確定する (申し送り)。

理由: KsDialogs の売りは 3 機能をどこからでも呼べることで、翻案元 KsSettingsView の Modern / Classic に相当する対比軸がこの 3 機能。iOS と Android を 2 列で並べれば「Native が土台」の核心が一目で伝わる。撮影はデモ ID で機械的に再現できコストが低い。

却下: 4 枚 (Loading を文で — 操作ブロックの絵は言葉で伝えにくい) / 載せない (翻案元でオーナーが不足と判断した形)。

### Reduce Motion (OS の「視覚効果を減らす」設定) は取り扱わない (2026-09-04)

オーナー判断: ライブラリはこの設定を無視してよく、利用者向け文書 (Skill / README) でも言及しない。ライブラリ側の自動縮退を導入する予定もなく、change の起票もしない。phase-5-3 から持ち越した設計 Open Question はこれで閉じる。

却下: 文書で「自動では尊重しない」事実と `none` の到達範囲を案内する / 自動縮退を別 change として簡易起票する / 自動縮退を phase-2 の change に同梱する。

### 移行 Skill の Toast 節は対応表に 2 行、iOS の throws 化は記載しない (2026-09-04)

移行 Skill (`ksdialogs-aiforms-migration`) の `references/api-mapping.md` に Toast の節を 1 つ置き、次の 2 行で書く: (1) 旧 `Toast.Instance.Show(message)` → 新 Toast の message 入口。挙動差 3 点 (duration の上限クランプなし — 移植元は OS Toast API 由来で実質 3.5 秒 / 多重は重なって表示 — 移植元は OS のキューで 1 枚ずつ / 完全非対話でタッチ素通し) を添える (2) 旧 `Show<TView>()` の View 登録 → 対応先なし。カスタム View の登録経路は新機能として MAUI Skill を読むよう案内する (phase-1 決定「Toast は対応先なし・新機能として MAUI Skill を読む」と両立)。源泉は toast-semantics の移植元との差分記述。

iOS の factory 契約 throws 化 (core/ADR-0033) は移行 Skill にもリリースノートにも記載しない。KsDialogs は未リリース (tag なし) で外部に `KsToast` / `KsLoading` / `KsDialogs` protocol へ準拠した型は存在せず、移行 Skill の読者 (AiForms.Maui.Dialogs からの MAUI 乗り換え者) にも無関係のため読者がいない。リリースノートは phase-9 の初回リリースから始まる。

却下: throws 化を初回リリースノートに残す (読者不在のノイズ) / Toast を「対応先なし」の 1 行だけにする (移行者が 3.5 秒・重なりの挙動差を踏んでから気づく)。

### KMP Skill の `@Throws` 注意書きは最小コードに宣言 + 本文 3 行、詳細は references (2026-09-04)

KMP Skill (`ksdialogs-kmp`) の共有コード側の最小コード (SKILL.md 本文) は、show を包む関数に `@Throws(DialogException::class, CancellationException::class)` を付けた形で見せる (KMP Sample の共有コードと同形)。Setup の iOS ホスト側に「なぜ要るか (Swift から呼ぶ共有コードの関数は suspend でも非 suspend でも宣言が要る)・線引き (ライブラリの公開面は失敗しうる VM 経路にだけ宣言し、message 経路は宣言しない)・無いと Kotlin/Native が例外を NSError に変換せずクラッシュする」の 3 行を本文に置き、経路ごとの宣言の有無の表など詳細は `references/` の iOS ホスト側レシピへ回す。源泉は result-notification-semantics の「KMP→Swift 境界の補足」で、KMP Skill の manifest `targets` に含める。

理由: 知らないと iOS でクラッシュする種類の注意で、references に隠すと段階開示が裏目に出る。最小コードに宣言が付いていれば写した人は自然に守れ、本文は 3 行で足りる。

却下: 本文に段落で詳しく (KMP 3 側の Setup が最も重くなる) / references だけで最小コードは宣言なし (Sample と食い違い、読み飛ばした人が踏む)。

### KMP Skill の iOS ホスト側 Setup は前提 1 + 手順 3、源泉として kmp concept を新設 (2026-09-04)

`KotlinMultiplatformLinkedPackage` は Kotlin Gradle plugin の SwiftPM 連携が `integrateLinkagePackage` タスク (xcodeproj パス指定) で生成する合成パッケージで、利用者は手書きしない (phase-1 申し送りの回答)。消費者側での SwiftPM 依存の再宣言は不要 (発行 metadata に配信リポジトリ URL + exact 版が乗り推移的に伝わる、phase-10 PoC 項目 2)。cross/ADR-0008 の「手動 1 点」は Swift 側の登録 API (`Dialog.shared.kmp`) 用に iOS アプリが `KsDialogs-SPM` を Package Dependencies へ足すことで、合成パッケージ経由の参照と同じ identity にデデュープされる (PoC 項目 4)。

KMP Skill (`ksdialogs-kmp`) の iOS ホスト側 Setup は次で書く:

- 前提: KMP プロジェクト標準の iOS 連携 (共有 framework の Run Script リンク) が済んでいること (本ライブラリ固有ではないため手順化しない)
- 手順 1: 共有モジュールに Maven 依存 1 点 (`api("jp.kamusoft:ksdialogs-kmp:<ver>")`、公開座標は phase-7 で確定。SwiftPM 参照は再宣言しない)
- 手順 2: `integrateLinkagePackage` を xcodeproj パス指定で 1 回実行し、生成された合成パッケージを VCS に含める (以後は自動更新)
- 手順 3: Xcode の Package Dependencies に `KsDialogs-SPM` を 1 点足す (identity `KsDialogs-SPM`、表示 `KsDialogs`)
- SwiftPM 連携が Kotlin 側で Alpha (実験的 API) である旨は対応プラットフォーム表の KMP 行の注記に置く。対応 Kotlin 範囲は phase-7 待ち (暫定印)

源泉: kmp の concepts は未整備 (PoC 記録・samples/kmp/README・ADR は Skill の源泉にできない) のため、この手順を記述する kmp concept を 1 本新設する (ksn-concept、出典は phase-10 PoC 記録 `poc-swiftpm-remote-distribution.md`・samples/kmp/README・cross/ADR-0008・kmp/ADR-0002)。phase-1 決定「architecture 系は既定で除外候補、利用者に効くものは targets へ」の例外に当たり、KMP Skill の manifest `targets` に載せる。phase-2 change の前提として起こす。

却下: concept を作らず Sample README と PoC を直接出典に書く (源泉なしで追従不能・phase-1 原則違反) / iOS 側手順を phase-7 へ先送り (公開時に KMP Skill の iOS 側 Setup が空)。

### `samples/` 配下 README 5 本は廃止し、正を handbook / concepts へ移送する (2026-09-04)

オーナー判断: ADR は決定時の記録であって規約ではなく、SSOT として筋が良いなら ADR を改訂して正を handbook へ寄せる。cross/ADR-0010 が「デモ ID・アプリ識別子の正は samples/README.md」と定めている点は ADR-0010 の**本文を直接修正**して (accepted だが注記ではなく本文修正をオーナーが許可) 指し先を handbook へ差し替える。`samples/README.md` + 4 ルートの README (計 5 本) は phase-2 の change で廃止し、中身は次のとおり移送する:

| 分類 | 中身 | 行き先 |
|---|---|---|
| A. 既出で捨てる | パリティ節と「してはいけないこと」(正は sample-parity.md の写像) / 器の責務と演出の添付の注意 (ios・android・maui の「注意 / 実装メモ」。transition-semantics・registration-show に既出) / KMP の演出の分担メモ (Sample 内部構造。必要なら Sample のソースコメントへ) | 捨てる |
| B. 規範 | 撮影のための起動引数 (キー 2 つ・安定デモ ID 14 件・アプリ識別子・iOS / Android の外部表現) | `handbook/cross/sample-parity.md` に「撮影支援の起動引数」節として合流。ADR-0010 本文の指し先と `kasane/config.yaml` `ui.screenshot` のポインタを同時に差し替える |
| B'. 規範 | 各ルートの参照方式 (Local Swift Package / composite build と `dependencySubstitution` の理由 / ProjectReference / KMP ルートの `apply false` の理由) とビルド・実行コマンド | `handbook/cross/local-development-setup.md` に「Sample のビルドと実行」節 (ルート別の小節) を追加 |
| C. 記述 | KMP iOS アプリの 3 点リンク (共有 framework の Run Script・合成パッケージ・`ios/` パッケージ) と `integrateLinkagePackage` の再生成手順 | 論点 5 で新設する kmp concept に合流 (利用者向け手順と Sample 固有の再生成手順を別節に) |
| D. 対象外 | `android/layout-case-fixtures/README.md` (テスト補助コードのディレクトリ注記) | 現状維持 |

「利用者の入口にしない」の範囲: ルート README の「リポジトリ構成」表には `samples/` の 1 行だけを置き、開発者向け文書へのリンクは張らない。docs-refresh の追従対象は 4 枚のまま。

却下: 現状維持 + 4 枚からリンクしない (ADR-0010 を動かせない前提での案。オーナーが前提を否定) / ルート README から samples/README へリンク / handbook に `sample-build.md` を新設して B と B' をまとめる (正の本数が増える) / 撮影引数を config へ吸収 (規範が config に埋もれる)。

### Issue Forms の Platform は「形態 × ホスト OS」の 7 択 (2026-09-04)

Issue Forms は踏襲どおり英語 3 本 (バグ報告 / 提案 / 質問)・blank issue 無効・必須項目 (バグ: Version / Platform / 再現手順 / 実際の挙動 / 期待した挙動、提案: 解決したい課題 / 現状の困りごと / 検討した代替案、質問: Version / Platform / 試したこと / 参照した Skill・README 節) とし、KsDialogs で変えるのは Platform の dropdown だけ。バグ報告と質問の両方で同じ 7 択を使う: iOS / Android / .NET MAUI on iOS / .NET MAUI on Android / Kotlin Multiplatform on iOS / Kotlin Multiplatform on Android / Multiple platforms。

理由: MAUI と KMP は 2 ホストを持ち、形態だけでは開くビルドルートとホストが決まらない (MAUI は binding、KMP の iOS ホストは Swift 側登録 API が別面)。1 回の選択で確定し、報告者に追加の欄を要求しない。

却下: 4 形態 + Multiple の 5 択 (MAUI / KMP のホストが手順本文を読まないと分からない) / 形態とホスト OS の 2 dropdown (Native で 2 つ目が冗長)。

## TODO

- [x] 論点の解消 (2026-09-04: 論点 1〜7 をすべて決定事項へ。論点 2 は「取り扱わない」で確定)
- [x] 3e の仕分け基準 (利用者向け Skill の API 掲載基準) を KsDialogs 固有の内容として handbook に起こす — 掲載除外リストは Skill 本文と一緒に決まる規約 (adopt-docs-refresh の Non-Goals からの申し送り、2026-09-04)
- [x] KMP の iOS ホスト統合手順を記述する kmp concept を ksn-concept で新設する (phase-2 change の前提。出典は phase-10 PoC 記録・samples/kmp/README・cross/ADR-0008・kmp/ADR-0002)
- [ ] phase-7 の結論待ち: KMP Skill と README の KMP 行に書く対応 Kotlin 範囲と公開座標 (暫定印で生成し、phase-7 後に docs-refresh で追従)
- [x] phase-2 change に含める: `samples/` 配下 README 5 本の廃止 (trash) と移送 (sample-parity.md・local-development-setup.md・kmp concept)
- [x] 同 change に含める参照の付け替え: cross/ADR-0010 本文の指し先、config `ui.screenshot` のポインタ、sample-parity.md「関連」節
- [x] phase-3 へ申し送り: README の画像 (`assets/`) を指す raw.githubusercontent の絶対 URL のブランチ名をブランチモデルの決定で確定し、README の URL を追随させる
- [x] ksn-propose で変更提案を起こす (rollout-user-docs、2026-09-05 完了)

## 実装結果 (2026-09-05 反映)

change `rollout-user-docs` (L 級) で完了。アーカイブ: `kasane/changes/archive/2026-09-05-rollout-user-docs/`。決定事項 8 節 + 踏襲 5 件の反映先は同 change の tasks.md 9.2 の照合表が持つ。決定と異なる形になった点と申し送りは次のとおり。

- 移行 Skill の Toast 節: 決定は「旧 `Toast.Instance.Show(message)` → 新 message 入口」の行を要求したが、指定された移植元 clone に旧 message overload は実在しなかった。旧 `Show<TView>()` と具象 View 経路を「対応先なし」、新 message 入口を移行後の代替 (挙動差 3 点つき) として案内する形に倒した (deviation)
- `samples/` 配下 README の廃止: 参照残存 0 件の検査は append-only の履歴 (concepts/log・lessons inbox・進行中 change の足場・accepted ADR 本文) を除外した active 文書で達成。cross/ADR-0006・0007 には現行照合 footer で現在の所在を記した (deviation)
- ADR: cross/0012 (README ルート 2 枚 + `samples/` README 廃止) と cross/0013 (貢献は Issue Forms、Platform 7 択) を蒸留で起票
- 申し送り → phase-3 agenda TODO: README 画像 (`assets/`) の raw.githubusercontent URL のブランチ名 (現在 `main` 暫定) の確定と、Pull requests 設定 (collaborators only) の実施
- 申し送り → phase-7 agenda (既存論点): KMP Skill と README の KMP 行に暫定印で書いた公開座標と対応 Kotlin 範囲の確定後、docs-refresh で追従
- 申し送り → 独立変更 `generalize-kmp-iosmain-throws-note` (簡易起票): iosMain の override に `@Throws` を書かない注意の適用範囲の裏取り。2026-09-05 完了 (`changes/archive/2026-09-05-generalize-kmp-iosmain-throws-note`)
- 申し送り → 独立変更 `split-concepts-platform-surface`: 2026-09-06 完了 (`changes/archive/2026-09-06-split-concepts-platform-surface`、cross/ADR-0014 accepted)。内容は次の段落

旧 ID `refine-docs-refresh-api-token-extraction`、相方レビュー Suggestion 8 由来。API 名網羅検査の候補の約 6 割が他 platform の公開名になり除外リストが約 100 行に肥大した根本原因は、core/api concept が 4 platform の公開名を 1 本文に同居させる構造にあった。検査器は変えず、concepts を「core = platform 非依存の契約 / `<platform>/api/` = 公開面」へ分割し、manifest `targets` の組み替え・Skill 再生成・除外リストの再仕分け (約 95 行 → 20 行) まで行った。ロードマップのフェーズには足していない
- 見送り: `doc-structure-lint.py` の既存 baseline 261 件 / 42 ファイル (concepts と過去ロードマップ) は本 change の範囲外として報告のみ。整理は ksn-drift の棚卸しで扱う
