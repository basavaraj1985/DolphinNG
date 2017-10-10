package com.javasbar.framework.lib.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 * @author Basavaraj M
 */
public class NetworkUtil
{
    private static final Logger LOG = LogManager.getLogger(NetworkUtil.class);

    /**
     * Pings a HTTP URL. This effectively sends a HEAD/GET request and returns <code>true</code> if the response code is in
     * the 200-399 range.
     *
     * @param url     The HTTP URL to be pinged.
     * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
     *                the total timeout is effectively two times the given timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     * given timeout, otherwise <code>false</code>.
     */
    public static boolean ping(String url, int timeout)
    {
        if (null == url)
        {
            return false;
        }
        url = url.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            LOG.info("pinging " + url + " Resulted: " + responseCode);
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception)
        {
            return false;
        }
    }

    /**
     * @param hostname
     * @param port
     * @return
     */
    public static boolean pingService(String hostname, int port)
    {
        InetAddress inet = null;
        try
        {
            inet = InetAddress.getByName(hostname);
        } catch (UnknownHostException e)
        {
            LOG.error("Unable to reach " + hostname + ":" + port + ", reason: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
        InetSocketAddress socketAddress = new InetSocketAddress(inet, port);
        SocketChannel sc = null;
        try
        {
            sc = SocketChannel.open();
        } catch (IOException e)
        {
            LOG.error("Unable to reach " + hostname + ":" + port + ", reason: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
        try
        {
            sc.configureBlocking(true);
        } catch (IOException e)
        {
            LOG.error("Unable to reach " + hostname + ":" + port + ", reason: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
        Date start = new Date();
        Date stop = null;
        try
        {
            if (sc.connect(socketAddress))
            {
                stop = new Date();
                long timeToRespond = (stop.getTime() - start.getTime());
                LOG.info("time taken(ms) to ping service" + hostname + ":" + port + " : " + timeToRespond);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            LOG.error("Unable to reach " + hostname + ":" + port + ", reason: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    public static void main(String[] args)
    {
        System.out.println(ping("http://yahoo.com", 800));
        System.out.println(pingService("google.com", 80));
    }

}
