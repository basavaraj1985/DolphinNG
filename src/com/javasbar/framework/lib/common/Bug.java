package com.javasbar.framework.lib.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Bug
{
    private static final Logger LOG = LogManager.getLogger(Bug.class);

    String ID;
    String status;
    String priority;
    String severity;
    String resolution;
    String summary;

    public String getResolution()
    {
        return this.resolution;
    }

    public void setResolution(String resolution)
    {
        this.resolution = resolution;
    }

    public String getID()
    {
        if (null == ID)
        {
            return "-";
        }
        return ID;
    }

    public void setID(String iD)
    {
        ID = iD;
    }

    public String getStatus()
    {
        if (null == status)
        {
            return "-";
        }
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPriority()
    {
        if (null == priority)
        {
            return "-";
        }
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getSummary()
    {
        if (null == summary)
        {
            return "-";
        }
        return this.summary;
    }

    public void setSummary(String sum)
    {
        this.summary = sum;
    }

    public String getSeverity()
    {
        if (null == severity)
        {
            return "-";
        }
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    @Override
    public String toString()
    {
        return getID() + " Summary:" + summary + "\n"
                + "Severity:" + severity + "\n"
                + "Priority: " + priority + "\n"
                + "Status:" + status + "\n";
    }

}
