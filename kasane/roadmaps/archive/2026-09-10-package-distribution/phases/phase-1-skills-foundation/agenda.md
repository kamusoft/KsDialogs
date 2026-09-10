# Skills 基盤 (skills-foundation)

利用者向けドキュメントを Agent Skills として提供する形を KsSettingsView から踏襲し、docs-refresh (追従の道具) を検査スクリプト込みで取り込んで KsDialogs の concepts に向ける change フェーズ。

## 論点

(なし — 全論点を決定事項へ移した)

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView cross/ADR-0022 (利用者向けドキュメントは Agent Skills) と同 phase-10 / phase-11 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-10-skills-design/agenda.md`・`phase-11-docs-refresh-retarget/agenda.md`)。

- `docs/` は作らず、利用者向けドキュメントは `skills/{en,ja}/<name>/` (SKILL.md + 必要なら references/) の Agent Skills 標準形で提供する。索引は `skills/README.md` + `README_ja.md`
- 知識の正は `kasane/concepts/` とコード・テスト。`skills/` は派生物で手で直接育てない。初期生成と構成見直しは変更フロー (本ロードマップの phase-2)、docs-refresh は追従専用
- 分割軸は利用者の状況 (platform × 新規 / 移行)。frontmatter は Agent Skills 標準フィールドのみ、en / ja は同名 `name` で `metadata.language` を分ける
- 閉世界性: Skill 外のファイル・URL を参照しない、リポジトリ内部用語を漏らさない。内容は「概念説明 → 能力マップ表 → Setup → 最小コード → references 振り分け」。コード例は原則コメントレス (en / ja のコードブロック byte 一致のため)
- manifest は v3 (`concepts` ハッシュ・`targets` 逆引き (言語抜き)・`excluded`・`readmes`・翻訳ロックステップ)。manifest 不在・v3 でないときは書き換えず停止
- docs-refresh は `.agents/skills/docs-refresh/` を検査スクリプト 8 本と prompt 2 本ごとコピーする。起動はユーザーの明示依頼のみ (自動発動禁止)、委譲単位は Skill 単位 (en / ja ペアを同一文脈で生成)、最大 3 並列
- 却下済み: `docs/` 維持 / トピック別分割 / `SKILL_ja.md` 同居 / `skills/<name>/{en,ja}/` / ja 版 name に `-ja` / manifest の targets に en / ja 明示列挙 (理由は cross/ADR-0022 の Alternatives)

### KMP Skill は 1 本 (2026-09-04)

Skill は platform 4 本 (`ksdialogs-{ios,android,maui,kmp}`) + 移行 1 本 (論点 2 で確定) の構成とし、KMP Skill は 1 本 (`ksdialogs-kmp`) で共有コード側・Android ホスト側・iOS ホスト側の 3 側を扱う。SKILL.md は「共有コード側 (commonMain の ViewModel 定義と show / Loading / Toast 呼び出し) + 3 側の Setup + 最小コード」に絞り、Android ホスト側と iOS ホスト側の登録レシピは `references/` で振り分ける。

根拠 (KMP Sample の実物、`samples/kmp/`): KMP 利用者が書くコードは使う API の名前空間が 3 つに割れている (共有 = `jp.kamusoft.ksdialogs.kmp.*` / Android ホスト = Android Native API そのもの / iOS ホスト = iOS Native パッケージの KMP 向けサブ面 `Dialog.shared.kmp`) が、書く人は同じ 1 プロジェクトの人なので分割軸「利用者の状況」では 1 状況。iOS ホスト側は KMP 専用面 (kmp/ADR-0003・0004) なので分けても純 iOS Skill と共有できない。Android ホスト側は Android Native Sample と逐語一致するため Android Skill と内容が重複するが、閉世界性の代償として受け入れ、manifest の `targets` で両 Skill から同じ源泉 concept を指して追従する。

却下: 3 本分割 (共有 / Android ホスト / iOS ホスト — 各 Skill に共有コード側の前提説明が要り発火条件も細かくなる) / 1 本 + Android ホスト側は Android Skill 併用 (閉世界性違反)。ADR は蒸留時に cross/ADR-0022 の翻案 ADR へ KsDialogs 固有の追加分として含めるか判断する (roadmap.md 前提)。

### 移行 Skill は独立 (2026-09-04)

AiForms.Maui.Dialogs からの移行 Skill は MAUI Skill に含めず、独立した `ksdialogs-aiforms-migration` (SKILL.md + `references/api-mapping.md`) とする。Skill 構成は platform 4 本 + 移行 1 本の計 5 本 × en / ja で確定。

理由: 踏襲済みの分割軸「利用者の状況 (platform × 新規 / 移行)」で移行は MAUI 新規導入と別の状況であり、同居させると新規導入者のエージェントに旧 API 対応表まで読ませて段階開示に反する。リブランド方針 (cross/ADR-0001、互換 shim なし) のもとで移行者に必要なのは「旧メンバーごとの対応先と無くなったものの代替」の表で、MAUI Skill のレシピ形式とは別の読み物になる。新 API 自体の説明は KsSettingsView 同様、Skill 名での相互案内 (「新 API を調べるときは ksdialogs-maui Skill」) で済ませる。

KsDialogs 固有の前提: 移植元で Toast は Obsolete だったため互換を持たない (toast-semantics.md)。移行 Skill は「Toast は対応先なし・新機能として MAUI Skill を読む」と明記する。

却下: MAUI Skill の references/ に移行章として同居 (MAUI Skill が新規・移行の両方を抱えて重くなる)。

### 移行 Skill の源泉は新 API 側の concepts だけ (2026-09-04)

移行 Skill (`ksdialogs-aiforms-migration`) の manifest `targets` は新 API 側の concepts (core/api のうち対応表が触れるもの + maui/api/di-registration.md) だけとし、移植元 API の要約 concept は新設しない。旧 API 側 (AiForms.Maui.Dialogs のメンバー一覧) は移植元 README を `kasane/concepts/cross/reference/reference-repositories.md` の対応表で解決して phase-2 の生成時に一度だけ書き起こし、以後は追従対象にしない。

理由: 旧 API 側は凍結対象 (cross/ADR-0001 で互換を約束せず、移行者は特定版から乗り換える) で追従の実益がない。docs-refresh が守るべきは「新 API 側が変わったら対応先も変わる」の方向だけで、新 API 側の concepts を targets に置けば逆引きできる。移植元 API の要約 concept は外部一次情報 (移植元 README) の写しで層が重なり、handbook の aiforms-origin-reference.md も「正は移植元 README → コード」と要約を挟まない運用を定めている。KsSettingsView が要約 concept を源泉にできたのは移植期の legacy 資料が既存だったためで、新規に起こした先例ではない。

却下: 移植元 API の要約 concept を新設 (外部一次情報の複製) / 旧 → 新の対応表 concept を新設 (Skill と同内容の複製で派生物と正が二重化、追従が 2 段になる)。

制約: 移行 Skill の生成・再生成は移植元のローカル clone (参考リポジトリの在り処の運用) を前提とする。3e (API 名網羅検査) で新 API のトークンが対応表に出ない件は「未掲載候補」の報告のみで、意図的な絞り込みは維持する。

### manifest の excluded は reference-repositories.md の 1 本 + architecture カテゴリの既定 (2026-09-04)

初期 manifest の `excluded` は `cross/reference/reference-repositories.md` の 1 本 (理由: 開発環境のローカルパス対応表で利用者向け Skill の対象外)。残る 9 本 (core/api 8 本 + maui/api/di-registration.md) はすべてどれかの Skill の `targets` に載せる — core/api は 4 platform Skill (KMP Skill は「KMP の共有コード」「KMP での見え方」節を持つ registration-show / model-binding を含む)、maui/api/di-registration.md は maui Skill と移行 Skill、移行 Skill は対応表が触れる core/api も含める。

将来規則: `architecture/` カテゴリ (core / cross) に concept が増えたら既定で除外候補として扱い、3c の網羅検査が落ちた時点で理由つきで確定する。既定は候補であって自動除外ではなく、利用者に効くもの (例: Compose 分離 module の構成) は targets へ回す。KsSettingsView の excluded 5 本がすべて architecture カテゴリ (ビルド基盤・内部責務境界・リポジトリ境界・binding 統合) だったことを根拠とする。

却下: 将来規則を書かない (都度ゼロから判断) / excluded 空で始めて初回の網羅検査の失敗を見て決める (phase-2 で手戻り)。

### docs-refresh 3d の取得元は KsDialogs の 4 ビルドルートに合わせた 4 行 (2026-09-04)

docs-refresh の 3d (コードを正とするツール最低バージョンの突合) の取得元表を、KsSettingsView の表の固有値差し替えではなく次の 4 行に置き換える:

| 項目 | 取得元 (コード = 正) |
|---|---|
| AGP / Kotlin / minSdk / compileSdk | `android/gradle/libs.versions.toml` の `[versions]` (`agp` / `kotlin` / `android-minSdk` / `android-compileSdk`)。KMP ルートは同じ catalog を `from(files("../android/gradle/libs.versions.toml"))` で共有するため Android / KMP 両形態をこの 1 ファイルで賄う (各 module の build.gradle.kts は `libs.versions.android.minSdk` の参照だけで値を持たないため取得元にしない) |
| Gradle | `android/gradle/wrapper/gradle-wrapper.properties` と `kmp/gradle/wrapper/gradle-wrapper.properties` の `distributionUrl` (2 本読み、食い違いはそれ自体を報告) |
| Swift tools / iOS Deployment Target | `ios/Package.swift` (`// swift-tools-version:` と `.iOS(.vNN)`) |
| .NET TFM / 対象 OS 下限 / MAUI 本体下限 | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` (`<TargetFrameworks>`・`<SupportedOSPlatformVersion>` ios / android・`Microsoft.Maui.Controls` の `Version`) |

突合先は KsSettingsView と同じ (ルート README 群の対応プラットフォーム表・開発環境要件 + 該当記載を持つ場合は各 SKILL.md の導入節)。README の表に toolchain 版を載せるかは phase-2 の内容判断で、3d は読む場所だけを持つ。

却下: KsSettingsView の表を固有値の差し替えだけで写す (minSdk / compileSdk が取れず、KMP の wrapper と MAUI 本体下限を見ない)。

## TODO

- [x] 論点の解消 (2026-09-04: 論点 1 / 2 / 2b / 3 / 4 をすべて決定事項へ)
- [x] phase-7 へ申し送り: KMP Skill に書く依存宣言 (`api("jp.kamusoft:ksdialogs-kmp:<ver>")`) が公開座標で解決できる形かの確定 → 2026-09-04 phase-7 agenda「publication の形」に追記 (Sample のローカル解決の説明もそちらへ移した)
- [x] phase-2 へ申し送り: iOS ホストのリンク構成で `KotlinMultiplatformLinkedPackage` が KMP プラグインの生成物か利用者が手書きする範囲かを確認してから Setup 節を書く → 2026-09-04 phase-2 agenda「文書の内容」に追記
- [x] ksn-propose で変更提案を起こす (2026-09-04: adopt-docs-refresh)

## 実装結果 (2026-09-04 反映)

change [adopt-docs-refresh](../../../../changes/archive/2026-09-04-adopt-docs-refresh/proposal.md) (M 級) として実装し、review-004 APPROVED / verify-004 VALID / 相方 code-007 APPROVED で完了。決定事項 5 件はすべて `.agents/skills/docs-refresh/SKILL.md` と prompt 2 本に反映され、AGENTS.md と `kasane/config.yaml` に運用宣言が入った。方針の ADR は cross/ADR-0011 として起票した (SKILL.md の根拠参照も差し替え済み)。

スペックからの乖離 (deviation.md、4 件): 翻案元の「platform / Sample ディレクトリに README を置かない」規範は KsDialogs に根拠となる決定が無く `samples/README.md` 等 6 本が現役のため書かず、追従対象の範囲だけを述べた / `lint.identity.scope` にルート README 2 枚も追加 / `/tmp` 一時ファイルをプロジェクト名入りに分け `link-resolution-check.py` だけ環境変数化 (翻案元との byte 一致は 7 本に) / 付随修正として `.gitignore` を整備。review-004 の Minor 1 件と Suggestion 3 件は最終版で解消済み (env var 警告の網羅・scope 実値・証跡例外の範囲・残留 grep の許容箇所は冒頭の 1 箇所に戻った)。

申し送り (受け皿確定済み):

- 3e の仕分け基準 (利用者向け Skill の API 掲載基準) の handbook 起こし → phase-2 agenda の TODO に追記
- `samples/` 配下 README の位置づけ (上記 deviation 1 件目。決定事項「platform / Sample の README は利用者の入口にしない」を確定するときの前提) → phase-2 agenda「adopt-docs-refresh からの申し送り」節に追記
- `KotlinMultiplatformLinkedPackage` の手書き要否 → phase-2 agenda「文書の内容」に追記 (上の TODO)
- KMP 依存宣言の公開座標 → phase-7 agenda「publication の形」に追記 (上の TODO)
