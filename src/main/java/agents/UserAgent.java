package agents;

//import jade.core.Agent;
//import jade.core.behaviours.CyclicBehaviour;
//import jade.lang.acl.ACLMessage;
//import jade.lang.acl.MessageTemplate;
//
//public class UserAgent extends Agent {
//    protected void setup() {
//        System.out.println("UserAgent " + getAID().getName() + " is ready.");
//
//        addBehaviour(new CyclicBehaviour(this) {
//            public void action() {
//                // Get user input
//                String query = javax.swing.JOptionPane.showInputDialog(
//                        "Enter your search query (simple or complex):");
//
//                if (query != null && !query.trim().isEmpty()) {
//                    // Send query to BrokerAgent
//                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//                    msg.addReceiver(getAID("BrokerAgent"));
//                    msg.setContent(query);
//                    send(msg);
//
//                    // Wait for response
//                    ACLMessage reply = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
//                    System.out.println("\n=== Search Results ===");
//                    System.out.println(reply.getContent());
//                    System.out.println("=====================");
//                }
//            }
//        });
//    }
//}



import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

public class UserAgent extends Agent {
    private ResearchFrame frame;

    protected void setup() {
        System.out.println("UserAgent " + getAID().getName() + " is ready.");

        // Create the GUI on the EDT
        SwingUtilities.invokeLater(() -> {
            frame = new ResearchFrame(this);
            frame.setVisible(true);
        });

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // Modify to accept both INFORM and PROPOSE messages
                MessageTemplate template = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)
                );

                ACLMessage msg = receive(template);
                if (msg != null) {
                    // Create a final copy of the response to use in the lambda
                    final String responseContent = processResponse(msg);
                    SwingUtilities.invokeLater(() -> {
                        if (frame != null) {
                            frame.displayResults(responseContent);
                        }
                    });
                } else {
                    block();
                }
            }
        });
    }

    private String processResponse(ACLMessage msg) {
        String response = msg.getContent();
        // Format the response if it's from OpenRouter
        if (msg.getPerformative() == ACLMessage.PROPOSE) {
            return formatOpenRouterResponse(response);
        }
        return response;
    }

    private String formatOpenRouterResponse(String jsonResponse) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            JSONArray choices = response.getJSONArray("choices");
            StringBuilder formatted = new StringBuilder();

            formatted.append("[OpenRouter Result]\n");
            for (int i = 0; i < choices.length(); i++) {
                JSONObject choice = choices.getJSONObject(i);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");
                formatted.append(content).append("\n\n");
            }
            return formatted.toString();
        } catch (Exception e) {
            return "\n" + jsonResponse;
        }
    }

    public void sendQuery(String query) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("BrokerAgent", AID.ISLOCALNAME));
        msg.setContent(query);
        send(msg);
    }
}