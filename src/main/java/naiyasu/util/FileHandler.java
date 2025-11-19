package naiyasu.util;

import java.io.*;

public class FileHandler {
    public static String read(File file) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            return new String(bis.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
