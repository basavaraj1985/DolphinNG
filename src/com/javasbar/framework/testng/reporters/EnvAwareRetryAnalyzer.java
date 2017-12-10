package com.javasbar.framework.testng.reporters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestNGListener;
import org.testng.ITestResult;

import com.javasbar.framework.lib.common.NetworkUtil;

/**
 * @author Basavaraj M
 */
public class EnvAwareRetryAnalyzer implements IRetryAnalyzer, ITestNGListener
{
    private static final Logger LOG = LogManager.getLogger(EnvAwareRetryAnalyzer.class);
    private static final String MAX_RETRY_COUNT = "maxRetryCount";
    private static final String RETRY_WAIT_SECONDS = "retryWaitSeconds";
    private static final String BASE_URL = "baseUrl";
    private int retryCount = 0;
    private int maxRetryCount;
    private int retryWaitSeconds;

    /**
     * If a testcase is failed and if baseUrl is not pingable, retry the test case.
     */
    public boolean retry(ITestResult testResult)
    {
        String maxRetryStr = System.getProperty(MAX_RETRY_COUNT,
                (String) testResult.getTestContext().getAttribute(MAX_RETRY_COUNT));
        String maxRetryWaitSecondsStr = System.getProperty(RETRY_WAIT_SECONDS,
                (String) testResult.getTestContext().getAttribute(RETRY_WAIT_SECONDS));
        String url = System.getProperty(BASE_URL, (String) testResult.getTestContext().getAttribute(BASE_URL));
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
        }
        if (null != maxRetryWaitSecondsStr)
        {
            try
            {
                retryWaitSeconds = Integer.valueOf(maxRetryWaitSecondsStr);
            } catch (NumberFormatException e)
            {
                e.printStackTrace();
                retryWaitSeconds = 180;
            }
        }

        LOG.info("@EnvAwareRetryAnalyzer retry");
        if (!testResult.isSuccess() && (retryCount++ < maxRetryCount))
        {
            if (null == url)
            {
                System.getProperty("baseUrl", "http://google.com");
            }
            if (!NetworkUtil.ping(url, 1000))
            {
                LOG.info("@EnvAwareRetryAnalyzer... retrying failed testcase - " + testResult.getName());
                LOG.info("Pinging url : " + System.getProperty("baseUrl"));
                // site is down, sleep for 10 minutes MAX and retry
                System.err.println("Website " + url + " is down, poll-waiting for 10 minutes at max!");
                long start = System.currentTimeMillis();
                while ((System.currentTimeMillis() - start) < (retryWaitSeconds * 1000))
                {
                    try
                    {
                        Thread.sleep(500);
                    } catch (InterruptedException e)
                    {
                    }
                    if (NetworkUtil.ping(System.getProperty("baseUrl"), 1000))
                    {
                        testResult.setStatus(ITestResult.SKIP);
                        return true;
                    }
                }
                testResult.setStatus(ITestResult.SKIP);
                return true;
            }
        }
        return false;
    }
}
