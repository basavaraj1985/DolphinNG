# Dolphin
Test automation framework with smart reporting, html logging, jira integration.

# How to use:

-	Maven dependency:
	```
	<dependency>
		<groupId>com.javasbar.framework</groupId>
		<artifactId>DolphinNG</artifactId>
		<version>1.0.0-SNAPSHOT</version>
	</dependency>
	...

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
	Please note that if you are using JIRA auto bug create on test failure feature, the order of listeners in testng.xml matters. Put the SmartReporter at the end.

-	If you want to tweak the report format, you could do so. Take [this template](https://github.intuit.com/idea/Dolphin/blob/develop/resources/report.vm "Template") and modify as per your needs
	and then provide the path to the template as
	```
		-Dsmart.report.template=path/to/your/templatefile/template.vm
	```
-	To enable JIRA bug linking
	```)
		-Dbuglink=true
		-Dbuglink.config=jiraclient.properties
	```
	Providing buglink.config is optional, default properties will be taken if not provided. If you want to change the jira connection related settings,
	[use this file](https://github.intuit.com/idea/Dolphin/blob/develop/resources/jiraclient.properties).

-	If you want to create bugs automatically for test failures, use ```bug.auto.create=true``` either in jiraclient.properties or runtime property setting.
    Your JIRA project might have different mandatory fields, custom fields. It is important to make sure that the bug creation request has proper body.
    If default does not work, then please take [this file](https://github.intuit.com/idea/Dolphin/blob/develop/resources/jiracreateTemplate.vm) and edit
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
    projectKey=IDFKABINI
    severity=10314
    impact=Medium
    version=KABINI_0.53.0_SNAPSHOT
    environment=Production

    ```
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

- Default data provider available with parametrizable filenames; i.e. could be driven from configuration.
  
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
            <groups>
      	        <run>
		    <include name="smoke" />
   		    <include name="functional" />
	  	    <exclude name="todo" />
		    <exclude name="notForDesktop" />
      	        </run>        
    	    </groups>
    	    <classes>
    		<class name="sampleTests.GoogleSearchTest"></class>
	    </classes>
     </test>
	
     <test verbose="5" name="GoogleSRPMobile" annotations="JDK" parallel="classes" thread-count="6" >
		<parameter name="baseUrl" value="http://google.com"/>
		<parameter name="locatorFile" value="resources/configuration/mobileElementLocators.xml" />
		<parameter name="userAgent" value="Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3"/>
		<parameter name="browserWidth" value="390"/>
		<parameter name="browserHeight" value="780" />
	        <parameter name="browser" value="ff"/>		
		<groups>
                    <run>
                        <include name="smoke" />
                        <include name="functional" />
                        <exclude name="todo" />
                        <exclude name="notForMobile" />
                    </run>        
		</groups>
		<classes>
		    <class name="sampleTests.GoogleSearchTest"></class>
		</classes>
     </test>
	
	<listeners>
		<listener class-name="com.javasbar.framework.testng.reporters.SmartReporter" />
		<listener class-name="com.javasbar.framework.testng.reporters.EmailableReporter" />
		<listener class-name="com.javasbar.framework.testng.reporters.ProgressReporter" />
	</listeners> 
</suite>
```
  And much more.. Mail me on basavaraj1985@gmail.com, if you need any help using this framework.
