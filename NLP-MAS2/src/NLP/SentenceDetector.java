package NLP;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SentenceDetector {
    private SentenceDetectorME detector;

    public SentenceDetector(String modelPath) {
        try (InputStream modelIn = new FileInputStream(modelPath)) {
            SentenceModel model = new SentenceModel(modelIn);
            this.detector = new SentenceDetectorME(model);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] detectSentences(String text)
    {
        return detector.sentDetect(text);
    }
}

