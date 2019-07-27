#FROM gennyproject/wildfly:16.0.0.Final 
FROM gennyproject/wildfly:v3.0.0 

USER root

ADD docker-entrypoint.sh /opt/jboss/docker-entrypoint.sh
ADD docker-entrypoint2.sh /opt/jboss/

EXPOSE 8080

RUN mkdir -p /.m2/conf
ADD settings.xml /.m2/conf/settings.xml
ARG m2_variable=/.m2
ENV M2_HOME=$m2_variable

RUN mkdir /opt/realm
RUN mkdir /opt/jboss/wildfly/realm
RUN mkdir /realm
RUN mkdir /rules

ADD realm /opt/realm
ADD rulesservice-ear/target/rulesservice-ear.ear $JBOSS_HOME/standalone/deployments/rulesservice-ear.ear
RUN rm -Rf /opt/jboss/wildfly/standalone/data/*
ENTRYPOINT [ "/opt/jboss/docker-entrypoint2.sh" ]
