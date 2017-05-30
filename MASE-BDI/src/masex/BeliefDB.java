package masex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.StringTokenizer;

import jadex.bridge.IComponentIdentifier;

public abstract class BeliefDB {
	private static int height;
	private static int width;
	private static int maxThread = 10;

	private static String[] transformationAgentTypes = {};
	private static String[] transformationAgentClasses = {};
	private static int[] transformationAgentQty = {};
	private static int[] transformationAgentGroupPercentage = {};
	private static String[] managersAgentTypes = {};
	private static String[] managersAgentClasses = {};
	private static int qtyOfSteps = 365;

	private static int individualExploration = 500;
	private static int groupExploration = 1500;

	private static int[][] initialMap = null;
	private static int[][] initialMapCopy = null;
	private static int[][] finalMap = null;
	private static int[][][] layers = null;
	private static int[][] PDOT = null;
	private static int[][] matrixProximal = null;
	private static String[] observedSpaceSources = {
			"model/inputs/spaces/2002.bmp", "model/inputs/spaces/2008.bmp" };
	private static String[] spatialAttributesSources = {
			"model/inputs/layers/cursosdagua.bmp",
			"model/inputs/layers/corposdagua.bmp",
			"model/inputs/layers/edificacoes.bmp",
			"model/inputs/layers/ferrovias.bmp",
			"model/inputs/layers/rodovias.bmp", "model/inputs/layers/ruas.bmp",
			"model/inputs/layers/ucs.bmp" };
	private static int[] spatialAttributesFactors = {};
	private static String publicPolicySource = "model/inputs/pdot/pdot.bmp";
	private static String saved[] = new String[] {
			"model/processed/initial.ser", "model/processed/final.ser",
			"model/processed/layers.ser", "model/processed/policy.ser",
			"model/processed/proximalmatrix.ser" };

	// Identifiers for agents
	private static IComponentIdentifier GRIDID = null;
	private static IComponentIdentifier SMID = null;
	private static IComponentIdentifier TMID = null;
	private static IComponentIdentifier[] TAID = null;

	private static String GRIDname = "GRID";

	// beliefs for space choosing
	private static int[][] usedPositions;
	private static int[][] impossiblePositions;
	public static int[] explorationMap = new int[] { +0, +0, +1, +1 };

	public static boolean DEBUG = false;
	public static boolean DEBUG_IM = false;

	private static boolean toggle_boost = true;
	// private static HashMap<String, String> map;

	public static boolean increaseAgents = true;
	public static int amountOfIncrease = 0;
	public static int amountOfPercentageIncrease = 0;

	public static boolean ride = false;
	public static String savePath = "";

	public static int bestProximalPosition = 0;
	public static int percentageIncrement = 0;
	public static int preservedState = 1500;
	public static int ignoredState = -1;
	public static int qtyOfExecutions = 0;

	public static boolean deleteFilesAtEnd = false;

	// initial method
	/*
	 * public static void init() throws IOException { HashMap<String, String>
	 * map = initParameters(); initCommonBeliefs(map); initMatrices(); }
	 */

	public static HashMap<String, String> initParameters() throws IOException {
		HashMap<String, String> map;
		// reading settings
		File f = new File("settings.ini");
		BufferedReader br = new BufferedReader(new FileReader(f));
		StringBuffer contents = new StringBuffer();
		String line = "";
		while (line != null) {
			contents.append(line);
			line = br.readLine();
		}
		br.close();
		// breaking into tokens
		StringTokenizer tokens = new StringTokenizer(contents.toString(), "=;");
		map = new HashMap<String, String>();
		// creating parameters map
		do {
			map.put(tokens.nextToken(), tokens.nextToken());
		} while (tokens.hasMoreTokens());
		return map;
	}

