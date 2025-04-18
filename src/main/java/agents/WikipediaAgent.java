package agents;// WikipediaAgent.java
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class WikipediaAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for Wikipedia searches.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("Searching Wikipedia for: " + query);
                    String response = HttpHelper.searchExternalSource("wikipedia", query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("\n" + formatWikipediaResponse(response));
                    send(reply);
                } else {
                    block();
                }
            }

            private String formatWikipediaResponse(String raw) {
                if (raw == null) return "No result.";
                return raw; // Return the full response
            }
        });
    }
}
