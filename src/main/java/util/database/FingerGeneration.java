package util.database;

import olddata.WifiFingerBssidContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * FingerGeneration class trains the indoor fingerprint library for positioning based on offline collected magnetic field and WiFi data.
 *
 * @author Qchrx
 * @version 1.0
 */
public class FingerGeneration {
    private DatabaseOperation databaseOperation;

    public FingerGeneration(DatabaseOperation databaseOperation) {
        this.databaseOperation = databaseOperation;
    }

    /**
     * generate wifi fingerprint library using collected Wifi data
     * The generated fingerprint library is used for traditional positioning algorithms such as NN and K-NN.
     *
     * @param wifiFingerBssidContainer Container for RSSID sequence of AP sources used for WiFi positioning
     */
    public void generateWifiFingerPrintLibrary(WifiFingerBssidContainer wifiFingerBssidContainer) {
        try {
            databaseOperation.deleteTable("testWifiResult");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("删除表wifiFinger失败");
        }
        List<String> wifiFingerBssidList = wifiFingerBssidContainer.getWifiFingerBssidList();
        StringBuilder sqlStringBuilder = new StringBuilder("CREATE TABLE testWifiResult AS " +
                "(SELECT rssid,x,y,avg(level) as level FROM testwifi WHERE ");
        for (int i = 0; i < wifiFingerBssidList.size(); i++) {
            if (i == wifiFingerBssidList.size() - 1)
                sqlStringBuilder.append("rssid= ").append("'").append(wifiFingerBssidList.get(i)).
                        append("' ").append("GROUP BY x,y,rssid)");
            else
                sqlStringBuilder.append("rssid= ").append("'").append(wifiFingerBssidList.get(i)).
                        append("' ").append("OR ");
        }
        String sqlString = sqlStringBuilder.toString();
        databaseOperation.createTable(sqlString);
    }

    /**
     * generate wifi data collected in time series
     * Used to test the training effect of neural networks
     *
     * @param wifiFingerBssidContainer Container for RSSID sequence of AP sources used for WiFi positioning
     */

    public void generateTestWifiData(WifiFingerBssidContainer wifiFingerBssidContainer) {
        List<String> wifiFingerBssidList = wifiFingerBssidContainer.getWifiFingerBssidList();

        try {
            databaseOperation.deleteTable("tempTimeWifiResult");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("删除表timeWifiResult失败");
        }
     // Generate intermediate table tempTimeWifiResult
        String sqlString = "CREATE TABLE tempTimeWifiResult AS \n" +
                " (SELECT *  FROM testwifi where x = 0 and y = 2 and ( rssid = '6c:f3:7f:bc:1e:22' \n" +
                "\tor rssid = '6c:f3:7f:bc:1e:20' or rssid = '6c:f3:7f:bc:1e:30' or rssid = '6c:f3:7f:bc:1e:32' \n" +
                "   or rssid = '6c:f3:7f:bc:1e:21'  or rssid = '6c:f3:7f:bc:1e:31') order by id) ";
        databaseOperation.createTable(sqlString);

        try {
            databaseOperation.deleteTable("timeWifi");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("删除表timeWifi失败");
        }
        StringBuilder initTableString = new StringBuilder
                ("CREATE TABLE timeWifi ( " + "id Integer PRIMARY KEY, " + "x  Integer, " + "y Integer, ");
        for (int i = 0; i < wifiFingerBssidList.size(); i++) {
            if (i == wifiFingerBssidList.size() - 1) {
                initTableString.append("level").append(i + 1).append(" ").append("numeric);");
            } else {
                initTableString.append("level").append(i + 1).append(" ").append("numeric ").append(", ");
            }
        }
        sqlString = initTableString.toString();
        databaseOperation.createTable(sqlString);
     // Initialize the contents of the timeWifi table
        for (int i = 0; i < 25; i++) {
            String insertStr = "INSERT INTO timeWifi VALUES( " + (i + 1) + "," + 0 + ", " + 2 + ", " +
                    "0,0,0,0,0,0)";
            databaseOperation.updateTable(insertStr);
        }
     // Update records in the table
        for (int i = 0; i < 25; i++) {
            for (int j = 0; j < wifiFingerBssidList.size(); j++) {
                sqlString = "UPDATE timeWifi SET level" + (j + 1) + "= " +
                        "(SELECT level FROM (SELECT * FROM temptimewifiresult limit 6 " +
                        " offset " + (6 * i) + " ) as a where rssid = '" + wifiFingerBssidList.get(j) + "') " +
                        "where id =  " + (i + 1);
                databaseOperation.updateTable(sqlString);
            }
        }
    }