	public static void initCommonBeliefs(HashMap<String, String> map,
			boolean starting) {

		// now, setting each one of the beliefs
		if (map.containsKey("height")) {
			height = Integer.parseInt(map.get("height"));
		}

		if (map.containsKey("width")) {
			width = Integer.parseInt(map.get("width"));
		}

		if (map.containsKey("transformationAgentTypes")) {

			transformationAgentTypes = map.get("transformationAgentTypes")
					.split(",");
		}

		if (map.containsKey("transformationAgentClasses")) {
			transformationAgentClasses = map.get("transformationAgentClasses")
					.split(",");
		}

		if (map.containsKey("transformationAgentQty")) {

			if (starting) {
				String[] transformationAgentsQtyString = map.get(
						"transformationAgentQty").split(",");
				transformationAgentQty = new int[transformationAgentsQtyString.length];
				for (int i = 0; i < transformationAgentsQtyString.length; i++) {
					transformationAgentQty[i] = Integer
							.parseInt(transformationAgentsQtyString[i]);
				}
			}
		}

		if (map.containsKey("transformationAgentGroupPercentage")) {
			if (starting) {
				String[] transformationAgentGroupPercentageString = map.get(
						"transformationAgentGroupPercentage").split(",");
				transformationAgentGroupPercentage = new int[transformationAgentGroupPercentageString.length];
				for (int i = 0; i < transformationAgentGroupPercentageString.length; i++) {
					transformationAgentGroupPercentage[i] = Integer
							.parseInt(transformationAgentGroupPercentageString[i]);
				}
			}
		}

		if (map.containsKey("managersAgentTypes")) {
			managersAgentTypes = map.get("managersAgentTypes").split(",");
		}

		if (map.containsKey("managersAgentClasses")) {
			managersAgentClasses = map.get("managersAgentClasses").split(",");
		}

		if (map.containsKey("individualExploration")) {
			individualExploration = Integer.parseInt(map
					.get("individualExploration"));
		}

		if (map.containsKey("groupExploration")) {
			groupExploration = Integer.parseInt(map.get("groupExploration"));
		}

		if (map.containsKey("observedSpaceSources")) {
			observedSpaceSources = map.get("observedSpaceSources").split(",");
		}
		if (map.containsKey("spatialAttributesSources")) {
			spatialAttributesSources = map.get("spatialAttributesSources")
					.split(",");
		}
		if (map.containsKey("spatialAttributesSources")) {

			String[] spatialAttributesFactorsString = map.get(
					"spatialAttributesFactors").split(",");
			spatialAttributesFactors = new int[spatialAttributesFactorsString.length];
			for (int i = 0; i < spatialAttributesFactorsString.length; i++) {
				spatialAttributesFactors[i] = Integer
						.parseInt(spatialAttributesFactorsString[i]);
			}

		}
		if (map.containsKey("publicPolicySource")) {
			publicPolicySource = map.get("publicPolicySource");
		}
		if (map.containsKey("saved")) {

			saved = map.get("saved").split(",");
		}
		if (map.containsKey("increaseAgents")) {

			increaseAgents = Boolean.parseBoolean(map.get("increaseAgents"));
		}
		if (map.containsKey("qtyOfExecutions")) {
			if (starting) {
				qtyOfExecutions = Integer.parseInt(map.get("qtyOfExecutions"));
			}
		}
		if (map.containsKey("amountOfIncrease")) {
			amountOfIncrease = Integer.parseInt(map.get("amountOfIncrease"));
		}
		if (map.containsKey("amountOfPercentageIncrease")) {
			amountOfPercentageIncrease = Integer.parseInt(map
					.get("amountOfPercentageIncrease"));
		}
		if (map.containsKey("ride")) {
			ride = Boolean.parseBoolean(map.get("ride"));
		}
		if (map.containsKey("resultFolder")) {
			savePath = map.get("resultFolder").trim();
		}
		if (map.containsKey("deleteFilesAtEnd")) {
			deleteFilesAtEnd = Boolean.parseBoolean(map.get("deleteFilesAtEnd"));
		}
	}

