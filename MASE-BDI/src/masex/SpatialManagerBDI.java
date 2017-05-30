package masex;

import java.util.HashMap;
import java.util.Map;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalResult;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.SUtil;
import jadex.micro.annotation.*;

@Agent
public class SpatialManagerBDI {

	@Agent
	BDIAgent manager;
	@Belief
	private static int width;
	@Belief
	private static int height;
	@Belief
	private static int[][] GRID;
	@Belief
	private int[][] GRIDMap;
	@Belief
	private int defaultFactorRangeX = 20;
	@Belief
	private int defaultFactorRangeY = 20;
	@Belief
	private static int[][] proximalMatrix;
	@Belief
	private static int bestValueInProximalMatrix = 0;
	@Belief
	private int worstValueInProximalMatrix = 0;
	@Belief
	private int messageQueueSize = 0;
	@Belief
	private Object[] messageQueue;
	@Belief
	private String[] managersNames;
	@Belief
	private static int[][] usedPositions;
	@Belief
	private static int[] deltaX = { 0, +1 };
	@Belief
	private static int[] deltaY = { 0, +1 };
	@Belief
	private static boolean exploreNeighbourhood = true;
	@Belief
	private static int impact = 150;
	@Belief
	private static int quantityOfPositions;

	@AgentCreated
	public void created() {
		Starter.print("created:" + this);
		// registers itself
		BeliefDB.setSMID(manager.getComponentIdentifier());
		// now initializes some beliefs
		GRID = BeliefDB.getInitialMap();
		width = BeliefDB.getWidth();
		height = BeliefDB.getHeight();
		// int GRIDMapX = (GRID.length / defaultFactorRangeX);
		// int GRIDMapY = (GRID[0].length / defaultFactorRangeY);
		// if (GRID.length % defaultFactorRangeX > 0) {
		// GRIDMapX++;
		// }
		// if (GRID[0].length % defaultFactorRangeY > 0) {
		// GRIDMapY++;
		// }
		// GRIDMap = new int[GRIDMapX][GRIDMapY];
		// for (int j = 0; j < GRIDMapY; j++) {
		// for (int i = 0; i < GRIDMapX; i++) {
		// GRIDMap[i][j] = 0;
		// }
		// }
		usedPositions = new int[width][height];
		proximalMatrix = BeliefDB.getMatrixProximal();
		bestValueInProximalMatrix = BeliefDB.bestProximalPosition;
	}

