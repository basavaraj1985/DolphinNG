package com.javasbar.framework.testng.reporters;

import com.javasbar.framework.lib.common.Bug;
import com.javasbar.framework.lib.common.IOUtil;
import com.javasbar.framework.lib.common.JIRAClient;
import com.javasbar.framework.lib.common.StringUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.testng.*;
import org.testng.xml.XmlSuite;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @author Basavaraj M
 */
public class SmartReporter implements IReporter
{
    private static final Logger LOG = LogManager.getLogger(SmartReporter.class);

    private static final String SMART_REPORT_TEMPLATE_FILE = "smart.report.template";
    /**
     * Number of words in fail reason to be considered for bug search/creation; default is 10.
     */
    public static final String BUG_WORD_COUNT = "bug.word.count";
    public static String LOG_LINK = "logLink";
    public static String BUG_LINK_KEY = "buglink"; // buglink=true for enabling jira linking
    public static String BUG_LINK_CONFIG = "buglink.config"; // buglink.config=config/jirclient.properties

    private static String INTL_KEY = "intl";
    private static String fileName = "briefSummary.properties";
    private static String persistedHistory = "history.csv";
    private static String prepend = "";
    private static String intl = "";
    private static String PREV_STATIC_REPORT_KEY = "prev";
    private static File previous = null;

    private String logLinkFile;
    private Boolean buglink;
    private String buglinkConfig;
    private JIRAClient jira;

    public SmartReporter()
    {
        super();
        LOG.info("DolphinNG: SmartReporter() constructor");
        System.out.println("~~~~~~~> DolphinNG: SmartReporter() constructor, your test suite is super powered! <~~~~~~~~~");
        String prevReport = System.getProperty(PREV_STATIC_REPORT_KEY, null);
        logLinkFile = System.getProperty(LOG_LINK);
        buglink = Boolean.parseBoolean(System.getProperty("buglink", "false"));
        buglinkConfig = System.getProperty("buglink.config", "");
        if (prevReport != null)
        {
            File prevReportFile = new File(prevReport);
            if (prevReportFile.exists())
            {
                LOG.info("SUCCESS: Threshold failures considered!");
                previous = prevReportFile;
            } else
            {
                System.err.println("ERROR: Could not locate file : " + prevReportFile.getAbsolutePath());
            }
        }

        if (logLinkFile != null)
        {
            LOG.info("SUCCESS : Log deep linking to smart report    considered!");
            try
            {
                URL url = new URL(logLinkFile);
                url.openConnection();
                LOG.info("SUCCESS : Log link verified to be valid!");
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        if (buglink)
        {
            LOG.info("DolphinNG: Jira bug linking enabled!");
            jira = new JIRAClient(buglinkConfig);
        }
    }

    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
                               String testNGoutputDirectory)
    {
        String outputDirectory = "./Reports/MetaData/";
        String report = outputDirectory + "/" + fileName;
        String finalReport = outputDirectory + "/../report.html";
        File dir = new File(outputDirectory);
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        StringBuffer buf = new StringBuffer();
        buf.append("#Smart reporting ....").append("\n");
        buf.append("#outputDir=").append(outputDirectory).append("\n");

        intl = System.getProperty(INTL_KEY);
        buf.append("#What did I get from System.getProperty(\"intl\") : " + intl);
        if (intl != null && intl.trim().length() > 1)
        {
            fileName = intl + "_" + fileName;
            prepend = intl + "_";
        } else
        {
            buf.append("#No intl could be retrieved for intl key 'intl' from  env").append("\n");
        }
        buf.append("##intl = " + intl).append("\n");
        deriveAndCompute(suites, buf, outputDirectory);
        buf.append("#Writing file to: " + outputDirectory + "/" + fileName);
        writeToFile(report, buf.toString());

        VelocityEngine ve = new VelocityEngine();
        try
        {
            Properties resourceLoadingConfigProps = new Properties();
            resourceLoadingConfigProps.load(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("velocity.properties"));
            String reportTemplateUserConfigd = System.getProperty(SMART_REPORT_TEMPLATE_FILE);
            String resourceName = null;
            String templateValue = null;
            if (reportTemplateUserConfigd != null && reportTemplateUserConfigd.trim().length() > 0)
            {
                String resourcePath = reportTemplateUserConfigd.substring(0, reportTemplateUserConfigd.lastIndexOf("/"));
                resourceName = reportTemplateUserConfigd.substring(reportTemplateUserConfigd.lastIndexOf("/") + 1,
                        reportTemplateUserConfigd.length());
                resourceLoadingConfigProps.put("file.resource.loader.path", resourcePath);
                templateValue = resourceName;
            } else
            {
                templateValue = "report.vm";
            }

            ve.init(resourceLoadingConfigProps);
            LOG.info("Using template : " + templateValue);
            Template template = ve.getTemplate(templateValue);
            LOG.info("DolphinNG: using template - " + template.getName());
            VelocityContext context = new VelocityContext();
            Properties reportedProps = new Properties();
            reportedProps.load(new FileInputStream(report));
            Iterator<Object> iterator = reportedProps.keySet().iterator();
            while (iterator.hasNext())
            {
                String key = (String) iterator.next();
                context.put(key, reportedProps.get(key));
            }
            Iterator<Object> sysIterator = System.getProperties().keySet().iterator();
            while (sysIterator.hasNext())
            {
                String key = (String) sysIterator.next();
                context.put(key, System.getProperty(key));
            }
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            buf.append("#Writing file to: " + finalReport);
            writeToFile(finalReport, writer.toString());
        } catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("DolphinNG: Velocity exception while generating report from template!");
            LOG.warn(e);
        }
        LOG.info("====================================================================================");
        LOG.info("Smart Report: " + finalReport);
        LOG.info("Emailable default testng report: target/surefire-reports/emailable-report.html");
        LOG.info("====================================================================================");
    }

