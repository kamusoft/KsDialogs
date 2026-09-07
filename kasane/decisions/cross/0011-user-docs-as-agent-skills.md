---
id: 0011
title: 利用者向けドキュメントは Agent Skills (skills/、en/ja 2 版・5 Skill) として提供し、docs-refresh で concepts から追従させる
status: accepted
date: 2026-09-04
---

## Context

KsDialogs は 4 形態 (Native iOS / Native Android / .NET MAUI / KMP) を公開レジストリで配信する予定であり、公開前に利用者向けドキュメントの提供形態を決める必要があった。姉妹ライブラリ KsSettingsView は先行して「章立て型の `docs/` ではなく、利用者が自分のプロジェクトへコピーして使う Agent Skills」を採用し (`../KsSettingsView/kasane/decisions/cross/0022-user-docs-as-agent-skills.md`)、`kasane/concepts/` から派生物を追従させる道具 docs-refresh (検査スクリプト 8 本 + 生成プロンプト 2 本) を運用実績つきで持っている。KsDialogs には `docs/` も `skills/` も無く、白紙からこの形を踏襲できる。

設計の指針は KsSettingsView と同じ 4 観点 — ①パッと何ができるか把握できる、②スマートに効率よく使い方を伝える、③使い方を網羅する、④エージェントが目的を達成しやすい — で、設計が衝突したときの優先度はエージェント > 人間とする。

KsDialogs 固有の前提が 3 つある: (1) KsSettingsView に無い KMP 形態があり、KMP 利用者の書くコードは共有コード側 (`jp.kamusoft.ksdialogs.kmp.*`)・Android ホスト側 (Android Native API そのもの)・iOS ホスト側 (iOS Native パッケージの KMP 向けサブ面 `Dialog.shared.kmp`) の 3 つの名前空間に割れる (kmp/ADR-0002・0003・0004)。(2) 移植元 AiForms.Maui.Dialogs からの移行者がいるが、互換 shim を持たないリブランド (cross/ADR-0001) であり、移植元で Obsolete だった Toast は互換を持たない。(3) 移植元 API のメンバー一覧を持つ concept は存在せず、移植元の参照は「正は移植元 README → コード」で要約を挟まない運用 (handbook cross/aiforms-origin-reference.md) を定めている。

## Decision

### KsSettingsView から踏襲する部分

- `docs/` は作らず、利用者向けドキュメントは `skills/{en,ja}/<name>/` の Agent Skills 標準形 (`SKILL.md` + 必要なら `references/`) で提供する。利用者は自分のプロジェクトへ片言語だけコピーして使う。索引は `skills/README.md` + `README_ja.md` で、ルート README 英日 2 枚と合わせた README 4 枚を追従対象の README 群とする
- 知識の正は `kasane/concepts/` とコード・テスト。`skills/` はそこから利用者向けに翻訳した派生物で、手で直接育てない。`skills/` 一式と manifest の初期生成、および Skill 構成の見直し (Skill の増減・references の再編・除外方針の変更) は変更フローの承認を通し、docs-refresh は既存構成への追従更新に限定する。docs-refresh はユーザーの明示依頼でのみ起動する (自動発動禁止)。エージェントは開発時に `skills/` を知識参照先にしない
- manifest (`skills/.manifest.json`) は v3 — concepts ハッシュのスナップショット・`targets` (Skill ファイル → 源泉 concepts の逆引き。言語抜きで en/ja 共通)・`excluded` (利用者向けでない concepts の理由つき除外)・`readmes`・翻訳ロックステップ。manifest 不在・v3 でないときは全再生成へフォールバックせず、書き換えずに停止して初期生成を案内する
- 分割軸は利用者の状況 (platform × 新規 / 移行)。frontmatter は Agent Skills 標準フィールドのみ、en / ja は同名 `name` で `metadata.language` を分ける。en / ja は常に同一構成・同時更新の翻訳ロックステップとし、コード例は原則コメントレスにして en / ja のコードブロックを byte 一致させる
- 閉世界性: Skill は 1 ディレクトリを単体コピーして使われるため、Skill 外のファイル・URL への参照を持たない (配布座標の URL は可)。リポジトリ内部の用語 (`kasane/` 配下の文書・ADR 番号) と、利用者向けでない機械面の名前 (`KsDialogsInteropBridge` / `KsDialogsInteropResultType` — KMP cinterop 委譲専用の public ABI) を漏らさない。内容は「概念説明 → 能力マップ表 → Setup → 最小コード → references 振り分け」
- docs-refresh は `.agents/skills/docs-refresh/` に検査スクリプト 8 本と生成プロンプト 2 本ごと置き、`.claude/skills/` からは symlink で結ぶ。生成の委譲単位は Skill 単位 (en / ja ペアを同一文脈で生成)、最大 3 並列。ツール最低バージョンの突合はコードを正とし、取得元は 4 ビルドルートの実ファイル (version catalog・Gradle wrapper 2 本・`ios/Package.swift`・MAUI csproj)