	public static void initMatrices() {
		if (verifySerializableFiles()) {

			if (bestProximalPosition == 0) {
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						if (matrixProximal[i][j] > bestProximalPosition) {
							bestProximalPosition = matrixProximal[i][j];
						}
					}
				}
			}
			Starter.print("Best position value is: "
					+ bestProximalPosition);
			return;
		}
		initialMap = ImageUtil
				.filtragem_classes_iniciais(observedSpaceSources[0]);
		initialMapCopy = ImageUtil
				.filtragem_classes_iniciais(observedSpaceSources[0]);
		serializeArrayList(initialMap, saved[0]);
		finalMap = ImageUtil
				.filtragem_classes_iniciais(observedSpaceSources[1]);
		serializeArrayList(finalMap, saved[1]);
		layers = new int[spatialAttributesSources.length][width][height];
		for (int i = 0; i < layers.length; i++) {
			if (ride && i == 9) {
				layers[i] = ImageUtil
						.filtragem_solo(spatialAttributesSources[i]);
			} else if (ride && i == 10) {
				layers[i] = ImageUtil
						.filtragem_relevo(spatialAttributesSources[i]);
			} else if (ride && i == 11) {

			} else if (ride && i == 12) {

			} else {

				layers[i] = ImageUtil
						.filtragem_gaussiana(spatialAttributesSources[i]);
			}
		}
		serializeArrayList(layers, saved[2]);
		PDOT = ImageUtil.filtragem_pdot(publicPolicySource);
		serializeArrayList(PDOT, saved[3]);
		matrixProximal = new int[width][height];
		int max = 0;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {

				int layersSum = 0;
				for (int k = 0; k < spatialAttributesFactors.length; k++) {
					if (ride && (k == 9 || k == 10)) {
						continue;
					}
					if ((layers[k][i][j] < 0 && spatialAttributesFactors[k] != 0)
							|| initialMap[i][j] < 0) {
						layersSum = ride ? -9 : -7;
						break;
					} else {
						layersSum += (spatialAttributesFactors[k] * layers[k][i][j]);
					}

				}

				int layersMean = (int) (ride ? Math
						.ceil(((float) layersSum) / 9f) : Math
						.ceil(((float) layersSum) / 7f));
				if (ride) {
					layersMean += (spatialAttributesFactors[9] * layers[9][i][j])
							+ (spatialAttributesFactors[10] * layers[10][i][j]);
				} else if (PDOT[i][j] == 2 && layersMean > 0) {
					layersMean -= 1;
				} else if (PDOT[i][j] == 3 && layersMean > 0) {
					layersMean += 2;
					if (toggle_boost && initialMap[i][j] == 1500) {
						layersMean += 2;
					}
				}
				if (layersMean < 0)
					layersMean = 0;
				if (layersMean > max)
					max = layersMean;
				matrixProximal[i][j] = layersMean;
			}

		}
		bestProximalPosition = max;
		serializeArrayList(matrixProximal, saved[4]);
	}

	private static boolean verifySerializableFiles() {
		for (int i = 0; i < saved.length; i++) {
			int[][] atual = null;
			int[][][] atual2 = null;
			try (InputStream file = new FileInputStream(saved[i]);
					InputStream buffer = new BufferedInputStream(file);
					ObjectInput input = new ObjectInputStream(buffer);) {
				// deserialize the List
				if (i != 2) {
					atual = ((int[][]) input.readObject());
				} else {
					atual2 = ((int[][][]) input.readObject());
				}
			} catch (ClassNotFoundException ex) {
				Starter.print("Couldn't deserialize " + i);
				return false;
			} catch (IOException ex) {

				Starter.print("Couldn't deserialize " + i);
				return false;
			}
			switch (i) {
			case 0:
				initialMap = atual;
				initialMapCopy = atual;
				break;
			case 1:
				finalMap = atual;
				break;
			case 2:
				layers = atual2;
				break;
			case 3:
				PDOT = atual;
				break;
			case 4:
				matrixProximal = atual;
				break;
			default:

			}
		}
		return true;
	}

	private static void serializeArrayList(int[][] array, String name) {
		// serialize the List
		try {
			/*
			 * File f = new File(name); if (!f.exists()) { f.createNewFile(); }
			 */
			OutputStream file = new FileOutputStream(name);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(array);
			output.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static void serializeArrayList(int[][][] array, String name) {
		// serialize the List
		try {
			/*
			 * File f = new File(name); if (!f.exists()) { f.createNewFile(); }
			 */
			OutputStream file = new FileOutputStream(name);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(array);
			output.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// getters and setters
	public static synchronized int getHeight() {
		return height;
	}

	public static synchronized void setHeight(int height) {
		BeliefDB.height = height;
	}

	public static synchronized int getWidth() {
		return width;
	}

	public static synchronized void setWidth(int width) {
		BeliefDB.width = width;
	}

	public static synchronized int getMaxThread() {
		return maxThread;
	}

	public static synchronized void setMaxThread(int maxThread) {
		BeliefDB.maxThread = maxThread;
	}

	public static synchronized String[] getTransformationAgentTypes() {
		return transformationAgentTypes;
	}

	public static synchronized void setTransformationAgentTypes(
			String[] transformationAgentTypes) {
		BeliefDB.transformationAgentTypes = transformationAgentTypes;
	}

	public static synchronized String[] getTransformationAgentClasses() {
		return transformationAgentClasses;
	}

	public static synchronized void setTransformationAgentClasses(
			String[] transformationAgentClasses) {
		BeliefDB.transformationAgentClasses = transformationAgentClasses;
	}

	public static synchronized int[] getTransformationAgentQty() {
		return transformationAgentQty;
	}

	public static synchronized void setTransformationAgentQty(
			int[] transformationAgentQty) {
		BeliefDB.transformationAgentQty = transformationAgentQty;
	}

	public static synchronized int[] getTransformationAgentGroupPercentage() {
		return transformationAgentGroupPercentage;
	}

	public static synchronized void setTransformationAgentGroupPercentage(
			int[] transformationAgentGroupPercentage) {
		BeliefDB.transformationAgentGroupPercentage = transformationAgentGroupPercentage;
	}

	public static synchronized String[] getManagersAgentTypes() {
		return managersAgentTypes;
	}

	public static synchronized void setManagersAgentTypes(
			String[] managersAgentTypes) {
		BeliefDB.managersAgentTypes = managersAgentTypes;
	}

	public static synchronized String[] getManagersAgentClasses() {
		return managersAgentClasses;
	}

	public static synchronized void setManagersAgentClasses(
			String[] managerAgentClasses) {
		BeliefDB.managersAgentClasses = managerAgentClasses;
	}

	public static synchronized int getQtyOfSteps() {
		return qtyOfSteps;
	}

	public static synchronized void setQtyOfSteps(int qtyOfSteps) {
		BeliefDB.qtyOfSteps = qtyOfSteps;
	}

	public static synchronized int getIndividualExploration() {
		return individualExploration;
	}

	public static synchronized void setIndividualExploration(
			int individualExploration) {
		BeliefDB.individualExploration = individualExploration;
	}

	public static synchronized int getGroupExploration() {
		return groupExploration;
	}

	public static synchronized void setGroupExploration(int groupExploration) {
		BeliefDB.groupExploration = groupExploration;
	}

	public static synchronized int[][] getInitialMap() {
		return initialMap;
	}

	public static synchronized void setInitialMap(int[][] a) {
		BeliefDB.initialMap = a;
	}

	public static synchronized int[][] getFinalMap() {
		return finalMap;
	}

	public static synchronized void setFinalMap(int[][] a) {
		BeliefDB.finalMap = a;
	}

	public static synchronized int[][][] getLayers() {
		return layers;
	}

	public static synchronized void setLayers(int[][][] layers) {
		BeliefDB.layers = layers;
	}

	public static synchronized int[][] getPDOT() {
		return PDOT;
	}

	public static synchronized void setPDOT(int[][] pDOT) {
		PDOT = pDOT;
	}

	public static synchronized int[][] getMatrixProximal() {
		return matrixProximal;
	}

	public static synchronized void setMatrixProximal(int[][] matrixProximal) {
		BeliefDB.matrixProximal = matrixProximal;
	}

	public static synchronized String[] getObservedSpaceSources() {
		return observedSpaceSources;
	}

	public static synchronized void setObservedSpaceSources(
			String[] observedSpaceSources) {
		BeliefDB.observedSpaceSources = observedSpaceSources;
	}

	public static synchronized String[] getSpatialAttributesSources() {
		return spatialAttributesSources;
	}

	public static synchronized void setSpatialAttributesSources(
			String[] spatialAttributesSources) {
		BeliefDB.spatialAttributesSources = spatialAttributesSources;
	}

	public static synchronized int[] getSpatialAttributesFactors() {
		return spatialAttributesFactors;
	}

	public static synchronized void setSpatialAttributesFactors(
			int[] spatialAttributesFactors) {
		BeliefDB.spatialAttributesFactors = spatialAttributesFactors;
	}

	public static synchronized String getPublicPolicySource() {
		return publicPolicySource;
	}

	public static synchronized void setPublicPolicySource(
			String publicPolicySource) {
		BeliefDB.publicPolicySource = publicPolicySource;
	}

	public static synchronized String[] getSaved() {
		return saved;
	}

	public static synchronized void setSaved(String[] saved) {
		BeliefDB.saved = saved;
	}

	public static synchronized IComponentIdentifier getGRIDID() {
		return GRIDID;
	}

	public static synchronized void setGRIDID(IComponentIdentifier gRIDID) {
		GRIDID = gRIDID;
	}

	public static synchronized IComponentIdentifier getSMID() {
		return SMID;
	}

	public static synchronized void setSMID(IComponentIdentifier sMID) {
		SMID = sMID;
	}

	public static synchronized IComponentIdentifier getTMID() {
		return TMID;
	}

	public static synchronized void setTMID(IComponentIdentifier tMID) {
		TMID = tMID;
	}

	public static synchronized IComponentIdentifier[] getTAID() {
		return TAID;
	}

	public static synchronized void setTAID(IComponentIdentifier[] tAID) {
		TAID = tAID;
	}

	public static synchronized void setTAID(IComponentIdentifier tAID, int pos) {
		TAID[pos] = tAID;
	}

	public static synchronized String getGRIDname() {
		return GRIDname;
	}

	public static synchronized void setGRIDname(String gRIDname) {
		GRIDname = gRIDname;
	}

	public static synchronized int[][] getUsedPositions() {
		return usedPositions;
	}

	public static synchronized void setUsedPositions(int[][] usedPositions) {
		BeliefDB.usedPositions = usedPositions;
	}

	public static synchronized int[][] getImpossiblePositions() {
		return impossiblePositions;
	}

	public static synchronized void setImpossiblePositions(
			int[][] impossiblePositions) {
		BeliefDB.impossiblePositions = impossiblePositions;
	}

	public static synchronized int[] getExplorationMap() {
		return explorationMap;
	}

	public static synchronized void setExplorationMap(int[] explorationMap) {
		BeliefDB.explorationMap = explorationMap;
	}

	public static synchronized boolean isDEBUG() {
		return DEBUG;
	}

	public static synchronized void setDEBUG(boolean dEBUG) {
		DEBUG = dEBUG;
	}

	public static synchronized boolean isDEBUG_IM() {
		return DEBUG_IM;
	}

	public static synchronized void setDEBUG_IM(boolean dEBUG_IM) {
		DEBUG_IM = dEBUG_IM;
	}

	public static synchronized boolean isToggle_boost() {
		return toggle_boost;
	}

	public static synchronized void setToggle_boost(boolean toggle_boost) {
		BeliefDB.toggle_boost = toggle_boost;
	}

	public static int[][] getInitialMapCopy() {
		return initialMapCopy;
	}

	public static void setInitialMapCopy(int[][] initialMapCopy) {
		BeliefDB.initialMapCopy = initialMapCopy;
	}

}
