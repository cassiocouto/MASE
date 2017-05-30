package masex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.SUtil;
import jadex.micro.annotation.*;

@Agent
public class GRIDBDI {

	@Agent
	BDIAgent GRID;

	@Belief
	public static String managersNames[];
	@Belief
	private String managersPaths[];
	@Belief
	private boolean managersCreated = false;
	@Belief
	private int messageQueueSize = 0;
	@Belief
	private Object[] messageQueue;
	@Belief
	private int steps;
	@Belief
	private int currentStep = 0;
	@Belief
	private boolean simulationDone = false;
	@Belief
	private long simulationTime;
	@Belief
	private long stepTime;
	@Belief
	private StringBuffer stepEvaluation;
	public static IComponentManagementService cms;
	private boolean failure = false;

	@AgentCreated
	public void agentCreated() {
		Starter.print("created:" + this);
		// registering id
		BeliefDB.setGRIDID(GRID.getComponentIdentifier());
		// setting beliefs
		managersNames = BeliefDB.getManagersAgentTypes();
		managersPaths = BeliefDB.getManagersAgentClasses();
		steps = BeliefDB.getQtyOfSteps();
	}

	@AgentBody
	public void body() {
		// First goal
		boolean created = (Boolean) GRID.dispatchTopLevelGoal(new InstantiateManagers()).get();
		if (!created) {
			// exception: The managers couldn't be created
		} else {
			managersCreated = true;
		}
		// second goal
		GRID.dispatchTopLevelGoal(new WaitForManagers());

	}

	@AgentMessageArrived
	public void messageArrived(Map<String, Object> msg, final MessageType mt) {
		addMessageToQueue(msg);
	}

	/*
	 * First Goal: Instantiate Managers, so they can instantiate the environment
	 * and the transformation agents. The success of this goal returns the ids
	 * for the created managers.
	 */
	@Goal
	public class InstantiateManagers {

		@GoalResult
		private boolean created = false;

		public synchronized boolean isCreated() {
			return created;
		}

		public synchronized void setCreated(boolean created) {
			this.created = created;
		}

		public InstantiateManagers() {

		}
	}

	@Plan(trigger = @Trigger(goals = InstantiateManagers.class) )
	public boolean startManagers() {
		final StringBuffer areAgentCreated = new StringBuffer();
		areAgentCreated.append("true");

		for (int i = 0; i < managersNames.length; i++) {

			Map<String, Object> args = new HashMap<String, Object>();
			cms = GRID.getServiceContainer().searchServiceUpwards(IComponentManagementService.class).get();
			Exception aux = cms.createComponent(managersNames[i], managersPaths[i],
					new CreationInfo(null, args, GRID.getComponentIdentifier())).getException();
			if (aux != null)
				Starter.print(aux.getMessage());
		}

		return Boolean.parseBoolean(areAgentCreated.toString());
	}

	/*
	 * The second goal is to wait for the Managers to start the simulation.
	 */
	@Goal
	public class WaitForManagers {

	}

	/*
	 * The third goal is to maintain the simulation running
	 */
	@Goal
	public class RunSimulation {
	}

	/*
	 * The last goal is to evaluate the simulation
	 */
	public class EvaluateResults {
	}

	@SuppressWarnings("unchecked")
	@Plan(trigger = @Trigger(factchangeds = "messageQueueSize") )
	public void attendRequisition(IPlan plan) throws OutOfSpacesException {
		Map<String, Object> msg = (Map<String, Object>) getMessageFromQueue();
		if (msg != null) {

			if (msg.get(SFipa.CONTENT) instanceof String
					&& (((String) msg.get(SFipa.CONTENT)).contains(TransformationManagerBDI.AGENTS_READY)
							|| ((String) msg.get(SFipa.CONTENT)).contains(TransformationManagerBDI.AGENTS_DONE))) {
				// agents are ready to be deployed
				GRID.dispatchTopLevelGoal(new RunSimulation());
			} else if (msg.get(SFipa.CONTENT) instanceof String
					&& (((String) msg.get(SFipa.CONTENT)).contains(TransformationManagerBDI.AGENTS_READY)
							|| ((String) msg.get(SFipa.CONTENT)).contains(TransformationManagerBDI.AGENTS_DONE))) {
				// agents are ready to be deployed
				GRID.dispatchTopLevelGoal(new RunSimulation());
			} else if (msg.get(SFipa.CONTENT) instanceof String
					&& (msg.get(SFipa.PERFORMATIVE).equals(SFipa.FAILURE))) {
				if(!failure){
				    // agents are failing
				    Starter.print("simulation interrupted!");
					failure = true;
					avaliarResultados();
					destruirRecursos();
					GRID.killAgent();
				}
			}
		}
	}

