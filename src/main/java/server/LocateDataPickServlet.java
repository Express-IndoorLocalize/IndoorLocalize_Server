package server;

import data.MagData;
import data.WifiData;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import util.database.DatabaseOperation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Server-side data collection servlet to store data collected by clients in the database.
 *
 * @author Qchrx
 * @version 1.1
 */
public class LocateDataPickServlet extends HttpServlet {

    private DatabaseOperation mDatabaseOperation;   // Database operation class
    private String magTableName = "mag_data";
    private String wifiTableName = "wifi_data";
    private final DiskFileItemFactory factory = new DiskFileItemFactory();  // Create a DiskFileItemFactory factory
    private final ServletFileUpload upload = new ServletFileUpload(factory);      // Create a file upload parser

    @Override
    public void init() throws ServletException {
        String driverName = "org.postgresql.Driver";    // Driver name
        String url = "jdbc:postgresql://127.0.0.1:5432/indoor_location_data";
        String user = "postgres";
        String password = "c724797";
        mDatabaseOperation = new DatabaseOperation(driverName, url, user, password);
        // Solve the problem of Chinese garbled characters in the uploaded file names
        upload.setHeaderEncoding("UTF-8");
        System.out.println("Server data collection module loaded successfully");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<FileItem> list = null;
        try {
            list = upload.parseRequest(req);
        } catch (FileUploadException e) {
            e.printStackTrace();
            System.out.println("Error parsing uploaded data" + e.getMessage());
        }
        Map<String, String> paraMap = new HashMap<>();
        for (FileItem fileItem : Objects.requireNonNull(list)) {
            if (fileItem.isFormField()) {
                paraMap.put(fileItem.getFieldName(), fileItem.getString("utf-8"));
            }
        }
        String type = paraMap.get("type");
        int id;
        String imei = paraMap.get("imei");
        float xPos = Float.valueOf(paraMap.get("x_pos"));
        float yPos = Float.valueOf(paraMap.get("y_pos"));
        float ori = Float.valueOf(paraMap.get("ori"));
        switch (type) {
            case "mag":
                id = mDatabaseOperation.checkTableLastID(magTableName);
                double magLevel = Double.valueOf(paraMap.get("mag_level"));
                MagData magData = new MagData(++id, imei, xPos, yPos, ori, magLevel);
                mDatabaseOperation.updateTable(magTableName, magData);
                break;
            case "wifi":
                id = mDatabaseOperation.checkTableLastID(wifiTableName);
                String ssid = paraMap.get("ssid");
                String bssid = paraMap.get("bssid");
                int wifiLevel = Integer.valueOf(paraMap.get("wifi_level"));
                id++;
                WifiData wifiData = new WifiData(id, imei, xPos, yPos, ori, ssid, bssid, wifiLevel);
                mDatabaseOperation.updateTable(wifiTableName, wifiData);
                break;
            default:
                break;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mDatabaseOperation.closeConnection();
    }
}
