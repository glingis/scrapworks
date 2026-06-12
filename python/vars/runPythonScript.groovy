import groovy.json.JsonSlurperClassic

def call(def configInput) {
    def config = normalizeConfig(configInput)
    validateConfig(config)

    def pythonSourceFolder = config.pythonSourceFolder
    def environmentPath = config.environmentPath ?: config.environmentInstalledCheck
    def pythonVersion = config.pythonVersion
    def requiredPackages = (config.requiredPackages ?: []) as List
    def cleanupEnvironment = config.containsKey('cleanupEnvironment') ? config.cleanupEnvironment : true

    def packageInstallCommands = requiredPackages.collect { pkg ->
        "uv pip install ${pkg.toString().trim()}"
    }.join('\n')

    env.PYTHON_SOURCE_FOLDER = pythonSourceFolder
    env.PYTHON_ENV_PATH = environmentPath
    env.PYTHON_BIN = pythonVersion
    env.PYTHON_CLEANUP_ENVIRONMENT = cleanupEnvironment ? 'true' : 'false'

    def shellScript = """#!/usr/bin/env bash
set -euo pipefail

PYTHON_SOURCE_FOLDER=${shellQuote(pythonSourceFolder)}
PYTHON_ENV_PATH=${shellQuote(environmentPath)}
PYTHON_BIN=${shellQuote(pythonVersion)}

cd \"${WORKSPACE}\"

if [ ! -d \"$PYTHON_SOURCE_FOLDER\" ]; then
  echo \"Python source folder not found: $PYTHON_SOURCE_FOLDER\"
  exit 1
fi

if [ ! -x \"$PYTHON_BIN\" ]; then
  echo \"Python executable not found or not executable: $PYTHON_BIN\"
  exit 1
fi

ACTUAL_PYTHON_VERSION=\"$($PYTHON_BIN --version 2>&1)\"
echo \"Using $ACTUAL_PYTHON_VERSION\"

if [ -d \"$PYTHON_ENV_PATH\" ]; then
  echo \"Existing environment detected at $PYTHON_ENV_PATH\"
else
  echo \"Creating environment at $PYTHON_ENV_PATH\"
  uv venv --python \"$PYTHON_BIN\" \"$PYTHON_ENV_PATH\"
fi

# shellcheck disable=SC1091
source \"$PYTHON_ENV_PATH/bin/activate\"

cd \"$PYTHON_SOURCE_FOLDER\"

${packageInstallCommands}

deactivate || true

echo \"Python environment setup complete\"
"""

    sh(label: 'Setup Python environment', script: shellScript)
}

def normalizeConfig(def configInput) {
    if (configInput instanceof Map) {
        return configInput
    }

    if (configInput instanceof String) {
        return new JsonSlurperClassic().parseText(configInput as String) as Map
    }

    error("runPythonScript expects a Map or a JSON String")
}

def validateConfig(Map config) {
    def requiredKeys = [
        'pythonSourceFolder',
        'pythonVersion',
        'requiredPackages'
    ]

    requiredKeys.each { key ->
        if (!config.containsKey(key) || config[key] == null || config[key].toString().trim().isEmpty()) {
            error("Missing required config field: ${key}")
        }
    }

    if (!config.environmentPath && !config.environmentInstalledCheck) {
        error('Missing required config field: environmentPath')
    }

    if (!(config.requiredPackages instanceof List)) {
        error('requiredPackages must be a JSON array or Groovy List')
    }
}

def shellQuote(String value) {
    return "'${value.replace("'", "'\\''")}'"
}
