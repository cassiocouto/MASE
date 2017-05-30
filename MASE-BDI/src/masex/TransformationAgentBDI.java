package masex;

import java.util.HashMap;
import java.util.Map;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.SUtil;
import jadex.micro.annotation.*;

@Agent
@Arguments({ @Argument(name = "positionX", clazz = Integer.class), @Argument(name = "positionY", clazz = Integer.class),
		@Argument(name = "explorationLevel", clazz = Integer.class), @Argument(name = "index", clazz = Integer.class),
		@Argument(name = "type", clazz = Integer.class) })
public class TransformationAgentBDI {
	private final int FARMER = 0;
	private final int RANCHER = 1;
	@Agent
	BDIAgent agent;
	@Belief
	private int positionX;
	@Belief
	private int positionY;
	@Belief
	private int explorationLevel;
	@Belief
	private int currentExploration;
	@Belief
	private int type;
	@Belief
	private int index;
	@Belief
	private boolean ready;
	@Belief
	private int messageQueueSize = 0;
	@Belief
	private Object[] messageQueue;

	@AgentCreated
	public void created() {
		Starter.print("created: " + this);

		positionX = (Integer) agent.getArgument("positionX");
		positionY = (Integer) agent.getArgument("positionY");
		explorationLevel = (Integer) agent.getArgument("explorationLevel");
		index = (Integer) agent.getArgument("index");
		type = (Integer) agent.getArgument("type");
		// registering itself
		BeliefDB.setTAID(agent.getComponentIdentifier(), index);
		currentExploration = explorationLevel;
	}

	@AgentMessageArrived
	public void messageArrived(Map<String, Object> msg, final MessageType mt) {
		addMessageToQueue(msg);
	}

	@SuppressWarnings("unchecked")
	@Plan(trigger = @Trigger(factchangeds = "messageQueueSize"))
	public void attendRequisition(IPlan plan) {
		Map<String, Object> msg = (Map<String, Object>) getMessageFromQueue();
		if (msg != null) {
			if (msg.get(SFipa.CONTENT) instanceof String
					&& (((String) msg.get(SFipa.PERFORMATIVE)).equals(SFipa.REQUEST))) {
				// agents are ready to be deployed
				ready = true;
			}
		}
	}

	@Plan(trigger = @Trigger(factchangeds = "ready"))
	public void deliberate() {
		boolean worked = false;
		while (ready && currentExploration > 0) {
			// tries to explore
			Explore nowexploring = new Explore();
			boolean explored = (Boolean) agent.dispatchTopLevelGoal(nowexploring).get();
			if (!explored) {
				// tries to move
				agent.dropGoal(nowexploring);
				boolean move = moveFromPosition();
				if (!move) {
					try {
						int[] newPosition = SpatialManagerBDI.getBrandNewPosition();
						positionX = newPosition[0];
						positionY = newPosition[1];
					} catch (OutOfSpacesException o) {
						// enviar mensagem ao GRID
						String convid = SUtil.createUniqueId(agent.getAgentName());
						Map<String, Object> msg = new HashMap<String, Object>();
						msg.put(SFipa.CONTENT, "");
						msg.put(SFipa.PERFORMATIVE, SFipa.FAILURE);
						msg.put(SFipa.CONVERSATION_ID, convid);
						msg.put(SFipa.RECEIVERS, BeliefDB.getGRIDID());

						agent.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
					}
				}
			} else if (explored && currentExploration <= 0) {
				worked = true;
			}
		}
		ready = false;
		if (worked) {
			currentExploration = explorationLevel;
			boolean allDone = TransformationManagerBDI.setAgentDone(true, index);
			if (allDone) {

				String convid = SUtil.createUniqueId(agent.getAgentName());
				Map<String, Object> msg = new HashMap<String, Object>();
				msg.put(SFipa.CONTENT, "");
				msg.put(SFipa.PERFORMATIVE, SFipa.CONFIRM);
				msg.put(SFipa.CONVERSATION_ID, convid);
				msg.put(SFipa.RECEIVERS, BeliefDB.getTMID());

				agent.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
			}
			ready = false;
		}

	}

