package util.file;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

public class EncryptFileOperation {
    /**
     * Encrypts a file using the DES algorithm.
     *
     * @param filePath     The path of the file to be encrypted.
     * @param destFilePath The destination file path for the encrypted file.
     * @param keyStr       The encryption key.
     * @return True if the file is encrypted successfully, false otherwise.
     */
    public static boolean encryptFile(String filePath, String destFilePath, String keyStr)
            throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        if (!FileOperation.judeFileExists(filePath)) {
            return false;
        }

        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec("12345678".getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, getKey(keyStr), paramSpec);

        InputStream is = new FileInputStream(filePath);
        OutputStream out = new FileOutputStream(destFilePath);
        CipherInputStream cis = new CipherInputStream(is, cipher);
        byte[] buffer = new byte[1024];
        int r;
        while ((r = cis.read(buffer)) > 0) {
            out.write(buffer, 0, r);
        }
        cis.close();
        is.close();
        out.close();
        return true;
    }

    /**
     * Decrypts a file using the DES algorithm.
     *
     * @param filePath     The path of the encrypted file.
     * @param destFilePath The destination file path for the decrypted file.
     * @param keyStr       The decryption key.
     * @return True if the file is decrypted successfully, false otherwise.
     */
    public static boolean decryptFile(String filePath, String destFilePath, String keyStr)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IOException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        if (!FileOperation.judeFileExists(filePath)) {
            return false;
        }
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec("12345678".getBytes());
        cipher.init(Cipher.DECRYPT_MODE, getKey(keyStr), paramSpec);

        InputStream is = new FileInputStream(filePath);
        OutputStream out = new FileOutputStream(destFilePath);
        CipherOutputStream cos = new CipherOutputStream(out, cipher);
        byte[] buffer = new byte[1024];
        int r;
        while ((r = is.read(buffer)) >= 0) {
            cos.write(buffer, 0, r);
        }
        cos.close();
        out.close();
        is.close();
        return true;
    }

    /**
     * Generates a key based on the specified parameter.
     *
     * @param keyStr The key parameter.
     * @return The generated key.
     */
    private static Key getKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        // Check if the Key is correct
        if (keyStr == null) {
            System.out.print("Key is null");
            return null;
        }
        // Check if the Key is 16 characters long
        if (keyStr.length() != 16) {
            System.out.print("Key length is not 16 characters");
            return null;
        }
        DESKeySpec dks = new DESKeySpec(keyStr.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        // The key length must not be less than 8 bytes
        return keyFactory.generateSecret(dks);
    }
}
