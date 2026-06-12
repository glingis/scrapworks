import groovy.json.JsonSlurperClassic

def call(def configInput) {
    def config = normalizeConfig(configInput)
    validateConfig(config)

    def pythonSourceFolder = config.pythonSourceFolder
    def scriptToRun = config.scriptToRun
    def environmentInstalledCheck = config.environmentInstalledCheck
    def pythonVersion = config.pythonVersion
    def requiredPackages = (config.requiredPackages ?: []) as List
    def cleanupEnvironment = config.containsKey('cleanupEnvironment') ? config.cleanupEnvironment : true

    def packageInstallCommands = requiredPackages.collect { pkg ->
        "uv pip install ${pkg.toString().trim()}"
    }.join('\n')

    def shellScript = """#!/usr/bin/env bash
set -euo pipefail

PYTHON_SOURCE_FOLDER=${shellQuote(pythonSourceFolder)}
SCRIPT_TO_RUN=${shellQuote(scriptToRun)}
ENVIRONMENT_INSTALLED_CHECK=${shellQuote(environmentInstalledCheck)}
PYTHON_BIN=${shellQuote(pythonVersion)}
CLEANUP_ENVIRONMENT=${cleanupEnvironment ? 'true' : 'false'}

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

if [ -d \"$ENVIRONMENT_INSTALLED_CHECK\" ]; then
  echo \"Existing environment detected at $ENVIRONMENT_INSTALLED_CHECK\"
else
  echo \"Creating environment at $ENVIRONMENT_INSTALLED_CHECK\"
  uv venv --python \"$PYTHON_BIN\" \"$ENVIRONMENT_INSTALLED_CHECK\"
fi

# shellcheck disable=SC1091
source \"$ENVIRONMENT_INSTALLED_CHECK/bin/activate\"

cd \"$PYTHON_SOURCE_FOLDER\"

${packageInstallCommands}

python3 \"$SCRIPT_TO_RUN\"

deactivate || true

echo \"Deactivated Virtual Env\"

if [ \"$CLEANUP_ENVIRONMENT\" = \"true\" ]; then
  rm -rf \"${WORKSPACE}/$ENVIRONMENT_INSTALLED_CHECK\"
  echo \"Removed virtual environment at $ENVIRONMENT_INSTALLED_CHECK\"
fi
"""

    sh(label: 'Run Python with uv virtual environment', script: shellScript)
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
        'scriptToRun',
        'environmentInstalledCheck',
        'pythonVersion',
        'requiredPackages'
    ]

    requiredKeys.each { key ->
        if (!config.containsKey(key) || config[key] == null || config[key].toString().trim().isEmpty()) {
            error("Missing required config field: ${key}")
        }
    }

    if (!(config.requiredPackages instanceof List)) {
        error('requiredPackages must be a JSON array or Groovy List')
    }
}

def shellQuote(String value) {
    return "'${value.replace("'", "'\\''")}'"
}
