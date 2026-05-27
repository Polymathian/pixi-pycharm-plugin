# PixiEnv

<!-- Plugin description -->
PyCharm plugin that automatically detects and registers [Pixi](https://pixi.sh) environments as Python interpreters — no manual configuration required.

While official Pixi support may eventually land in PyCharm, this plugin provides a seamless workflow today.
<!-- Plugin description end -->

## Features

- **Automatic SDK registration** — on project open, all Pixi environments are discovered and added as Python interpreters
- **Multi-root project support** — detects environments across all content roots in multi-module projects
- **Correct command execution** — Python runs are wrapped with `pixi run --environment <env> --` so environment activation scripts run properly
- **Package detection** — integrates with PyCharm's package management system, enabling automatic tool detection (e.g. pytest)
- **Auto-exclusion** — the `.pixi/` directory is automatically excluded from project indexing, preventing PyCharm from indexing hundreds of conda packages
- **Cross-platform** — works on Windows, macOS, and Linux

## Requirements

- PyCharm 2023.3 or later (Community or Professional)
- [Pixi](https://pixi.sh) installed on your system
- At least one Pixi environment already created (i.e. `pixi install` has been run)

## Installation

1. Download the latest `.zip` from the Releases page
2. Open **Settings / Preferences** → **Plugins** → gear icon → **Install Plugin from Disk...**
3. Select the downloaded file and restart PyCharm

## Usage

### Basic workflow

1. Open a project that contains a `pixi.toml` (or `pyproject.toml` with a `[tool.pixi]` section)
2. Run `pixi install` in the project directory if you have not already
3. Open the project in PyCharm — the plugin detects `.pixi/envs/` and registers each environment as a Python SDK automatically

No further action is needed. The first discovered environment is set as the project interpreter if no Python SDK is currently configured.

### Selecting an interpreter

If you have multiple Pixi environments (e.g. `default`, `dev`, `test`) you can switch between them via:

**Settings / Preferences** → **Project** → **Python Interpreter** → dropdown

Each environment appears as **Python \<version\> (pixi: \<env-name\>)**.

### Running code and tests

Run configurations work as-is. When PyCharm executes Python, the plugin wraps the call as:

```
pixi run --environment <env> --manifest-path <pixi.toml> -- python <args>
```

This ensures the full Pixi activation lifecycle runs — including any `activation.scripts` defined in `pixi.toml`.

### Package management

PyCharm's **Python Packages** tool window shows installed packages pulled from `pixi list`. Automatic tool detection (e.g. discovering pytest as the test runner) works without any extra setup.

## Limitations

- The plugin reads environments that have already been created. Run `pixi install` before opening the project, or re-open the project after running it.
- Package installation via the **Python Packages** UI is not yet supported. Use `pixi add <package>` from the terminal.
- Package search in the **Python Packages** UI is not available (Pixi manages packages via `pixi.toml`, not a live index).

## How it works

On project startup the plugin:

1. Walks each content root looking for a `.pixi/` directory
2. Lists subdirectories of `.pixi/envs/` and checks each for a valid Python executable (`bin/python` on Unix, `python.exe` on Windows)
3. Registers each environment as a `PyPixiEnvSdkFlavor` Python SDK (a custom flavor extending CPython)
4. Marks `.pixi/` as excluded so PyCharm does not index the conda packages inside it
5. Sets the first found environment as the project interpreter if none is configured

When Python is executed, `PyPixiFlavorData.prepareTargetCommandLine()` intercepts the command and prepends the `pixi run` wrapper so that environment variables and activation scripts are applied correctly.

## Building from source

```bash
git clone https://github.com/Polymathian/pixi-pycharm-plugin.git
cd pixi-pycharm-plugin
./gradlew buildPlugin          # produces build/distributions/*.zip
./gradlew runIde               # launches a sandbox PyCharm with the plugin installed
./gradlew test                 # runs the test suite
```

## Contributing

Pull requests are welcome.

## License

See [LICENSE](LICENSE) in the repository.