    private void deriveAndCompute(List<ISuite> suites, StringBuffer buf, String outputDirectory)
    {
        List<String> historyLines = initializeHistory(outputDirectory);
        int prevBuildNumber = getPrevBuildNumber(historyLines);
        if (null != intl)
        {
            persistedHistory = intl + "_" + persistedHistory;
        }
        int passed = 0, totalPassedMethods = 0, failed = 0, totalFailedMethods = 0,
                skipped = 0, totalSkippedMethods = 0, total = 0, totalNumberOfMethods = 0;
        StringBuffer includedGroupsBuf = new StringBuffer();
        StringBuffer excludedGroupsBuf = new StringBuffer();
        StringBuffer failedMethodBuff = new StringBuffer();
        StringBuffer passedMethodBuff = new StringBuffer();
        List<String> currentFailList = new ArrayList<String>();
        List<String> currentPassList = new ArrayList<String>();
        Map<String, String> failReasonBasedGrouping = new HashMap<String, String>();
        Map<String, String> failReasonBasedGroupingNonHTML = new HashMap<String, String>();
        //failReason, commaSepdCases

        for (ISuite su : suites)
        {
            String suiteName = su.getName();
            buf.append("testsuiteName=" + suiteName).append("\n");

            for (ISuiteResult sr : su.getResults().values())
            {
                ITestContext testContext = sr.getTestContext();
                String intlFromCtxt = (String) testContext.getAttribute("intl");
                buf.append("#intl from context : " + intlFromCtxt).append("\n");
                if (prepend.length() < 1 && intlFromCtxt != null && intlFromCtxt.length() > 0)
                {
                    prepend = intlFromCtxt + "_";
                }
                passed += testContext.getPassedTests().size();
                failed += testContext.getFailedTests().size();
                skipped += testContext.getSkippedTests().size();

                String[] includedGroups = testContext.getIncludedGroups();
                String[] excludedGroups = testContext.getExcludedGroups();
                for (String s : includedGroups)
                {
                    includedGroupsBuf.append(s).append(",");
                }
                for (String s : excludedGroups)
                {
                    excludedGroupsBuf.append(s).append(",");
                }

                buf.append("IncludedGroups =" + includedGroupsBuf.toString()).append("\n");
                buf.append("ExcludedGroups =" + excludedGroupsBuf.toString()).append("\n\n");

                ITestNGMethod[] allTestMethods = testContext.getAllTestMethods();
                totalNumberOfMethods += allTestMethods.length;

                IResultMap passedTests = testContext.getPassedTests();
                int passedMethods = getMethodSet(passedTests).size();
                totalPassedMethods += passedMethods;
                Set<ITestResult> allPassedResults = passedTests.getAllResults();
                Iterator<ITestResult> ptestIterator = allPassedResults.iterator();
                while (ptestIterator.hasNext())
                {
                    ITestResult next = ptestIterator.next();
                    String className = next.getMethod().getRealClass().getName();
                    String testCaseName = next.getName();
//				    int invocationCount = next.getMethod().getInvocationCount();
                    Object[] parameters = next.getParameters();
                    StringBuffer paraBuff = new StringBuffer();
                    getParametersAsString(parameters, paraBuff);
                    String classNMethod = className + "." + testCaseName;
                    if (passedMethodBuff.indexOf(classNMethod) == -1)
                    {
                        passedMethodBuff.append(classNMethod).append("-->").append(paraBuff).append(",");
                        currentPassList.add(className + "." + testCaseName + "->" + paraBuff);
                        passedMethodBuff.append("<br/>");
                    }
                }

                int failedMethods = getMethodSet(testContext.getFailedTests()).size();
                totalFailedMethods += failedMethods;

                IResultMap failedTests = testContext.getFailedTests();
                Set<ITestResult> allFailedResults = failedTests.getAllResults();
                Iterator<ITestResult> ftestIterator = allFailedResults.iterator();
                while (ftestIterator.hasNext())
                {
                    ITestResult next = ftestIterator.next();
                    String className = next.getMethod().getRealClass().getName();
                    String testCaseName = next.getName();
//				    int invocationCount = next.getMethod().getInvocationCount();
                    Object[] parameters = next.getParameters();
                    StringBuffer paraBuff = new StringBuffer();
                    getParametersAsString(parameters, paraBuff);

                    String classNMethod = className + "." + testCaseName;
                    if (failedMethodBuff.indexOf(classNMethod) == -1)
                    {
                        failedMethodBuff.append(classNMethod).append("-->").append(paraBuff).append(",");
                        currentFailList.add(className + "." + testCaseName + "->" + paraBuff);
                        failedMethodBuff.append("<br/>");
                    }

                    String message = next.getThrowable().getMessage();
                    if (null != message)
                    {
                        message = message.replaceAll("\n", " ").replaceAll("\\n", " ")
                                .replaceAll("\r", " ").replaceAll("\\r", " ")
                                .replaceAll("\\[.+?\\]", "#");
                    }
                    String toBePut = "<font size=\"=1\">" + className + "</font>" +
                            "<b>." + testCaseName + "</b>" + "|" + paraBuff.toString();
                    String toBePutNonHTML = "|" + className + "." + testCaseName + "|" + paraBuff.toString();
                    if (null != failReasonBasedGrouping.get(message))
                    {
                        String existingListOfFails = failReasonBasedGrouping.get(message);
                        String existingListOfFailsNonHTML = failReasonBasedGroupingNonHTML.get(message);
                        String listOfFailsUpdated = null, listOfFailsUpdatedNonHTML = null;
                        if (existingListOfFails.contains(toBePut))
                        {
                            listOfFailsUpdated = existingListOfFails.replace(toBePut, toBePut + "+");
                            listOfFailsUpdatedNonHTML = existingListOfFailsNonHTML.replace(toBePutNonHTML, toBePutNonHTML + "+");
                        } else
                        {
                            listOfFailsUpdated = existingListOfFails + ",," + toBePut;
                            listOfFailsUpdatedNonHTML = existingListOfFailsNonHTML + ",," + "\n" + toBePutNonHTML;
                        }
                        failReasonBasedGrouping.put(message, listOfFailsUpdated);
                        failReasonBasedGroupingNonHTML.put(message, listOfFailsUpdatedNonHTML);
                    } else
                    {
                        failReasonBasedGrouping.put(message, toBePut);
                        failReasonBasedGroupingNonHTML.put(message, toBePutNonHTML);
                    }
                }

                int skippedMethods = getMethodSet(testContext.getSkippedTests()).size();
                totalSkippedMethods += skippedMethods;
            }
        }
        total = passed + failed + skipped;

        buf.append("passed=" + passed + "\n");
        buf.append("failed=" + failed + "\n");
        buf.append("skipped=" + skipped + "\n");
        buf.append("total=" + total + "\n\n");

        buf.append("passedMethods=" + totalPassedMethods + "\n");
        buf.append("failedMethods=" + totalFailedMethods + "\n");
        buf.append("skippedMethods=" + totalSkippedMethods + "\n");
        buf.append("totalMethodCount=" + totalNumberOfMethods + "\n");

        buf.append("FailedMethodDetails =" + failedMethodBuff.toString()).append("\n");

        float overallFailPercentage = 0, methodFailPercentage = 0;
        if (0 == total)
        {
            total = 1;  // to avoid arithmetic /0 error
        }
        if (0 == totalNumberOfMethods)
        {
            totalNumberOfMethods = 1;
        }
        overallFailPercentage = ((failed * 100) / total);
        methodFailPercentage = (totalFailedMethods * 100) / totalNumberOfMethods;

        buf.append("OverallFailurePercentage =" + overallFailPercentage).append("\n");
        buf.append("MethodFailurePercentage =" + methodFailPercentage).append("\n\n");


        if (null == previous)
        {
            buf.append("#Threshold failures NOT taken into account - prev=" +
                    System.getProperty(PREV_STATIC_REPORT_KEY, null)).append("\n");
            previous = new File(outputDirectory + "/" + fileName);
        }
        else
        {
            buf.append("#Threshold failures taken into account - prev=" +
                    System.getProperty(PREV_STATIC_REPORT_KEY, null)).append("\n");
        }

        buf.append("#Previous report expected : " + previous.getAbsolutePath() +
                " Exists: " + previous.getAbsoluteFile().exists()).append("\n");
        if (!previous.getAbsoluteFile().exists())
        {
            buf.append("buildNumber=1").append("\n");
            buf.append("#No previous report!").append("\n");
//            return;
        }

        Properties previousProperties = IOUtil.loadFileIntoProperties(previous.getAbsolutePath());
        if (prevBuildNumber == 0)
        {
            prevBuildNumber = Integer.valueOf(previousProperties.getProperty("buildNumber", "0").trim()).intValue();
        }
        buf.append("buildNumber=" + (prevBuildNumber + 1)).append("\n");

        int totalCountDelta = 0, methodCountDelta = 0;
        String pTotalCount = previousProperties.getProperty("total", "0");
        totalCountDelta = total - Integer.valueOf(pTotalCount);

        String pMethodTotalCount = previousProperties.getProperty("totalMethodCount", "0");
        methodCountDelta = totalNumberOfMethods - Integer.valueOf(pMethodTotalCount);

        buf.append("TotalCountDelta=" + totalCountDelta).append("\n");
        buf.append("TotalMethodCountDelta=" + methodCountDelta).append("\n");

        int redDeltaCount = 0, greenDeltaCount = 0;
        //1. Calculate Red Delta count
        String pFailCount = previousProperties.getProperty("failed", "0");
        redDeltaCount = failed - Integer.valueOf(pFailCount);
        if (redDeltaCount < 0)
        {
            greenDeltaCount = Math.abs(redDeltaCount);
            redDeltaCount = 0;
        }

        //2. Calculate Green Delta count
        String pPassCount = previousProperties.getProperty("passed", "0");
        greenDeltaCount = passed - Integer.valueOf(pPassCount);
        if (greenDeltaCount < 0)
        {
//			rDeltaCount = rDeltaCount + Math.abs(gDeltaCount);
            greenDeltaCount = 0;
        }


        //3. Calculate Red Delta method count
        int redMethodDeltaCount = 0, greenMethodDeltaCount = 0;
        String pFailMethodCount = previousProperties.getProperty("failedMethods", "0");
//		redMethodDeltaCount = totalFailedMethods - Integer.valueOf(pFailMethodCount);
//		if ( redMethodDeltaCount < 0 )
//		{
//			greenMethodDeltaCount = Math.abs(redDeltaCount);
//			redMethodDeltaCount = 0;
//		}

        //4. Calculate Green Delta method count
        String pPassMethodCount = previousProperties.getProperty("passedMethods", "0");
        greenMethodDeltaCount = totalPassedMethods - Integer.valueOf(pPassMethodCount);
        if (greenMethodDeltaCount < 0)
        {
            greenMethodDeltaCount = 0;
        }

        //5. Derive Red Delta methods
        StringBuffer redDeltaMethodList = new StringBuffer();
        redDeltaMethodList.append("<table border=\"2\" cellspacing=\"0\" cellpadding=\"2\" class=\"param\" width=\"100%\">");
        if (buglink)
        {
            redDeltaMethodList.append("<tr> <th bgcolor=\"red\" colspan=\"6\">Red Delta [PASS-to-FAIL]  Count : " +
                    "$redMethodDeltaCount" + " </th></tr>");
        } else
        {
            redDeltaMethodList.append("<tr> <th bgcolor=\"red\" colspan=\"2\">Red Delta [PASS-to-FAIL]  Count : " +
                    "$redMethodDeltaCount" + " </th></tr>");
        }
        redDeltaMethodList.append("<tr bgcolor=\"red\">");
        redDeltaMethodList.append("<th>Test Case</th>");
        redDeltaMethodList.append("<th>Parameters</th>");
        if (buglink)
        {
            redDeltaMethodList.append("<th>Bug</th>");
            redDeltaMethodList.append("<th>BugStatus</th>");
            redDeltaMethodList.append("<th>BugSeverity</th>");
            redDeltaMethodList.append("<th>BugPriority</th>");
        }
        redDeltaMethodList.append("</tr>");

        StringBuffer stagnantFailureList = new StringBuffer();
        stagnantFailureList.append("<table border=\"2\" cellspacing=\"0\" cellpadding=\"2\" class=\"param\" width=\"100%\">");
        if (buglink)
        {
            stagnantFailureList.append("<tr> <th bgcolor=\"red\" colspan=\"6\">Stagnant failures [FAIL-remained-FAIL]</th></tr>");
        } else
        {
            stagnantFailureList.append("<tr> <th bgcolor=\"red\" colspan=\"2\">Stagnant failures [FAIL-remained-FAIL]</th></tr>");
        }
        stagnantFailureList.append("<tr bgcolor=\"red\">");
        stagnantFailureList.append("<th>Test Case</th>");
        stagnantFailureList.append("<th>Parameters</th>");
        if (buglink)
        {
            stagnantFailureList.append("<th>Bug</th>");
            stagnantFailureList.append("<th>BugStatus</th>");
            stagnantFailureList.append("<th>BugSeverity</th>");
            stagnantFailureList.append("<th>BugPriority</th>");
        }
        stagnantFailureList.append("</tr>");

        StringBuffer failBasedReportingBuff = new StringBuffer();
        failBasedReportingBuff.append("<table border=\"2\" cellspacing=\"0\" cellpadding=\"2\" class=\"param\" width=\"100%\">");
        if (buglink)
        {
            failBasedReportingBuff.append("<tr> <th bgcolor=\"#F74040\" colspan=\"7\">Failure Based report Count :" +
                    failReasonBasedGrouping.size() + " </th></tr>");
        } else
        {
            failBasedReportingBuff.append("<tr> <th bgcolor=\"#F74040\" colspan=\"3\">Failure Based report Count :" +
                    failReasonBasedGrouping.size() + " </th></tr>");
        }
        failBasedReportingBuff.append("<tr bgcolor=\"#F45C4B\">");
        failBasedReportingBuff.append("<th>Fail Reason</th>");
        failBasedReportingBuff.append("<th>Fail Cases</th>");
        failBasedReportingBuff.append("<th>Fail Count</th>");
        if (buglink)
        {
            failBasedReportingBuff.append("<th>Bug</th>");
            failBasedReportingBuff.append("<th>BugStatus</th>");
            failBasedReportingBuff.append("<th>BugSeverity</th>");
            failBasedReportingBuff.append("<th>BugPriority</th>");
        }
        failBasedReportingBuff.append("</tr>");

        Set<String> keySet = failReasonBasedGrouping.keySet();
        Iterator<String> failBasedMapKeyIterator = keySet.iterator();
        int timeouts = 0;
        while (failBasedMapKeyIterator.hasNext())
        {
            String failReason = failBasedMapKeyIterator.next(); // is a failReason and a key
            String casesData = failReasonBasedGrouping.get(failReason);
            String casesDataNonHTML = failReasonBasedGroupingNonHTML.get(failReason);
            if (null != failReason)
            {
                failReason = failReason.replaceAll("\n", " ").replaceAll("\\n", " ")
                        .replaceAll("\r", " ").replaceAll("\\r", " ")
                        .replaceAll("\\[.+?\\]", "#");
            } else if (StringUtils.isBlank(failReason))
            {
                failReason = "No Fail Reason found!";
            }
            String[] failCaseList = casesData.split(",,");
            int sameTCFails = countSubstrings(casesData, "+");
            int numberOfFailsForThisReason = failCaseList.length + sameTCFails;

            failBasedReportingBuff.append("<tr>");
            failBasedReportingBuff.append("<td>").append(failReason).append("</td>");
            failBasedReportingBuff.append("<td>");
            String temp = "";
            for (String testCase : failCaseList)
            {
                // format : classNMethod + " | " + paraBuff.toString() ;
                String[] nameNParas = testCase.split("\\|");
                if (temp.contains(nameNParas[0]))
                {
                    failBasedReportingBuff.append("+");
                    continue;
                }
                temp = temp + nameNParas[0] + ",";
                failBasedReportingBuff.append(nameNParas[0]);
                failBasedReportingBuff.append("<br/>");
            }
            failBasedReportingBuff.append("</td>");
            failBasedReportingBuff.append("<td>").append(numberOfFailsForThisReason).append("</td>");

            if (buglink)
            {
                try
                {
                    String jiraKeyString = StringUtil.truncateAfterWords(Integer.parseInt(System.getProperty(BUG_WORD_COUNT, "25").trim()), failReason);
                    jiraKeyString = jiraKeyString.replaceAll("\t", "    ");
                    casesDataNonHTML = casesDataNonHTML.replaceAll("\t", "    ");
                    String jiraTicket = jira.createJiraTicket(jiraKeyString, casesDataNonHTML, null);
                    Bug bug = jira.getBugByBugId(jiraTicket);
                    if (null != bug)
                    {
                        failBasedReportingBuff.append("<td>").append("<a href=\"").append(jira.getBrowseURLForBug(bug
                                .getID())).append("\">").append(bug.getID()).append("</a>").append("</td>");
                        failBasedReportingBuff.append("<td>").append(bug.getStatus()).append("</td>");
                        failBasedReportingBuff.append("<td>").append(bug.getSeverity()).append("</td>");
                        failBasedReportingBuff.append("<td>").append(bug.getPriority()).append("</td>");
                    } else
                    {
                        failBasedReportingBuff.append("<td>").append("<a href=\"").append(jira.getBrowseURLForBug
                                ("NoBugFound")).append("\">").append("-").append("</a>").append("</td>");
                        failBasedReportingBuff.append("<td>").append("-").append("</td>");
                        failBasedReportingBuff.append("<td>").append("-").append("</td>");
                        failBasedReportingBuff.append("<td>").append("-").append("</td>");
                    }
                } catch (ClientProtocolException e)
                {
                    e.printStackTrace();
                    failBasedReportingBuff.append("<td>").append("CP_ERROR").append("</td>");
                    failBasedReportingBuff.append("<td>").append("CP_ERROR").append("</td>");
                    failBasedReportingBuff.append("<td>").append("CP_ERROR").append("</td>");
                    failBasedReportingBuff.append("<td>").append("CP_ERROR").append("</td>");
                } catch (IOException e)
                {
                    e.printStackTrace();
                    failBasedReportingBuff.append("<td>").append("IO_ERROR").append("</td>");
                    failBasedReportingBuff.append("<td>").append("IO_ERROR").append("</td>");
                    failBasedReportingBuff.append("<td>").append("IO_ERROR").append("</td>");
                    failBasedReportingBuff.append("<td>").append("IO_ERROR").append("</td>");
                    if (e.getMessage().toLowerCase().contains("timed out") && timeouts++ > 2)
                    {
                        break;
                    }
                }
            }
        }
        failBasedReportingBuff.append("</table>");

        String pFailedMethodList = previousProperties.getProperty("FailedMethodDetails", "nil");
        // convert to array[class.methodName.count]
        buf.append("PreviousFailedMethodDetails =" + pFailedMethodList).append("\n");

        String[] pFailsList = pFailedMethodList.split(",");
        List<String> previousFailsArrList = new ArrayList<String>();
        for (String f : pFailsList)
        {
            String pCurated = f.replaceAll("-->", "").replaceAll("<br/>", "");
            previousFailsArrList.add(pCurated);
        }

        for (String currentFail : currentFailList)
        {
            if (pFailedMethodList == null)
            {
                break;
            }

            String[] failParts = currentFail.split("->");
            String methodName = failParts[0].substring(failParts[0].lastIndexOf(".") + 1);
            if (!pFailedMethodList.contains(failParts[0]))
            {
                redDeltaMethodList.append("<tr>");
//				redDeltaMethodList.append(currentFail);
                if (null != logLinkFile)
                {
                    redDeltaMethodList.append("<td>").append("<a href=\"" + logLinkFile + "#" + methodName + "\" >")
                            .append(failParts[0]).append("</a>").append("</td>");
                } else
                {
                    redDeltaMethodList.append("<td>").append(failParts[0]).append("</td>");
                }
                if (failParts.length > 1)
                {
                    redDeltaMethodList.append("<td>").append(failParts[1]).append("</td>");
                } else
                {
                    redDeltaMethodList.append("<td>").append("-").append("</td>");
                }

                if (buglink)
                {
                    try
                    {
                        Bug bug = jira.getBugsBySearchQuery("description~" + methodName);
                        if (bug == null)
                        {
                            redDeltaMethodList.append("<td>").append("<a href=\"").append(jira.getBrowseURLForBug
                                    ("NoBugFound")).append("\">").append("-").append("</a>").append("</td>");
                            redDeltaMethodList.append("<td>").append("-").append("</td>");
                            redDeltaMethodList.append("<td>").append("-").append("</td>");
                            redDeltaMethodList.append("<td>").append("-").append("</td>");
                        } else
                        {
                            redDeltaMethodList.append("<td>").append("<a href=\"").append(jira.getBrowseURLForBug(bug
                                    .getID())).append("\">").append(bug.getID()).append("</a>").append("</td>");
                            redDeltaMethodList.append("<td>").append(bug.getStatus()).append("</td>");
                            redDeltaMethodList.append("<td>").append(bug.getSeverity()).append("</td>");
                            redDeltaMethodList.append("<td>").append(bug.getPriority()).append("</td>");
                        }
                    } catch (ClientProtocolException e)
                    {
                        e.printStackTrace();
                        redDeltaMethodList.append("<td>").append("CP_ERROR").append("</td>");
                        redDeltaMethodList.append("<td>").append("CP_ERROR").append("</td>");
                        redDeltaMethodList.append("<td>").append("CP_ERROR").append("</td>");
                        redDeltaMethodList.append("<td>").append("CP_ERROR").append("</td>");
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                        redDeltaMethodList.append("<td>").append("IO_ERROR").append("</td>");
                        redDeltaMethodList.append("<td>").append("IO_ERROR").append("</td>");
                        redDeltaMethodList.append("<td>").append("IO_ERROR").append("</td>");
                        redDeltaMethodList.append("<td>").append("IO_ERROR").append("</td>");
                        if (e.getMessage().toLowerCase().contains("timed out") && timeouts++ > 2)
                        {
                            break;
                        }
                    }
                }
                redDeltaMethodList.append("</tr>");
                redMethodDeltaCount++;
            } else
            {
                stagnantFailureList.append("<tr>");
                if (null != logLinkFile)
                {
                    stagnantFailureList.append("<td>").append("<a href=\"" + logLinkFile + "#" + methodName + "\" >")
                            .append(failParts[0]).append("</a>").append("</td>");
                } else
                {
                    stagnantFailureList.append("<td>").append(failParts[0]).append("</td>");
                }
                if (failParts.length > 1)
                {
                    stagnantFailureList.append("<td>").append(failParts[1]).append("</td>");
                } else
                {
                    stagnantFailureList.append("<td>").append("-").append("</td>");
                }
                if (buglink)
                {
                    try
                    {
                        Bug bug = jira.getBugsBySearchQuery("description~" + methodName);
                        if (bug == null)
                        {
                            bug = new Bug();
                            stagnantFailureList.append("<td>").append("<a href=\"").append(jira.getBrowseURLForBug
                                    ("NoBugFound")).append("\">").append("-").append("</a>").append("</td>");
                            stagnantFailureList.append("<td>").append("-").append("</td>");
                            stagnantFailureList.append("<td>").append("-").append("</td>");
                            stagnantFailureList.append("<td>").append("-").append("</td>");
                        } else
                        {
                            stagnantFailureList.append("<td>").append("<a href=\"").append(
                                    jira.getBrowseURLForBug(bug.getID())).append("\">").append(bug.getID())
                                    .append("</a>").append("</td>");
                            stagnantFailureList.append("<td>").append(bug.getStatus()).append("</td>");
                            stagnantFailureList.append("<td>").append(bug.getSeverity()).append("</td>");
                            stagnantFailureList.append("<td>").append(bug.getPriority()).append("</td>");
                        }
                    } catch (ClientProtocolException e)
                    {
                        e.printStackTrace();
                        stagnantFailureList.append("<td>").append("CP_ERROR").append("</td>");
                        stagnantFailureList.append("<td>").append("CP_ERROR").append("</td>");
                        stagnantFailureList.append("<td>").append("CP_ERROR").append("</td>");
                        stagnantFailureList.append("<td>").append("CP_ERROR").append("</td>");
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                        stagnantFailureList.append("<td>").append("IO_ERROR").append("</td>");
                        stagnantFailureList.append("<td>").append("IO_ERROR").append("</td>");
                        stagnantFailureList.append("<td>").append("IO_ERROR").append("</td>");
                        stagnantFailureList.append("<td>").append("IO_ERROR").append("</td>");
                        if (e.getMessage().toLowerCase().contains("timed out") && timeouts++ > 2)
                        {
                            break;
                        }
                    }
                }
                stagnantFailureList.append("</tr>");
            }
        }
        redDeltaMethodList.append("</table>");
        String rDeltaMList = redDeltaMethodList.toString();
        String rDCount = Integer.toString(redMethodDeltaCount);
        LOG.info("============>> redDeltaMethod count is : " + rDCount);
        rDeltaMList = rDeltaMList.replace("$redMethodDeltaCount", rDCount);
        redDeltaMethodList = new StringBuffer();
        redDeltaMethodList.append(rDeltaMList);
        stagnantFailureList.append("</table>");

        //6. Derive Green Delta methods - the previous fails which got converted to passes i.e. found in
        // CurrentPassList but not found in currentFailList
        StringBuffer greenDeltaMethodList = new StringBuffer();
        greenDeltaMethodList.append(
                "<table border=\"2\" cellspacing=\"0\" cellpadding=\"2\" class=\"param\" width=\"100%\">");
        greenDeltaMethodList.append(
                "<tr> <th bgcolor=\"green\">Green Delta [ FAIL-to-PASS ] - " + "$greenMethodDeltaCount" + " </th></tr>");

        for (String previousFail : pFailedMethodList.split(","))
        {
            String[] prevFailParts = previousFail.split("->");
            String previousFailure = prevFailParts[0]; //
            if (passedMethodBuff.indexOf(previousFailure) != -1 && failedMethodBuff.indexOf(previousFailure) == -1)
            {
                greenDeltaMethodList.append("<tr><td>");
                greenDeltaMethodList.append(previousFail);
                greenDeltaMethodList.append("</td></tr>");
                greenMethodDeltaCount++;
            }
        }
        greenDeltaMethodList.append("</table>");
        String gDeltaMList = greenDeltaMethodList.toString();
        String gDCount = Integer.toString(greenMethodDeltaCount);
        gDeltaMList = gDeltaMList.replace("$greenMethodDeltaCount", gDCount);
        greenDeltaMethodList = new StringBuffer();
        greenDeltaMethodList.append(gDeltaMList);

        buf.append("RedDeltaCountOverall =" + redDeltaCount).append("\n");
        buf.append("GreenDeltaCountOverall =" + greenDeltaCount).append("\n");
        buf.append("RedDeltaMethodCount =" + redMethodDeltaCount).append("\n");
        buf.append("GreenDeltaMethodCount =" + greenMethodDeltaCount).append("\n").append("\n");
        buf.append("RedDeltaMethods =" + redDeltaMethodList).append("\n").append("\n");
        buf.append("GreenDeltaMethods =" + greenDeltaMethodList).append("\n").append("\n");
        buf.append("StagnantFailures =" + stagnantFailureList).append("\n").append("\n");
        buf.append("FailureBasedReport =" + failBasedReportingBuff).append("\n").append("\n");
        if (redMethodDeltaCount > 0)
        {
            buf.append("X-Priority=1").append("\n");
            buf.append("importance=1").append("\n");
        } else if (redMethodDeltaCount == 0 && greenMethodDeltaCount > 0)
        {
            buf.append("X-Priority=5").append("\n");
            buf.append("importance=5").append("\n");
        } else if (failed > 0)
        {
            buf.append("X-Priority=3").append("\n");
            buf.append("importance=3").append("\n");
        } else
        {
            buf.append("X-Priority=5").append("\n");
            buf.append("importance=5").append("\n");
        }

        String historyUpdateLine = String.valueOf(prevBuildNumber + 1) + "," + String.valueOf(total) + ","
                + String.valueOf(totalNumberOfMethods) + "," + String.valueOf(totalPassedMethods)
                + "," + String.valueOf(totalFailedMethods) + "," + String.valueOf(totalSkippedMethods)
                + "," + String.valueOf(passed) + "," + String.valueOf(failed) + ","
                + String.valueOf(skipped) + "," + String.valueOf(redDeltaCount) + ","
                + String.valueOf(greenDeltaCount) + "," + String.valueOf(redMethodDeltaCount)
                + "," + String.valueOf(greenMethodDeltaCount);
        updateHistory(outputDirectory, historyUpdateLine);
    }

