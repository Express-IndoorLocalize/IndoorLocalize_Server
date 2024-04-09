package util.database;

import java.sql.*;

/**
 * The class DatabaseOperation provides methods for database operations including
 * connecting to the database, disconnecting, creating tables, dropping tables,
 * inserting records, updating tables, etc.
 *
 * @author Qchrx
 * @version 1.2
 */
public class DatabaseOperation {

    private Connection connection;

    public DatabaseOperation(String driverName, String url, String user, String password) {
        connectDatabase(driverName, url, user, password);
    }

    /**
     * Connect to the database
     *
     * @param driverName Driver name
     * @param url        Uniform Resource Identifier
     * @param user       Database username
     * @param password   Database password
     */
    private void connectDatabase(String driverName, String url, String user, String password) {
        try {
            // Load the driver
            Class.forName(driverName);
            // Connect to the database
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected successfully...");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Failed to create driver...");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the database...");
        }
    }

    /**
     * Create a table
     *
     * @param tableName Table name
     * @param member    Column members of the table
     */
    public void createTable(String tableName, String member) {
        String sql = "CREATE TABLE IF NOT EXISTS  " + tableName + " ( " + member + " ); ";
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
            System.out.println("Table " + tableName + " created successfully...");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to create table " + tableName + "...");
        }
    }

    /**
     * Create a table using SQL statement
     *
     * @param sqlString SQL statement for creating the table
     */
    public void createTable(String sqlString) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sqlString);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to create table...");
        }
    }

    /**
     * Insert data into the specified table
     *
     * @param tableName Table name for inserting data
     * @param m         Object to insert
     * @param <V>       Generic type for classes that override the toString() method in the data package
     */
    public <V> void updateTable(String tableName, V m) {
        try {
            Statement statement = connection.createStatement();
            String sql = "INSERT INTO " + tableName + " VALUES " + "(" + m.toString() + ")";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to insert record..." + e.getMessage());
        }
    }

    /**
     * Insert record directly using SQL statement
     *
     * @param insertStr SQL statement for inserting data
     */
    public void updateTable(String insertStr) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(insertStr);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to insert record..." + e.getMessage());
        }
    }

    /**
     * Delete all records from the table
     *
     * @param tableName Table name
     * @throws SQLException Thrown for SQL exceptions
     */
    public void deleteAllRecords(String tableName) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "DELETE FROM " + tableName;
        statement.executeUpdate(sql);
    }

    /**
     * Delete the specified table
     *
     * @param tableName Table name
     * @throws SQLException Thrown for SQL exceptions
     */
    public void deleteTable(String tableName) throws SQLException {
        Statement statement = connection.createStatement();
        String sqlString = "DROP TABLE IF EXISTS " + tableName;
        statement.executeUpdate(sqlString);
    }

    /**
     * Disconnect from the database
     */
    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to disconnect..." + e.getMessage());
        }
    }

    /**
     * Return the ID of the last record in the table, or 0 if there are no records in the table
     *
     * @param tableName Table name
     * @return ID of the last record in the table
     */
    @SuppressWarnings("finally")
	public int checkTableLastID(String tableName) {
        int id = 0;
        String sql = "SELECT count(*) AS maxid FROM " + tableName;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                id = rs.getInt("maxid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("checkTableLastID error:" + e.getMessage());
        } finally {
            return id;
        }
    }

    /**
     * Return the connection to the database held by the class
     *
     * @return Connection to the database
     */
    public Connection getConnection() {
        return connection;
    }
}
