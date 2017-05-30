package masex;

import java.util.HashMap;
import java.util.Map;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.SUtil;
import jadex.micro.annotation.*;

@Agent
public class TransformationManagerBDI {
	@Agent
	private BDIAgent manager;
	@Belief
	private String[] transformationTypes;
	@Belief
	private String[] transformationPaths;
	@Belief
	private int[] numberOfTransformationAgents;
	@Belief
	private int[] percentageOfGroupAgents;
	@Belief
	private int[] agentsPositions;
	@Belief
	private int messageQueueSize = 0;
	@Belief
	private Object[] messageQueue;
	@Belief
	private static boolean[] agentsDone;
	@Belief
	private boolean creatingAgents = false;
	@Belief
	private boolean allDone = areAllDone();

	// public messages
	public static final String DEFAULT_SEPARATOR = " ";
	public static final String NEW_POSITIONS = "new_positions";
	public static final String AGENTS_READY = "agents_ready";
	public static final String AGENTS_DONE = "agents_done";

	@AgentCreated
	public void initTransformationManager() {
		Starter.print("created:" + this);
		// registers itself
		BeliefDB.setTMID(manager.getComponentIdentifier());
		// initialize beliefs
		transformationTypes = BeliefDB.getTransformationAgentTypes();
		transformationPaths = BeliefDB.getTransformationAgentClasses();
		numberOfTransformationAgents = BeliefDB.getTransformationAgentQty();
		percentageOfGroupAgents = BeliefDB.getTransformationAgentGroupPercentage();
	}

	@AgentBody
	public void body() {
		// the adopts a new goal
		manager.dispatchTopLevelGoal(new GetSuitablePositionsForAgents());
	}

	@AgentMessageArrived
	public void messageArrived(Map<String, Object> msg, final MessageType mt) {
		addMessageToQueue(msg);
	}

	@Goal
	public class GetSuitablePositionsForAgents {
		private boolean acquired = false;

		public boolean isAcquired() {
			return acquired;
		}

		public void setAcquired(boolean acquired) {
			this.acquired = acquired;
		}

	}

	@SuppressWarnings("unchecked")
	@Plan(trigger = @Trigger(factchangeds = "messageQueueSize"))
	public void attendRequisition(IPlan plan) {
		Map<String, Object> msg = (Map<String, Object>) getMessageFromQueue();
		if (msg != null) {
			if (msg.get(SFipa.PERFORMATIVE).equals(SFipa.INFORM)) {
				String agentsStr[] = ((String) msg.get(SFipa.CONTENT)).split(DEFAULT_SEPARATOR);
				agentsPositions = new int[agentsStr.length];
				for (int i = 0; i < agentsStr.length; i++) {
					agentsPositions[i] = Integer.parseInt(agentsStr[i]);
				}
				manager.dispatchTopLevelGoal(new InstantiateTransformationAgents()).get();

			} else if (msg.get(SFipa.PERFORMATIVE).equals(SFipa.CONFIRM)) {

				allDone = true;
			}

		}
	}

