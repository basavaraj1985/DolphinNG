package com.javasbar.framework.testng.reporters;

import com.javasbar.framework.lib.common.IOUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This RuleBasedStatusModifier can be used as follows:
 * By run time config -DskipKnownFailures=know failure1~known failure two~etc -Drmd=~
 * #rmd = <Rulebased Modifier Delimiter>, Default is ~
 * Configuration can be provided in configuration file.
 * Config file default location config/resultModifier.properties. Can be overriden using -DruleModifierConfig
 *
 * Created by Basavaraj M on 7/22/17.
 */
public class RuleBasedStatusModifier implements ITestListener
{
    private static final Logger LOG = LogManager.getLogger(RuleBasedStatusModifier.class);
    private static final java.lang.String RULE_MODIFIER_SKIP_KNOWN_FAILURES_KEY = "skipKnownFailures";
    private String configFile = "config/resultModifier.properties";

    public static final String RULE_MODIFIER_CONFIG_KEY = "ruleModifierConfig";
    public static final String RULE_MODIFIER_DELIMITER = "'rmd";
    public static final String SKIP_FOR_KEY = "skipFor";

    public static final String PASS_FOR_KEY = "passFor";
    private String delimiter = "~";
    private Properties config;
    private List<String> skipForValues = new ArrayList<>();
    private List<String> passForValues = new ArrayList<>();

    public RuleBasedStatusModifier()
    {
        LOG.info("RuleBasedStatusModifier initialization!");
        logUsage();
        try
        {
            initializeLists();
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage(), e);
        }
    }

    private void logUsage()
    {
        LOG.info("This RuleBasedStatusModifier can be used as follows: ");
        LOG.info("By run time config -DskipFor=know failure1~known failure two~etc -Drmd=~");
        LOG.info("#rmd = <Rulebased Modifier Delimiter>, Default is ~");
        LOG.info("Configuration can be provided in configuration file. " +
                 "Config file default location config/resultModifier.properties. Can be overriden using -DruleModifierConfig");
    }

    private void initializeLists()
    {
        if ( System.getProperty(RULE_MODIFIER_CONFIG_KEY) != null )
        {
            configFile = System.getProperty(RULE_MODIFIER_CONFIG_KEY);
        }
        if ( System.getProperty(RULE_MODIFIER_DELIMITER) != null )
        {
            delimiter = System.getProperty(RULE_MODIFIER_DELIMITER);
        }
        config = new Properties();
        try
        {
            config.load(new FileReader(new File(configFile)));
            LOG.info("Loaded " + configFile + " for RulesBasedStatusModifier!");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOG.error("Failed loading " + configFile + " for RulesBasedStatusModifier", e);
        }
        String skipFor = config.getProperty(SKIP_FOR_KEY, "");
        StringTokenizer tokenizer = new StringTokenizer(skipFor, delimiter);
        while(tokenizer.hasMoreTokens())
        {
            skipForValues.add(tokenizer.nextToken());
        }
        String passFor = config.getProperty(PASS_FOR_KEY, "");
        tokenizer = new StringTokenizer(passFor, delimiter);
        while (tokenizer.hasMoreTokens())
        {
            passForValues.add(tokenizer.nextToken());
        }

        String runtimeKnownFailures = System.getProperty(RULE_MODIFIER_SKIP_KNOWN_FAILURES_KEY);
        if ( runtimeKnownFailures != null )
        {
            tokenizer = new StringTokenizer(runtimeKnownFailures, delimiter);
            while (tokenizer.hasMoreTokens())
            {
                skipForValues.add(tokenizer.nextToken());
            }
        }
    }

    @Override
    public void onTestStart(ITestResult iTestResult)
    {
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult)
    {

    }

    @Override
    public void onTestFailure(final ITestResult iTestResult)
    {
        final String failReason = iTestResult.getThrowable().getMessage();
        if (StringUtils.isBlank(failReason))
        {
            return;
        }
        skipForValues.forEach( each ->
        {
            if ( failReason.contains(each) )
            {
                iTestResult.setStatus(ITestResult.SKIP);
                LOG.info("Fail reason contains [" + each + "] marking the result to *skipped*");
                return;
            }
        }
        );
        passForValues.forEach( each ->
        {
            if ( failReason.contains((each)))
            {
                iTestResult.setStatus(ITestResult.SUCCESS);
                LOG.info("Fail reason contains [" + each + "] marking the result to *success*");
                return;
            }
        });

    }

    @Override
    public void onTestSkipped(ITestResult iTestResult)
    {
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult)
    {
    }

    @Override
    public void onStart(ITestContext iTestContext)
    {
    }

    @Override
    public void onFinish(ITestContext iTestContext)
    {
    }
}
