package com.javasbar.framework.lib.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class SQLLiteDBHelper
{
    public static void main(String[] args) throws ClassNotFoundException
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

    public static void _main(String[] args) throws FileNotFoundException, ParseException, SQLException, ClassNotFoundException{
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:mydb.db");
            stmt = c.createStatement();
            String drop_sql = "DROP TABLE IF EXISTS MyTable";
            stmt.executeUpdate(drop_sql);
            String create_sql = "CREATE TABLE MyTable " +
                    "(VAR1     CHAR(50) NOT NULL, " +
                    "VAR2 CHAR(10) PRIMARY KEY NOT NULL," +
                    " VAR3   TEXT   NOT NULL, " +
                    " VAR4      TEXT   NOT NULL )";

            stmt.executeUpdate(create_sql);
            File premFile = new File("MyFile.csv");
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            Scanner scanner = new Scanner(premFile);
            scanner.useDelimiter(",");
            int i = 0, count = 500000;

            while (i < count){
                String myRecord = scanner.nextLine();
                String[] cols = myRecord.split(",");
                String var1 = cols[0];
                String var2 = cols[1];
                Date var3 = df.parse(cols[2]);
                Date var4 = df.parse(cols[3]);

                String query = "INSERT INTO MyTable VALUES (" +
                        "'" + var1 + "', " +
                        "'" + var2 + "', " +
                        "'" + var3 +  "', " +
                        "'" + var4 + "')";
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
