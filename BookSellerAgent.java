package examples.bookTrading;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import net.sf.clipsrules.jni.*;

import java.util.*;

public class BookSellerAgent extends Agent {
    // The catalogo of books for sale (maps the title of a book to its price)
    // El catálogo de libros a la venta (asigna el título de un libro a su precio)
    private Hashtable catalogo;
    // The GUI by means of which the user can add books in the catalogo
    // La GUI por medio de la cual el usuario puede agregar libros en el catálogo
    private BookSellerGui myGui;

    private Environment clips;

    // Put agent initializations here
    //Pon aquí las inicializaciones de los agentes
    protected void setup() {

          try {
           clips = new Environment();
        
      } catch (Exception e){}
      
        // Create the catalogo
        //crea el catalogo 
        catalogo = new Hashtable();

        // Create and show the GUI
        // crea la vista al gui

        myGui = new BookSellerGui(this);
        myGui.show();

        // Register the book-selling service in the yellow pages
        // Dar de alta el servicio de venta de libros en las páginas amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("venta de libros");
        sd.setName("Comercio de libros de JADE");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        

        // Add the behaviour serving queries from buyer agents
        // Agregue el comportamiento que atiende las consultas de los agentes compradores.
        addBehaviour(new OfferRequestsServer());

        // Add the behaviour serving purchase orders from buyer agents
        // Agregar el comportamiento de servicio de órdenes de compra de agentes compradores
        addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    // Poner operaciones de limpieza de agentes aquí
    protected void takeDown() {
        // Deregister from the yellow pages
        // Darse de baja de las páginas amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Close the GUI
        // Cierra la GUI
        myGui.dispose();
        // Printout a dismissal message
        // Imprimir un mensaje de despido
        System.out.println("agente-vendedor "+getAID().getName()+" terminando.");
    }

    /**
       This is invoked by the GUI when the user adds a new book for sale
     */
     /*
     Esto es invocado por la GUI cuando el usuario agrega un nuevo libro para la venta.
      */
    public void updateCatalogo(final String producto, final int precio) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogo.put(producto, new Integer(precio));
                System.out.println(producto+" insertado en el catálogo. Precio = "+precio);
                
            }
        });
    }

    /**
         Inner class OfferRequestsServer.
         This is the behaviour used by Book-seller agents to serve incoming requests
         for offer from buyer agents.
         If the requested book is in the local catalogo the seller agent replies
         with a PROPOSE message specifying the price. Otherwise a REFUSE message is
         sent back.
    */

    /*Clase interna OfferRequestsServer.
         Este es el comportamiento utilizado por los agentes de librería para atender las solicitudes entrantes.
         para la oferta de los agentes compradores.
         Si el libro solicitado está en el catálogo local, el agente vendedor responde
         con un mensaje de PROPUESTA especificando el precio. De lo contrario, aparecerá un mensaje de RECHAZO.
         devuelto.
     */

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
             try{
             
            }catch (Exception e){}    
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                // Mensaje CFP recibido. Procesalo
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Integer price = (Integer) catalogo.get(title);
                if (price != null) {
                    // The requested book is available for sale. Reply with the price
                    // El libro solicitado está disponible para la venta. Responder con el precio
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                } else {
                    // The requested book is NOT available for sale.
                    // El libro solicitado NO está disponible para la venta.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("No disponible");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer / Fin de la clase interna OfferRequestsServer

    /**
         Inner class PurchaseOrdersServer.
         This is the behaviour used by Book-seller agents to serve incoming
         offer acceptances (i.e. purchase orders) from buyer agents.
         The seller agent removes the purchased book from its catalogo
         and replies with an INFORM message to notify the buyer that the
         purchase has been successfully completed.
    */

    /*
     Clase interna PurchaseOrdersServer.
         Este es el comportamiento que utilizan los agentes de librería para atender
         aceptaciones de ofertas (es decir, órdenes de compra) de los agentes compradores.
         El agente vendedor elimina el libro comprado de su catálogo.
         y responde con un mensaje INFORM para notificar al comprador que el
         la compra se ha completado con éxito.
    */
    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                // ACCEPT_PROPOSAL Mensaje recibido. Procesalo
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Integer price = (Integer) catalogo.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(title+" Vendido al agente "+msg.getSender().getName());
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    // Mientras tanto, el libro solicitado se vendió a otro comprador.
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("No disponible");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer / Fin de la clase interna OfferRequestsServer

} // end of the agent class / fin de la clase de agente