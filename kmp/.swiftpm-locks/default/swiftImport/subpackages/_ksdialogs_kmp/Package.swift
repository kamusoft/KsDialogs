// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_ksdialogs_kmp",
  platforms: [
    .iOS("17.0")
  ],
  products: [
    .library(
      name: "_ksdialogs_kmp",
      type: .none,
      targets: ["_ksdialogs_kmp"]
    )
  ],
  dependencies: [
    .package(
      path: "../../../../../../ios"
    )
  ],
  targets: [
    .target(
      name: "_ksdialogs_kmp",
      dependencies: [
        .product(
          name: "KsDialogs",
          package: "ios"
        )
      ]
    )
  ]
)
