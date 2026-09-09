// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0",
  platforms: [
    .iOS("17.0")
  ],
  products: [
    .library(
      name: "jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0",
      type: .none,
      targets: ["jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/kamusoft/KsDialogs-SPM",
      exact: "0.0.0-alpha.0"
    )
  ],
  targets: [
    .target(
      name: "jp_kamusoft_ksdialogs_kmp_0_0_0_alpha_0",
      dependencies: [
        .product(
          name: "KsDialogs",
          package: "KsDialogs-SPM"
        )
      ]
    )
  ]
)
