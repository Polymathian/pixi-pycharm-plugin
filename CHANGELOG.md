<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# PixiEnv Changelog

## [Unreleased]

### Modified

- Updated gradle to 9.4
- Dropped support for 2025.2, minimal supported version now is 2023.3

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

[Unreleased]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.4...HEAD
[0.0.4]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.3...0.0.4
[0.0.3]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.2...0.0.3
[0.0.2]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/Polymathian/pixi-pycharm-plugin/commits/0.0.1
