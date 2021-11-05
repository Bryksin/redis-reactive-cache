if [[ -z "${ARTIFACT_VERSION}" ]]; then
  ARTIFACT_VERSION="0.0.1-SNAPSHOT"
fi

ACTION="clean build"

if [ $# -eq 1 ]; then
    ARTIFACT_VERSION="$1"
elif [ $# -eq 2 ]; then
  ARTIFACT_VERSION="$1"
  ACTION="$2"
fi

cmd="./gradlew ${ACTION} -PbuildVersion=${ARTIFACT_VERSION}"

echo "Executing:" "$cmd"
/bin/sh -c "$cmd"
