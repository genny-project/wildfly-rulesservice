#!/bin/bash
project=wildfly-rulesservice
file="rulesservice-war/src/main/resources/rulesservice-war-git.properties"

function prop {
    grep "${1}=" ${file}|cut -d'=' -f2
}

if [ -z "${1}" ]; then
   version="latest"
else
   version="${1}"
fi

if [ -f "$file" ]
then
  echo "$file found."

  echo "git.commit.id = " $(prop 'git.commit.id')
  echo "git.build.version = " $(prop 'git.build.version')
  docker push gennyproject/wildfly-rulesservice:latest
  docker push gennyproject/wildfly-rulesservice:"${version}"
  docker push gennyproject/${project}:$(prop 'git.commit.id')
  docker push gennyproject/${project}:$(prop 'git.build.version')
else
  echo "ERROR: git properties $file not found."
fi
