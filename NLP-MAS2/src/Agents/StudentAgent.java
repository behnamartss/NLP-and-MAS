package Agents;

import NLP.SentenceDetector;
import NLP.Tokenizer;
import NLP.POStagger;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;


import java.io.*;
import java.util.*;

public class StudentAgent extends Agent {
    private SentenceDetector sentenceDetector;
    private Tokenizer tokenizer;
    private POStagger posTagger;
    private POStagger posTagger2;
    private String corpusFilePath;
    private List<String> corpusSentences;

    private String nextStudent;
    private AID teacherAID;
    private int chances = 3;  // Initialize with 3 chances
    private boolean isMyTurn = false;  // New flag to manage turns
    private Random random = new Random();
    private String lastSentence="";
    private static final Map<String, String> tagTransitions = new HashMap<>();

    @Override
    protected void setup() {
        String sentenceModelPath = "Models/opennlp-en-ud-ewt-sentence-1.0-1.9.3.zip";
        String posModelPath = "Models/opennlp-en-ud-ewt-pos-1.0-1.9.3.zip";
        String posModelPath2 = "Models/en-pos-perceptron.zip";
        String tokenModelPath = "Models/opennlp-en-ud-ewt-tokens-1.0-1.9.3.zip";


        this.sentenceDetector = new SentenceDetector(sentenceModelPath);
        this.posTagger = new POStagger(posModelPath);
        this.posTagger2 = new POStagger(posModelPath2);
        this.tokenizer = new Tokenizer(tokenModelPath);



        // Load and process the full text corpus
        // Read corpus and use it in NLP processing. I have provided you professor with two different corpuses.
        // One is the one I found on an article which is not a standard corpus, but for fast testing is adequate.
        // The other one conllu which many of the models of the Apache OpenNLP are trained based on that.
        corpusFilePath = "full_corpus.txt"; // Update with the path to your full text corpus file
        //Read it
        String filePath = "./en_ewt-ud-train.conllu"; // Adjust the path as necessary



        //
         //corpusSentences= loadCorpus(corpusFilePath);
        corpusSentences = loadCorpusBig(filePath);
        System.out.println(corpusSentences);
        // Example usage
//        if (!corpusSentences.isEmpty()) {
//            String processedResult = NLPProcessor("hello");
//            System.out.println(processedResult);
//        } else {
//            System.out.println("No sentences found in the corpus.");
//        }

        // Get arguments passed from the Main agent
        System.out.println(getLocalName() + " has started working");
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            teacherAID = (AID) args[0];
        }

        // Start behaviour
        addBehaviour(new StudentBehaviour());

