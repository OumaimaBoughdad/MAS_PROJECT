package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class LangSearchAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for LangSearch queries.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    String response = HttpHelper.searchExternalSource("langsearch", query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("\n\n" + response);
                    send(reply);
                } else {
                    block();
                }
            }
        });
    }
}
