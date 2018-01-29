[![Build Status](https://travis-ci.org/basavaraj1985/DolphinNG.svg?branch=master)](https://travis-ci.org/basavaraj1985/DolphinNG)[![GitHub release](https://img.shields.io/badge/DolphinNG-Releases-blue.svg)](https://github.com/basavaraj1985/DolphinNG/releases)[![Maven Central](https://img.shields.io/badge/Maven%20Central-Releases-green.svg)](https://mvnrepository.com/artifact/com.github.basavaraj1985/DolphinNG)

# DolphinNG
##### TestNG add on for smarter, simpler reports and analytics!
The TestNG listeners, reporters with smart reporting, progress reporter, jira integration. This can be thought as TestNG++,
library with advanced features as failure based reporting, red delta.
-	Red delta : new failures, compared to previous run. These failures are separated and shown as Red delta in the smart report. These fails indicate regression failure.
- 	Green delta: new passes, compared to previous run. These are separated and shown as Green delta in the smart report.
These results indicate a regression pass (usually because a fix made).
- 	Stagnant failures: The test fails that failed in the previous run also.
-	Failure Based Report (FBR): test fails with same/similar fail reasons are clubbed together and reported with test fail count.
-	JIRA integration: The report will have a ticket created/queried from JIRA for every failure. The FBR will have the mapping between failure reason to jira ticket.

# How to use:

-	Maven dependency:
	```
	<dependency>
		<groupId>com.github.basavaraj1985</groupId>
		<artifactId>DolphinNG</artifactId>
		<version>1.1.0</version>
	</dependency>
	```

-	Include Reporters in your testng:
	```
	<listeners>
	    <listener class-name="org.testng.reporters.EmailableReporter" />
	    <listener class-name="com.javasbar.framework.testng.reporters.TimeoutRetryAnalyzer" />
		<listener class-name="com.javasbar.framework.testng.reporters.ProgressReporter" />
		<listener class-name="com.javasbar.framework.testng.reporters.RuleBasedStatusModifier" />
		<listener class-name="com.javasbar.framework.testng.reporters.SmartReporter" />
	</listeners>
	```


-	If you want to tweak the report format, you could do so. Take [__**this template**__](https://github.com/basavaraj1985/DolphinNG/blob/master/resources/report.vm "Template") and modify as per your needs
	and then provide the path to the template as
	```
		-Dsmart.report.template=path/to/your/templatefile/template.vm
	```
-	To enable JIRA bug linking
	```
		-Dbuglink=true
		-Dbuglink.config=jiraclient.properties
	```
	Providing buglink.config is optional, default properties will be taken if not provided. Configuration file to be provided with values,
	[__**use this file**__](https://github.com/basavaraj1985/DolphinNG/blob/master/resources/jiraclient.properties).

-	If you want to create bugs automatically for test failures, use
    ```
    bug.auto.create=true
    ```
    either in jiraclient.properties or runtime property setting.
    Your JIRA project might have different mandatory fields, custom fields. It is important to make sure that the bug creation request has proper body.
    If default does not work, then please take [this file](https://github.com/basavaraj1985/DolphinNG/blob/master/resources/jiracreateTemplate.vm) and edit
    according to your needs. And then provide this filepath as system property:
	```
	-Dbug.create.body=resources/jiracreateTemplate.vm
	```
	OR
	in the jiraclient.properties file itself.
- 	Sample configuration for auto creation of bugs on test failures:
    ```
    #For auto creation of bugs
    bug.auto.create=false
    bug.create.body=resources/jiracreateTemplate.vm
    bug.auto.attach.if.found=true
    bug.attachments=Reports/report.html,target/surefire-reports/emailable-report.html,logs/att.log
    #Prefix in the summary of the bug auto created
    summary.prefix=[Automation]
    projectKey=JIRAPROJECTKEY
    severity=10314
    impact=Medium
    version=PROJECT_VERSION_IN_JIRA
    environment=Production

    ```
-   Complete configuration file with keys, [config file](https://github.com/basavaraj1985/DolphinNG/blob/master/resources/jiraclient.properties).
-	To link log file to the failures
	```
		-DlogLink=path/to/logfile
	```
	If a method m1 is failed, the report will have link to specific line in log (only if its html logging). 
	i.e. the linke would be to ${BUILD_URL}/artifacts/Reports/testLog.html#m1.

-	More configuration:
	To provide all of the above configuration and more you can put all of them in a config file. To provide such config file -
	```
		-DconfigFile=path/to/config.properties
	```	
	The priority of configuration loading is:
		1. System level - runtime arguments
		2. flat file - properties file
		3. testng.xml 


#Features: 

- **Smart Reporting** : Smart reporting enabled - lot lesser time in automation report analysis. Delta and failure based reports are provided.

- **Smart Reporting history** :  Smart reporting with history trend is provided. You can see red delta trend over number of runs. 

- **EnvAwareRetryAnalyser** : if env under test goes down, your _tests will wait upto configurable time before failing_.

- **Sample testng configuration**
  ```
  <suite name="DolphinTests" parallel="tests" thread-count="10" >
	<parameter name="configFile" value="resources/configuration/config.properties" />
	
	<test verbose="5" name="GoogleSRP" annotations="JDK" parallel="classes" thread-count="6" >
		<parameter name="baseUrl" value="http://google.com"/>  
		<parameter name="locatorFile" value="resources/configuration/elementLocators.xml" />
		<!-- supported browsers: chrome, ff, remote,, safari(no UA), -->
		<parameter name="browser" value="phantom"/>		
    	    <classes>
    		<class name="sampleTests.GoogleSearchTest"></class>
	    </classes>
     </test>
	
	<listeners>
		<!-- This is Smart report generator. It also generates JIRA tickets if enabled. And it generates the execution history too. -->
		<listener class-name="com.javasbar.framework.testng.reporters.SmartReporter" />

		<!-- This enables you see the test progress immediately. You could start analysing the report as soon as your tests start executing! -->
		<listener class-name="com.javasbar.framework.testng.reporters.ProgressReporter" />

		<!-- Using the below listener, you could make tests retry if dependent system is down. Set these properties in test context, or at runtime:
		        baseUrl, retryWaitSeconds, maxRetryCount -->
		<listener class-name="com.javasbar.framework.testng.reporters.EnvAwareRetryAnalyzer" />

		<!-- Using the below listener you can convert known failues into skips by providing whitelist -DskipFor=known fails~anotherFailure-->
		<listener class-name="com.javasbar.framework.testng.reporters.RuleBasedStatusModifier" />s
	</listeners>
</suite>
```
  And much more.. Mail me on basavaraj1985@gmail.com, if you need any help using this framework.
