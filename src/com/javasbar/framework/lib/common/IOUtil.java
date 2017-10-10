package com.javasbar.framework.lib.common;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IOUtility to handle file writing, reading, reading randomStrings.
 *
 * @author Basavaraj M
 */
public class IOUtil
{
    public static final long KB = 1024;
    public static final long MB = 1024 * KB;
    public static final long GB = 1024 * MB;

    private static final Logger LOG = LogManager.getLogger(IOUtil.class);

    /**
     * Delete all files in a directory
     *
     * @param directory
     * @return
     * @throws IOException
     */
    public static int deleteAllFilesInDirectory(File directory) throws IOException
    {
        int deletedCount = 0;
        if (!directory.exists() || !directory.isDirectory())
        {
            LOG.error("No such directory (may be file/not directory) exists " + directory.getAbsolutePath());
        }

        File[] fileList = directory.listFiles();
        for (File file : fileList)
        {
            if (file.isDirectory())
            {
                deleteAllFilesInDirectory(file);
            }
            deleteFile(file);
        }
        return deletedCount;
    }

    /**
     * Delete a file
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static boolean deleteFile(String fileName) throws IOException
    {
        File file = new File(fileName);
        if (!file.exists())
        {
            throw new IOException("File " + fileName + " doesn't exist!");
        }
        return deleteFile(file);
    }

    /**
     * Delete a file 'file'
     *
     * @param file
     * @return
     */
    public static boolean deleteFile(File file)
    {
        boolean result = file.delete();
        return result;
    }