    private void updateHistory(String outputDirectory, String historyUpdateLine)
    {
        List<String> historyLines;
        try
        {
            historyLines = IOUtil.readAllLinesFromFileAsList(outputDirectory + "/" + persistedHistory, "#");
            if (null != historyLines)
            {
                File historyFile = new File(outputDirectory + "/" + persistedHistory);
                BufferedWriter hbw = new BufferedWriter(new FileWriter(historyFile, true));
//				hbw.write("BuildNo, totalRunTCcount, totalRunTestMethodCount, totalPassedMethods, " +
//						"totalFailedMethods, totalSkippedMehtods, totalPassedCount, totalFailedCount," +
//						" totalSkippedCount, redDeltalCount, greenDeltaCount, redMethodDeltaCount, " +
//						"greenMethodDeltaCount");
                hbw.write(historyUpdateLine);
                LOG.info("History file " + historyFile.getAbsolutePath() + " updated with this run stats!");
                hbw.write("\n");
                hbw.flush();
                hbw.close();
            } else
            {
                System.err.println("No history found at " + outputDirectory + "/" + persistedHistory);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("History file " + outputDirectory + "/" + persistedHistory +
                    " could not be updated - " + e.getMessage());
        }
    }

    private void getParametersAsString(Object[] parameters, StringBuffer paraBuff)
    {
        for (Object obj : parameters)
        {
            if (null == obj)
            {
                paraBuff.append("null");
                continue;
            }
            if (obj instanceof String)
            {
                obj = (String) obj;
            } else if (obj instanceof Integer)
            {
                obj = (Integer) obj;
            }
            if (obj.toString().contains("@") || obj.toString().contains("org.testng"))
            {
                paraBuff.append(".");
                continue;
            }
            paraBuff.append("\t" + obj.toString()).append(" | ");
        }
    }

    private int getPrevBuildNumber(List<String> historyLines)
    {
        int prevBuildNumber;
        int lastLineNumber = 0;
        if (null != historyLines)
        {
            lastLineNumber = (historyLines.size() - 1) >= 0 ? historyLines.size() - 1 : 0;
        }
        prevBuildNumber = Integer.valueOf(System.getProperty("BUILD_NUMBER", "0")).intValue() - 1;
        if (lastLineNumber > 0 && prevBuildNumber < 1)
        {
            String lastLine = historyLines.get(lastLineNumber);
            String buildNumberString = lastLine.substring(0, (lastLine.indexOf(",") == -1) ? 0 : lastLine.indexOf(","));
            try
            {
                prevBuildNumber = Integer.valueOf(buildNumberString).intValue();
            } catch (NumberFormatException e)
            {
//				e.printStackTrace();
                System.err.println("Ignoring NumberFormatException : " + e.getMessage());
            }
        }
        if (prevBuildNumber < 1)
        {
            prevBuildNumber = Integer.valueOf(System.getProperty("build.number", "0")).intValue();
        }
        return prevBuildNumber;
    }

    private List<String> initializeHistory(String outputDirectory)
    {
        List<String> historyLines = null;
        try
        {
            historyLines = IOUtil.readAllLinesFromFileAsList(outputDirectory + "/" + persistedHistory, "#");
            if (null == historyLines || (null != historyLines && historyLines.size() < 1))
            {
                File historyFile = new File(outputDirectory + "/" + persistedHistory);
                BufferedWriter hbw = new BufferedWriter(new FileWriter(historyFile));
                hbw.write("#Smart Reporting - history");
                hbw.write("\n");
                hbw.write(System.getProperty("line.separator"));
                hbw.write("BuildNo, totalRunTCcount, totalRunTestMethodCount, totalPassedMethods, " +
                        "totalFailedMethods, totalSkippedMehtods, totalPassedCount, totalFailedCount," +
                        " totalSkippedCount, redDeltalCount, greenDeltaCount, redMethodDeltaCount, " +
                        "greenMethodDeltaCount");
                hbw.write(System.getProperty("line.separator"));
                hbw.flush();
                hbw.close();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return historyLines;
    }

    public static int countSubstrings(String inputData, String searchString)
    {
        if (searchString.length() > inputData.length())
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < inputData.length(); i++)
        {
            if (inputData.indexOf(searchString, i) == i)
            {
                count++;
            }
        }
        return count;
    }

    private Collection<ITestNGMethod> getMethodSet(IResultMap tests)
    {
        Set<ITestNGMethod> r = new TreeSet<ITestNGMethod>(new TestSorter<ITestNGMethod>());
        r.addAll(tests.getAllMethods());
        return r;
    }

    private class TestSorter<T extends ITestNGMethod> implements Comparator<T>
    {
        /**
         * Arranges methods by classname and method name
         */
        public int compare(T o1, T o2)
        {
            int r = ((T) o1).getTestClass().getName().compareTo(((T) o2).getTestClass().getName());
            if (r == 0)
            {
                r = ((T) o1).getMethodName().compareTo(((T) o2).getMethodName());
            }
            return r;
        }
    }

    /**
     * @param fileToWrite   - in which file to write - filename with location
     * @param stringToWrite - what to write
     */
    public static void writeToFile(String fileToWrite, String stringToWrite)
    {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(fileToWrite, false));
            writer.write(stringToWrite);
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("Could not write report! " + e.getLocalizedMessage());
            System.exit(-1);
        }
    }
}
