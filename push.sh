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

if [ -f "$file" ]; then
  echo "$file found."
  echo "git.commit.id = " "$(prop 'git.commit.id')"
  echo "git.build.version = " "$(prop 'git.build.version')"

  docker push gennyproject/wildfly-rulesservice:"${version}"

  docker tag gennyproject/${project}:"${version}" gennyproject/${project}:latest
  docker push gennyproject/wildfly-rulesservice:latest

  docker tag gennyproject/${project}:"${version}" gennyproject/${project}:"$(prop 'git.build.version')"
  docker push gennyproject/${project}:"$(prop 'git.build.version')"

  #create the dummy container that is used in docker-compose to disable things easier
  docker pull gennyproject/dummy:latest
  docker tag gennyproject/dummy:latest gennyproject/${project}:dummy
  docker push gennyproject/${project}:dummy
else
  echo "ERROR: git properties $file not found."
fi
