#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
VERSION=$(./gradlew properties | grep ^version: | sed 's/^.*: //')
VERSION_WILDCARD_BUGFIX=$(echo $VERSION | sed 's/.$/x/')
if [[ "$VERSION" == *SNAPSHOT ]]
then
  echo "Version is still a SNAPSHOT"
  exit 1
fi
ETM_PUBLIC_VERSION=$(etm-public/gradlew properties -b etm-public/build.gradle | grep ^version: | sed 's/^.*: //')
if [[ "$VERSION" != "$ETM_PUBLIC_VERSION" ]]
then
  echo "etm-public has a different version: $ETM_PUBLIC_VERSION. Expected version $VERSION."
  exit 1
fi
ES_VERSION=$(./gradlew properties | grep ^version_elasticsearch: | sed 's/^.*: //')
ES_PUBLIC_VERSION=$(etm-public/gradlew properties -b etm-public/build.gradle | grep ^version_elasticsearch: | sed 's/^.*: //')
if [[ "$ES_VERSION" != "$ES_PUBLIC_VERSION" ]]
then
  echo "etm-public has a different elasticsearch version: $ES_PUBLIC_VERSION. Expected version $ES_VERSION."
  exit 1
fi

RELEASE_DATE=$(cat "$SCRIPT_DIR"/etm-public/etm-documentation/docs/support-matrix/README.md | grep "ETM $VERSION_WILDCARD_BUGFIX" | cut -d'|' -f2)
if [[ -z "${RELEASE_DATE// }" ]]
then
  echo "Release date in support matrix documentation is empty"
  exit 1
fi
EOL_DATE=$(cat "$SCRIPT_DIR"/etm-public/etm-documentation/docs/support-matrix/README.md | grep "ETM $VERSION_WILDCARD_BUGFIX" | cut -d'|' -f6)
if [[ -z "${EOL_DATE// }" ]]
then
  echo "End of life date in support matrix documentation is empty"
  exit 1
fi
echo "Releasing Enterprise Telemetry Monitor $VERSION"

echo "Compiling the project"
./gradlew clean build
if [ $? -ne 0 ]; then
  echo "Build failed"
  exit 1
fi

echo "Generating Linux x86_64 distribution"
./gradlew :etm-distribution:linuxJreX64DistTar
if [ $? -ne 0 ]; then
  echo "Generating Linux x86_64 distribution failed"
  exit 1
fi
echo "Uploading distributions to www.jecstar.com"
scp -r "$SCRIPT_DIR"/etm-distribution/build/distributions/* www.jecstar.com:/home/mark/etm-dist
cd "$SCRIPT_DIR" || exit

echo "Checking for support matrix in documentation"

echo "Building the documentation"
cd "$SCRIPT_DIR"/etm-public/etm-documentation || exit
yarn docs:build
if [ $? -ne 0 ]; then
  echo "Generating documentation failed"
  exit 1
fi

echo "Uploading the documentation to www.jecstar.com"
scp -r docs/.vuepress/dist www.jecstar.com:/home/mark/etm-docs
cd "$SCRIPT_DIR" || exit

echo "Generating OCI image"
buildah unshare "$SCRIPT_DIR"/etm-public/etm-buildah/build-oci.sh $VERSION
if [ $? -ne 0 ]; then
    echo "Generating OCI image failed"
    exit 1
fi
echo "Pushing OCI image to Google Container Registry"
podman push eu.gcr.io/virtual-ellipse-208415/etm:$VERSION

#Check release date in support matrix document!!

#==== Push subtree
#git subtree push --prefix=etm-public git@github.com:jecstarinnovations/etm.git develop
#cd ../etm-public
#git checkout develop
#git pull
#git checkout master
#git merge develop
#git tag -a v$VERSION
#git push --tags

