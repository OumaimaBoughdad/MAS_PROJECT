package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class DuckDuckGoAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for general web searches.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("Searching DuckDuckGo for: " + query);
                    String response = HttpHelper.searchExternalSource("duckduckgo", query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(formatDDGResponse(response));
                    send(reply);
                } else {
                    block();
                }
            }

            private String formatDDGResponse(String raw) {
                return "\n" +
                        (raw.equals("No result found.") ?
                                "No instant answer available. Try a more specific query."
                                : raw);
            }
        });
    }
}