    /**
     * generate the final fingerprint library for magnetic field and WiFi, used for positioning
     *
     * @param xMin                     Minimum value of x-coordinate for the area
     * @param yMin                     Minimum value of y-coordinate for the area
     * @param xMax                     Maximum value of x-coordinate for the area
     * @param yMax                     Maximum value of y-coordinate for the area
     * @param wifiFingerBssidContainer Container for RSSID sequence of AP sources used for WiFi positioning
     */

    public void generateFinalFingerPrintLibrary(int xMin, int yMin, int xMax, int yMax,
                                                WifiFingerBssidContainer wifiFingerBssidContainer) {
        try {
            databaseOperation.deleteTable("finalFinger");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to delete table finalFinger");
        }
     // Create table finalFinger
        StringBuilder initTableString = new StringBuilder
                ("CREATE TABLE finalFinger ( " + "x  Integer, " + "y Integer, ");
        List<String> wifiFingerBssidList = wifiFingerBssidContainer.getWifiFingerBssidList();
        for (int i = 0; i < wifiFingerBssidList.size(); i++) {
            if (i == wifiFingerBssidList.size() - 1) {
                initTableString.append("level").append(i + 1).append(" ").append("real);");

            } else {
                initTableString.append("level").append(i + 1).append(" ").append("real ").append(", ");
            }
        }
        // initTableString.append("mag").append(" ").append("numeric);");
        String sqlString = initTableString.toString();
        databaseOperation.createTable(sqlString);
     // Initialize the content of the table
        for (int i = xMin; i <= xMax; i++) {
            for (int j = yMin; j <= yMax; j++) {
                String insertStr = "INSERT INTO finalFinger VALUES( " + i + ", " + j + ", " +
                        "0,0,0,0,0,0)";
                databaseOperation.updateTable(insertStr);
            }
        }
        // Update the records in FinalFingerPrint based on the generated WiFi fingerprint library and magnetic field fingerprint library
        for (int i = xMin; i <= xMax; i++) {
            for (int j = yMin; j <= yMax; j++) {
                for (int k = 0; k < wifiFingerBssidList.size(); k++) {
//                    if (k < wifiFingerBssidList.size()) {
                    String updateStr = "UPDATE finalFinger SET level" + (k + 1) + "= " +
                            " (SELECT level FROM wififinger WHERE x= " + i + " and y= " + j +
                            " and rssid = " + "'" + wifiFingerBssidList.get(k) + "'" + " ) " +
                            " WHERE x= " + i + " and " + " y= " + j;
                    databaseOperation.updateTable(updateStr);
//                    } else {
//                        String updateStr = "UPDATE finalFinger SET mag= " + (i + j) +
//                                " WHERE x= " + i + " and " + " y= " + j;
////                        String updateStr = "UPDATE finalFinger SET mag= " +
////                                " (SELECT level FROM magFinger WHERE x= " + i + " and " + " y= " + j + " ) " +
////                                " WHERE x= " + i + " and " + " y= " + j;
//                        databaseOperation.updateTable(updateStr);
//                    }
                }
            }
        }
    }


    /**
     * generate wifi data for neural network training
     *
     * @param wifiFingerBssidContainer Container for RSSID sequence of AP sources used for WiFi positioning
     */

