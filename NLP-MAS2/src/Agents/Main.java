package Agents;

import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        // Initialize JADE runtime
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(profile);

        try {
            // Create and start TeacherAgent
            AgentController teacherAgent = mainContainer.createNewAgent("Teacher", TeacherAgent.class.getName(), null);
            teacherAgent.start();

            // Create and start StudentAgents
            AgentController student1 = mainContainer.createNewAgent("Behnam", StudentAgent.class.getName(), new Object[]{getTeacherAID()});
            AgentController student2 = mainContainer.createNewAgent("Viviana", StudentAgent.class.getName(), new Object[]{getTeacherAID()});
            AgentController student3 = mainContainer.createNewAgent("Alex", StudentAgent.class.getName(), new Object[]{getTeacherAID()});

            student1.start();
            student2.start();
            student3.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private static AID getTeacherAID() {
        // Provide logic to get Teacher AID
        return new AID("Teacher", AID.ISLOCALNAME);
    }
}