	@Plan(trigger = @Trigger(goals = RunSimulation.class) )
	public void step() {
		if (currentStep < steps) {

			Starter.print("step " + currentStep);

			// send msg to agents
			Map<String, Object> msg = new HashMap<String, Object>();
			msg.put(SFipa.CONTENT, "");
			msg.put(SFipa.PERFORMATIVE, SFipa.REQUEST);
			String convid = SUtil.createUniqueId(GRID.getAgentName());
			msg.put(SFipa.CONVERSATION_ID, convid);
			msg.put(SFipa.RECEIVERS, BeliefDB.getTAID());
			GRID.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
			currentStep++;
		} else {
			Starter.print("simulation finished!");
			avaliarResultados();
			destruirRecursos();
			GRID.killAgent();
		}
	}

	public void avaliarResultados() {
		long current = System.currentTimeMillis();
		simulationTime = (current - Starter.time);

		// informar aqui o nome da imagem

		String imageName = Starter.platformName + "agentes"
				+ (BeliefDB.getTransformationAgentQty()[0] + BeliefDB.getTransformationAgentQty()[1]) + "porcentagem"
				+ (BeliefDB.getTransformationAgentGroupPercentage()[0]);
		if (!BeliefDB.increaseAgents) {
			imageName = imageName + "rodada" + BeliefDB.qtyOfExecutions;
		}
		String pathImagem = SpatialManagerBDI.generateIm(imageName);

		Starter.print("time:" + simulationTime);
		double resultados[] = SpatialManagerBDI.generateResults(pathImagem);
		String zipname = null;
		try {
			String csvName = imageName + ".csv";
			criaCSV(csvName, resultados, currentStep, simulationTime);
			try {
				zipname = ZipAndSend.zip(imageName, new String[] { imageName + ".bmp", csvName });
				ZipAndSend.upload(zipname);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (BeliefDB.deleteFilesAtEnd) {
				new File(pathImagem).delete();
				new File(pathImagem.replace(".bmp", ".csv")).delete();
				new File(zipname).delete();
			}
		}
	}

	public void destruirRecursos() {
		for (int i = 0; i < BeliefDB.getTAID().length; i++) {
			cms.destroyComponent(BeliefDB.getTAID()[i]);
		}
		BeliefDB.setSMID(null);
		cms.destroyComponent(BeliefDB.getSMID());
		BeliefDB.setTMID(null);
		cms.destroyComponent(BeliefDB.getTMID());
	}

	public static void criaCSV(String csvName, double[] resultados, int steps, long simulationTime) throws IOException {

		String cabecalhoCsv = "time;qtyAgents;percentageAgents;individualExploration;groupExploration;figureOfMerit;producersaccuracy;usersaccuracy;wrongchange;rightchange;wrongpersistance;nullModel;simulatedNullModel;steps\n";
		String resultadosCsv = "" + simulationTime;
		resultadosCsv = resultadosCsv + ";"
				+ (BeliefDB.getTransformationAgentQty()[0] + BeliefDB.getTransformationAgentQty()[1]);
		resultadosCsv = resultadosCsv + ";" + (BeliefDB.getTransformationAgentGroupPercentage()[0]);
		resultadosCsv = resultadosCsv + ";" + (BeliefDB.getIndividualExploration()) + ";"
				+ (BeliefDB.getGroupExploration());
		resultadosCsv = resultadosCsv + ";" + resultados[0] + ";" + resultados[1] + ";" + resultados[2] + ";"
				+ resultados[3] + ";" + resultados[4] + ";" + resultados[5] + ";" + resultados[6] + ";" + resultados[7]
				+ ";" + steps;
		File reseval = new File(csvName);
		if (!reseval.exists()) {
			reseval.createNewFile();
		}
		FileWriter fw = new FileWriter(reseval);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(cabecalhoCsv);
		bw.write(resultadosCsv);
		bw.flush();
		bw.close();
		fw.close();

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

}
