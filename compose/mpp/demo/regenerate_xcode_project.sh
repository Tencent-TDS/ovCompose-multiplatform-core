# Script to regenerate xcode project

# make script folder current or exit
cd "$(dirname "$0")" || exit

if command -v xcodegen >/dev/null 2>&1; then
  # xcodegen exists
  PROJECT_NAME=ComposeDemo

  if [[ -z "$JAVA_HOME" ]]; then
    echo "JAVA_HOME is not set"
    exit 1
  fi

  PROJECT_FILE_PATH="$PROJECT_NAME.xcodeproj"

  if [ -d "$PROJECT_FILE_PATH" ]; then
    echo "Removing existing project"
    rm -rf "$PROJECT_FILE_PATH"
  fi

  INPUT_FILE="project.template.yml"
  OUTPUT_FILE="project.generated.yml"

  # replace template placeholders with actual values
  sed -e "s|%@JAVA_HOME@%|$JAVA_HOME|g" -e "s|%@PROJECT_NAME@%|$PROJECT_NAME|g" $INPUT_FILE > $OUTPUT_FILE

  xcodegen --spec $OUTPUT_FILE
  open $PROJECT_FILE_PATH
else
  # xcodegen does not exist
  echo "Error: xcodegen not found. Please install it using 'brew install xcodegen'."
  exit 1
fi
