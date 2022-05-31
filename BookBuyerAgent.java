
package examples.bookTrading;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BookBuyerAgent extends Agent {
    // The title of the book to buy
    //El titulo del libro a comprar
    private String targetBookTitle;

    // The list of known seller agents
    //la lista de agentes vendedores conocidos 
    private AID[] sellerAgents;

    // Put agent initializations here
    //Pon aquí las inicializaciones de los agentes
    protected void setup() {
        // Printout a welcome message
        System.out.println("Hola! Buyer-agent "+getAID().getName()+" estas listo.");

        // Get the title of the book to buy as a start-up argument
        //Obten el título del libro para comprar como argumento inicial
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            targetBookTitle = (String) args[0];
            System.out.println("El libro es "+targetBookTitle);

            // Add a TickerBehaviour that schedules a request to seller agents every minute
            //Agrega un TickerBehaviour que programe una solicitud a los agentes vendedores cada minuto
            addBehaviour(new TickerBehaviour(this, 10000) {
                protected void onTick() {
                    System.out.println("tratando de comprar "+targetBookTitle);
                    // Update the list of seller agents
                    // Actualiza la lista de agentes vendedores
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("venta de libros");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Encontré los siguientes agentes de ventas:");
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    // Realizar la solicitud
                    myAgent.addBehaviour(new RequestPerformer());
                }
            } );
        } else {
            // Make the agent terminate
            //Hace que el agente termine
            System.out.println("No se ha especificado ningún título de libro de destino");
            doDelete();
        }
    }

    // Put agent clean-up operations here
    // Pone operaciones de limpieza de agentes aquí
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("agente-comprador"+getAID().getName()+" terminando.");
    }

    /**
       Inner class RequestPerformer.
       This is the behaviour used by Book-buyer agents to request seller
       agents the target book.
     */

     /*
       RequestPerformer de clase interna.
       Esta es el comportamiento utilizado por los agentes compradores de libros 
       para solicitar al agente vendedor el libro de destino. 
       */
    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer // El agente que ofrece la mejor oferta
        private int bestPrice;  // The best offered price //El mejor precio ofrecido
        private int repliesCnt = 0; // The counter of replies from seller agents // El contador de respuestas de agentes vendedores.(
        private MessageTemplate mt; // The template to receive replies // La plantilla para recibir respuestas
        private int step = 0;

        public void action() {
            switch (step) {
            case 0:
                // Send the cfp to all sellers
                // Enviar el cfp a todos los vendedores
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < sellerAgents.length; ++i) {
                    cfp.addReceiver(sellerAgents[i]);
                }
                cfp.setContent(targetBookTitle);
                cfp.setConversationId("comercio de libros");
                cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value // unico valor 
                myAgent.send(cfp);
                // Prepare the template to get proposals
                //Preparar la plantilla para recibir propuestas
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("comercio de libros"),
                                         MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                step = 1;
                break;
            case 1:
                // Receive all proposals/refusals from seller agents
                // Reciba todas las propuestas/rechazos de los agentes vendedores
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    // Reply received
                    // Respuesta recibida
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        // This is an offer
                        // esta es una oferta
                        int price = Integer.parseInt(reply.getContent());
                        if (bestSeller == null || price < bestPrice) {
                            // This is the best offer at present
                            //Esta es la mejor oferta en este momento.
                            bestPrice = price;
                            bestSeller = reply.getSender();
                        }
                    }
                    repliesCnt++;
                    if (repliesCnt >= sellerAgents.length) {
                        // We received all replies
                        //Nosotros recibimos todas las respuestas
                        step = 2;
                    }
                } else {
                    block();
                }
                break;
            case 2:
                // Send the purchase order to the seller that provided the best offer
                // Enviar la orden de compra al vendedor que hizo la mejor oferta
                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                order.addReceiver(bestSeller);
                order.setContent(targetBookTitle);
                order.setConversationId("comercio de libros");
                order.setReplyWith("pedido"+System.currentTimeMillis());
                myAgent.send(order);
                // Prepare the template to get the purchase order reply
                // Prepare la plantilla para obtener la respuesta de la orden de compra
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("comercio de libros"),
                                         MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                step = 3;
                break;
            case 3:
                // Receive the purchase order reply
                // Recibir la respuesta de la orden de compra
                reply = myAgent.receive(mt);
                if (reply != null) {
                    // Purchase order reply received
                    // Recibida respuesta de orden de compra
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        // Purchase successful. We can terminate
                        // Compra exitosa. podemos terminar
                        System.out.println(targetBookTitle+" Comprado con éxito del agente "+reply.getSender().getName());
                        System.out.println("Precio = "+bestPrice);
                        myAgent.doDelete();
                    } else {
                        System.out.println("Intento fallido: el libro solicitado ya se vendió.");
                    }

                    step = 4;
                } else {
                    block();
                }
                break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Intento fallido: "+targetBookTitle+" no disponible para la venta");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer / Fin de la clase interna RequestPerformer
}