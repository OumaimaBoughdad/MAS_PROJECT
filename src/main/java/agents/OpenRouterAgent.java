package agents;


import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class OpenRouterAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for AI-powered responses.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("OpenRouterAgent processing query: " + query);

                    String response = HttpHelper.searchExternalSource("openrouter", query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("\n" + response);
                    send(reply);
                } else {
                    block();
                }
            }
        });
    }
}