	@Plan(trigger = @Trigger(goals = GetSuitablePositionsForAgents.class))
	public boolean requestSpacesToSpatialManager(IPlan plan) {
		int qtyOfAgents = 0;
		for (int aux : numberOfTransformationAgents) {
			qtyOfAgents += aux;
		}
		Starter.print("TM is requesting " + qtyOfAgents + " positions to SM");
		String convid = SUtil.createUniqueId(manager.getAgentName());
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put(SFipa.CONTENT, NEW_POSITIONS + DEFAULT_SEPARATOR + qtyOfAgents);
		msg.put(SFipa.PERFORMATIVE, SFipa.REQUEST);
		msg.put(SFipa.CONVERSATION_ID, convid);
		// waits a little for SM to register itself
		while (BeliefDB.getSMID() == null) {

			try {
				Thread.sleep(200); // 200 milliseconds is one second.
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

		}
		msg.put(SFipa.RECEIVERS, BeliefDB.getSMID());
		manager.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
		return true;
	}

	@Goal
	public class InstantiateTransformationAgents {
		@GoalResult
		private boolean created = false;

		public void setCreated(boolean created) {
			this.created = created;
		}

		public boolean getCreated() {
			return created;
		}
	}

	@Plan(trigger = @Trigger(goals = InstantiateTransformationAgents.class))
	public boolean createTransformationAgents(IPlan plan) {
		if (creatingAgents) {
			return true;
		} else {
			creatingAgents = true;
		}
		final StringBuffer areAgentCreated = new StringBuffer();
		areAgentCreated.append("true");
		int qtyOfAgents = 0;
		for (int i = 0; i < numberOfTransformationAgents.length; i++) {
			qtyOfAgents += numberOfTransformationAgents[i];
		}
		agentsDone = new boolean[qtyOfAgents];
		BeliefDB.setTAID(new IComponentIdentifier[qtyOfAgents]);
		IComponentManagementService cms = manager.getServiceContainer()
				.searchServiceUpwards(IComponentManagementService.class).get();
		int typeCount = 0;
		for (int a = 0; a < transformationTypes.length; a++) {
			int numberOfGroupAgents = percentageOfGroupAgents[a] * agentsDone.length;
			for (int b = 0; b < numberOfTransformationAgents[a]; b++) {
				int index = (a * numberOfTransformationAgents[a]) + b;
				agentsDone[index] = false;
				int positionX = agentsPositions[2 * index];
				int positionY = agentsPositions[(2 * index) + 1];
				int explorationLevel = 0;
				if (index < numberOfGroupAgents) {
					explorationLevel = BeliefDB.getGroupExploration();
				} else {
					explorationLevel = BeliefDB.getIndividualExploration();
				}

				try {
					Map<String, Object> args = new HashMap<String, Object>();
					args.put("positionX", positionX);
					args.put("positionY", positionY);
					args.put("index", index);
					args.put("explorationLevel", explorationLevel);
					args.put("type", typeCount % 2);
					cms.createComponent(transformationTypes[a] + b, transformationPaths[a],
							new CreationInfo(null, args, manager.getComponentIdentifier(), false)).getFirstResult();
					typeCount++;
				} catch (Exception e) {
					areAgentCreated.delete(0, areAgentCreated.length() - 1);
					areAgentCreated.append("false");

				}
			}
		}

		// sending a message to GRID to inform that the agents a ready to start
		// simulating
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put(SFipa.CONTENT, AGENTS_READY);
		msg.put(SFipa.PERFORMATIVE, SFipa.REQUEST);
		String convid = SUtil.createUniqueId(manager.getAgentName());
		msg.put(SFipa.CONVERSATION_ID, convid);
		msg.put(SFipa.RECEIVERS, BeliefDB.getGRIDID());
		manager.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
		return Boolean.parseBoolean(areAgentCreated.toString());
	}

	@Plan(trigger = @Trigger(factchangeds = "allDone"))
	public void agentsDone() {
		if (allDone) {
			allDone = false;
			for (int i = 0; i < agentsDone.length; i++) {
				agentsDone[i] = false;
			}
			Map<String, Object> msg = new HashMap<String, Object>();
			msg.put(SFipa.CONTENT, AGENTS_DONE);
			msg.put(SFipa.PERFORMATIVE, SFipa.REQUEST);
			String convid = SUtil.createUniqueId(manager.getAgentName());
			msg.put(SFipa.CONVERSATION_ID, convid);
			msg.put(SFipa.RECEIVERS, BeliefDB.getGRIDID());
			manager.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
		}
	}

	// getters and setters
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

	public static synchronized boolean setAgentDone(boolean done, int index) {
		TransformationManagerBDI.agentsDone[index] = done;
		boolean allAgentsDone = false;
		for (boolean aux : agentsDone) {
			if (!aux) {
				return allAgentsDone;
			}
		}
		allAgentsDone = true;
		return allAgentsDone;
	}

	public synchronized boolean isAllDone() {

		return allDone;
	}

	public synchronized boolean areAllDone() {
		if (agentsDone == null) {
			return false;
		}
		for (boolean aux : agentsDone) {
			if (!aux) {
				return false;
			}
		}
		return true;
	}

	public synchronized void setAllDone(boolean allDone) {

		this.allDone = true;
	}
}
