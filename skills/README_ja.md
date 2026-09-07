# KsDialogs Agent Skills

## Skill 一覧

| name | 対象 | 1 行説明 | English | 日本語 |
|---|---|---|---|---|
| `ksdialogs-ios` | Native iOS | SwiftUI または UIKit で Dialog・Loading・Toast のフローを実装する。 | [en](en/ksdialogs-ios/SKILL.md) | [ja](ja/ksdialogs-ios/SKILL.md) |
| `ksdialogs-android` | Native Android | Android View または Jetpack Compose で Dialog・Loading・Toast のフローを実装する。 | [en](en/ksdialogs-android/SKILL.md) | [ja](ja/ksdialogs-android/SKILL.md) |
| `ksdialogs-maui` | .NET MAUI | .NET MAUI と dependency injection で Dialog・Loading・Toast のフローを実装する。 | [en](en/ksdialogs-maui/SKILL.md) | [ja](ja/ksdialogs-maui/SKILL.md) |
| `ksdialogs-kmp` | Kotlin Multiplatform | 共有 Kotlin コードから KsDialogs を使い、Android と iOS の host を統合する。 | [en](en/ksdialogs-kmp/SKILL.md) | [ja](ja/ksdialogs-kmp/SKILL.md) |
| `ksdialogs-aiforms-migration` | AiForms.Maui.Dialogs からの移行 | AiForms.Maui.Dialogs API を KsDialogs.Maui に対応付け、廃止機能を置き換える。 | [en](en/ksdialogs-aiforms-migration/SKILL.md) | [ja](ja/ksdialogs-aiforms-migration/SKILL.md) |

## Skill をコピーする

選んだ言語の Skill を、利用するプロジェクトの `.agents/skills/` ディレクトリへコピーする。Claude Code では、代わりに `.claude/skills/` へコピーする。

```sh
mkdir -p .agents/skills
cp -R skills/<language>/<skill-name> .agents/skills/
```

```sh
mkdir -p .claude/skills
cp -R skills/<language>/<skill-name> .claude/skills/
```

## 片方の言語を選ぶ

各 Skill は片方の言語版だけをコピーする。`<language>` は `en` または `ja` に置き換える。両言語版は同じ Skill 名を使うため、同じプロジェクトへ両方をコピーしない。
