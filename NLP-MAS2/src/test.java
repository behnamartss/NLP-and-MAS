import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceDetectorME;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

public class test {
    public static void main(String[] args) {
        String modelPath = "Models/opennlp-en-ud-ewt-sentence-1.0-1.9.3.zip";
        File modelFile = new File(modelPath);
        System.out.println("Model file can be read: " + modelFile.canRead());
        System.out.println("Model file exists: " + modelFile.exists());
        System.out.println("Model file path: " + modelFile.getAbsolutePath());
        try (InputStream modelIn = new FileInputStream(modelPath)) {
            SentenceModel model = new SentenceModel(modelIn);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
            System.out.println("Model loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
