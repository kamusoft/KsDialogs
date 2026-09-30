---
id: 0045
title: 接頭辞 ks は OS の View 型に後付けする属性とその SwiftUI の対に限る
status: accepted
date: 2026-09-30
---

## Context

決定の時点で、iOS と Android の公開 API には小文字の `ks` で始まるメンバーが 2 種類あった。ダイアログ・Loading・Toast の中身の View に配置や演出の属性を付けるもの (UIKit の `UIView` と Android の `View` への後付けプロパティ、それと同名の SwiftUI の modifier。例: `ksDialogOptions`) と、基準領域「表示中のページ」のためにアプリのページを名乗らせる印 (SwiftUI と Compose の modifier `ksDialogCurrentPage()`) である。MAUI と KMP の共有層には小文字の `ks` で始まる公開メンバーはない。

この接頭辞は 2026-08-18〜19 の add-layout-spec で UIKit の後付けプロパティ名として初めて現れ、SwiftUI の modifier は同じ名前で core/ADR-0015 に載った。付ける理由を検討した記録は、phase の履歴・change・ADR・レビューのどこにもない。2026-09-06 の rename-swiftui-transition-modifier では、1 つだけ接頭辞のなかった SwiftUI の演出 modifier を、UIKit と綴りを揃えるために `ks` 付きへ改めた。このとき全部の `ks` を外す案は変更規模を理由に候補から外され、付けること自体の是非は問われていない。2026-09-27 に追加された表示中のページの印も、既存の形に合わせて `ks` 付きで入った。

2026-09-30 の探索で、オーナーは表示中のページの印の名前を「`ks` を付けずに、何をするかが分かる名前にしたい」と起こした。それに先立って、`ks` をどこに付けるかをここで決める。

前提:
- ダイアログの中身に属性を付ける面は、UIKit のプロパティと SwiftUI の modifier で対になっている
- 属性を付けるメンバーは `0.1.0-beta.1` / `0.1.0-beta.2` で公開済みだが、beta の利用者はほぼいないため、互換への影響は決め手にしない (オーナー判断)

## Decision

接頭辞 `ks` を付けるのは、ダイアログの中身に属性を付ける公開 API のうち、次の 2 つに限る。

- OS の View 型 (UIKit の `UIView`、Android の `View`) への後付けプロパティ。OS の型に一般的な名前を足すと、他のライブラリや OS 自身が将来足すメンバーとぶつかる余地があるため、接頭辞で自ライブラリのものだと分けておく
- 同じ属性を付ける SwiftUI の modifier。UIKit と SwiftUI を行き来する利用者が同じ綴りで探せるように、対になる UIKit のプロパティと名前を揃える

自ライブラリの型・関数 (MAUI の添付プロパティ、Compose の属性宣言 composable、契約の型名 — core/ADR-0034) は OS の型への後付けではないので、この決定の対象外とする。

## Alternatives Considered

- **SwiftUI の modifier だけ `ks` を外す** — 却下。SwiftUI 自身が `dialog` で始まる modifier を持っていて、Apple の標準と見分けにくくなる。2026-09-06 に揃えた UIKit との綴りがまた崩れる
- **すべての `ks` を外す (UIKit・Android の後付けプロパティも含む)** — 却下。OS の View 型に接頭辞のない一般的な名前が生えて、ぶつかる余地が生まれる。公開済みのメンバーがすべて変わり、iOS・Android・Sample・concepts・skills・公開面の検査にまたがる大きな変更になる

## Consequences

- 正: OS の View 型に生える自ライブラリの名前が `ks` でまとまり、他のライブラリや OS のメンバーとぶつかりにくい
- 正: 同じ属性は UIKit と SwiftUI で同じ綴りのまま探せる
- 正: 公開済みの属性メンバーを動かさずに済む
- 負: 属性を付ける modifier は、`ks` の分だけ読みやすさが落ちたまま残る
- 負: 同じライブラリの公開 API でも、OS の View 型への後付けとその SwiftUI の対かどうかで `ks` の有無が分かれ、その規則を利用者と実装者が知っておく必要がある

## Revisit When

- 前提 (Context) が崩れたとき

出典: kasane/changes/archive/2026-09-30-rename-current-page-marker/exploration.md (探索で確認した現状・`ks` 付きの仲間の扱いの選択肢・決定事項) / kasane/changes/archive/2026-09-06-rename-swiftui-transition-modifier/exploration.md (綴りを揃えた改名と、全部外す案を候補から外した経緯) / kasane/changes/archive/2026-08-19-add-layout-spec/design.md (後付けプロパティ名の初出) / kasane/decisions/core/0015-attribute-supply-content-attachment.md (添付の面)
関連: 表示中のページの印は、この決定のもとで `ks` を付けない `markAsDialogCurrentPage()` に改名された (kasane/changes/archive/2026-09-30-rename-current-page-marker/)。規則の現在の記述先は kasane/concepts/ios/api/layout-surface.md の「SwiftUI での添付」と kasane/concepts/android/api/layout-surface.md の「従来 View 系での添付」

現行照合: 2026-09-30 確認 (rename-current-page-marker の蒸留時)。`ks` で始まる公開メンバーは、iOS の `UIView` の extension プロパティ 3 個 (`ios/Sources/KsDialogs/Contract/UIViewDialogAttributes.swift`)・SwiftUI の modifier 3 個 (`ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift`)・Android の `View` の拡張プロパティ 3 個 (`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ViewDialogAttributes.kt`) だけで、どれも中身に属性を付ける面である。表示中のページの印は iOS・Android とも `markAsDialogCurrentPage()`。判定: 維持
