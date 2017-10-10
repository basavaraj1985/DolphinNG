package com.javasbar.framework.lib.common;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class JSONUtil
{
    private static final Logger LOG = LogManager.getLogger(JSONUtil.class);
    Object document;

    /**
     * @param file
     */
    public JSONUtil(File file)
    {
        document = Configuration.defaultConfiguration().jsonProvider().parse(file.getAbsolutePath());
    }

    /**
     * @param inputStream
     */
    public JSONUtil(InputStream inputStream)
    {
        document = Configuration.defaultConfiguration().jsonProvider().parse(inputStream, "UTF-8");
    }

    /**
     * @param jsonString - json content as string
     */
    public JSONUtil(String jsonString)
    {
        document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
    }

    public static void main(String[] args) throws FileNotFoundException
    {
        JSONUtil util = new JSONUtil(new FileInputStream("test.json"));
        LOG.info(util.getValue("$.header.cdc"));
    }

    /**
     * @param path
     * @return
     */
    public String getValue(String path)
    {
        return JsonPath.read(document, path);
    }

    public JSONArray getValueArray(String path)
    {
        return JsonPath.read(document, path);
    }

    @Override
    public String toString()
    {
        return document.toString();
    }
}
