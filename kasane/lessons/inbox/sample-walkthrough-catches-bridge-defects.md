---
scope: process
kind: success
severity: normal
count: 2
first-seen: 2026-08-21
last-seen: 2026-08-27
evidence:
  - add-toast (2件: ① tasks 6.3 の4ルート通しで、Android 本体の既定 announce 実装が支援技術オフの端末で `IllegalStateException` を投げ Toast 表示のたびにクラッシュする不具合を検出。instrumented 299 件が green の状態で潜んでおり、既存テストは差し替え可能な通知口しか見ておらず既定実装が無検査だった。② tasks 8.5 の失敗系実機観測 (オーナー決定 A 方式) で、MAUI iOS の Loading / Dialog factory 例外が SIGSEGV でプロセスを落とすことを検出。診断の結果、コードは正しく **.NET for iOS のネイティブリンクが xcframework の更新を検知せず修正前バイナリが .app に残っていた** (ビルド衛生の欠陥) と判明 — 静的レビュー2周と両端の自動テストは「ソースが正しいこと」しか見ておらず、実機で動くバイナリが古いことは実機通しでしか可視化できなかった。成果物破棄 + 再リンクで4ケースすべて期待どおりを確認)
  - add-presentation-behavior (tasks 8.4「パリティ準拠の Sample 通し + 演出の実機確認証跡」を完了条件に置いたことで、MAUI 本体の不具合 2 件 — iOS binding の block 変換誤りによる SIGSEGV と、両 OS の `ShowAsync` がラッチ時点で返る配送タイミングの取り違え — を検出。どちらも maui `dotnet test` 55 件・互換面 14 件が green の状態で潜んでおり、ユニットテストは継ぎ目 (Task → completion 変換) までしか見ていなかった。連写ストリップ「中身が消えたコマで初めて結果表示が切り替わる」が契約違反を可視化した。さらに同 change で 3 件目 — MAUI iOS で演出の透明度だけが動き位置・大きさ (Slide / Zoom / Custom Hook の移動) が動かない不具合 — も連写の画素計測 (覆いの進み 2% の時点でカードが最終位置) で検出。いずれも同一 change 内のため count は 1 のまま)
---

## ルール文

ブリッジ (binding / 互換面) を経由する形態を含む変更では、ユニットテストが green でも、公開 API を利用者と同じ側から使う Sample で「契約が観察できる操作」(結果表示の切り替わるコマ・クラッシュの有無・演出の有無) を実機 / シミュレータで通し、連写または前後の状態を証跡として残すことを完了条件にする。継ぎ目のテスト (変換・パススルー) が通っていることを「経路が成立している」ことの証明にしない。

## 経緯

- 2026-08-21 add-presentation-behavior: ブリッジ層のテストは設計上「継ぎ目の検証のみ」(core/ADR-0009 の層別) で、実行時の結線 (block の型・待ち先の取り違え) はテストの射程外だった。Sample 通しが唯一その射程を持ち、実際に 2 件を捕まえた。handbook/cross/runtime-behavior-verification.md (実行時挙動の完了判定) の適用範囲を「不具合修正」から「ブリッジ経路の新規実装」へ広げる候補
