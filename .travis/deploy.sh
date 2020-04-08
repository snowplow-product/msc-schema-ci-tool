#!/bin/bash

tag_version=$1

cd $TRAVIS_BUILD_DIR

project_version=$(sbt version -Dsbt.log.noformat=true | perl -ne 'print $1 if /(\d+\.\d+\.\d+[^\r\n]*)/')
if [ "${project_version}" == "${tag_version}" ]; then
    sbt +publish
else
    echo "Tag version '${tag_version}' doesn't match version in scala project ('${project_version}'). Aborting!"
    exit 1
fi