<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# PixiEnv Changelog

## [Unreleased]

## [0.0.6] - 2026-05-28

### Added

- Support for multi-root projects: the plugin now detects and registers Pixi environments from all content roots, not just the project root
- PyCharm 2026 compatibility for Pixi SDK path recognition

### Fixed

- `.pixi` directory is now correctly excluded per content root in multi-module projects

## [0.0.5] - 2026-04-08

### Modified

- Tested compatibility with 2026.1
- Dropped support for 2025.2, minimal supported version now is 2023.3
- Updated gradle to 9.4

### Added

- `PixiPackageManager` to fetch a list of installed packages. This is required for automatic tools (like pytest) detection  
Package Manager is created dynamically to ensure compatibility with 2025.3 as well as 2026.1

## [0.0.4] - 2026-03-27

### Added

- `PyPixiEnvSdkFlavor` to register pixi sdk, existing sdks should update automatically  
This also avoids using internal extension points, improving future maintainability

### Modified

- Instead of injecting paths, wrap the call with `pixi run --environment {env} -- {command}`

## [0.0.3] - 2026-03-24

### Added

- Automatically mark `.pixi` directory as excluded

### Modified

- RunConfigListener replaced with PythonCommandLineTargetEnvironmentProvider. This avoids modifying run profile xml files

## [0.0.2]

### Added

- RunConfigListener that automatically injects correct PATH from pixi environment

## [0.0.1]

### Added

- Initial project scaffold
- Basic functionality - detect all pixi environments in the current project, and add them to PyCharm
- GitHub Actions to automate testing and deployment

[Unreleased]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.6...HEAD
[0.0.6]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.5...0.0.6
[0.0.5]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.4...0.0.5
[0.0.4]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.3...0.0.4
[0.0.3]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.2...0.0.3
[0.0.2]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/Polymathian/pixi-pycharm-plugin/commits/0.0.1
