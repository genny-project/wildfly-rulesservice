#!/bin/bash
if [ -z "${1}" ]; then
   version="latest"
else
   version="${1}"
fi

docker push gennyproject/wildfly-rulesservice:"${version}"
docker tag gennyproject/wildfly-rulesservice:"${version}" gennyproject/wildfly-rulesservice:latest
docker push gennyproject/wildfly-rulesservice:latest
