package Agents;

import NLP.NamedEntityRecognition;
import NLP.Tokenizer;
import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;
import opennlp.tools.util.Span;

import java.util.*;
import java.util.stream.Stream;

public class TeacherAgent extends Agent {
    private Map<String, Integer> studentChances = new HashMap<>();
    private List<String> activeStudents = new ArrayList<>(Arrays.asList("Behnam", "Viviana", "Alex"));
    private List<String> usedTokens = new ArrayList<String>();
    boolean mistake;
    boolean reward;
    private Tokenizer tokenizer;
    private NamedEntityRecognition nerPerson;
    private NamedEntityRecognition nerLocation;
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " has started working");
        String tokenModelPath = "Models/opennlp-en-ud-ewt-tokens-1.0-1.9.3.zip";
        String nerPersonModelPAth ="Models/en-ner-person.zip";
        String nerLocationModelPAth ="Models/en-ner-location.zip";

        this.tokenizer = new Tokenizer(tokenModelPath);

        this.nerPerson=new NamedEntityRecognition(nerPersonModelPAth);
        this.nerLocation=new NamedEntityRecognition(nerLocationModelPAth);
        // Initialize the activeStudents list and studentChances map
//        for (String student : activeStudents) {
//            studentChances.put(student, 3); // We initialize all students with 3 chances
//        }
        addBehaviour(new TeacherBehaviour());
    }

    private class TeacherBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();
                    System.out.println(sender + " sent this to the teacher:\n " + content);
                    if (content.startsWith("Remove "))
                        RemoveAgent(sender, msg.getSender());
                    else{

                        reward=rewardResponse(content);
                        mistake = evaluateResponse(content);
                    Tokenization(content);

                    // Send feedback to the current student
                    ACLMessage feedback = new ACLMessage(ACLMessage.INFORM);
                    feedback.setContent(mistake ? "Mistake detected" : "No mistake");
                    feedback.addReceiver(msg.getSender());

                    ACLMessage feedback2 = new ACLMessage(ACLMessage.INFORM);
                    feedback2.setContent(reward ? "Reward detected" : "No Reward");
                    feedback2.addReceiver(msg.getSender());
                    // Introduce a 1-second delay before sending
                    try {
                        Thread.sleep(1000); // 1000 milliseconds = 1 second
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    send(feedback);
                    send(feedback2);

                    // Determine the next student
                    String nextStudent = determineNextStudent(sender);
                    if (nextStudent != null) {
                        // Inform the current student who the next one is
                        ACLMessage nextNotification = new ACLMessage(ACLMessage.INFORM);
                        nextNotification.setContent("Next student: " + nextStudent);
                        nextNotification.addReceiver(msg.getSender());

                        ACLMessage newmsg = new ACLMessage(ACLMessage.INFORM);
                        newmsg.setContent("Sentence: " + content);
                        newmsg.addReceiver((msg.getSender()));


                        // Introduce a 1-second delay before sending
                        try {
                            Thread.sleep(1000); // 1000 milliseconds = 1 second
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        send(nextNotification);
                        send(newmsg);
                    }
                }
                } else if (msg.getPerformative() == ACLMessage.REQUEST) {
                    // Handle other types of messages if needed
                }
            } else {
                block();
            }
        }


        private void RemoveAgent(String agent,AID aid) {
            activeStudents.remove((agent));
            ACLMessage removeNotification = new ACLMessage(ACLMessage.INFORM);
            removeNotification.setContent("Remove yourself" );
            removeNotification.addReceiver(aid);
            System.out.println(activeStudents);
            if(activeStudents.size()==1) {
                System.out.println(activeStudents.get(0) + " is the winner!");
                shutdownPlatform();
            }
        }

        private String determineNextStudent(String currentStudent) {

            int index = activeStudents.indexOf(currentStudent);
            System.out.println("Current student: " + currentStudent + " has index: " + index);
            if (index >= 0) {
                int nextIndex = (index + 1) % activeStudents.size();  // Circular rotation
                System.out.println("Next index calculated: " + nextIndex);
                return activeStudents.get(nextIndex);
            }
            return null; // No next student
        }
        private boolean evaluateResponse(String response) {

            // Example logic: A "Mistake" is detected if response contains a specific keyword.
            Boolean mistake=Stream.of(response.split(" ")).anyMatch(usedTokens::contains);
            System.out.println("Is there any repetitive token used before? "+ mistake);

            return mistake; // Example evaluation logic
        }

        private boolean rewardResponse(String response) {
            String[] tokens=tokenizer.tokenize(response);
            boolean localReward=false;
            //Named Entity Recognition:
            String [] sentence = new String[]{
                    "Diana"
            };
            Span[] resultsPerson= nerPerson.findPerson(tokens);
            Span[] resultsLocation= nerLocation.findLocation(tokens);
            if (resultsPerson.length == 0 && resultsLocation.length==0) {
                System.out.println("No named entities detected.");
            } else {

                for (Span span : resultsPerson) {
                    System.out.println("Here is the Person span: " + span.toString());

                }
                for (Span span : resultsLocation) {
                    System.out.println("Here is the Loc span: " + span.toString());

                }
                localReward=true;
            }
            return localReward;
        }

        private void shutdownPlatform() {
            try {
                AgentContainer container = getContainerController();
                container.getPlatformController().kill(); // Terminates the entire platform
            }  catch (ControllerException e) {
                throw new RuntimeException(e);
            }
        }


    }

    private void Tokenization(String sentence) {
        String[] tokens = tokenizer.tokenize(sentence);
        for (String token : tokens) {
            // Add all tokens for later random selection. This part is very important in case of not matching the taggers
            // otherwise, the error rises.
            usedTokens.add(token);

        }
        System.out.println("all tokens"+ usedTokens);

    }
}
