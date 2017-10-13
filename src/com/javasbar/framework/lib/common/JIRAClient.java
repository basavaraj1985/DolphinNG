package com.javasbar.framework.lib.common;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Basavaraj M
 */
public class JIRAClient
{
    private static final Logger LOG = LogManager.getLogger(JIRAClient.class);
    /**
     * use it to filter bugs based on project or label etc. ex: project~blink AND labels in (must,forRelease)
     */
    public static final String ADDITIONAL_ALWAYS_FILTER = "additionalAlwaysFilter";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String BUG_CREATE_BODY = "bug.create.body";
    public static final String BUG_SUMMARY_PREFIX = "summary.prefix";
    public static final String AUTO_CREATE_BUG = "bug.auto.create";
    public static final String BUG_ATTACHMENTS = "bug.attachments";
    public static final String AUTO_ATTACH_FILES_ON_FOUNDBUGS = "bug.auto.attach.if.found";
    public static final String SEARCH_STOP_WORDS_REGEX = "search.stopwords";
    public static final String SEARCH_TRUNCATE_AFTER_WORDS = "search.truncateAfter";
    public static final String AUTO_CREATE_STOP_WORDS_REGEX = "bug.create.summary.stopwords";
    public static final String AUTO_CREATE_ADDITIONAL_DETAILS = "bug.create.additionalDetails";
    /**
     *
     */
    private static final String FIELDS_TO_FILTER = "id,key,status,resolution,summary,priority,customfield_10302";
    /**
     * ex: jira.xyz.com
     */
    public static String DEFECT_MGMT_HOST = "bug.management.host";

    /**
     * /rest/api/latest/search?jql=
     */
    public static String SEARCH_API = "search.uri";

    /**
     * /rest/api/latest/issue/
     */
    public static String BUG_API = "bug.uri";

    public static String BUG_BROWSE_URI = "bug.browse.uri";

    /**
     * Authorization
     */
    public static String AUTH_HEADER_KEY = "authHeaderKey";

    /**
     * Basic aWRlYV9hdXRvbWF0aW9uOnBhc3N3b3Jk
     */
    public static String AUTH_HEADER_VALUE = "authHeaderValue";

    /**
     * Proxy settings to use while connecting to jira server
     */
    public static String PROXY_ENABLED = "proxy.enabled";
    public static String PROXY_HOST = "proxy.host";
    public static String PROXY_PORT = "proxy.port";
    private Proxy proxy;
    private Properties configuration;
    private String theAlwaysFilter;
    private VelocityEngine velocityEngine = new VelocityEngine();
    private String defaultBugCreateTemplate =
            "{\n" +
                    "  \"fields\": {\n" +
                    "    \"project\":\n" +
                    "    {\n" +
                    "      \"key\": \"$bug.projectKey\"\n" +
                    "    },\n" +
                    "    \"summary\": \"$bug.summary\",\n" +
                    "    \"description\": \"$bug.description\",\n" +
                    "    \"issuetype\":\n" +
                    "    {\n" +
                    "      \"name\": \"Bug\"\n" +
                    "    },\n" +
                    "    \"components\": [{\"name\": \"$bug.component\" }],\n" +
                    "    \"customfield_10302\": { \"id\": \"$bug.severity\" }   ,\n" +
                    "    \"customfield_10207\": [{ \"value\": \"$bug.environment\" }],\n" +
                    "    \"customfield_10502\": { \"value\": \"$bug.impact\" },\n" +
                    "    \"versions\": [{ \"name\": \"$bug.version\"}]\n" +
                    "  }\n" +
                    "}";
    private String buildUrl = System.getProperty("BUILD_URL",
            RunTimeLib.runCommandBlocking("hostname").toString() + "/" + System.getProperty("user.dir"));
    private Header authenticationHeader;

