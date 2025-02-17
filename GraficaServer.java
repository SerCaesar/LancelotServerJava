import java.awt.*;
import java.awt.event.*;
import java.util.Date;

import javax.swing.*;

public class GraficaServer extends JFrame{

	private static final long serialVersionUID = 1L;
    private SystemTray sysTray;
    private TrayIcon trayIcon;
    static JTextArea OutputArea;

	
    public GraficaServer(Image iconImg){
        super("Lancelot Server");
        setDefaultCloseOperation(3);
        setBounds(350, 100, 600, 600);
        OutputArea = new JTextArea();
        OutputArea.setFont(new Font("Times New Roman", 0, 11));
        OutputArea.setLineWrap(true);
        OutputArea.setWrapStyleWord(true);
        OutputArea.setEditable(false);
        OutputArea.setAutoscrolls(true);
        JScrollPane areaScrollPane2 = new JScrollPane(OutputArea);
        areaScrollPane2.setVerticalScrollBarPolicy(20);
        areaScrollPane2.setPreferredSize(new Dimension(600, 550));
        areaScrollPane2.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Output"), BorderFactory.createEmptyBorder(5, 5, 5, 5)), areaScrollPane2.getBorder()));
        JPanel rightPane = new JPanel(new BorderLayout());
        rightPane.add(areaScrollPane2, "First");
        add(rightPane, "Center");
        sysTray = SystemTray.getSystemTray();
        trayIcon = new TrayIcon(iconImg, "Lancelot-Server");
        addWindowListener(new WindowAdapter() {

            public void windowIconified(WindowEvent we){
                setVisible(false);
                try
                {
                    sysTray.add(trayIcon);
                }
                catch(Exception exception) { }
            }
        });
        trayIcon.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae){
                setVisible(true);
                setState(0);
                sysTray.remove(trayIcon);
            }
        });
    }

    public static void scriviOutput(String text)
        throws NullPointerException {
        	OutputArea.append("\r\n"+new Date()+"\r\n"+text);
        	OutputArea.setSelectionStart(OutputArea.getText().length());
    }
}
