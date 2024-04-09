package util.file;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The FileOperation class is a utility class that provides some static methods for file operations.
 *
 * @author Qchrx
 * @version 1.0
 */
public class FileOperation {

    /**
     * Delete a file or directory.
     *
     * @param fileName The name of the file to be deleted.
     * @return True if deleted successfully, false otherwise.
     */
    public static boolean deleteMnyFileOrDir(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile())
                return deleteFile(fileName);
            else
                return deleteDirectory(fileName);
        }
    }

    /**
     * Delete a single file.
     *
     * @param filePath The file path of the file to be deleted.
     * @return True if the file is deleted successfully, false otherwise.
     */
    private static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        // If the file exists and is a file, delete it directly
        return file.exists() && file.isFile() && file.delete();
    }

    /**
     * Delete a directory and all files under it.
     *
     * @param dirPath The directory path to be deleted.
     * @return True if the directory is deleted successfully, false otherwise.
     */
    private static boolean deleteDirectory(String dirPath) {
        // If dir does not end with a file separator, automatically add a file separator
        if (!dirPath.endsWith(File.separator))
            dirPath = dirPath + File.separator;
        File dirFile = new File(dirPath);
        // If the dir file does not exist, or is not a directory, exit
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            return false;
        }
        boolean flag = true;
        // Delete all files in the folder including subdirectories
        File[] files = dirFile.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            // Delete child files
            if (file.isFile()) {
                flag = FileOperation.deleteFile(file.getAbsolutePath());
                if (!flag)
                    break;
            }
            // Delete child directories
            else if (file.isDirectory()) {
                flag = FileOperation.deleteDirectory(file.getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag) {
            return false;
        }
        // Delete the current directory
        return dirFile.delete();
    }

    /**
     * Read all content from a text file line by line and store each line as an element in a List.
     *
     * @param filePath The path of the text file.
     * @return A List<String> where each element represents a line from the text file.
     * @throws IOException Thrown to indicate an error in reading from the file.
     */
    public static List<String> readTextByLine(String filePath) throws IOException {
        List<String> postureList = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));   // Construct a BufferedReader to read the file
        String s;
        while ((s = br.readLine()) != null) {   // Use the readLine method to read one line at a time
            postureList.add(s);
        }
        br.close();
        return postureList;
    }

    /**
     * Check if a file exists given its path.
     *
     * @param filePath The file path.
     * @return True if the file exists, false otherwise.
     */
    public static boolean judeFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
}
