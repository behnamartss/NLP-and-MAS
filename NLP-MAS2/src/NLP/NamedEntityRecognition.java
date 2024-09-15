package NLP;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NamedEntityRecognition {
    private NameFinderME nameFinder;
    private NameFinderME locationFinder;
    // Constructor to initialize the POS tagger
    public NamedEntityRecognition(String modelFilePath) {
        try (InputStream modelInputStream = new FileInputStream(modelFilePath)) {
            switch (modelFilePath){
                case"Models/en-ner-person.zip":
                {
                    TokenNameFinderModel nameModel = new TokenNameFinderModel(modelInputStream);
                    nameFinder = new NameFinderME(nameModel);
                }
                case"Models/en-ner-location.zip":{
                    TokenNameFinderModel locationModel = new TokenNameFinderModel(modelInputStream);

                    locationFinder = new NameFinderME(locationModel);
                }
            }




        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to tag parts of speech for a given array of tokens
    public Span[] findPerson(String[] sentence) {

        return nameFinder.find(sentence);
    }
    public Span[] findLocation(String[] sentence) {

        return locationFinder.find(sentence);
    }
}
