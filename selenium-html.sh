#!/bin/sh
CLASSPATH=lib/selenium-html-client-driver-0.2.jar:lib/selenium-java-client-driver-1.0.1.jar:lib/nekohtml-1.9.8.jar:lib/xercesImpl-2.8.1.jar:lib/xml-apis-1.0.b2.jar
java -cp $CLASSPATH com.snowmochi.selenium.SeleniumHtmlClient $@

