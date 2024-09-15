package NLP;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Tokenizer {
    private TokenizerME tokenizer;

    public Tokenizer(String modelPath) {
        try (InputStream modelIn = new FileInputStream(modelPath)) {
            TokenizerModel model = new TokenizerModel(modelIn);
            this.tokenizer = new TokenizerME(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] tokenize(String text) {
        return tokenizer.tokenize(text);
    }
}