        // Send an initial sentence to the teacher only if the agent is "Behnam"
        if (getLocalName().equals("Behnam")) {
            isMyTurn = true;  // Behnam starts, so it's his turn
            String sentence = NLPProcessor("I am very Beautiful");

            sendSentenceToTeacher(sentence);
            isMyTurn = false;

        }
    }


    private class StudentBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    String content = msg.getContent();
                    if (msg.getSender().getLocalName().equals("Teacher")) {

                        if (content.startsWith("Next student: ")) {
                            isMyTurn = true;
                            nextStudent = content.substring("Next student: ".length());
                            System.out.println(getLocalName() + " received Next student: " + nextStudent);


                        }
                        if (content.startsWith("Sentence: ")) {
                            String sentence = content.substring("Sentence: ".length());

                            // Introduce a 1-second delay before forwarding the message
                            try {
                                Thread.sleep(1000); // 1000 milliseconds = 1 second
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            forwardSentence(sentence);
                        }
                        else if (content.startsWith("Reward detected") && chances<3 ) {
                            // Handle the mistake and update chances
                            chances++;
                            System.out.println(getLocalName() + " used an Entity." + getLocalName() +
                                    " now has " + chances + " chances left.");

                        }
                        else if (content.startsWith("Mistake detected")) {
                            // Handle the mistake and update chances
                            chances--;
                            System.out.println(getLocalName() + " made a mistake." + getLocalName() +
                                    " now has " + chances + " chances left.");
                            if (chances <= 0) {
                                // Notify teacher and handle removal logic
                                lastSentence=content;
                                notifyTeacherOfFailure();
                            }
                        }
                        else if(content.startsWith("Remove yourself"))
                        {
                            forwardSentence(lastSentence);
                            doDelete();
                        }
                    } else {
                        String sentence = NLPProcessor(content);
                        sendSentenceToTeacher(sentence);
                        isMyTurn = false;  // Reset turn after sending the sentence

                    }

                }
            } else {
                block();
            }
        }

        private void forwardSentence(String content) {
            if (nextStudent != null & isMyTurn) {
                // Forward the sentence to the next student
                ACLMessage forwardMsg = new ACLMessage(ACLMessage.INFORM);

                //String sentence = NLPProcessor(content);
                //forwardMsg.setContent(sentence);
                //System.out.println(getLocalName() + " to" + nextStudent + ":\n" + sentence);
                forwardMsg.setContent(content);
                System.out.println(getLocalName() + " sent this to " + nextStudent + ":\n" + content);

                forwardMsg.addReceiver(new AID(nextStudent, AID.ISLOCALNAME));
                send(forwardMsg);

                isMyTurn = false;  // Reset turn after forwarding the message
            }
        }

        private void notifyTeacherOfFailure() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("Remove " + getLocalName());
            msg.addReceiver(teacherAID);
            send(msg);
        }
    }

    private void sendSentenceToTeacher(String sentence) {
        if (teacherAID != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(sentence);
            msg.addReceiver(teacherAID);
            send(msg);
        }
    }
    private List<String> loadCorpusBig(String corpusFilePath) {
        System.out.println("This loadCorpus is running");
        List<String> sentences = new ArrayList<>();
        File file = new File(corpusFilePath);

        // Check if the file exists
        if (!file.exists()) {
            System.err.println("File not found: " + corpusFilePath);
            return sentences;
        }

        System.out.println("Loading file: " + corpusFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isProcessingText = false; // To check when we're inside a valid text block

            while ((line = reader.readLine()) != null) {
                // Debugging output
                System.out.println("Reading line: " + line);

                // Detect raw sentence (starts with "# text =")
                if (line.startsWith("# text =")) {
                    String sentence = line.substring(8).trim(); // Extract sentence after "# text ="
                    sentence = sentence.replaceAll("[,.;!?]", ""); // Adjust regex if needed
                    System.out.println("Detected raw sentence: " + sentence);
                    if (!sentence.isEmpty()) {
                        sentences.add(sentence); // Add the raw sentence
                    }
                    isProcessingText = true; // Mark that we've found a sentence
                }
                // Ignore token lines (those that start with a number and contain token info)
                else if (line.matches("\\d+\\t.*")) {
                    // These lines contain token and POS info, so we ignore them
                    System.out.println("Ignoring token/POS line: " + line);
                }
                // Handle end of a sentence block
                else if (line.trim().isEmpty()) {
                    isProcessingText = false; // End of the current sentence block
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the corpus file: " + e.getMessage());
        }

        // Debugging output for final corpus sentences
        System.out.println("Total sentences loaded: " + sentences.size());

        return sentences;
    }

    // We need to read our corpus through function.
    private List<String> loadCorpus(String corpusFilePath) {
        System.out.println("this loadcoprpus is running");
        List<String> sentences = new ArrayList<>();
        File file = new File(corpusFilePath);

        // Check if file exists, o.w: error.
        if (!file.exists()) {
            System.err.println("File not found: " + corpusFilePath);
            return sentences;
        }

        System.out.println("Loading file: " + corpusFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Debugging output
                System.out.println("Reading line: " + line);

                // Split line into sentences
                String[] detectedSentences = sentenceDetector.detectSentences (line);
                System.out.println("Detected sentences: " + String.join(", ", detectedSentences));
                // without regular expression, many errors can arise and the agents are terminated.
                // Text analysis and preprocessing is one of the most important parts in NLP.
                for (String sentence : detectedSentences) {
                    sentence = sentence.replaceAll("[,.;!?]", "");
                    if (!sentence.trim().isEmpty()) {
                        sentences.add(sentence);
                        System.out.println("Added sentence: " + sentence);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the corpus file: " + e.getMessage());
        }

        // Debugging output for final corpus sentences
        System.out.println("Total sentences loaded: " + sentences.size());

        return sentences;
    }


    // NLPProcessor method to generate a four-token sentence
    private String NLPProcessor( String content) {
        // Get the last letter of the provided content
        char lastLetter = getLastLetter(content);

        // Generate a new four-token sentence starting with the last letter
        String[] newSentenceTokens = generateFourTokenSentence(lastLetter, corpusSentences);

        // Construct a new sentence from the tokens
        return String.join(" ", newSentenceTokens);
    }

    // Get the last letter of a given content
    private char getLastLetter(String content) {
        content = content.trim();
        if (!content.isEmpty()) {
            return content.charAt(content.length() - 1);
        }
        return ' '; // Return a default space character if content is empty
    }

    // Generate a four-token sentence based on the last letter of the previous content
    private String[] generateFourTokenSentence(char startingLetter, List<String> corpusSentences) {
        List<String> validTokens = new ArrayList<>();
        List<String> allTokens = new ArrayList<>();

        for (String sentence : corpusSentences) {
            // Tokenize the sentence
            String[] tokens = tokenizer.tokenize(sentence);
            for (String token : tokens) {
                // Add all tokens for later random selection
                allTokens.add(token);

                // Filter tokens that start with the desired letter
                if (token.toLowerCase().charAt(0) == Character.toLowerCase(startingLetter)) {
                    validTokens.add(token);
                }
            }
        }

        // If no tokens are available for the starting letter, return a default sentence. Although it can be seldom,
        // it is needed always to consider a seldom possibility in programming
        if (validTokens.isEmpty()) {
            return new String[]{"No", "valid", "tokens", "found"};
        }


        // Select the first token randomly from the valid tokens
        List<String> validTokensFirst=new ArrayList<>();

        for (String token : validTokens){
            if(posTagger2.tag(new String[]{token})[0].equals("NOUN")||
                    posTagger2.tag(new String[]{token})[0].equals("NNS")||
                    posTagger2.tag(new String[]{token})[0].equals("NAPS")||
                    posTagger2.tag(new String[]{token})[0].equals("EX")||
                    posTagger2.tag(new String[]{token})[0].equals("PROP"))

                validTokensFirst.add(token);
        }

        int numberOfTokensToCheck = Math.min(validTokens.size(), 50);

        for (int i = 0; i < numberOfTokensToCheck; i++) {
            String token = validTokens.get(i);

            // Tag the token using the POS tagger
            String[] tags = posTagger2.tag(new String[]{token});

            // Print the token and its corresponding tag
           //System.out.println("Token: " + token + ", Tag: " + (tags.length > 0 ? tags[0] : "No tag"));
        }

        String firstToken = validTokensFirst.get( random.nextInt(validTokensFirst.size()));
        //System.out.println("write me down the first token:"+ firstToken);
        // Use POS tagging to build a grammatically correct sentence
        String[] sentenceTokens = new String[4];
        sentenceTokens[0] = firstToken;
        for (int i = 1; i < 4; i++) {
            // Example of simple POS-based generation:
            String[] selectedTokenTags = posTagger2.tag(new String[]{sentenceTokens[i - 1]});
            String requiredTag = getNextTag(selectedTokenTags[0],sentenceTokens[i-1]);

            // Filter tokens by POS tag
            List<String> filteredTokens = filterTokensByPOSTag(allTokens, requiredTag);
            if (!filteredTokens.isEmpty()) {
                sentenceTokens[i] = filteredTokens.get(random.nextInt(filteredTokens.size()));
                //System.out.println(sentenceTokens[i]+ "  "+posTagger2.tag(new String[]{sentenceTokens[i]})[0]);
            } else {
                sentenceTokens[i] = allTokens.get(random.nextInt(allTokens.size())); // Fallback if no POS match
                System.out.println("randomly made");
            }
        }

        return sentenceTokens;
    }

    // Some rules to get the next POS tag based on the current one.
    // Unfortunately due to limitations,They are not complex
    private String getNextTag(String currentTag,String token) {
       // logger.info("Determining the next tag for current tag: " + currentTag);

        return switch (currentTag) {
            case "NOUN", "NN", "NNS", "NNP", "NNPS","MD","TO","PRP","AUX+PART","PRON" -> {
                //boolean isSingular=isSingularNoun(token);
                yield "VERB";
//                if(isSingular)
//                yield "VBZ";
//                else
//                    yield "VBP";
            }
            case "VERB", "VBD", "VBG", "VBN", "VBP", "VBZ" -> {
               // logger.fine("Current tag is a verb, selecting adverb as next tag.");
                yield "ADV";
            }
            case "ADV", "RB", "RBR", "RBS" -> {
               // logger.fine("Current tag is an adverb, selecting adjective as next tag.");
                yield "ADJ";
            }
            case "ADJ", "JJ", "JJR", "JJS","CC","IN","DT","CCONJ","SCONJ","DET" -> {
                //logger.fine("Current tag is an adjective, selecting noun as next tag.");
                yield "NOUN";
            }
            default -> {
                //logger.warning("Current tag is unknown, defaulting to noun.");
                yield "NOUN";
            }
        };
    }
    private boolean isSingularNoun(String noun) {
        // Basic check: if it ends with "s", it's likely plural
        return !noun.toLowerCase().endsWith("s");
    }
    // Filter tokens by POS tag
    private List<String> filterTokensByPOSTag(List<String> tokens, String posTag) {
        List<String> filteredTokens = new ArrayList<>();
        for (String token : tokens) {
            token = token.trim();

//            if (!token.matches("[a-zA-Z'-]+")) {
//                continue;
//            }
            String[] tokenArray = new String[]{token};
            String[] tokenTags = posTagger2.tag(tokenArray);
            if (tokenTags[0].equals(posTag)) {
                filteredTokens.add(token);
            }
        }
        return filteredTokens;
    }
}



//    private String NLPProcessor(String content) {
//        sentenceDetector.detectSentences(content);
//        String sentence= content+"a"; // later I will process this content.
//        return sentence;
//    }
//}