    /**
     * Writes 'results' lines into the 'fileToWrite' file.
     *
     * @param fileToWrite
     * @param results
     * @throws IOException
     */
    public static void writeFile(String fileToWrite, List<String> results) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileToWrite)));
        for (String line : results)
        {
            writer.write(line);
            writer.write('\n');
        }
        writer.flush();
        writer.close();
    }

    /**
     * Writes 'content' into 'fileName'.
     *
     * @param fileName
     * @param content
     * @throws IOException
     */
    public static void writeFile(String fileName, String content, boolean append) throws IOException
    {
        writeFile(fileName, content, append, true);
    }

    /**
     * Writes 'content' into 'fileName'.
     *
     * @param fileName
     * @param content
     * @throws IOException
     */
    public static void writeFile(String fileName, String content, String charset, boolean append) throws IOException
    {
        writeFile(fileName, content, charset, append, true);
    }

    /**
     * @param fileName
     * @param content
     * @param append
     * @param newLineRequired
     * @throws IOException
     */
    public static void writeFile(String fileName, String content, boolean append, boolean newLineRequired) throws
            IOException
    {
        writeFile(fileName, content, "UTF-8", append, true);
    }

    /**
     * Writes 'content' into 'fileName'.
     *
     * @param fileName
     * @param content
     * @param charSet         ex: UTF-8, UTF-16
     * @param append
     * @param newLineRequired
     * @throws IOException
     */
    public static void writeFile(String fileName, String content, String charSet, boolean append, boolean
            newLineRequired) throws IOException
    {
        File file = new File(fileName);
        if (!file.exists())
        {
            if (fileName.contains("/"))
            {
                int directoryIndex = fileName.lastIndexOf("/");
                String directory = fileName.substring(0, directoryIndex);
                File dir = new File(directory);
                dir.mkdirs();
            }
            file.createNewFile();
        }
        if (!StringUtils.isNotBlank(charSet))
        {
            charSet = "UTF-8";
        }
        BufferedWriter writer = new BufferedWriter(new FileWriterWithEncoding(file, charSet, append));
        writer.write(content);
        if (newLineRequired)
        {
            writer.write('\n');
        }
        writer.flush();
        writer.close();
    }

    /**
     * Check if a file 'filename' exists
     *
     * @param filename
     * @return
     */
    public static boolean isFileExists(String filename)
    {
        File file = new File(filename);
        return file.exists();
    }

    /**
     * Reads all lines from 'fileName' and return as list. Exclude the lines
     * commented with 'excludeStartingWith'
     *
     * @param fileName
     * @param excludeStartingWith
     * @return
     */
    public static List<String> readAllLinesFromFileAsList(String fileName, String excludeStartingWith)
    {
        if (!isFileExists(fileName))
        {
            return null;
        }

        List<String> result = new ArrayList<String>();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(new File(fileName)));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                if (!line.startsWith(excludeStartingWith))
                {
                    result.add(line);
                }
            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                reader.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Memory efficient implementation of counting number of lines in a given file
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static int getLineCountInFile(String filePath) throws IOException
    {
        int lineCount = 0;
        InputStream in = new BufferedInputStream(new FileInputStream(filePath));
        {
            byte[] buf = new byte[4096 * 16];
            int c;
            while ((c = in.read(buf)) > 0)
            {
                for (int i = 0; i < c; i++)
                {
                    if (buf[i] == '\n') lineCount++;
                }
            }
        }
        return lineCount;
    }

    /**
     * @param string   - to be searched string
     * @param filePath - in which file
     * @return - non zero, non negative line number if found, else -1
     * @throws FileNotFoundException
     */
    public static int fileContainsLine(String string, String filePath) throws FileNotFoundException
    {
        File file = new File(filePath);
        if (!file.exists())
        {
            throw new FileNotFoundException("File " + filePath + " Doesnt exist!");
        }

        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        int lineCount = 1;
        try
        {
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                if (line.compareTo(string) == 0)
                {
                    LOG.info("String - " + string + " found in " + filePath + " @line " + lineCount);
                    return lineCount;
                }
                lineCount++;
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            LOG.info("Could not read file : " + filePath);
        } finally
        {
            try
            {
                reader.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public static void main(String[] args)
    {
        LOG.info(IOUtil.readFileAsBuffer("/Users/bm3/Code/Mars/data-critik/config/createTable.sql"));
    }

    /**
     * Read a file as StringBuffer
     *
     * @param fileToRead
     * @param removeLineBreaks - If true, whole file content will be provided as a single line (with no line breaks)
     * @return
     */
    public static StringBuffer readFileAsBuffer(String fileToRead, boolean removeLineBreaks)
    {
        StringBuffer resultBuffer = new StringBuffer();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(new File(fileToRead)));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.err.println("Could not find the file : " + fileToRead);
            return null;
        }

        String line = null;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                resultBuffer.append(line);
                if (!removeLineBreaks)
                {
                    resultBuffer.append('\n');
                }
            }
        } catch (IOException e)
        {
            LOG.info(e.getMessage());
        } finally
        {
            try
            {
                reader.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return resultBuffer;
    }

    /**
     * @param fileToRead - reads this file and returns the contents as
     *                   StringBuffer
     * @return
     */
    public static StringBuffer readFileAsBuffer(String fileToRead)
    {
        return readFileAsBuffer(fileToRead, false);
    }

    /**
     * Remove duplicate lines from a file, and return the number of duplicates
     * found
     *
     * @param ipFile
     * @return
     * @throws Exception
     */
    public static int removeDuplicateLines(String ipFile) throws Exception
    {
        int duplicateCount = 0;
        BufferedReader reader = new BufferedReader(new FileReader(ipFile));
        StringBuffer toWriteBuffer = new StringBuffer();

        String line = null;
        while ((line = reader.readLine()) != null)
        {
            if (toWriteBuffer.indexOf(line.trim()) == -1)
            {
                toWriteBuffer.append(line.trim());
                toWriteBuffer.append('\n');
            } else
            {
                LOG.info("Duplicate : " + line);
                duplicateCount++;
            }
        }
        reader.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(ipFile));
        writer.write(toWriteBuffer.toString());
        writer.flush();
        writer.close();
        return duplicateCount;
    }

    public static void deleteAllFilesInDirectory(String directory) throws IOException
    {
        File dir = new File(directory);
        deleteAllFilesInDirectory(dir);
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

    /**
     * @param resourceAsStream
     * @return
     */
    public static Properties loadInputStreamIntoProperties(InputStream resourceAsStream)
    {
        Properties props = new Properties();
        try
        {
            props.load(resourceAsStream);
        } catch (FileNotFoundException e)
        {
            LOG.info("Could not find input stream : " + resourceAsStream);
            e.printStackTrace();
        } catch (IOException e)
        {
            LOG.info("Could not find input stream : " + resourceAsStream);
            e.printStackTrace();
        }
        return props;
    }

    /**
     * Loads the properties file into properties, with added functionality of
     * loading the files itself for values of the properties mentioned in the file
     * <p>
     * i.e. if file=/home/hello.properties which contains
     * key1=val1,,val2,,file::test.properties,val3
     * key2=a,,b,,c,,file::test.properties and test.properties contains - x y z..
     * <p>
     * then effecive properties returned will be - key1=val1,,val2,,x,,y,,z,,val3
     * key2=a,,b,,c,,x,,y,,z
     *
     * @param file
     * @param fileValueIndicator
     * @param valueSeparator
     * @return
     */
    public static Properties loadProperties(String file, String fileValueIndicator, String valueSeparator)
    {
        Properties props = loadFileIntoProperties(file);
        Set<Entry<Object, Object>> entrySet = props.entrySet();
        for (Entry<Object, Object> entry : entrySet)
        {
            String value = (String) entry.getValue();
            if (value.contains(fileValueIndicator))
            {
                // the value might have multiple file urls, load each of them append,
                // and remove these file urls on the go
                String[] allValues = value.split(valueSeparator);
                for (String each : allValues)
                {
                    if (each.contains(fileValueIndicator))
                    {
                        String fileToBeLoaded = each.split(fileValueIndicator)[1];
                        String newValue = getValueListFormatted(fileToBeLoaded, valueSeparator);
                        entry.setValue(value.replace(each, newValue));
                    }
                }
            }
        }
        return props;
    }

    /**
     * Generates a file at path @dataFilePath with random <br>
     * length randomly generated String.
     *
     * @param dataFilePath
     */
    public static int generateFileWithRandomContent(String dataFilePath)
    {
        Random rand = new Random();
        int random = rand.nextInt(1024 * 10);
        String randomString = StringUtil.getRandomString(random);
        try
        {
            writeFile(dataFilePath, randomString, false);
        } catch (IOException e)
        {
            e.printStackTrace();
            LOG.error(e);
        }
        return random;
    }

    /**
     * Generates a file at path @dataFilePath with random <br>
     * length randomly generated String.
     *
     * @param dataFilePath
     */
    public static long generateFileWithRandomContent(String dataFilePath, int min, int max)
    {
        Random rand = new Random();
        int random = rand.nextInt(max);
        if (random < min)
        {
            random = random + min;
        }
        return random;
    }

    /**
     * Generates a file at path @dataFilePath with requiredDataSize while writing
     * a chunk of 512kb at a time.
     *
     * @param dataFilePath
     * @param requiredDataSize
     * @param pattern
     * @return returns MD5Checksum of generate file
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String generateFileWithRandomContentOfSize(String dataFilePath, long requiredDataSize, String
            pattern) throws NoSuchAlgorithmException, IOException
    {
        if (0 == requiredDataSize)
        {
            try
            {
                IOUtil.writeFile(dataFilePath, "", false, false);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }
        long numberOfIterations = 1;
        int eachPieceSize = 1024 * 5;
        if (requiredDataSize > eachPieceSize) // if required size is greater than
        // 5kb then divide and conquer
        {
            numberOfIterations = requiredDataSize / (eachPieceSize);
        }
        long notToRoundOff = requiredDataSize - (numberOfIterations * eachPieceSize);

        if (requiredDataSize < eachPieceSize)
        {
            String randomString = StringUtil.getRandomString((int) requiredDataSize); // safe
            // cast
            try
            {
                IOUtil.writeFile(dataFilePath, randomString, true, false);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            return getMD5CheckSumForFile(dataFilePath);
        }

        String randomString = StringUtil.getRandomString(eachPieceSize, pattern);
        for (int iteration = 0; iteration < numberOfIterations; iteration++)
        {
            try
            {
                IOUtil.writeFile(dataFilePath, randomString, true, false);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        if (notToRoundOff > 0)
        {
            try
            {
                IOUtil.writeFile(dataFilePath, StringUtil.getRandomString((int) notToRoundOff), true, false);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return getMD5CheckSumForFile(dataFilePath);
    }

    /**
     * Generates a file at path @dataFilePath with requiredDataSize while writing
     * a chunk of 512kb at a time.
     *
     * @param dataFilePath
     * @return returns MD5Checksum of generate file
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String generateFileWithRandomContentOfSize(String dataFilePath, long requiredDataSize) throws
            NoSuchAlgorithmException, IOException
    {
        String pattern = "[a-z]*[A-Z]*[0-9]*[#@$%?><^&*()_+=]*";
        return generateFileWithRandomContentOfSize(dataFilePath, requiredDataSize, pattern);
    }

    /**
     * Returns MD5 checksum for the 'file'
     *
     * @param file
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getMD5CheckSumForFile(String file) throws IOException, NoSuchAlgorithmException
    {
        File dataFile = new File(file);
        return getMD5CheckSumForFile(dataFile);
    }

    /**
     * Returns MD5 checksum for the 'file'
     *
     * @param toDownload
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String getMD5CheckSumForFile(File toDownload) throws NoSuchAlgorithmException, IOException
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(toDownload);
        byte[] dataBytes = new byte[1024];

        int nread = 0;

        while ((nread = fis.read(dataBytes)) != -1)
        {
            md.update(dataBytes, 0, nread);
        }
        fis.close();

        byte[] mdbytes = md.digest();

        // convert the byte to hex format
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < mdbytes.length; i++)
        {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /*
     * Reads a file filled with values separated by new line characters into a
     * string value separated by VALUE SEPARATOR - ,,. It ignores the lines
     * starting with '#'
     */
    private static String getValueListFormatted(String fileToBeLoaded, String valueSeparator)
    {
        FileInputStream fstream = null;
        try
        {
            fstream = new FileInputStream(new File(fileToBeLoaded.trim()));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
        DataInputStream ds = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(ds));
        Pattern p;
        Matcher m;
        String strLine;
        String inputText = "";
        try
        {
            while ((strLine = br.readLine()) != null)
            {
                if (!strLine.startsWith("#"))
                {
                    inputText = inputText + strLine + "\n";
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                br.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        // p = Pattern.compile("(?m)$^|[\\n]+\\z");
        p = Pattern.compile("\n");
        m = p.matcher(inputText);
        String str = m.replaceAll(valueSeparator);
        return str;
    }

}