package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class DeepInfraAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for DeepInfra-powered responses.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // Receive requests
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("DeepInfraAgent processing query: " + query);

                    try {
                        // Use existing HttpHelper method
                        String response = HttpHelper.queryDeepInfra(query);

                        // Format and send response
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(formatResponse(response));
                        send(reply);

                    } catch (Exception e) {
                        // Error handling
                        System.err.println("DeepInfra processing error: " + e.getMessage());
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Error: " + e.getMessage());
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });
    }

    private String formatResponse(String rawResponse) {
        return "[DeepInfra Response]\n" + rawResponse;
    }
}