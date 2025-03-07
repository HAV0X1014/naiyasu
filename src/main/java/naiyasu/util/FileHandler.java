package naiyasu.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileHandler {
    public static String read(File file) {
        StringBuilder fullText = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                fullText.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fullText.toString();
    }
}
