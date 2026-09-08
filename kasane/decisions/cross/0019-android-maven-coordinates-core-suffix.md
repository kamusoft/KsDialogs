---
id: 0019
title: Android の Maven 座標は View 系本体を ksdialogs-core、Compose 側を素の ksdialogs とする
status: proposed
date: 2026-09-08
amends: [cross/0005, android/0001]
---

## Context

公開識別子の写像表 (cross/ADR-0005) は素の artifactId `jp.kamusoft:ksdialogs` を Android Native に割り当てた。その理由は KMP (`ksdialogs-kmp`) との主従 (Native 主) であり、Android Native の 2 つの配布物 (View 系本体と Compose 系、android/ADR-0001) のどちらに素の名前を渡すかは検討していない。

Android 利用者の主な使い方は Jetpack Compose であり (オーナー判断 2026-09-08)、多数派が接尾辞付き `ksdialogs-compose` を選ぶ配置は主従が逆に見える。iOS は 1 パッケージ `KsDialogs` に SwiftUI 対応まで入る「素の名前 = 全部入り」の形で、Android だけ素の名前が View 系のみを指すのはずれる。

前提: 初回リリース前で、Maven Central に公開済みの artifact がない (改名の影響がリポジトリ内の設定と文書に閉じる)。

## Decision

cross/ADR-0005 の決定のうち Android 行の配布上の識別子と、android/ADR-0001 の決定のうち Compose 系モジュールの Maven 座標名を本決定で置き換える。他の決定 (KMP・MAUI・iOS の識別子、Compose 系 API の別モジュール分離と本体の Compose 非依存) は維持する。

| 配布物 | Maven 座標 | 利用者が書く依存 |
|---|---|---|
| View 系本体 (Compose 非依存) | `jp.kamusoft:ksdialogs-core` | View 系だけを使う人・MAUI binding・KMP androidMain |
| Compose 系 (本体に `api` で依存) | `jp.kamusoft:ksdialogs` | Compose で中身を書く人 (本体は推移的依存で自動) |

Gradle のディレクトリと project 名は座標に揃える (`android/ksdialogs-core` = `:ksdialogs-core`、`android/ksdialogs` = `:ksdialogs`)。Kotlin パッケージ名 (本体 `jp.kamusoft.ksdialogs`、Compose 側 `jp.kamusoft.ksdialogs.compose`) と AGP namespace は動かさない。cross/ADR-0005 のコード上の識別子の行はそのまま有効である。

## Alternatives Considered

- **2 モジュールを 1 つの artifact に統合する** — 却下。本体が Compose 本体 (runtime / ui / lifecycle / savedstate) に依存し、Compose を使わない消費者 (MAUI Android・KMP・View 系 Android) に数 MB の Compose が届く。MAUI binding には Compose 系 NuGet の追加と Gradle 側との版合わせが要る (姉妹ライブラリ KsSettingsView が統合後に dex の二重定義で版固定を強いられた実績)。Compose 側の配布物自体は 25 KB でサイズは理由にならない
- **現状名 (`ksdialogs` = 本体 / `ksdialogs-compose`) を維持する** — 却下。素の名前の中身が iOS (全部入り) と Android (View 系のみ) でずれ、多数派の Compose 利用者が接尾辞付きを選ぶことになる
- **座標だけ差し替え、ディレクトリ / project 名は現状のままにする** — 却下。`ksdialogs` ディレクトリが `ksdialogs-core` を発行する形が残り、読み手を惑わせ続ける
- **Kotlin パッケージ名も座標に揃える (`jp.kamusoft.ksdialogs.core` 等)** — 却下。artifact 名とパッケージ名を一致させる慣習はなく、利用者の import とソース・テスト・文書の全体が書き換わるのに利用者の得がない

## Consequences

- 正: Compose 利用者は素の名前 1 行で主役の配布物に当たり、iOS と「素の名前 = 全部入り」で揃う
- 正: 分離は維持されるため、MAUI / KMP / View 系の消費者に Compose が届かない構図は変わらない
- 負: View 系だけを使う人は `-core` を選ぶことを知る必要があり、README と Skills での案内が要る
- 負: 公開前とはいえ、写像表を参照する文書 (README・skills・concepts・handbook) と設定 (Gradle のディレクトリ / project 参照・MAUI binding の aar パスと gradlew の task 名・KMP の依存宣言・samples の依存置換) の書き換えが発生する

## Revisit When

- 前提 (Context) が崩れたとき。特に Maven Central へ公開した後は改名が利用者の移行を伴う

---
出典: kasane/roadmaps/package-distribution/phases/phase-5-native-packaging/history.md (2026-09-08: Android の配布物の名前)
関連: cross/ADR-0005 (公開識別子の写像表。Android 行の配布上の識別子を本決定で置き換え) / android/ADR-0001 (Compose 系の別モジュール分離。Maven 座標名を本決定で置き換え)
