
package examples.bookTrading;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
  @author Giovanni Caire - TILAB
 */
class BookSellerGui extends JFrame {
    private BookSellerAgent myAgent;

    private JTextField campoProducto, campoPrecio;

    BookSellerGui(BookSellerAgent a) {
        super(a.getLocalName());

        myAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("Nombre del producto:"));
        campoProducto = new JTextField(15);
        p.add(campoProducto);
        p.add(new JLabel("Precio:"));
        campoPrecio = new JTextField(15);
        p.add(campoPrecio);
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Agregar");
        addButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String producto = campoProducto.getText().trim();
                    String precio = campoPrecio.getText().trim();
                    myAgent.updateCatalogo(producto, Integer.parseInt(precio));
                    campoProducto.setText("");
                    campoPrecio.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(BookSellerGui.this, "Valores no válidos. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } );
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes / Hacer que el agente finalice cuando el usuario cierre
        // the GUI using the button on the upper right corner / la GUI usando el botón en la esquina superior derecha 
        addWindowListener(new   WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        } );

        setResizable(false);
    }

    public void show() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.show();
    }
}