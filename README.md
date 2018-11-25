# sipcall

## About

sipcall is a SIP and RTP test tool written in Java. It uses the jain-sip SIP stack and the efflux RTP stack.
It provides a web service interface to place and receive calls, play and record audio and send a DTMF.
It includes a web application to test the functionality: ![sipcall web app screenshot](sipcall_web_app_screenshot.png?raw=true "sipcall Web App")

## Install

```
git clone git@github.com:jdebroin/efflux.git
cd efflux
mvn install
```

```
git clone git@github.com:jdebroin/jsip.git
cd jsip/m2
mvn install -Dmaven.javadoc.skip=true
cd ../..
```

```
git clone git@github.com:jdebroin/sipcall.git
cd sipcall
mvn install
```

## Run with web service listening on port 8085

```
java -Dlog4j2.configurationFile=log4j2.xml -jar target/sipcall-0.2.0-jar-with-dependencies.jar 8085
```

## Try it

Access the web application at http://localhost:8085.

## Samples

Sample nodejs scripts using sipcall can be found in the src/main/config/sipcallclient directory. 
