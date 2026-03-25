<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# PixiEnv Changelog

## [Unreleased]

### Added
- `PyPixiEnvSdkFlavor` to register pixi sdk, existing sdks should update automatically

### Modified
- Instead of intecting paths, wrap the call with `pixi run --`

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

[Unreleased]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.3...HEAD
[0.0.3]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.2...0.0.3
[0.0.2]: https://github.com/Polymathian/pixi-pycharm-plugin/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/Polymathian/pixi-pycharm-plugin/commits/0.0.1
