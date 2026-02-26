Phoebus Olog Documentation
==========================

Documentation for <https://github.com/Olog/phoebus-olog>

View latest snapshot at <https://olog.readthedocs.io>

You can build a local copy using Pixi or a local installation of sphinx.

## Option 1: Using Pixi (Recommended, except on Mac OS)

Install [Pixi](https://pixi.sh) and run:

```bash
# Navigate to the docs directory
cd docs

# Build the documentation
pixi run build

# Or directly build HTML
pixi run html

# Serve documentation locally
pixi run serve

# Clean build artifacts
pixi run clean
```

The Pixi configuration is in `pyproject.toml` under the `[tool.pixi.*]` sections.

## Option 2: Using Sphinx directly

You need to install sphinx and its dependencies:

```bash
# Navigate to the docs directory
cd docs

# Create a virtual environment
python -m venv .venv
# Enter the environment (for Bash, Zsh)
source .venv/bin/activate

# Install from pyproject.toml (installs all dependencies)
pip install .
```

Alternatively, on some RedHat setups:
```bash
sudo yum install python-sphinx
```

Then build the web version
(make sure you've entered the Python virtual environment):

```bash
make clean html
```

The above creates a document tree starting with `_build/html/index.html`.

