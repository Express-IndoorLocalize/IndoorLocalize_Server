package server;

import algorithm.PositionAlgorithm;
import olddata.SpatialPosition;
import olddata.WifiFingerBssidContainer;
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
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IndoorLocateHttpServlet extends HttpServlet {
    private DatabaseOperation mDatabaseOperation;   // Database operation class
    private PositionAlgorithm mPositionAlgorithm;   // Indoor positioning algorithm class
    private final DiskFileItemFactory factory = new DiskFileItemFactory();    // Create a DiskFileItemFactory factory
    private final ServletFileUpload upload = new ServletFileUpload(factory);     // Create a file upload parser
    
    @Override
    public void init() throws ServletException {
        super.init();
        String driverName = "org.postgresql.Driver";    // Driver name
        String url = "jdbc:postgresql://127.0.0.1:5432/indoor_location_data";
        String user = "postgres";
        String password = "postgres";
        mDatabaseOperation = new DatabaseOperation(driverName, url, user, password);
        System.out.println("Inside DB SETUP Method");
        WifiFingerBssidContainer wifiFingerBssidContainer = new WifiFingerBssidContainer(
                "40:e3:d6:76:44:33", "40:e3:d6:76:44:30", "40:e3:d6:76:43:a3",
                "94:b4:0f:cc:89:b1", "40:e3:d6:76:44:03");
        mPositionAlgorithm = new PositionAlgorithm(mDatabaseOperation, wifiFingerBssidContainer);
        // Solve the problem of Chinese garbled characters in the uploaded file names
        upload.setHeaderEncoding("UTF-8");
        System.out.println("Indoor positioning module loaded successfully on the server");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	System.out.println("Inside Get Method");
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	System.out.println("Inside post");
        // Check if the submitted data is an upload form data
        if (!ServletFileUpload.isMultipartContent(req)) {
        	System.out.println("TRYINGG22");
            // Get data in traditional way
            return;
        }
        /*
          Use ServletFileUpload parser to parse uploaded data,
          The parsing result returns a List<FileItem> collection, where each FileItem corresponds to an input item in the form
         */
        List<FileItem> list = null;
        try {
        	System.out.println("TRYINGG");
            list = upload.parseRequest(req);
        } catch (FileUploadException e) {
            e.printStackTrace();
            System.out.println("Error parsing uploaded data" + e.getMessage());
        }
        for (FileItem item : Objects.requireNonNull(list)) {
            // If the FileItem encapsulates ordinary input data
            if (item.isFormField()) {
                // Solve the problem of Chinese garbled characters in ordinary input data
                String value = item.getString("UTF-8");
                value = new String(value.getBytes("iso8859-1"), "UTF-8");
                String[] wifiStrArr = value.split("\n");

                List<String> bssidList = new ArrayList<>();
                List<Float> levelList = new ArrayList<>();
                for (String wifiStr : wifiStrArr) {   // Use readLine method to read one line at a time
                    String[] tempString = wifiStr.split(",");
                    bssidList.add(tempString[0]);
                    levelList.add(Float.valueOf(tempString[1]));
                }

                try {
                    SpatialPosition spatialPosition = mPositionAlgorithm.wknnLocate
                            (bssidList, levelList, 3, PositionAlgorithm.W_K_NN);
                    System.out.println(spatialPosition);
                    OutputStream outputStream = resp.getOutputStream();
                    outputStream.write(spatialPosition.toString().getBytes());
                    outputStream.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println("Wifi positioning error" + e.getMessage());
                }
            }
        }
    }
}
