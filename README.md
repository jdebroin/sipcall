# sipcall

## About

sipcall is a SIP and RTP test tool written in Java. It uses the jain-sip SIP stack and the efflux RTP stack.
It provides a web service interface to place and receive calls and play and record audio.
It includes a web application to test the functionality: ![sipcall web app screenshot](sipcall_web_app_screenshot.png?raw=true "sipcall Web App")

## Install

```
git clone git@github.com:jdebroin/efflux.git
cd efflux
mvn install
```

```
git clone git@github.com:jdebroin/sipcall.git
cd sipcall
mvn install
```

## Run

```
java -jar target/sipcall-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Try it

Access the web application at http://localhost:8084.

## Samples

Sample nodejs scripts using sipcall can be found in the src/main/config/sipcallclient directory. 
