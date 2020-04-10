#!/bin/bash

set -e

tag=$1

cd "${TRAVIS_BUILD_DIR}"
export TRAVIS_BUILD_RELEASE_TAG="${tag}"

echo "DEPLOY: starting schema-ci deploy for version $TRAVIS_BUILD_RELEASE_TAG..."

release-manager \
    --config "./.travis/release.yml" \
    --check-version \
    --make-version \
    --make-artifact \
    --upload-artifact

echo "DEPLOY: schema-ci deployed..."