	@AgentBody
	public void body() {
		// String convid = SUtil.createUniqueId(manager.getAgentName());
		// Map<String, Object> msg = new HashMap<String, Object>();
		// msg.put(SFipa.CONTENT, "ready");
		// msg.put(SFipa.PERFORMATIVE, SFipa.INFORM);
		// msg.put(SFipa.CONVERSATION_ID, convid);
		// msg.put(SFipa.RECEIVERS, new IComponentIdentifier[] { GRIDId });
		// manager.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE);
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
					&& ((String) msg.get(SFipa.CONTENT)).contains(TransformationManagerBDI.NEW_POSITIONS)) {
				String content = (String) msg.get(SFipa.CONTENT);
				quantityOfPositions = Integer.parseInt(content.split(TransformationManagerBDI.DEFAULT_SEPARATOR)[1]);
				int[] positions = (int[]) manager.dispatchTopLevelGoal(new GetBestPositions()).get();

				// transforming positions in string
				String positionsStr = "";
				for (int i = 0; i < positions.length; i++) {
					positionsStr = positionsStr + positions[i] + TransformationManagerBDI.DEFAULT_SEPARATOR;
				}
				positionsStr = positionsStr.trim();
				String convid = SUtil.createUniqueId(manager.getAgentName());
				Map<String, Object> msg2 = new HashMap<String, Object>();
				msg2.put(SFipa.CONTENT, positionsStr);
				msg2.put(SFipa.PERFORMATIVE, SFipa.INFORM);
				msg2.put(SFipa.CONVERSATION_ID, convid);
				msg2.put(SFipa.RECEIVERS, BeliefDB.getTMID());
				manager.sendMessage(msg2, SFipa.FIPA_MESSAGE_TYPE);

			}
		}
	}

	@Goal
	public class GetBestPositions {

		@GoalResult
		private int[] positionsRequired;

		public synchronized int[] getPositionsRequired() {
			return positionsRequired;
		}

		public synchronized void setPositionsRequired(int[] positionsRequired) {
			this.positionsRequired = positionsRequired;
		}

		public GetBestPositions() {
		}

	}

	@Plan(trigger = @Trigger(goals = GetBestPositions.class))
	public int[] getBestPositions() {
		int[] positions = new int[quantityOfPositions * 2];
		int currentValue = bestValueInProximalMatrix;
		for (int index = 0; index < quantityOfPositions * 2;) {
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {

					if (proximalMatrix[i][j] == currentValue && usedPositions[i][j] == 0) {
						positions[index] = i;
						index++;
						positions[index] = j;
						index++;
						usedPositions[i][j] = 1;
						if (index >= quantityOfPositions * 2) {
							// break two fors
							j = height;
							i = width;
						}
					}
				}
			}
			currentValue--;
		}
		return positions;
	}

	public static int[] getBrandNewPosition() throws OutOfSpacesException {
		int[] positions = new int[2];
		int currentValue = bestValueInProximalMatrix;
		for (int index = 0; index < 2;) {
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					if (proximalMatrix[i][j] == currentValue && usedPositions[i][j] == 0 && GRID[i][j] > 1) {
						positions[index] = i;
						index++;
						positions[index] = j;
						index++;
						usedPositions[i][j] = 1;
						if (index >= 2) {
							// break two fors
							j = height;
							i = width;
						}
					}
				}
			}
			currentValue--;
			if (currentValue < 0) {
				// Starter.print("No more possible spaces!");
				throw new OutOfSpacesException();
				// System.exit(0);
			}
		}
		return positions;
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

	public synchronized static int checkPlace(int positionX, int positionY) {
		int a = 0;
		for (int j = 0; j < deltaY.length; j++) {
			for (int i = 0; i < deltaX.length; i++) {
				int x = positionX + deltaX[i];
				int y = positionY + deltaY[j];
				if (x > width || x < 0 || y > height || y < 0) {
					continue;
				}
				// getting lock for GRID
				// synchronized (GRID) {
				int level = GRID[x][y];
				if (level > a) {
					a = level;
				}

			}
		}
		return a;
	}

	public synchronized static int explorePlace(int positionX, int positionY, int amount) {
		if (positionX > width || positionY > height) {
			return -1;
		} else {
			boolean explored = false;
			int minAmount = amount;
			for (int j = 0; j < deltaY.length; j++) {
				for (int i = 0; i < deltaX.length; i++) {
					int x = positionX + deltaX[i];
					int y = positionY + deltaY[j];
					if (x > width || x < 0 || y > height || y < 0) {
						continue;
					}
					// getting lock for GRID
					// synchronized (GRID) {
					int level = GRID[x][y];
					if (level <= 1) {
						continue;
					}

					if (level - amount > 1) {
						GRID[x][y] = level - amount;
						minAmount = 0;
					} else {
						GRID[x][y] = 1;
						if (amount - level < minAmount) {
							minAmount = amount - level;
						}
					}
					explored = true;
					// }

				}
			}

			if (!explored) {
				return -1;
			}

			if (exploreNeighbourhood) {
				int[] neighbourhood = new int[] { positionX - 2, positionY, positionX, positionY - 2, positionX + 2,
						positionY, positionX, positionY + 2 };
				for (int index = 0; index < neighbourhood.length;) {
					int x = neighbourhood[index];
					index++;
					int y = neighbourhood[index];
					index++;

					for (int j = 0; j < deltaY.length; j++) {
						for (int i = 0; i < deltaX.length; i++) {
							int x1 = x + deltaX[i];
							int y1 = y + deltaY[j];
							if (x1 > width || x1 < 0 || y1 > height || y1 < 0) {
								continue;
							}
							// locking the GRID
							// synchronized (GRID) {
							int level = GRID[x1][y1];
							if (level < 0) {
								continue;
							}
							if (level - impact > 1) {
								GRID[x1][y1] = level - impact;
							} else {
								GRID[x1][y1] = 1;
							}
							// }
						}
					}
				}
			}

			if (minAmount <= 1)
				return 0;
			else
				return minAmount;

		}

	}

	public static synchronized boolean getSpace(int positionX, int positionY) {

		if (usedPositions[positionX][positionY] == 0) {

			for (int j = 0; j < deltaY.length; j++) {
				for (int i = 0; i < deltaX.length; i++) {
					int x = positionX + deltaX[i];
					int y = positionY + deltaY[j];
					if (x >= width || x < 0 || y >= height || y < 0) {
						continue;
					}
					usedPositions[x][y] = 1;
					proximalMatrix[x][y] = 0;
				}
			}

			return true;
		}
		return false;
	}

	public static String getNeighbours(int positionX, int positionY, int threshold) {
		String result = "";
		int[] neighbourhood = new int[] { positionX, positionY - 6,

				positionX - 2, positionY - 4, positionX, positionY - 4, positionX + 2, positionY - 4,

				positionX - 4, positionY - 2, positionX - 2, positionY - 2, positionX, positionY - 2, positionX + 2,
				positionY - 2, positionX + 4, positionY - 2,

				positionX - 6, positionY, positionX - 4, positionY, positionX - 2, positionY, positionX + 2, positionY,
				positionX + 4, positionY, positionX + 6, positionY,

				positionX - 4, positionY + 2, positionX - 2, positionY + 2, positionX, positionY + 2, positionX + 2,
				positionY + 2, positionX + 4, positionY + 2,

				positionX - 2, positionY + 4, positionX, positionY + 4, positionX + 2, positionY + 4,

				positionX, positionY + 6 };
		for (int index = 0; index < neighbourhood.length;) {
			int x = neighbourhood[index];
			index++;
			int y = neighbourhood[index];
			index++;

			for (int j = 0; j < deltaY.length; j++) {
				for (int i = 0; i < deltaX.length; i++) {
					int x1 = x + deltaX[i];
					int y1 = y + deltaY[j];
					if (x1 >= width || x1 < 0 || y1 >= height || y1 < 0) {
						continue;
					}
					if (GRID[x1][y1] < 0 || GRID[x1][y1] < threshold) {
						continue;
					}
					if (usedPositions[x1][y1] != 0) {
						continue;
					}
					if (proximalMatrix[x][y] <= 0) {
						continue;
					}
					result = result + x + TransformationManagerBDI.DEFAULT_SEPARATOR + y
							+ TransformationManagerBDI.DEFAULT_SEPARATOR + proximalMatrix[x][y]
							+ TransformationManagerBDI.DEFAULT_SEPARATOR;
					i = deltaX.length;
					j = deltaY.length;
				}
			}

		}
		return result;
	}

	public static String getNeighbours(int positionX, int positionY) {
		String result = "";
		int[] neighbourhood = new int[] { positionX, positionY - 6,

				positionX - 2, positionY - 4, positionX, positionY - 4, positionX + 2, positionY - 4,

				positionX - 4, positionY - 2, positionX - 2, positionY - 2, positionX, positionY - 2, positionX + 2,
				positionY - 2, positionX + 4, positionY - 2,

				positionX - 6, positionY, positionX - 4, positionY, positionX - 2, positionY, positionX + 2, positionY,
				positionX + 4, positionY, positionX + 6, positionY,

				positionX - 4, positionY + 2, positionX - 2, positionY + 2, positionX, positionY + 2, positionX + 2,
				positionY + 2, positionX + 4, positionY + 2,

				positionX - 2, positionY + 4, positionX, positionY + 4, positionX + 2, positionY + 4,

				positionX, positionY + 6 };
		for (int index = 0; index < neighbourhood.length;) {
			int x = neighbourhood[index];
			index++;
			int y = neighbourhood[index];
			index++;

			for (int j = 0; j < deltaY.length; j++) {
				for (int i = 0; i < deltaX.length; i++) {
					int x1 = x + deltaX[i];
					int y1 = y + deltaY[j];
					if (x1 >= width || x1 < 0 || y1 >= height || y1 < 0) {
						continue;
					}
					if (GRID[x1][y1] < 0) {
						continue;
					}
					if (usedPositions[x1][y1] != 0) {
						continue;
					}
					if (proximalMatrix[x][y] <= 0) {
						continue;
					}
					result = result + x + TransformationManagerBDI.DEFAULT_SEPARATOR + y
							+ TransformationManagerBDI.DEFAULT_SEPARATOR + proximalMatrix[x][y]
							+ TransformationManagerBDI.DEFAULT_SEPARATOR;
					i = deltaX.length;
					j = deltaY.length;
				}
			}

		}
		return result;
	}

	public static String generateIm(long simulationTime) {

		return ImageUtil.gerarImagem(width, height, GRID, usedPositions, quantityOfPositions,
				BeliefDB.getTransformationAgentGroupPercentage()[0], false, simulationTime);
	}

	public static String generateIm(String nomeImagem) {

		return ImageUtil.gerarImagem(width, height, GRID, usedPositions, quantityOfPositions,
				BeliefDB.getTransformationAgentGroupPercentage()[0], false, nomeImagem);
	}

	public static double[] generateResults(String imagemFinalPath) {
		int[][] mapaInicial = ImageUtil.filtragem_classes_iniciais(BeliefDB.getObservedSpaceSources()[0]);
		int[][] mapaFinal = ImageUtil.filtragem_classes_iniciais(BeliefDB.getObservedSpaceSources()[1]);
		int[][] mapaSimulado = ImageUtil.filtragem_classes_iniciais(imagemFinalPath);

		return MetricsUtil.evaluateChanges(BeliefDB.preservedState, mapaInicial, mapaFinal, mapaSimulado,
				BeliefDB.getLayers());

		// usar como desejar. O resultado do array é:
		/*
		 * figureOfMerit, producersaccuracy, usersaccuracy, wrongchange,
		 * rightchange, wrongpersistance, nullModel, simulatedNullModel
		 */
	}
}
