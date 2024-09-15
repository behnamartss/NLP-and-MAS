package NLP;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class POStagger {
    private POSTaggerME posTagger;

    // Constructor to initialize the POS tagger
    public POStagger(String modelFilePath) {
        try (InputStream modelInputStream = new FileInputStream(modelFilePath)) {
            POSModel posModel = new POSModel(modelInputStream);
            posTagger = new POSTaggerME(posModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to tag parts of speech for a given array of tokens
    public String[] tag(String[] tokens) {
        return posTagger.tag(tokens);
    }
}