	@Goal
	public class Explore {
		@GoalResult
		private boolean explored;

		public synchronized boolean isExplored() {
			return explored;
		}

		public synchronized void setExplored(boolean explored) {
			this.explored = explored;
		}

	}

	@Plan(trigger = @Trigger(goals = (Explore.class)))
	public boolean explorePosition() {
		int level = SpatialManagerBDI.checkPlace(positionX, positionY);
		if (level <= 500 && type == 1) {
			return false;
		}
		int result = SpatialManagerBDI.explorePlace(positionX, positionY, currentExploration);
		if (result >= 0) {
			currentExploration = result;
			return true;
		} else {
			return false;
		}
	}

	@Goal
	public class Move {
		@GoalResult
		private boolean moved;

		public synchronized boolean isMoved() {
			return moved;
		}

		public synchronized void setMoved(boolean moved) {
			this.moved = moved;
		}

	}

	public boolean moveFromPosition() {
		String neighbours = "";
		if (type == 0) {
			neighbours = SpatialManagerBDI.getNeighbours(positionX, positionY);
		} else if (type == 1) {
			neighbours = SpatialManagerBDI.getNeighbours(positionX, positionY, 500);
		}

		if (neighbours.equals("")) {
			return false;
		} else {
			String neighStr[] = neighbours.split(TransformationManagerBDI.DEFAULT_SEPARATOR);
			int neighPosX[] = new int[neighStr.length / 3];
			int neighPosY[] = new int[neighStr.length / 3];
			double[] neighProb = new double[neighStr.length / 3];
			double totalSum = 0;
			int a = 0;
			for (int i = 0; i < neighStr.length;) {

				neighPosX[a] = Integer.parseInt(neighStr[i]);
				i++;
				neighPosY[a] = Integer.parseInt(neighStr[i]);
				i++;
				int aux = Integer.parseInt(neighStr[i]);
				neighProb[a] = aux;
				totalSum += aux;
				i++;
				a++;

			}
			if (totalSum == 0) {
				return false;
			}
			for (int i = 1; i < neighProb.length; i++) {
				neighProb[i] += neighProb[i - 1];
			}
			for (int i = 0; i < neighProb.length; i++) {
				neighProb[i] = neighProb[i] / totalSum;
			}
			double tries[] = { Math.random(), Math.random(), Math.random() };
			for (int i = 0; i < neighProb.length; i++) {
				for (int j = 0; j < tries.length; j++) {
					if (neighProb[i] >= tries[j]) {
						boolean mine = SpatialManagerBDI.getSpace(neighPosX[i], neighPosY[i]);
						if (mine) {
							positionX = neighPosX[i];
							positionY = neighPosY[i];
							return true;
						}
					}
				}
			}
		}
		return false;

	}

	public Object[] getMessageQueue() {
		return messageQueue;
	}

	public void setMessageQueue(Object[] messageQueue) {
		this.messageQueue = messageQueue;
	}

	public void addMessageToQueue(Object msg) {
		if (messageQueue == null) {
			messageQueue = new Object[1];
			messageQueue[0] = msg;
		} else {
			for (Object mes : messageQueue) {
				if (mes.equals(msg)) {
					return;
				}
			}
			Object[] aux = new Object[messageQueue.length + 1];
			for (int i = 0; i < messageQueue.length; i++) {
				aux[i] = messageQueue[i];
			}
			aux[messageQueue.length] = msg;
			messageQueue = aux;
		}
		messageQueueSize++;
	}

	public Object getMessageFromQueue() {
		if (messageQueue == null) {
			return null;
		}
		Object pop = messageQueue[messageQueueSize - 1];
		return pop;
	}
}
