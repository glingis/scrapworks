# Python Jenkins Shared Library

This folder contains a Jenkins shared library step named `runPythonScript`.

## Step

`runPythonScript` accepts either:
- a JSON string, or
- a Groovy `Map`

It performs the following actions:
- validates the configuration
- checks whether the virtual environment folder already exists
- checks that the requested Python executable exists
- creates a uv virtual environment if needed
- activates the environment
- installs the requested packages
- runs the target Python script
- deactivates the environment
- optionally removes the environment afterward

## Required JSON structure

```json
{
  "pythonSourceFolder": ".",
  "scriptToRun": "helloworld.py",
  "environmentInstalledCheck": "runtime",
  "pythonVersion": "/home/gistest/WebServices/python/usr/local/bin/python3",
  "requiredPackages": [
    "-r updated_req_main_runner_linux_for_py314.txt"
  ]
}
```

## Optional fields

```json
{
  "cleanupEnvironment": true
}
```

- `cleanupEnvironment`: when `true`, removes the virtual environment folder after execution. Defaults to `true`.

## Example Jenkinsfile usage

```groovy
library identifier: 'scrapworks@main', retriever: modernSCM([
  $class: 'GitSCMSource',
  remote: 'https://github.com/glingis/scrapworks.git'
])

pipeline {
  agent any

  stages {
    stage('Run Python') {
      steps {
        script {
          runPythonScript('''
          {
            "pythonSourceFolder": ".",
            "scriptToRun": "helloworld.py",
            "environmentInstalledCheck": "runtime",
            "pythonVersion": "/home/gistest/WebServices/python/usr/local/bin/python3",
            "requiredPackages": [
              "-r updated_req_main_runner_linux_for_py314.txt"
            ],
            "cleanupEnvironment": true
          }
          ''')
        }
      }
    }
  }
}
```
