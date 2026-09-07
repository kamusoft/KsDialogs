---
type: reference
title: 参考リポジトリの在り処
description: ADR / concepts / handbook が「リポジトリ名 + 識別子」で参照する外部リポジトリのローカルパス対応表 (時限情報)
tags: [reference, repositories, cross-repo]
timestamp: 2026-08-13
---

# 参考リポジトリの在り処

この文書を読むと、本リポジトリ (KsDialogs — AiForms.Maui.Dialogs をリブランドするダイアログ UI ライブラリ) の ADR (Architecture Decision Record) / concepts (`kasane/concepts/` 配下の概念ドキュメント群) / handbook (`kasane/handbook/` 配下の規約群) が参照している外部リポジトリが、ローカル環境のどこにあるかが分かる。

KsDialogs の ADR / concepts / handbook は外部リポジトリを**相対パスで書かず**「リポジトリ名 + 安定識別子 (ADR 番号または文書タイトル)」で参照する。参照の書き方の例:

> 出典: KsSettingsView リポジトリ: cross/ADR-0017 / KsAppKMP リポジトリ: concepts「レイヤードアーキテクチャ」

パスは移動・リネームで腐る時限情報 (時間の経過で不正確になりうる情報) なので、**この1ファイルだけを例外として**集約する — パスが変わったらここだけ更新すればよい。

## 対応表

パスは本リポジトリのルートからの相対表記 (リポジトリ群が同じ親ディレクトリに clone されている前提)。

| リポジトリ名 | パス | 役割 |
|---|---|---|
| AiForms.Maui.Dialogs | `../AiForms.Maui.Dialogs` | 移植元 (MAUI 版ダイアログライブラリ)。機能・仕様の正 (何を提供すべきかの基準)。README が API リファレンスを含む仕様の一次情報源 |
| KsSettingsView | `../KsSettingsView` | AiForms シリーズの SettingsView を Native + MAUI 対応でリビルドした先例。リポジトリ構成・公開識別子・パリティ規約 (プラットフォーム間で Sample の文言・画面構成を揃える規約) など、リポジトリ横断規約と ADR の出典 |
| KsAppKMP | `../KsAppKMP` | KMP アプリ共通基盤。KMP アーキテクチャ (Swift interop・モノレポ構成) の規約と、立ち上げロードマップの進め方の出典 |

- 識別子の解決: KsSettingsView / KsAppKMP は各リポジトリの `kasane/decisions/index.md` / `kasane/concepts/index.md` から辿る (いずれも本リポジトリと同じ Kasane 構造 — `kasane/` 配下に decisions / concepts を持つ運用 — を持つ)。AiForms.Maui.Dialogs は Kasane 構造を持たないため、コード識別子 (型名・ファイル名) と README の節タイトルで参照する
- パスに実体がない場合 (未 clone・別マシン・移動後) は、オーナーに現在の場所を確認してこの表と timestamp を更新する

## してはいけないこと

- この表以外の場所 (ADR / concepts / handbook の本文・出典) に外部リポジトリのローカルパスを直接書く — パスが分散すると移動時に追跡不能になる
- 本リポジトリ内のディレクトリをここに載せる — リポジトリと一緒に動くので腐らない。通常の相対パスで書けばよい

## 関連

- [ソースコメント規約](../../../handbook/cross/comment-policy.md) — この対応表を参照先の唯一の情報源とするコメント規約。Kasane ハーネス由来の用語 (変更・アーカイブ等) の定義もそちらにある
