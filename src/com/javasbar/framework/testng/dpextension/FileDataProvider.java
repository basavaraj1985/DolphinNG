package com.javasbar.framework.testng.dpextension;

import com.javasbar.framework.lib.common.IOUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Basavaraj M
 */
public class FileDataProvider
{
    private static final Logger LOG = LogManager.getLogger(FileDataProvider.class);

    private static Iterator<Object[]> getDataIterator(Method testMethod) throws Exception
    {
        Map<String, String> arguments = DataProviderUtils.resolveDataProviderArguments(testMethod);
        List<String> lines = FileDataProvider.getRawLinesFromFile(arguments.get("filePath"));
        List<Object[]> data = new ArrayList<Object[]>();
        for (String line : lines)
        {
            data.add(new Object[]{line});
        }
        return data.iterator();
    }

    /**
     * @param testMethod
     * @return
     * @throws Exception
     */
    @DataProvider(name = "getDataFromFile", parallel = false)
    public static Iterator<Object[]> getDataFromFile(Method testMethod) throws Exception
    {
        return getDataIterator(testMethod);
    }

    /**
     * @param testMethod
     * @return
     * @throws Exception
     */
    @DataProvider(name = "getDataFromFileParallely", parallel = true)
    public static Iterator<Object[]> getDataFromFileParallely(Method testMethod) throws Exception
    {
        return getDataIterator(testMethod);
    }

    private static List<String> getRawLinesFromFile(Method testMethod) throws Exception
    {
        Map<String, String> arguments = DataProviderUtils.resolveDataProviderArguments(testMethod);
        return FileDataProvider.getRawLinesFromFile(arguments.get("filePath"));
    }

    private static List<String> getRawLinesFromFile(String filePath) throws Exception
    {
        InputStream is = new FileInputStream(new File(filePath));
        List<String> lines = IOUtil.readAllLinesFromFileAsList(filePath, "#");
        is.close();
        return lines;
    }
}