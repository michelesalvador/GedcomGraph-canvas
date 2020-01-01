package canvas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.CubicCurve2D;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.AncestryNode;
import graph.gedcom.AncestryNode.Ancestor;
import graph.gedcom.Card;
import graph.gedcom.CardNode;
import graph.gedcom.Graph;
import graph.gedcom.Util;
import graph.gedcom.Line;
import graph.gedcom.Node;
import static graph.gedcom.Util.pr;

public class Diagram {

	Graph graph;
	String fulcrumId;
	JPanel box;

	static int sizeHoriz = 1600;
	static int sizeVert = 1000;
	static int shiftX = 50;
	static int shiftY = 50;

	Diagram() throws Exception {

		// Swing stuff
		JFrame frame = new JFrame();
		frame.setSize(sizeHoriz, sizeVert);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		box = new JPanel();
		box.setBackground(Color.darkGray);
		JScrollPane scrollPane = new JScrollPane(box);
		frame.getContentPane().add(scrollPane);
		frame.setVisible(true);

		// Parse a Gedcom file
		File file = new File("src/main/resources/family.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		// Directly open a Json file
		// String content = FileUtils.readFileToString(new
		// File("..\\esempi\\famiglia.ged.json"), "UTF-8");
		// Gedcom gedcom = new JsonParser().fromJson(content);

		// Create the diagram model from the Gedcom object
		graph = new Graph(gedcom);
		graph.showFamily(0).maxAncestors(2);
		fulcrumId = "I1";
		paintDiagram();
	}

	public static void main(String[] args) throws Exception {
		new Diagram();
		// new Prova();
		// new GrouppoLayout();
	}

	private void paintDiagram() {

		box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS)); // This layout let the nodes auto-size

		if (!graph.startFrom(null, "Pippo", fulcrumId)) {
			JOptionPane.showMessageDialog(null, "Can't find a person with this id.");
			return;
		}
		// pr(graph.toString());

		// Define the dimensions of each card
		for (Card card : graph.getCards()) {
			card.width = Util.essence(card.getPerson()).length() * 5 + 15;
			card.height = (int) (25 + (40 * Math.random()));
		}
		// The same for the little ancestors
		for (Ancestor ancestor : graph.getAncestors()) {
			ancestor.width = (int) (Math.log10(ancestor.ancestry) + 1) * 5 + 10;
			ancestor.height = 20;
		}

		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();

		box.setLayout(null); // This layout let the nodes in absolute position
		box.setPreferredSize(new Dimension(graph.width + shiftX*2, graph.height + shiftY*2));

		// Place the nodes that place the cards on the canvas
		for (Node node : graph.getNodes()) {
			if (node instanceof CardNode)
				box.add(new GraphicCardNode((CardNode) node));
			else if (node instanceof AncestryNode)
				box.add(new GraphicAncestry((AncestryNode) node));
		}

		// Draw the lines
		box.add(new GraphicLines());

