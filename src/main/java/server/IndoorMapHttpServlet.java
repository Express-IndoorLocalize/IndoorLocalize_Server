package server;

import data.ClientPos;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import util.database.DatabaseOperation;
import util.file.FileOperation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

/**
 * Determines whether the client is indoors or outdoors and serves indoor maps.
 *
 * @author Qchrx
 * @version 1.1
 */
public class IndoorMapHttpServlet extends HttpServlet {

    private DatabaseOperation mDatabaseOperation;   // Database operation class
    private final DiskFileItemFactory factory = new DiskFileItemFactory();    // Create a DiskFileItemFactory factory
    private final ServletFileUpload upload = new ServletFileUpload(factory);     // Create a file upload parser

    @Override
    public void init() throws ServletException {
        super.init();
        String driverName = "org.postgresql.Driver";    // Driver name
        String url = "jdbc:postgresql://127.0.0.1:5432/indoor_map_para";
        String user = "postgres";
        String password = "c724797";
        mDatabaseOperation = new DatabaseOperation(driverName, url, user, password);
        // Solve the problem of Chinese garbled characters in the uploaded file names
        upload.setHeaderEncoding("UTF-8");
        System.out.println("Server location judgment module loaded successfully");
        System.out.println("Server indoor map management module loaded successfully");
    }

    /**
     * Responds to client's GET request (requesting indoor maps).
     *
     * @param req  request
     * @param resp response
     * @throws ServletException ServletException
     * @throws IOException      IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        double longitude = Double.valueOf(req.getHeader("longitude"));
        double latitude = Double.valueOf(req.getHeader("latitude"));
        int floor = Integer.valueOf(req.getHeader("floor"));
        try {
            String mapPath = getIndoorMapPath(longitude, latitude, floor);  // Get the path where the indoor map is stored
            String indoorMapXmlStr;
            if (mapPath.length() > 0) { // Traverse the map file, get all the information in the file, and store it in the indoorMapXml string
                List<String> indoorMapXmlList = FileOperation.readTextByLine(mapPath);
                StringBuilder indoorMapXmlStrBuilder = new StringBuilder();
                for (String indoorMapXml : indoorMapXmlList) {
                    indoorMapXmlStrBuilder.append(indoorMapXml).append("\n");
                }
                indoorMapXmlStr = indoorMapXmlStrBuilder.toString();
            } else {    // If the current area does not have an indoor map, return an empty string
                indoorMapXmlStr = "";
            }
            /*
              Write the indoor map string into the output stream and return it to the client
             */
            OutputStream outputStream = resp.getOutputStream();
            outputStream.write(indoorMapXmlStr.getBytes());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to get indoor map");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ClientPos clientPos = new ClientPos();
        // Check if the submitted data is an upload form data
        if (!ServletFileUpload.isMultipartContent(req)) {
            // Get data in traditional way
            return;
        }
        /*
          Use ServletFileUpload parser to parse uploaded data,
          The parsing result returns a List<FileItem> collection, where each FileItem corresponds to an input item in the form
         */
        List<FileItem> list = null;
        try {
            list = upload.parseRequest(req);
        } catch (FileUploadException e) {
            e.printStackTrace();
            System.out.println("Error parsing uploaded data" + e.getMessage());
        }
        for (FileItem item : Objects.requireNonNull(list)) {
            // If the FileItem encapsulates ordinary input data
            if (item.isFormField()) {
                String name = item.getFieldName();
                // Solve the problem of Chinese garbled characters in ordinary input data
                String value = item.getString("UTF-8");
                value = new String(value.getBytes("iso8859-1"), "UTF-8");
                switch (name) {
                    case "status":
                        boolean isIndoor = Boolean.valueOf(value);
                        clientPos.setIsIndoor(isIndoor);
                        break;
                    case "longitude":
                        double longitude = Double.valueOf(value);
                        clientPos.setLongitude(longitude);
                        break;
                    case "latitude":
                        double latitude = Double.valueOf(value);
                        clientPos.setLatitude(latitude);
                        break;
                    default:
                        break;
                }
            }
        }
        try {
            boolean isNowIndoor = judgeIndoor(clientPos);   // Whether the user is indoors
            boolean isLastIndoor = clientPos.getIsIndoor(); // Whether the user was indoors at the last moment
            OutputStream outputStream = resp.getOutputStream();
            byte statusCode;
            if (!isNowIndoor && !isLastIndoor) {    // The user is outdoors
                statusCode = 2;
                outputStream.write(statusCode);
                outputStream.close();
            } else if (isNowIndoor && isLastIndoor) {   // The user is indoors
                statusCode = 3;
                outputStream.write(statusCode);
                outputStream.close();
            } else if (!isNowIndoor) {  // The user moved from indoors to outdoors
                statusCode = 1;
                outputStream.write(statusCode);
                outputStream.close();
            } else {    // The user moved from outdoors to indoors
                statusCode = 0;
                outputStream.write(statusCode);
                outputStream.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Location judgment failed" + e.getMessage());
        }
    }


    /**
     * Determine whether the user is indoors or outdoors based on the latitude and longitude data uploaded by the user.
     *
     * @param clientPos User's location
     * @return Whether the user is indoors
     */
    private boolean judgeIndoor(ClientPos clientPos) throws SQLException {
        double longitude = clientPos.getLongitude();
        double latitude = clientPos.getLatitude();
        String sqlStr = "SELECT * FROM indoor_map WHERE (ST_Within(ST_GeomFromText(" +
                "'POINT( " + longitude + " " + latitude + " )',4326),geom_area))";
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlStr);
        rs.next();
        return rs.next();
    }

    /**
     * Get the path of the map XML file.
     *
     * @param longitude Longitude
     * @param latitude  Latitude
     * @param floor     Requested floor of the indoor map
     * @return Map XML file path
     * @throws SQLException SQLException
     */
    private String getIndoorMapPath(double longitude, double latitude, int floor) throws SQLException {
        String sqlStr = "SELECT xml_path,floor FROM (" + "ST_GeomFromText( 'POINT("
                + longitude + " " + latitude + ")',4326) as client "
                + " JOIN (SELECT geom_area,xml_path,floor FROM indoor_map) as tmp "
                + " ON ST_WITHIN(client,geom_area)) " + "WHERE floor = " + floor;
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlStr);
        if (rs.next()) {
            return rs.getString("xml_path");
        } else {
            return "";
        }
    }
}
