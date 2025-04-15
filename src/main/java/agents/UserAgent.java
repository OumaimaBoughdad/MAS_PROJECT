package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class UserAgent extends Agent {
    protected void setup() {
        System.out.println("UserAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // Get user input
                String query = javax.swing.JOptionPane.showInputDialog(
                        "Enter your search query (simple or complex):");

                if (query != null && !query.trim().isEmpty()) {
                    // Send query to BrokerAgent
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(getAID("BrokerAgent"));
                    msg.setContent(query);
                    send(msg);

                    // Wait for response
                    ACLMessage reply = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    System.out.println("\n=== Search Results ===");
                    System.out.println(reply.getContent());
                    System.out.println("=====================");
                }
            }
        });
    }
}