### KsDialogs で追加した部分

- Skill は **5 本 × en / ja**: platform 4 本 (`ksdialogs-{ios,android,maui,kmp}`) + 移行 1 本 (`ksdialogs-aiforms-migration`)
- **KMP Skill は 1 本** (`ksdialogs-kmp`) で共有コード側・Android ホスト側・iOS ホスト側の 3 側を扱う。`SKILL.md` は共有コード側 (commonMain の ViewModel 定義と show / Loading / Toast 呼び出し) + 3 側の Setup + 最小コードに絞り、ホスト側の登録レシピは `references/` で振り分ける。Android ホスト側は Android Skill と内容が重複するが、閉世界性の代償として受け入れ、manifest の `targets` で両 Skill から同じ源泉 concept を指して追従する
- **移行 Skill は独立** (`SKILL.md` + `references/api-mapping.md`)。移行者に必要なのは「旧メンバーごとの対応先と無くなったものの代替」の表であり、MAUI Skill のレシピ形式とは別の読み物として分ける。Toast は「対応先なし・新機能として MAUI Skill を読む」と明記し、新 API 自体の説明は Skill 名での相互案内 (リンクなし) で済ませる
- **移行 Skill の源泉は新 API 側の concepts のみ** (core/api のうち対応表が触れるもの + maui/api/di-registration.md)。旧 API 側 (移植元のメンバー一覧) は移植元 README を `kasane/concepts/cross/reference/reference-repositories.md` で解決するローカル clone から初期生成時に一度だけ書き起こし、以後は追従対象にしない。移植元 API の要約 concept は新設しない。API 名網羅検査が新 API のトークンを対応表に見つけない件は「未掲載候補」の報告のみで、意図的な絞り込みを維持する
- **manifest の `excluded` の初期値は `cross/reference/reference-repositories.md` の 1 本** (開発環境のローカルパス対応表で利用者向け Skill の対象外)。残る concepts (core/api・maui/api) はすべてどれかの Skill の `targets` に載せる。`architecture/` カテゴリ (core / cross) の concept は既定で除外候補として扱い、網羅検査が未参照・未除外として報告した時点で理由つきで確定する — 既定は候補であって自動除外ではなく、利用者に効くもの (例: Compose 分離 module の構成) は `targets` へ回す

## Alternatives Considered

踏襲部分の代替案と却下理由は翻案元 (KsSettingsView cross/ADR-0022) と同じ:

- **`docs/` を読み物ドキュメントとして持つ**: エージェントが必要箇所だけを読めず、公開後の主要読者 (AI エージェント併用の利用者) に合わない。却下
- **トピック別 Skill 分割 (導入 / styling / 移行など)**: 各 Skill に全 platform のコード例が並び、利用者のエージェントに無関係 platform を読ませる。コピーも全 Skill が必要になる。却下
- **`SKILL.md` + `SKILL_ja.md` 同居**: 日本語版が Agent Skills 標準外のファイルになり、どの実行系からも Skill として見えない。却下
- **`skills/<name>/{en,ja}/` (Skill 名の下に言語)**: コピーしたディレクトリ名が `en` / `ja` になり利用者のリネームが必要。却下
- **ja 版 name に `-ja` 接尾辞**: 両言語を同一プロジェクトへコピーする利用は想定されないため、パスの対称性と規約の単純さを優先して却下
- **manifest の `targets` に en / ja を明示列挙**: マップが倍増し、en / ja の構成がずれても列挙が正となって検査で気づけない。却下

