package util.database;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Export data from the database
 */
public class DataOutput {
    private DatabaseOperation databaseOperation;

    public DataOutput(DatabaseOperation databaseOperation) {
        this.databaseOperation = databaseOperation;
    }

    /**
     * Export acceleration data from the specified table
     *
     * @param tableName    Table name of the acceleration data
     * @param absolutePath Absolute path of the output file
     */
    public void outputAccData(String tableName, String absolutePath) {
        String rssid;
        double accX, accY, accZ, accTotal;
        Connection connection = databaseOperation.getConnection();
        String sqlString = "SELECT * FROM " + tableName;
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sqlString);
            FileWriter fw = new FileWriter(absolutePath);
            while (rs.next()) {
                rssid = rs.getString("rssid");
                accX = rs.getFloat("X");
                accY = rs.getFloat("Y");
                accZ = rs.getFloat("level");
                fw.write(rssid + ",");
                fw.write(accX + ",");
                fw.write(accY + ",");
                fw.write(accZ + ",");
                fw.write("\r\n");
                fw.flush();
            }
            fw.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query failed: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error exporting data to file: " + e.getMessage());
        }
    }

    /**
     * Export WiFi fingerprint data
     *
     * @param tableName    Table name of the WiFi fingerprint data
     * @param absolutePath Absolute path of the output file
     */
    public void outputWifiFinger(String tableName, String absolutePath) {
        float posX, posY;
        float[] level = new float[6];
        String sqlString = "SELECT * FROM " + tableName + " ORDER BY id";
        Connection connection = databaseOperation.getConnection();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sqlString);
            FileWriter fw = new FileWriter(absolutePath);
            while (rs.next()) {
                posX = rs.getFloat("x");
                posY = rs.getFloat("y");
                for (int i = 0; i < level.length; i++) {
                    level[i] = rs.getFloat("level" + (i + 1));
                }
                fw.write(posX + ",");
                fw.write(posY + ",");
                for (float aLevel : level) {
                    fw.write(aLevel + ",");
                }
                fw.write("\r\n");
                fw.flush();
            }
            fw.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query failed: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error exporting data to file: " + e.getMessage());
        }
    }
}