		// pr(graph.toString());
		box.repaint();
	}

	// Basic class for GraphicCardNode and GraphicAncestry
	abstract class GraphicNode extends JPanel {
		Node node;
	}

	// Graphical rappresentation of a card node
	class GraphicCardNode extends GraphicNode {

		GraphicCardNode(CardNode node) {
			this.node = node;
			setBounds(node.x + shiftX, node.y + shiftY, node.width, node.height);
			// setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			setOpaque(false);
			// Create the cards
			if (node.husband != null)
				box.add(new GraphicCard(node.husband));
			if (node.wife != null)
				box.add(new GraphicCard(node.wife));
		}

		@Override
		protected void paintComponent(Graphics g) {
			CardNode node = (CardNode) this.node;
			if (node.isCouple()) {
				// Draw the vertical line from marriage
				if (node.guardGroup != null && !node.guardGroup.getYouths().isEmpty()) {
					g.setColor(Color.lightGray);
					g.drawLine(node.centerXrel(), node.centerYrel(), node.centerXrel(), node.height);
				}
				// Draw the marriage
				if (node.marriageDate != null) {
					int w = 25;
					int h = 17;
					int x = node.centerXrel() - w / 2;
					int y = node.centerYrel() - h / 2;
					g.setColor(new Color(0xDDBBFF));
					g.fillOval(x, y, w, h);
					g.setColor(Color.black);
					g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
					g.drawString(node.marriageYear(), x, y + 12);
				} else {
					// Draw a simple horizontal line
					g.setColor(Color.lightGray);
					g.drawLine(node.husband.width, node.centerYrel(), node.husband.width + Util.MARGIN,
							node.centerYrel());
				}
			}
		}
	}

	// Graphical realization of a card
	class GraphicCard extends JButton {

		Card card;

		GraphicCard(Card card) {
			super(Util.essence(card.getPerson()));
			this.card = card;
			setBounds(card.x + shiftX, card.y + shiftY, card.width, card.height);
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			if (card.getPerson().getId().equals(graph.getStartId()))
				setBackground(Color.orange);
			else if (card.acquired) {
				setBackground(new Color(0xCCCCCC));
				box.add(new GraphicAncestry(card.ancestryNode));
			} else
				setBackground(Color.white);
			Border border = BorderFactory.createLineBorder(Color.gray, 2);
			if (Util.sex(card.getPerson()) == 1) {
				border = BorderFactory.createLineBorder(Color.blue, 2);
			} else if (Util.sex(card.getPerson()) == 2) {
				border = BorderFactory.createLineBorder(Color.pink, 2);
			}
			setBorder(border);
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					box.removeAll();
					fulcrumId = card.getPerson().getId();
					paintDiagram();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			// Death ribbon
			g.setColor(Color.black);
			if (card.dead) {
				int[] pX = { card.width - 12, card.width - 7, card.width, card.width };
				int[] pY = { 0, 0, 7, 12 };
				g.fillPolygon(pX, pY, 4);
			}
		}
	}

	class GraphicAncestry extends GraphicNode {
		Node node;

		GraphicAncestry(AncestryNode node) {
			this.node = node;
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBounds(node.x + shiftX, node.y + shiftY, node.width, node.height);
			// setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
			// Create the ancestor minicards
			if (node.foreFather != null)
				box.add(new GraphicAncestor(node, true));
			if (node.foreMother != null)
				box.add(new GraphicAncestor(node, false));
		}

		@Override
		protected void paintComponent(Graphics g) {
			AncestryNode node = (AncestryNode) this.node;
			// Draw the T lines
			if (node.isCouple()) {
				g.setColor(Color.lightGray);
				g.drawLine(node.foreFather.width, node.centerYrel(), node.foreFather.width + Util.GAP,
						node.centerYrel()); // Horizontal
				g.drawLine(node.centerXrel(), node.centerYrel(), node.centerXrel(), node.height); // Vertical
			}
		}
	}

	class GraphicAncestor extends JButton {
		GraphicAncestor(AncestryNode node, boolean male) {
			super(String.valueOf(((Ancestor) (male ? node.foreFather : node.foreMother)).ancestry));
			Ancestor ancestor = male ? node.foreFather : node.foreMother;
			setBounds(ancestor.x + shiftX, ancestor.y + shiftY, ancestor.width, ancestor.height);
			setFont(new Font("Segoe UI", Font.PLAIN, 11));
			setBackground(Color.white);
			Border border;
			if (Util.sex(ancestor.person) == 2)
				border = BorderFactory.createLineBorder(Color.pink, 1);
			else
				border = BorderFactory.createLineBorder(Color.blue, 1);
			setBorder(border);
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					box.removeAll();
					fulcrumId = ancestor.person.getId();
					paintDiagram();
				}
			});
		}
	}

	class GraphicLines extends JPanel {
		GraphicLines() {
			setBounds(shiftX, shiftY, graph.width, graph.height);
			setBorder(BorderFactory.createLineBorder(Color.green, 1));
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.lightGray);
			for (Line line : graph.getLines()) {
				int x1 = line.x1;
				int y1 = line.y1;
				int x2 = line.x2;
				int y2 = line.y2;
				// g.drawLine(x1, y1, x2, y2);
				Graphics2D g2 = (Graphics2D) g;
				CubicCurve2D c = new CubicCurve2D.Double();
				c.setCurve(x1, y1, x1, y2, x2, y1, x2, y2);
				g2.draw(c);
			}
		}
	}
}