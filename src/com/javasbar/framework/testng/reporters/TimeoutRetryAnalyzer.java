package com.javasbar.framework.testng.reporters;

import com.javasbar.framework.lib.common.IOUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestNGListener;
import org.testng.ITestResult;

public class TimeoutRetryAnalyzer implements IRetryAnalyzer, ITestNGListener
{
    private static final Logger LOG = LogManager.getLogger(TimeoutRetryAnalyzer.class);
    private int retryCount = 0;
    private int maxRetryCount;

    /**
     * If a testcase is failed and if baseUrl is not pingable, retry the test case.
     */
    @Override
    public boolean retry(ITestResult testResult)
    {
        LOG.info("@EnvAwareRetryAnalyzer retry");
        String maxRetryStr = (String) testResult.getTestContext().getAttribute("maxRetryCount");
        if (null != maxRetryStr)
        {
            try
            {
                maxRetryCount = Integer.valueOf(maxRetryStr);
            } catch (NumberFormatException e)
            {
                e.printStackTrace();
                maxRetryCount = 3;
            }
        } else
        {
            maxRetryCount = 3;
        }

        if (!testResult.isSuccess() && (retryCount++ < maxRetryCount))
        {
            String message = testResult.getThrowable().getMessage().toLowerCase();
            if (message.contains("time-out"))
            {
                LOG.info("Retrying for time out... ");
                return true;
            }
        }
        return false;
    }
}