    /**
     * @param configFile
     */
    public JIRAClient(String configFile)
    {
        Properties jiraConfig = new Properties();
        try
        {
            jiraConfig.load(new FileReader(new File(configFile)));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        initializeClient(jiraConfig);
    }

    /**
     * @param props
     */
    public JIRAClient(Properties props)
    {
        initializeClient(props);
    }

    /**
     * @param file
     * @return
     */
    public static Properties loadFileIntoProperties(String file)
    {
        Properties props = new Properties();
        try
        {
            props.load(new FileReader(new File(file)));
        } catch (FileNotFoundException e)
        {
            LOG.info("Could not find file : " + file);
            e.printStackTrace();
        } catch (IOException e)
        {
            LOG.info("Could not load file : " + file);
            e.printStackTrace();
        }
        return props;
    }

    public static Properties loadInputStreamIntoProperties(InputStream stream)
    {
        Properties props = new Properties();
        try
        {
            props.load(stream);
        } catch (IOException e)
        {
            System.err.println("Could not load file - jiraclient.properties as resource");
            e.printStackTrace();
        }
        return props;
    }

    private void initializeClient(Properties props)
    {
        if (null == props || props.size() == 0)
        {
            LOG.info("Loading default configuration for JIRAClient!");
            InputStream configAsStream =
                    Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("jiraclient.properties");
            configuration = loadInputStreamIntoProperties(configAsStream);
        } else
        {
            configuration = props;
        }

        if (System.getProperty(PROXY_ENABLED, configuration.getProperty(PROXY_ENABLED, "false")).equalsIgnoreCase("true"))
        {
            String host = System.getProperty(PROXY_HOST, configuration.getProperty(PROXY_HOST, PROXY_HOST + " not configured in jiraclient" +
                    ".properties!"));
            String port = System.getProperty(PROXY_PORT, configuration.getProperty(PROXY_PORT, PROXY_PORT + " not configured in jiraclient" +
                    ".properties"));
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, Integer.valueOf(port.trim())));
        }
        theAlwaysFilter = System.getProperty(ADDITIONAL_ALWAYS_FILTER, configuration.getProperty(ADDITIONAL_ALWAYS_FILTER, ""));
        authenticationHeader = new Header(configuration.getProperty(AUTH_HEADER_KEY), configuration.getProperty(AUTH_HEADER_VALUE));
        velocityEngine.init();
        velocityEngine.setProperty("input.encoding", "UTF-8");
        velocityEngine.setProperty("output.encoding", "UTF-8");
    }

    private String removeStopWordsFrom(String summary, String stopWordsList)
    {
        if (null == stopWordsList || stopWordsList.length() < 1)
        {
            return summary;
        }
        LOG.info("Stop words to be removed : " + stopWordsList + "\nFrom input: " + summary);
        StringTokenizer stopWords = new StringTokenizer(stopWordsList, ",");
        while (stopWords.hasMoreTokens())
        {
            String stopWord = stopWords.nextToken();
            summary = summary.replaceAll(stopWord, " ").replaceAll("\\s+", " ").trim();
        }
        LOG.info("Output after stopwords removal: " + summary);
        return summary;
    }

    private String truncateAfterStopWords(String summary, String truncateStopWords)
    {
        if (null == truncateStopWords || truncateStopWords.trim().length() < 1)
        {
            return summary;
        }
        LOG.info("Truncate after words: " + truncateStopWords + "\nFrom input: " + summary);
        StringTokenizer stopWords = new StringTokenizer(truncateStopWords, ",");
        while (stopWords.hasMoreTokens())
        {
            String stopWord = stopWords.nextToken();
            int end = summary.indexOf(stopWord);
            if (end > 0)
            {
                summary = summary.substring(0, end);
            }
        }
        return summary;
    }

    private String attachFilesToIssue(String jiraId)
    {
        String attachments = System.getProperty(BUG_ATTACHMENTS, configuration.getProperty(BUG_ATTACHMENTS));
        StringBuilder attachedFiles = new StringBuilder();
        if (null != attachments)
        {
            StringTokenizer tokenizer = new StringTokenizer(attachments, ",");
            while (tokenizer.hasMoreTokens())
            {
                String attach = tokenizer.nextToken();
                try
                {
                    if (attachFileToIssue(jiraId, attach))
                    {
                        LOG.info("File " + attach + " attached to jira " + jiraId + " successfully!");
                        attachedFiles.append(attach).append(",");
                    }
                } catch (Exception e)
                {
                    System.err.println("Error while attaching file " + attach + " : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return attachedFiles.toString();
    }

    /**
     * @param summary
     * @param description
     * @param payloadTemplateFile
     * @param overrideBugFields
     * @return
     */
    private String preparePayload(String summary, String description,
                                  String payloadTemplateFile, Map<String, Object> overrideBugFields)
    {
        Map<String, Object> bugMap = new HashMap<>();
        bugMap.put("summary", configuration.getProperty(BUG_SUMMARY_PREFIX,
                "[Automation Filed Bug]") + summary);
        description = prepareBugDescriptionForBugCreation(description);
        bugMap.put("description", description);
        Set<Map.Entry<Object, Object>> entries = configuration.entrySet();
        entries.forEach(each ->
        {
            bugMap.put(each.getKey().toString(), each.getValue().toString());
        });
        if (null != overrideBugFields && overrideBugFields.size() > 0)
        {
            bugMap.putAll(overrideBugFields);
        }
        // Override bugMap with system level/runtime configurations
        bugMap.keySet().forEach(each ->
        {
            if (null != System.getProperty(each.toString()))
            {
                bugMap.put(each.toString(), System.getProperty(each.toString()));
            }
            LOG.info(each + "=" + bugMap.get(each.toString()));
        });

        boolean templateFileFound = new File(payloadTemplateFile).exists();
        LOG.info("Bug body template found? = " + templateFileFound);
        if (!templateFileFound)
        {
            System.err.println("Bug body template was not found, using default template, writing it to : " +
                    payloadTemplateFile);
            try
            {
                IOUtil.writeFile(payloadTemplateFile, defaultBugCreateTemplate, false);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        Template template = velocityEngine.getTemplate(payloadTemplateFile, "UTF-8");
        VelocityContext context = new VelocityContext();
        context.put("bug", bugMap);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        String payload = writer.toString();
        LOG.info("Create Issue Input data : " + payload);
        return payload;
    }

    /**
     * Prepares the description of bug body. By adding build url, a nice header, a note and
     * excaptes html tags, json tags.
     *
     * @param description
     * @return
     */
    private String prepareBugDescriptionForBugCreation(String description)
    {
        description = "*[Automation failed tests]*\n" +
                "||Testcase failing|| Parameters||\n" +
                description + "\n" +
                System.getProperty(AUTO_CREATE_ADDITIONAL_DETAILS,
                        configuration.getProperty(AUTO_CREATE_ADDITIONAL_DETAILS, "")) + "\n" +
                "Build url: " + buildUrl;
        description = description + "\n\n\n" + "Note: This bug is created automatically by DolphinNG." +
                " Please do not edit summary line of the bug.";
        description = StringEscapeUtils.escapeHtml3(description);
        description = StringEscapeUtils.escapeHtml4(description);
        description = JSONObject.escape(description);
        return description;
    }

    /*
     *
     */
    private String getField(Object document, String fieldJsonPath)
    {
        String result = null;
        try
        {
            result = JsonPath.read(document, fieldJsonPath);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Returns the value loaded for given configuration
     *
     * @param key
     * @return
     */
    public String getConfigurationValue(String key)
    {
        return configuration.getProperty(key);
    }

    /**
     * @param bugid
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public Bug getBugByBugId(String bugid) throws IOException
    {
        if (StringUtils.isBlank(bugid))
        {
            return null;
        }
        URL request = new URL("https://" + configuration.getProperty(DEFECT_MGMT_HOST) +
                configuration.getProperty(BUG_API) + "/" + bugid + "?fields=" + FIELDS_TO_FILTER);
        HttpURLConnection connection = null;
        if (configuration.getProperty(PROXY_ENABLED, "false").equalsIgnoreCase("true"))
        {
            connection = (HttpURLConnection) request.openConnection(proxy);
        } else
        {
            connection = (HttpURLConnection) request.openConnection();
        }
        connection.setRequestProperty(configuration.getProperty(AUTH_HEADER_KEY), configuration.getProperty(AUTH_HEADER_VALUE));
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.connect();
        InputStream content = (InputStream) connection.getContent();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(content, "UTF-8");
        String summary = getField(document, "$.fields.summary");
        String id = getField(document, "$.key");
        String priority = getField(document, "$.fields.priority.name");
        String severity = getField(document, "$.fields.customfield_10302.value");
        String resolution = getField(document, "$.fields.resolution.name");
        String status = getField(document, "$.fields.status.name");

        Bug bug = new Bug();
        System.err.println("(getBugByBugId) found=" + id + " input=" + bugid
                + "~~~~~~~~~~>>>>> url: " + request.toExternalForm());
        bug.setID(bugid);
        bug.setStatus(status);
        bug.setPriority(priority);
        bug.setSeverity(severity);
        bug.setResolution(resolution);
        bug.setSummary(summary);
        LOG.info(bug);
        return bug;
    }

    /**
     * Returns a Bug that satisfies the jqlQuery
     *
     * @param jqlQuery - JIRA Query refer:
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public Bug getBugsBySearchQuery(String jqlQuery) throws IOException
    {
        LOG.info("Searching for bugs with jqlQuery: " + jqlQuery);
        jqlQuery = URLEncoder.encode(jqlQuery + " " + theAlwaysFilter, "UTF-8");
        LOG.info("Searching for bugs with jqlQuery(urlEncoded): " + jqlQuery);
        URL request = new URL("https://" + configuration.getProperty(DEFECT_MGMT_HOST) +
                configuration.getProperty(SEARCH_API) + jqlQuery + "&fields=" + FIELDS_TO_FILTER);
        LOG.info("JIRA request -~~-> " + request.toString());
        HttpURLConnection connection = null;
        if (configuration.getProperty(PROXY_ENABLED, "false").equalsIgnoreCase("true"))
        {
            connection = (HttpURLConnection) request.openConnection(proxy);
        } else
        {
            connection = (HttpURLConnection) request.openConnection();
        }
        connection.setRequestProperty(configuration.getProperty(AUTH_HEADER_KEY), configuration.getProperty(AUTH_HEADER_VALUE));
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.connect();
        InputStream content = (InputStream) connection.getContent();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(content, "UTF-8");
        JSONArray allIssues = JsonPath.read(document, "$.issues");
        LOG.info(allIssues.size() + " issues found!");
        if (allIssues.size() == 0)
        {
            LOG.info("No bug was found for query - " + jqlQuery + "\n get: " + request);
            return null;
        }
        System.err.println("********************************************");
        System.err.println(allIssues.size() + " issues found!");
        int openBugIndex = 0;
        if (allIssues.size() > 1)
        {
            for (int i = 0; i < allIssues.size(); i++)
            {
                String status = getField(document, "$.issues[" + i + "].fields.status.name");
                if (status != null && status.compareToIgnoreCase("Closed") != 0 && status.compareToIgnoreCase("Verify") != 0)
                {
                    openBugIndex = i;
                    break;
                }
            }
        }
        System.err.println("Taking the first open bug for reporting purposes!");
        System.err.println("********************************************");
        String summary = getField(document, "$.issues[" + openBugIndex + "].fields.summary");
        String priority = getField(document, "$.issues[" + openBugIndex + "].fields.priority.name");
        String severity = getField(document, "$.issues[" + openBugIndex + "].fields.customfield_10302.value");
        String resolution = getField(document, "$.issues[" + openBugIndex + "].fields.resolution.name");
        String status = getField(document, "$.issues[" + openBugIndex + "].fields.status.name");

        Bug bug = new Bug();
        bug.setID((String) JsonPath.read(document, "$.issues[" + openBugIndex + "].key"));
        bug.setStatus(status);
        bug.setPriority(priority);
        bug.setSeverity(severity);
        bug.setResolution(resolution);
        bug.setSummary(summary);
        LOG.info(bug);
        return bug;
    }

    /**
     *
     * @param jqlQuery
     * @return
     * @throws IOException
     */
    public Bug getBugsBySearchQueryRA(String jqlQuery) throws IOException
    {
//        jqlQuery =  URLEncoder.encode(jqlQuery + " " + theAlwaysFilter,"UTF-8");
        URL request = new URL("https://" + configuration.getProperty(DEFECT_MGMT_HOST) +
                configuration.getProperty(SEARCH_API) + jqlQuery + "&fields=" + FIELDS_TO_FILTER);
        LOG.info("JIRA request -~~-> " + request.toString());

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        Response response = null;
        RequestSpecification given = RestAssured.given();
        if (null != configuration.getProperty(PROXY_HOST))
        {
            given = given.proxy(configuration.getProperty(PROXY_HOST), Integer.parseInt(configuration.getProperty(PROXY_PORT)));
            LOG.info("Using proxy server - " + configuration.getProperty(PROXY_HOST) + ":" +
                    configuration.getProperty(PROXY_PORT) + " given: " + given);
        }
        given = given.header(authenticationHeader);
        response = given.contentType(CONTENT_TYPE_APPLICATION_JSON)
                .accept(CONTENT_TYPE_APPLICATION_JSON).when().get(request).then()
                .extract().response();
        LOG.info("JIRA ticket creation result--->  StatusCode=" +
                response.getStatusCode() + " status=" + response.getStatusLine() +
                " failResponse=" + response.getBody().prettyPrint());
        String document = response.getBody().print();

        JSONArray allIssues = JsonPath.read(document, "$.issues");
        LOG.info(allIssues.size() + " issues found!");
        if (allIssues.size() == 0)
        {
            LOG.info("No bug was found for query - " + jqlQuery + "\n get: " + request);
            return null;
        }
        System.err.println("********************************************");
        System.err.println(allIssues.size() + " issues found!");
        int openBugIndex = 0;
        if (allIssues.size() > 1)
        {
            for (int i = 0; i < allIssues.size(); i++)
            {
                String status = getField(document, "$.issues[" + i + "].fields.status.name");
                if (status != null && status.compareToIgnoreCase("Closed") != 0 && status.compareToIgnoreCase("Verify") != 0)
                {
                    openBugIndex = i;
                    break;
                }
            }
        }
        System.err.println("Taking the first open bug for reporting purposes!");
        System.err.println("********************************************");
        String summary = getField(document, "$.issues[" + openBugIndex + "].fields.summary");
        String priority = getField(document, "$.issues[" + openBugIndex + "].fields.priority.name");
        String severity = getField(document, "$.issues[" + openBugIndex + "].fields.customfield_10302.value");
        String resolution = getField(document, "$.issues[" + openBugIndex + "].fields.resolution.name");
        String status = getField(document, "$.issues[" + openBugIndex + "].fields.status.name");

        Bug bug = new Bug();
        bug.setID((String) JsonPath.read(document, "$.issues[" + openBugIndex + "].key"));
        bug.setStatus(status);
        bug.setPriority(priority);
        bug.setSeverity(severity);
        bug.setResolution(resolution);
        bug.setSummary(summary);
        LOG.info(bug);
        return bug;
    }

    /**
     * Attaches given file to the jira ticket provided.
     *
     * @param jiraTicket   - jira ticket id
     * @param fileToAttach - file path, that needs to be attached
     * @throws MalformedURLException
     */
    public boolean attachFileToIssue(String jiraTicket, String fileToAttach) throws MalformedURLException
    {
        URL request = new URL("https://" + configuration.getProperty(DEFECT_MGMT_HOST) +
                configuration.getProperty(BUG_API) + "/" + jiraTicket + "/attachments");
        File file = new File(fileToAttach);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RequestSpecification given = RestAssured.given();
        Header header2 = new Header("X-Atlassian-Token", "nocheck");
        given = given.header(authenticationHeader).and().header(header2);
        if (null != configuration.getProperty(PROXY_HOST))
        {
            given = given.proxy(configuration.getProperty(PROXY_HOST), Integer.parseInt(configuration.getProperty(PROXY_PORT)));
            LOG.info("Using proxy server - " + configuration.getProperty(PROXY_HOST) + ":" +
                    configuration.getProperty(PROXY_PORT) + " given: " + given);
        }
        Response response = given
                .accept(CONTENT_TYPE_APPLICATION_JSON)
                .when().multiPart(file)
                .post(request).then()
                .extract().response();
        return response.getStatusCode() == 200;
    }

    /**
     * Creates a jira ticket, and returns jira id.
     *
     * @param summary
     * @param description
     * @return
     */
    public String createJiraTicket(String summary, String description, Map<String, Object> overrideBugFields)
            throws IOException
    {
        String jiraId = null;
        String searchSummary = truncateAfterStopWords(summary, System.getProperty(SEARCH_TRUNCATE_AFTER_WORDS,
                configuration.getProperty(SEARCH_TRUNCATE_AFTER_WORDS)));
        searchSummary = searchSummary.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        searchSummary = removeStopWordsFrom(searchSummary, System.getProperty(SEARCH_STOP_WORDS_REGEX,
                configuration.getProperty(SEARCH_STOP_WORDS_REGEX)));
        String createConditionQuery = "summary~\"" + searchSummary + "\" ";
        Bug foundBug = getBugsBySearchQuery(createConditionQuery);
        jiraId = (null != foundBug && !foundBug.getStatus().equalsIgnoreCase("closed")) ? foundBug.getID() : null;
        if (!Boolean.parseBoolean(System.getProperty(AUTO_CREATE_BUG, configuration.getProperty(AUTO_CREATE_BUG, "false"))))
        {
            System.err.println("Auto bug creation disabled! Already open bug for same fail reason - " + jiraId);
            return jiraId;
        }
        summary = summary.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        if (null != foundBug && !foundBug.getStatus().equalsIgnoreCase("closed"))
        {
            LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            LOG.info("Bug already exists for " + createConditionQuery);
            LOG.info(foundBug);
            if (Boolean.parseBoolean(System.getProperty(AUTO_ATTACH_FILES_ON_FOUNDBUGS,
                    configuration.getProperty(AUTO_ATTACH_FILES_ON_FOUNDBUGS, "true"))))
            {
                LOG.info("Attaching files for this run!");
                String attached = attachFilesToIssue(foundBug.getID());
                description = "||Testcase failing|| Parameters||\n" +
                        description +
                        "\n\n" +
                        System.getProperty(AUTO_CREATE_ADDITIONAL_DETAILS,
                                configuration.getProperty(AUTO_CREATE_ADDITIONAL_DETAILS, "")) + "\n" +
                        "Attaching files for this failure, files are: " + attached;
                addCommentToIssue(foundBug.getID(), description);
            }
            LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            return foundBug.getID();
        }
        LOG.info("Creating jira ticket for " + summary);
        URL request = new URL("https://" + configuration.getProperty(DEFECT_MGMT_HOST) + configuration.getProperty(BUG_API));
        String payloadTemplateFile = configuration.getProperty(BUG_CREATE_BODY, "resources/jiracreateTemplate.vm");
        summary = removeStopWordsFrom(summary, System.getProperty(AUTO_CREATE_STOP_WORDS_REGEX,
                configuration.getProperty(AUTO_CREATE_STOP_WORDS_REGEX)));
        String payload = preparePayload(summary, description, payloadTemplateFile, overrideBugFields);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RequestSpecification given = RestAssured.given();
        given = given.header(authenticationHeader);
        if (null != configuration.getProperty(PROXY_HOST))
        {
            given = given.proxy(configuration.getProperty(PROXY_HOST), Integer.parseInt(configuration.getProperty(PROXY_PORT)));
            LOG.info("Using proxy server - " + configuration.getProperty(PROXY_HOST) + ":" +
                    configuration.getProperty(PROXY_PORT) + " given: " + given);
        }
        Response response = given
                .contentType(CONTENT_TYPE_APPLICATION_JSON)
                .accept(CONTENT_TYPE_APPLICATION_JSON)
                .when().body(payload).post(request).then()
                .extract().response();
        LOG.info("JIRA ticket creation result--->  StatusCode=" +
                response.getStatusCode() + " status=" + response.getStatusLine() +
                " payload=" + payload +
                " Response=" + response.getBody().prettyPrint());
        if (201 == response.getStatusCode())
        {
            JSONUtil jsonUtil = new JSONUtil(response.getBody().print());
            jiraId = jsonUtil.getValue("$.key");
            LOG.info("Created JIRA for [" + summary + "], bug: " + jiraId);
            attachFilesToIssue(jiraId);
        }
        return jiraId;
    }

    /**
     * Adds a comment to the jira ticket
     *
     * @param jiraTicket - jira ticket id
     * @param comment    - comment to add
     * @return
     * @throws MalformedURLException
     */
    public boolean addCommentToIssue(String jiraTicket, String comment) throws MalformedURLException
    {
        URL request = new URL("https://" + configuration.getProperty(DEFECT_MGMT_HOST) +
                configuration.getProperty(BUG_API) + "/" + jiraTicket + "/comment");
        comment = comment + "\n\n\n" + "Test Run URL: " + buildUrl;
        comment = comment + "\n" + "Note: This comment is created automatically by DolphinNG.";
        comment = StringEscapeUtils.escapeHtml3(comment);
        comment = StringEscapeUtils.escapeHtml4(comment);
        comment = JSONObject.escape(comment);
        String payload =
                "{" +
                        "\"body\":" + "\"" + comment + "\"" +
                        "}";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RequestSpecification given = RestAssured.given();
        given = given.header(authenticationHeader);
        Response response = given
                .contentType(CONTENT_TYPE_APPLICATION_JSON)
                .accept(CONTENT_TYPE_APPLICATION_JSON)
                .when().body(payload).post(request).then()
                .extract().response();
        LOG.info("JIRA ticket creation result--->  StatusCode=" +
                response.getStatusCode() + " status=" + response.getStatusLine() +
                " payload=" + payload +
                " Response=" + response.getBody().prettyPrint());
        return response.getStatusCode() == 201;
    }

    /**
     * Returns url to access a bug in front end.
     *
     * @param bugid
     * @return
     */
    public String getBrowseURLForBug(String bugid)
    {
        String url =
                "https://" + configuration.getProperty(DEFECT_MGMT_HOST)
                        + configuration.getProperty(BUG_BROWSE_URI) + bugid;
        return url;
    }

    public static void main(String[] args) throws IOException
    {
        JIRAClient jiraClient = new JIRAClient("resources/jiraclient.properties");
        String bugId = jiraClient.createJiraTicket("test summary", "test description\n multi line", null);
        jiraClient.addCommentToIssue(bugId, "Bug created today!");
        jiraClient.attachFileToIssue(bugId, "/path/to/file");
        System.err.println("================================test==============================================");
    }
}


