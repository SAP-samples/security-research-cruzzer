# Cruzzer - A coverage-guided Webapplication Fuzzer

[![REUSE status](https://api.reuse.software/badge/github.com/SAP-samples/security-research-cruzzer)](https://api.reuse.software/info/github.com/SAP-samples/security-research-cruzzer)

Cruzzer supports PHP and Java Web-Applications. It is capable to fuzz Web-Applications for coverage.
It uses the coverage information obtained from Xdebug in case of a PHP Web-Application or Jacoco in case of a Java Web-Application.
Read more about Cruzzer in our recent [Blog Post](https://blogs.sap.com/2023/04/25/cruzzer-combining-crawling-and-coverage-guided-fuzzing-for-web-applications/).


We use a mutation based fuzzer to reach a high backend coverage by crawling and fuzzing the frontend.
The reasons could be:
1. finding all spots that are reachable by unauthenticated users
2. generating tests with a high application coverage
3. revealing edge and corner cases

## Instrument Webapplication

The setup is meant to be easy pluggable into existing software.

### PHP

In case of a PHP application, we instrument Xdebug to connect to Cruzzer. Cruzzer will trigger the debugger on every page and proceeds to step through the code.

As an example, we can integrate it to Docker easily using:
```
FROM php:7-apache

RUN pecl install xdebug \
    && docker-php-ext-enable xdebug \
    && echo "xdebug.remote_enable=1" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini \
    && echo "xdebug.remote_host=docker.for.mac.localhost" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini \
    && echo "xdebug.remote_autostart=on" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini \
    && echo "xdebug.mode=trace,debug,coverage" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini \
    && echo "xdebug.start_with_request=trigger" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini \
    && echo "xdebug.log=/tmp/xdebug.log" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini \
    && echo "xdebug.client_host=docker.for.mac.localhost" >> /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini
```

The changes to the config file can also be done manually. Mind that the remote_host should point to Cruzzer.

### Java

In case of a Java Web-Application we make use of the Jacoco JVM agent. We can attach Jacoco
 to any JVM instance. Cruzzer connects to Jacoco and tracks the reached branches during each page visit.

A Dockerfile can be easily extended to attach Jacoco to the JVM via environment variables:
```
FROM tomcat:8.5.55-jdk8-openjdk

COPY jacocoagent.jar .
ENV JAVA_OPTS="-javaagent:/usr/local/tomcat/jacocoagent.jar=output=tcpserver,address=*,jmx=true,dumponexit=false"                                                                                                     
```

Similarly, we can attach Jacoco by adding it to the Maven pom.xml. You can get the latest Jacoco version from [here](https://www.eclemma.org/jacoco/).
E.g. in the Cargo Configuration:

```
<cargo.start.jvmargs>
    -javaagent:${basedir}/jacocoagent.jar=output=tcpserver,address=*,jmx=true,dumponexit=false
    ${tomcat.jvmargs}
    ${tomcat.jvmargs.debug}
</cargo.start.jvmargs>
```

## Command Args
```
-t --startpage  <URL>  Startpage of the Webapplication.
-l --language  <LANGUAGE>  Language of the Web Backend (PHP or Java).
-i --fuzz <Number> Number of fuzzing iterations.
-f --filter <Filter> Filter Classes or Files (optional).
-a --hostname <HOSTNAME> Hostname of the Debugger if Jacoco is used.
-c --classes <Path> Compiled War/Jar to recover Jacoco Traces.
-p --port <PORT> Port of the Debugger.
-s --seeds <Path> Path to the seeds. Every seed should be new line seperated
-m --matches <Path> A Textfile with new line seperated Name:Regex tuples. Will be matched against HTTP Response Body.
-h --help Show this help.
```



## Usage

E.g.

```
mvn package
java -jar target/cruzzer-0.0.1-SNAPSHOT.jar -t http://localhost:8081/calc.php -l PHP -i 10000 -p 9003 -s seeds.txt
```


## How does it work?

1. The application is crawled
2. All possible HTML-Formulars are stored
3. For each field in a Formular an appropriate Fuzzing Strategy is selected
   1. E.g. if the field is a text field, a seed is mutated and sent to the server
   2. E.g. if the field is a select field, a random option is selected
   3. E.g. if the field is a checkbox, the checkbox is checked per chance
   4. E.g. if the field is hidden, the default value will be selected
4. If a new coverage is reached, the seed is prioritized
5. If a new coverage is reached, the formular is prioritized

## Example Output:

*report.txt* will be generated. In case of a Java application each reached class and the reached probes are documented.
And in case of a PHP application each reached file and reached line.
E.g.
In case of a Java Backend:
```
Coverage: 0,606864 , Crashes: 1 
org/cysecurity/cspf/jvl/controller/XPathQuery 01111111111111110000111101010
org/cysecurity/cspf/jvl/controller/Install 0111111111111111111111111111110011101011000000000000000000000000000000000000000001111111010
org/cysecurity/cspf/jvl/controller/Register 01100000000000000000000000011010
org/cysecurity/cspf/jvl/controller/ForwardMe 0111111011100
org/cysecurity/cspf/jvl/model/HashMe 01111111101
org/cysecurity/cspf/jvl/model/DBConnect 111111100
...
```
If --classes is provided to we can decode the probe information:
E.g.
```
Coverage: 0,310963 , Crashes: 24 
org/hibernate/type/ImageType,44
org/hibernate/annotations/common/reflection/java/generics/TypeEnvironmentFactory,38
org/hibernate/type/CharArrayType,42,47
org/hibernate/type/TimeType,49,54
org/hibernate/cfg/Configuration$2,2310
org/hibernate/cfg/Configuration$1,1764
org/hibernate/service/internal/AbstractServiceRegistryImpl,63,68,79,80,83,84,88,89,109,114,115,117,119,124,125,129,130,134,138,139,140,141,142,145,150,151,156,170,171,177,180,181,193,195,197,198,200,204,205,206,207,215,216
org/hibernate/internal/util/xml/ErrorLogger,40,52
org/hibernate/persister/internal/PersisterFactoryInitiator,44
org/hibernate/type/DateType,48,53
org/hibernate/mapping/Property,47,52,53,54,55,56,89,93,101,137,138,141,142,145,146,151,152,173,174,177,178,181,182,189,190,212,213,216,224,225,257,258,273,274,289,290
org/hibernate/type/BinaryType,45,54
...
```
If the application is a PHP application, the coverage is calculated per file instead of per class:
```
Coverage: 0,166667 , Crashes: 0
file:///var/www/html/calc.php [28, 29, 30, 31, 32, 34, 35, 37, 38, 40, 41, 43, 44, 46, 47, 49, 50, 53, 57]
```

Furthermore *reproduction.txt* will be generated:
```
GET http://localhost:8080/JavaVulnerableLab/vulnerability/xss/search.jsp: 200 {action=Search, keyword=hello world}
_______________
GET http://localhost:8080/JavaVulnerableLab/vulnerability/xss/search.jsp: 200 {action=Search, keyword=hello world}
_______________
POST http://localhost:8080//JavaVulnerableLab/XPathQuery.do: 200 {password=, Login=Login, username=hello world}
_______________
POST http://localhost:8080//JavaVulnerableLab/XPathQuery.do: 200 {password=1234567890, Login=Login, username=' OR 1=1 --}
_______________
GET http://localhost:8080/JavaVulnerableLab/vulnerability/xss/xss4.jsp: 200 {Search=Search, keyword=}
_______________
POST http://localhost:8080/JavaVulnerableLab/Install: 200 {dbpass=hello world, dbname=, adminuser=, siteTitle=hello world, dbuser=, setup=1, adminpass=, Install=Install, dburl=hello world, jdbcdriver=hello world}
_______________
POST http://localhost:8080/JavaVulnerableLab/ForgotPassword.jsp: 500 {secret=hello world, GetPassword=GetPassword, username=hello world}
_______________
POST http://localhost:8080/JavaVulnerableLab/AddUser: 200 {password=hello world, Register=Register, secret=hello world, email=, username=hello world, About=hello world}
_______________
POST http://localhost:8080/JavaVulnerableLab/Install: 200 {dbpass=hllo hwYrld, dbname=N241+4551, adminuser=<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\" >]><foo>&xxe;</foo>, siteTitle=<?xml version=\"1.0\" encoding=\"UTF-812450?><!DOCTYPE foo [<!ENTITY xxe YSTEM \"file:///getc15passwd\" >]58<fo>&xxe;</fo>, dbuser=, setup=1, adminpass=12=345675q80, Install=Install, dburl=<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\" >]><foo>&xxe;</foo>, jdbcdriver=com.mysql.jdbc.Driver}
_______________

```

## known flaws
- radio button not yet implemented
- no CookieStore clearing strategy yet
- some hyperparameters than are not yet configurable

## How to obtain support

[Create an issue](https://github.com/SAP-samples/security-research-cruzzer/issues) in this repository if you find a bug or have questions about the content.

For additional support, [ask a question in SAP Community](https://answers.sap.com/questions/ask.html).

## Contributing
If you wish to contribute code, offer fixes or improvements, please send a pull request. Due to legal reasons, contributors will be asked to accept a DCO when they create the first pull request to this project. This happens in an automated fashion during the submission process. SAP uses [the standard DCO text of the Linux Foundation](https://developercertificate.org/).

## License
Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved. This project is licensed under the Apache Software License, version 2.0 except as noted otherwise in the [LICENSE](LICENSE) file.

