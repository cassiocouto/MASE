package masex;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalResult;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;

@Agent
@Arguments({ @Argument(name = "positionX", clazz = Integer.class),
		@Argument(name = "positionY", clazz = Integer.class),
		@Argument(name = "explorationLevel", clazz = Integer.class),
		@Argument(name = "index", clazz = Integer.class) })
public class TransformationAgentDummyBDI {

	@Agent
	BDIAgent agent;
	@Belief
	private int positionX;
	@Belief
	private int positionY;
	@Belief
	private int explorationLevel;
	@Belief
	private int index;
	@Belief
	private static boolean ready = false;
	@Belief
	private int currentExploration;

	@AgentCreated
	public void created() {
		Starter.print("created:" + this);
		positionX = (Integer) agent.getArgument("positionX");
		positionY = (Integer) agent.getArgument("positionY");
		explorationLevel = (Integer) agent.getArgument("explorationLevel");
		index = (Integer) agent.getArgument("index");
		currentExploration = explorationLevel;
	}

	@Plan(trigger = @Trigger(factchangeds = "ready"))
	public void deliberate() {
		boolean worked = false;
		boolean stuck = false;
		while (ready && currentExploration > 0) {
			// tries to explore
			boolean explored = (Boolean) agent.dispatchTopLevelGoal(
					new Explore()).get();
			if (!explored) {
				// tries to move
				boolean move = (Boolean) agent.dispatchTopLevelGoal(new Move())
						.get();
				if (!move) {
					// agent stuck - get help from Transformation Manager
					Starter.print(agent.getAgentName()
							+ "is out of moves.");
					stuck = true;
					break;
				}
			} else if (explored && currentExploration <= 0) {
				worked = true;
			}
		}

		if (worked) {
			currentExploration = explorationLevel;
			TransformationManagerBDI.setAgentDone(true, index);
		} else if (stuck) {

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
		int result = SpatialManagerBDI.explorePlace(positionX, positionY,
				currentExploration);
		if (result >= 0) {
			currentExploration = result;
			Starter.print(agent.getAgentName() + " explored position x:"
					+ positionX + ", y:" + positionY);
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
	@Plan(trigger = @Trigger(goals = (Move.class)))
	public boolean moveFromPosition() {
		String neighbours = SpatialManagerBDI.getNeighbours(positionX,
				positionY);

		if (neighbours.equals("")) {
			return false;
		} else {
			String neighStr[] = neighbours
					.split(TransformationManagerBDI.DEFAULT_SEPARATOR);
			int neighPosX[] = new int[neighStr.length / 3];
			int neighPosY[] = new int[neighStr.length / 3];
			double[] neighProb = new double[neighStr.length / 3];
			double totalSum = 0;
			for (int i = 0; i < neighStr.length;) {
				neighPosX[i] = Integer.parseInt(neighStr[i]);
				i++;
				neighPosY[i] = Integer.parseInt(neighStr[i]);
				i++;
				neighProb[i] = Integer.parseInt(neighStr[i]);
				i++;
				totalSum += neighProb[i];
			}
			if (totalSum == 0) {
				return false;
			}
			for (int i = 1; i < neighProb.length; i++) {
				neighProb[i] += neighProb[i--];
			}
			for (int i = 0; i < neighProb.length; i++) {
				neighProb[i] = neighProb[i] / totalSum;
			}
			double tries[] = { Math.random(), Math.random(), Math.random() };
			for (int i = 0; i < neighProb.length; i++) {
				for (int j = 0; j < tries.length; j++) {
					if (neighProb[i] >= tries[j]) {
						boolean mine = SpatialManagerBDI.getSpace(neighPosX[i],
								neighPosY[i]);
						if (mine) {
							positionX = neighPosX[i];
							positionY = neighPosY[i];
							Starter.print(agent.getAgentName()
									+ " changed to position x:" + positionX
									+ ", y:" + positionY);
							return true;
						}
					}
				}
			}
		}
		return false;

	}

	public static synchronized boolean isReady() {
		return ready;
	}

	public static synchronized void setReady(boolean ready) {
		TransformationAgentDummyBDI.ready = ready;
	}
}
