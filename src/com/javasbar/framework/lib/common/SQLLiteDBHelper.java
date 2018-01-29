package com.javasbar.framework.lib.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class SQLLiteDBHelper
{
    public static void __main(String[] args) throws ClassNotFoundException
    {
        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");

        Connection connection = null;
        try
        {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");
            ResultSet rs = statement.executeQuery("select * from person");
            while(rs.next())
            {
                // read the result set
                System.out.println("name = " + rs.getString("name"));
                System.out.println("id = " + rs.getInt("id"));
            }
        }
        catch(SQLException e)
        {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        }
        finally
        {
            try
            {
                if(connection != null)
                    connection.close();
            }
            catch(SQLException e)
            {
                // connection close failed.
                System.err.println(e);
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, ParseException, SQLException, ClassNotFoundException{
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:mydb.db");
            stmt = c.createStatement();
            String drop_sql = "DROP TABLE IF EXISTS MyTable";
            stmt.executeUpdate(drop_sql);
            String create_sql = "CREATE TABLE MyTable " +
                   "(" +
                    "BuildNo VARCHAR NOT NULL," +
                    " totalRunTCcount VARCHAR NOT NULL," +
                    " totalRunTestMethodCount VARCHAR NOT NULL," +
                    " totalPassedMethods VARCHAR NOT NULL," +
                    " totalFailedMethods VARCHAR NOT NULL," +
                    " totalSkippedMehtods VARCHAR NOT NULL," +
                    " totalPassedCount VARCHAR NOT NULL," +
                    " totalFailedCount VARCHAR NOT NULL," +
                    " totalSkippedCount VARCHAR NOT NULL," +
                    " redDeltalCount VARCHAR NOT NULL," +
                    " greenDeltaCount VARCHAR NOT NULL," +
                    " redMethodDeltaCount VARCHAR NOT NULL," +
                    " greenMethodDeltaCount VARCHAR NOT NULL" +
                    " )";

            stmt.executeUpdate(create_sql);
            File premFile = new File("sample.csv");
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            Scanner scanner = new Scanner(premFile);
            scanner.useDelimiter(",");
            int i = 0, count = 500000;

            while (i < count){
                String myRecord = scanner.nextLine();
                String[] cols = myRecord.split(",");

                String query = "INSERT INTO MyTable VALUES (" +
                        "'" + cols[0] + "', " +
                        "'" + cols[1] + "', " +
                        "'" + cols[2] +  "', " +
                        "'" + cols[3] +  "', " +
                        "'" + cols[4] +  "', " +
                        "'" + cols[5] +  "', " +
                        "'" + cols[6] +  "', " +
                        "'" + cols[7] +  "', " +
                        "'" + cols[8] +  "', " +
                        "'" + cols[9] +  "', " +
                        "'" + cols[10] +  "', " +
                        "'" + cols[11] +  "', " +
                        "'" + cols[12] + "')";
                stmt.addBatch(query);
                i++;
            }
            stmt.executeBatch();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }
}
