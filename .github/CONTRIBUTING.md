# Contributing to KsDialogs

[日本語](CONTRIBUTING_ja.md)

## Contribution policy

We welcome reports and proposals through GitHub Issues, but we do not accept pull requests from external contributors. Keeping implementation changes within the repository's Kasane workflow preserves the problem context, design decisions, verification evidence, and review quality. It also prevents a proposed patch from bypassing the exploration and specification work needed to assess its broader impact across iOS Native, Android Native, .NET MAUI, and Kotlin Multiplatform.

Repository collaborators make implementation changes through that workflow after an issue has been assessed. Opening an issue does not guarantee that a change will be adopted, but it gives the maintainers the evidence needed to investigate and prioritize it.

## How to open an issue

Use the [Issue template chooser](https://github.com/kamusoft/KsDialogs/issues/new/choose). You may write your answers in English or Japanese.

Choose **Bug report** for a reproducible problem. Provide the affected version and platform, the smallest reproducible sequence of steps, the actual behavior, and the expected behavior. For .NET MAUI and Kotlin Multiplatform, select the host OS where the problem occurs. Logs and screenshots are useful when they help isolate the problem.

Choose **Feature request** for a new capability or behavior. Describe the problem to solve, its current impact, and the alternatives or workarounds you considered. Focus on the need and evidence; maintainers will translate accepted proposals into the repository's exploration and change artifacts.

Choose **Question** when you need help using KsDialogs or are unsure whether the behavior is a bug. Provide the KsDialogs version and platform, what you have already tried, and the relevant sections you consulted in the `ksdialogs-ios`, `ksdialogs-android`, `ksdialogs-maui`, `ksdialogs-kmp`, or `ksdialogs-aiforms-migration` Skill or a README.
