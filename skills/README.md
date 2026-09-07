# KsDialogs Agent Skills

## Skills

| Name | Target | Description | English | Japanese |
|---|---|---|---|---|
| `ksdialogs-ios` | Native iOS | Build Dialog, Loading, and Toast flows with SwiftUI or UIKit. | [en](en/ksdialogs-ios/SKILL.md) | [ja](ja/ksdialogs-ios/SKILL.md) |
| `ksdialogs-android` | Native Android | Build Dialog, Loading, and Toast flows with Android Views or Jetpack Compose. | [en](en/ksdialogs-android/SKILL.md) | [ja](ja/ksdialogs-android/SKILL.md) |
| `ksdialogs-maui` | .NET MAUI | Build Dialog, Loading, and Toast flows with .NET MAUI and dependency injection. | [en](en/ksdialogs-maui/SKILL.md) | [ja](ja/ksdialogs-maui/SKILL.md) |
| `ksdialogs-kmp` | Kotlin Multiplatform | Use KsDialogs from shared Kotlin code with Android and iOS host integration. | [en](en/ksdialogs-kmp/SKILL.md) | [ja](ja/ksdialogs-kmp/SKILL.md) |
| `ksdialogs-aiforms-migration` | AiForms.Maui.Dialogs migration | Map AiForms.Maui.Dialogs APIs to KsDialogs.Maui and replace removed features. | [en](en/ksdialogs-aiforms-migration/SKILL.md) | [ja](ja/ksdialogs-aiforms-migration/SKILL.md) |

## Copy a Skill

Copy the selected language version of the Skill into your project's `.agents/skills/` directory. For Claude Code, copy it into `.claude/skills/` instead.

```sh
mkdir -p .agents/skills
cp -R skills/<language>/<skill-name> .agents/skills/
```

```sh
mkdir -p .claude/skills
cp -R skills/<language>/<skill-name> .claude/skills/
```

## Choose One Language

Copy only one language version of each Skill. Replace `<language>` with `en` or `ja`; do not copy both versions into the same project because they use the same Skill name.
