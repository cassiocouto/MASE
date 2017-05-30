package masex;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

public class MainScreen extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JPanel simPanel;
	private JPanel controllers;
	private JPanel simulationSite;

	private static MainScreen myInstance;

	public static MainScreen getInstance() {
		if (myInstance == null) {
			myInstance = new MainScreen();
		}
		return myInstance;
	}

	private MainScreen() {
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.getContentPane().setLayout(new GridBagLayout());

		simPanel = new JPanel();
		simPanel.setName("Simulation");

		simulationSite = new JPanel();
		simulationSite.setBounds(0, 0, 944, 640);

		simPanel.setLayout(new SpringLayout());
		simPanel.add(simulationSite);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 2.5;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		this.getContentPane().add(simPanel, c);

		controllers = new JPanel();
		controllers.setName("Controllers");

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.5;
		c.weighty = 1;
		c.gridx = 2;
		c.gridy = 0;
		this.getContentPane().add(controllers, c);

	}

}
