#!/bin/bash
echo "Setting up Realms ****"

KEYCLOAK_JSON_DIR=/opt/jboss/wildfly/realm
KEYCLOAK_ORIGINAL_JSON_DIR=/opt/realm
# copy all the keycloak files so they may be modified
cp -rf ${KEYCLOAK_ORIGINAL_JSON_DIR}/* ${KEYCLOAK_JSON_DIR}/

# change the package.json file
function escape_slashes {
    /usr/bin/sed 's/\//\\\//g'
}

function change_line {
  eval OLD_LINE_PATTERN="$1"
  eval NEW_LINE="$2"
  eval FILE="$3"

    local NEW=$(echo "${NEW_LINE}" | escape_slashes)
    /usr/bin/sed -i  '/'"${OLD_LINE_PATTERN}"'/s/.*/'"${NEW}"'/' "${FILE}"
}


for i in `ls ${KEYCLOAK_JSON_DIR}` ; do
if grep -r localhost ${KEYCLOAK_JSON_DIR}/${i}
then
   OLD_LINE_KEY="auth-server-url"
   NEW_LINE="\"auth-server-url\": \"${KEYCLOAKURL}/auth\","
   change_line "\${OLD_LINE_KEY}" "\${NEW_LINE}" "\${KEYCLOAK_JSON_DIR}\/\${i}"
fi

done
myip=
while IFS=$': \t' read -a line ;do
    [ -z "${line%inet}" ] && ip=${line[${#line[1]}>4?1:2]} &&
        [ "${ip#127.0.0.1}" ] && myip=$ip
  done< <(LANG=C /sbin/ifconfig eth0)

export IPMY=$myip


if [ $ADMIN_USERNAME ] && [ $ADMIN_PASSWORD ]; then
    /opt/jboss/wildfly/bin/add-user.sh  $ADMIN_USERNAME  $ADMIN_PASSWORD
fi

if [ $XMX ] ; then
  echo "JVM XMX = "$XMX
  sed -i 's,-Xmx512m,-Xmx'"$XMX"',g' /opt/jboss/wildfly/bin/standalone.conf
fi
if [ $XMS ] ; then
  echo "JVM XMS = "$XMS
  sed -i 's,-Xms64m,-Xms'"$XMS"',g' /opt/jboss/wildfly/bin/standalone.conf
fi

    /opt/jboss/wildfly/bin/add-user.sh  jmsuser jmspassword1

rm -Rf /opt/jboss/wildfly/data/*

#/subsystem=transactions:write-attribute(name=node-identifier,value=MyNode)



if [[ $DEBUG == "TRUE" ]]; then
echo "Remote Debug on port 8787 True";
#export JAVA_OPTS="${JAVA_OPTS}  -agentpath:/usr/local/YourKit-JavaProfiler-2019.8/bin/linux-x86-64/libyjpagent.so=port=10002,listen=all "
export JAVA_OPTS="${JAVA_OPTS}    -agentlib:jdwp=transport=dt_socket,address=*:8787,server=y,suspend=${DEBUG_SUSPEND:=n}  -Drebel.remoting_plugin=true "
/opt/jboss/wildfly/bin/standalone.sh --debug  -Djboss.bind.address.private=${CLUSTER_IP}  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -DHIBERNATE_SHOW_SQL=${HIBERNATE_SHOW_SQL:=false} -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQLA_PASSWORD -Djava.security.auth.login.config=''  -b 0.0.0.0 -Dresteasy.preferJacksonOverJsonB -Djboss.tx.node.id=${hostname}  -Dhazelcast.health.monitoring.level=OFF -Dhazelcast.http.healthcheck.enabled=false --server-config=standalone-full-ha.xml -Dorg.kie.executor.pool.size=10 -Dorg.kie.executor.disabled=false
else
echo "Debug is False";
#export JAVA_OPTS="${JAVA_OPTS} -agentpath:/usr/local/YourKit-JavaProfiler-2019.8/bin/linux-x86-64/libyjpagent.so=port=10002,listen=all "
/opt/jboss/wildfly/bin/standalone.sh -Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true  -Djboss.bind.address.private=${CLUSTER_IP}  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -Dresteasy.preferJacksonOverJsonB  -Djboss.tx.node.id=${hostname} -Dhazelcast.health.monitoring.level=OFF -Dhazelcast.http.healthcheck.enabled=false  --server-config=standalone-full-ha.xml  -Dorg.kie.executor.pool.size=10 -Dorg.kie.executor.disabled=false
#  -DHIBERNATE_SHOW_SQL=$HIBERNATE_SHOW_SQL -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQL_PASSWORD -Djava.security.auth.login.config=''
fi
