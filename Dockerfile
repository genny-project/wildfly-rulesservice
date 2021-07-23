ARG CODE_VERSION=wildfly24
FROM gennyproject/wildfly:wildfly24

#RUN apk add --no-cache libc6-compat

USER root


#RUN wget https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2019.8-docker.zip -P /tmp/ && \
#  unzip /tmp/YourKit-JavaProfiler-2019.8-docker.zip -d /usr/local && \
#  rm /tmp/YourKit-JavaProfiler-2019.8-docker.zip

#RUN wget http://download-keycdn.ej-technologies.com/jprofiler/jprofiler_linux_9_2.tar.gz -P /tmp/ &&\
#RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_12_0_2.tar.gz -P /tmp/ &&\
# tar -xzf /tmp/jprofiler_linux_12_0_2.tar.gz -C /usr/local &&\
# rm /tmp/jprofiler_linux_12_0_2.tar.gz

#ENV JPAGENT_PATH="-agentpath:/usr/local/jprofiler12.0.2/bin/linux-x64/libjprofilerti.so=nowait"
EXPOSE 8849

ADD docker-entrypoint.sh /opt/jboss/docker-entrypoint.sh
ADD docker-entrypoint2.sh /opt/jboss/

EXPOSE 8080
EXPOSE 10002

RUN mkdir -p /.m2/conf
ADD settings.xml /.m2/conf/settings.xml
ARG m2_variable=/.m2
ENV M2_HOME=$m2_variable
RUN mkdir /opt/realm
RUN mkdir /opt/jboss/wildfly/realm
RUN mkdir /realm
RUN mkdir /rules

ADD realm /opt/realm
ADD rulesservice-war/target/rulesservice-war.war $JBOSS_HOME/standalone/deployments/rulesservice-war.war
RUN rm -Rf /opt/jboss/wildfly/standalone/data/*
ENTRYPOINT [ "/opt/jboss/docker-entrypoint2.sh" ]