KsDialogs で追加した部分の代替案:

- **KMP Skill を 3 本に分割 (共有 / Android ホスト / iOS ホスト)**: 書く人は同じ 1 プロジェクトの人なので「利用者の状況」では 1 状況。各 Skill に共有コード側の前提説明が要り、発火条件も細かくなる。iOS ホスト側は KMP 専用面なので分けても純 iOS Skill と共有できない。却下
- **KMP Skill 1 本 + Android ホスト側は Android Skill を併用**: 閉世界性 (Skill 外への参照を持たない) に反する。却下
- **移行内容を MAUI Skill の `references/` に移行章として同居**: MAUI Skill が新規・移行の両方を抱えて重くなり、新規導入者のエージェントに旧 API 対応表まで読ませて段階開示に反する。却下
- **移植元 API の要約 concept を新設して源泉に加える (KsSettingsView 型)**: 外部一次情報 (移植元 README) の写しで層が重なり、要約を挟まない参照運用 (handbook cross/aiforms-origin-reference.md) とも矛盾する。KsSettingsView が要約 concept を源泉にできたのは移植期の資料が既存だったためで、新規に起こした先例ではない。却下
- **旧 → 新の対応表 concept を新設し Skill をその翻訳にする**: Skill と同内容の複製で派生物と正が二重化し、追従が 2 段になる。却下
- **`excluded` の将来規則を書かない (都度ゼロから判断) / `excluded` を空で始めて初回の網羅検査の失敗で決める**: 初期生成の change で手戻りになる。既定を持てば将来の concept 追加時も即決できる。却下

## Consequences

- 正: エージェントが description で発火 → 能力マップで全景把握 → 必要な references 1 本だけ読む段階開示になり、無関係な platform・トピックを読まずに利用目的を達成できる
- 正: 全公開 API とレシピの対応 (網羅) が manifest で機械検査でき、新 concept 追加時に配置判断 (どの Skill か / 除外か) が強制される。`architecture/` の既定があるため判断が即決できる
- 正: KMP 利用者は 1 Skill で 3 側を賄え、移行者は対応表と Toast の不在を 1 Skill で知れる
- 負: en / ja × 5 Skill = 10 部の生成・維持コストが生じ、共通概念の変更が複数 Skill に波及する (manifest の逆引きで機械的に追従する前提)。Android ホスト側の登録レシピは KMP Skill と Android Skill に二重に存在する
- 負: 移行 Skill の旧 API 側は追従しないため、対応表は書き起こし時点の移植元の版に固定される (移行者は特定版から乗り換える前提で意図どおり)。再生成には移植元のローカル clone が要る
- 負: 人間が通しで読むブラウズ型ドキュメントは持たず、人間の可読性は README 索引と各 SKILL.md のリード文・自然言語見出しに依存する。Agent Skills 標準の多言語慣行は未標準化で、標準の進化に応じて 2 言語規約の見直しが必要になり得る
- 負: docs-refresh の導入から `skills/` 一式の初期生成までの間、docs-refresh は manifest 不在で停止案内しか返さない
- 負 (実装結果): 姉妹リポジトリと同じ道具を同じマシンで動かすため、`/tmp` の一時ファイル名にプロジェクト名を入れ、`scripts/link-resolution-check.py` だけが翻案元と差分 (対象一覧のパスを環境変数 `DOCS_REFRESH_TARGETS` で受ける) を持つ。翻案元の以後の変化は自動では届かない

---
出典: kasane/roadmaps/package-distribution/phases/phase-1-skills-foundation/agenda.md (決定事項の各節) / kasane/roadmaps/package-distribution/phases/phase-1-skills-foundation/history.md (2026-09-04 の論点 1・2・2b・3・4) / kasane/changes/archive/2026-09-04-adopt-docs-refresh/proposal.md (What Changes・Impact) / kasane/changes/archive/2026-09-04-adopt-docs-refresh/deviation.md (`/tmp` 固有化) / ../KsSettingsView/kasane/decisions/cross/0022-user-docs-as-agent-skills.md (翻案元。踏襲部分の Decision・Alternatives・Consequences の原文)
