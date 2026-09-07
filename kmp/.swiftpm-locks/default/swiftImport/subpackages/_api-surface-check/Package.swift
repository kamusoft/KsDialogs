// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_api-surface-check",
  platforms: [
    .iOS("17.0")
  ],
  products: [
    .library(
      name: "_api-surface-check",
      type: .none,
      targets: ["_api-surface-check"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_api-surface-check",
      dependencies: [
      ]
    )
  ]
)