    public void generateNNTrainData(WifiFingerBssidContainer wifiFingerBssidContainer) {
        List<String> wifiFingerBssidList = wifiFingerBssidContainer.getWifiFingerBssidList();
        // Create table TempNNRssidWifi, which contains a subset of wifiData, only including data for 6 AP sources used for positioning
        try {
            databaseOperation.deleteTable("TempNNRssidWifi");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to delete table TempNNRssidWifi");
        }
        StringBuilder sqlStringBuilder = new StringBuilder("CREATE TABLE TempNNRssidWifi AS " +
                "(SELECT * FROM wifiData WHERE ");
        for (int i = 0; i < wifiFingerBssidList.size(); i++) {
            if (i == wifiFingerBssidList.size() - 1) {
                sqlStringBuilder.append("RSSID = ").append("'").append(wifiFingerBssidList.get(i)).append("' ").append(" ) ");
            } else {
                sqlStringBuilder.append("RSSID = ").append("'").append(wifiFingerBssidList.get(i)).append("' ").append(" or ");
            }
        }
        databaseOperation.createTable(sqlStringBuilder.toString());

        // Write data from table TempNNRssidWifi into Lists
        List<Float> xPosList = new ArrayList<>();
        List<Float> yPosList = new ArrayList<>();
        List<Float> levelList = new ArrayList<>();
        String sqlString = "SELECT * FROM tempnnrssidwifi";
        // Compile SQL statement
        // Execute query
        try {
            Statement stmt = databaseOperation.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sqlString);
            // Iterate through results
            while (rs.next()) {
                xPosList.add(rs.getFloat("x"));
                yPosList.add(rs.getFloat("y"));
                levelList.add(rs.getFloat("level"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query failed: " + e.getMessage());
        }

        // Create table NNRssidWifi
        try {
            databaseOperation.deleteTable("NNRssidWifi");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to delete table NNRssidWifi");
        }
        sqlStringBuilder = new StringBuilder
                ("CREATE TABLE NNRssidWifi ( " + "id Integer PRIMARY KEY, " + "x  Integer, " + "y Integer, ");
        for (int i = 0; i < wifiFingerBssidList.size(); i++) {
            if (i == wifiFingerBssidList.size() - 1) {
                sqlStringBuilder.append("level").append(i + 1).append(" ").append("numeric);");
            } else {
                sqlStringBuilder.append("level").append(i + 1).append(" ").append("numeric ").append(", ");
            }
        }
        databaseOperation.createTable(sqlStringBuilder.toString());

        for (int i = 0; i < 1461; i++) {
            sqlStringBuilder = new StringBuilder("INSERT INTO NNRssidWifi VALUES ( " + (i + 1));
            sqlStringBuilder.append(" , ").append(xPosList.get(6 * i)).append(" , ").append(yPosList.get(6 * i)).append(" , ");
            for (int j = 0; j < 6; j++) {
                if (j == 5) {
                    sqlStringBuilder.append(levelList.get(6 * i + j)).append(" ) ");
                } else {
                    sqlStringBuilder.append(levelList.get(6 * i + j)).append(" , ");
                }
            }
            databaseOperation.updateTable(sqlStringBuilder.toString());
        }
    }

    /**
     * Supplement missing AP sources in the fingerprint library
     *
     * @param wifiFingerBssidContainer Container for RSSID sequence of AP sources used for WiFi positioning
     */
    public void supplementFinger(WifiFingerBssidContainer wifiFingerBssidContainer) throws SQLException {
        List<String> wifiFingerBssidList = wifiFingerBssidContainer.getWifiFingerBssidList();
        Statement stmt = databaseOperation.getConnection().createStatement();
        for (int i = 6; i <= 6; i = i + 2) {
            for (int j = 4; j <= 4; j = j + 2) {
                for (String wifiFingerBssid : wifiFingerBssidList) {
                    String sqlStr = "SELECT * FROM avg_init_finger WHERE rssid = " + "'" + wifiFingerBssid + "'" +
                            " and x_pos = " + i + " and y_pos = " + j;
                    ResultSet rs = stmt.executeQuery(sqlStr);
                    if (!rs.next()) {    // If the AP source does not exist
                        String updateStr = "INSERT INTO avg_init_finger VALUES ( " + "'" + wifiFingerBssid + "'" +
                                " , " + i + " , " + j + " , " + " -100 )";
                        databaseOperation.updateTable(updateStr);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        String driverName = "org.postgresql.Driver";    // Driver name
        String url = "jdbc:postgresql://127.0.0.1:5432/indoor_location_data";
        String user = "postgres";
        String password = "c724797";
        DatabaseOperation dbo = new DatabaseOperation(driverName, url, user, password);
        WifiFingerBssidContainer wfbc = new WifiFingerBssidContainer("40:e3:d6:76:44:33",
                "40:e3:d6:76:44:30", "40:e3:d6:76:43:a3", "94:b4:0f:cc:89:b1", "40:e3:d6:76:44:03");
        FingerGeneration fg = new FingerGeneration(dbo);
        try {
            fg.supplementFinger(wfbc);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
