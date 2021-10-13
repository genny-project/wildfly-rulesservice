#!/bin/bash

project=wildfly-rulesservice
file="rulesservice-war/src/main/resources/rulesservice-war-git.properties"

function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}

if [ -z "${1}" ]; then
  version="latest"
else
  version="${1}"
fi

if [ -z "${2}" ]; then
        arch=""
else
        arch=".${2}"
fi

echo "arch=[$arch]"

if [ -f "$file" ]; then
  echo "$file found."
  echo "git.commit.id = " "$(prop 'git.commit.id')"
  echo "git.build.version = " "$(prop 'git.build.version')"

  docker build --build-arg CODE_VERSION=${version} -f Dockerfile${arch} -t gennyproject/${project}:"${version}" --network host .
else
  echo "ERROR: git properties $file not found."
fi

#clean up
image_ids=$(docker images | grep ${project} | grep none)
if [ "${image_ids:-0}" == 0 ]; then
  echo 'Skip clean up'
else
  docker images | grep ${project} | grep none | awk '{print $3}' | xargs docker rmi
fi
