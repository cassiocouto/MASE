package masex;

public class MetricsUtil {
	/*
	 * Esse método retorna a figura de mérito da simulação de acordo com Pontius
	 * et Al. Pegamos os estados inicial e final observados (ou seja, reais),
	 * mapeados nos valores preservados, antropizados e ignorados, e o estado
	 * simulado para compara onde houve preservação e antropização
	 * corretos/incorretos.
	 * 
	 * 
	 * 
	 * Parâmetros: preservedValue = valor que no mapa representa o estado de
	 * preservado. por exemplo, no cerrado-df é 1500. Qualquer valor abaixo
	 * disso mas superior a 0 será considerado como antropizado
	 * 
	 * 
	 * initialState = mapa inicial real (observado)
	 * 
	 * finalState = mapa final real (observado)
	 * 
	 * simulatedState = mapa final simulado
	 * 
	 * layers = mapa com as camadas da simulação que devem ser ignorados
	 */
	public static double[] evaluateChanges(int preservedValue,
			int[][] initialState, int[][] finalState,
			int[][] simulatedFinalState, int[][][] layers) {

		double figureOfMerit = 0; // esse será o nosso retorno no final, apenas
									// declarando e atribuindo um valor inicial
		double nullModel = 0; // esse será nosso retorno do modelo nulo
		double simulatedNullModel = 0;// esse será nosso retorno do modelo nulo
										// simulado
		int statePreserved = 0;
		int stateChanged = 1;
		/* esses são usados para o cálculo do modelo nulo */
		long observedQtyCorrectlyPreserved = 0;
		long observedQtyCorrectlyConverted = 0;
		long observedQtyIncorrectlyPreserved = 0;
		long observedQtyIncorrectlyConverted = 0;

		long simulatedQtyCorrectlyPreserved = 0;
		long simulatedQtyCorrectlyConverted = 0;
		long simulatedQtyIncorrectlyPreserved = 0;
		long simulatedQtyIncorrectlyConverted = 0;

		/* essas variáveis são usadas para o cálculo de figura de mérito */
		double type1 = 0;// correctly converted
		double type3 = 0;// incorrectly converted
		double type4 = 0;// incorrectly preserved
		double valid = 0;// valid positions (anything but borders and layers)

		int height = BeliefDB.getHeight();
		int width = BeliefDB.getWidth();

		int[][] initialVersusFinalMap = new int[width][height];

		// pegando onde as layers existem
		int ignoredLayers[][] = ImageUtil.filtragem_layers_basica(
				BeliefDB.getSpatialAttributesSources(), width, height);

		// primeiro vamos comparar 2002 com 2008
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				if (ignoredLayers[i][j] == 1 || initialState[i][j] < 0) {
					initialVersusFinalMap[i][j] = -1;
				} else if (initialState[i][j] == preservedValue
						&& finalState[i][j] == preservedValue) {

					initialVersusFinalMap[i][j] = statePreserved;
					observedQtyCorrectlyPreserved++;

				} else if (initialState[i][j] < preservedValue
						&& finalState[i][j] < preservedValue) {

					initialVersusFinalMap[i][j] = statePreserved;
					observedQtyCorrectlyConverted++;

				} else if (initialState[i][j] == preservedValue
						&& finalState[i][j] < preservedValue) {
					initialVersusFinalMap[i][j] = stateChanged;
					observedQtyIncorrectlyPreserved++;

				} else if (initialState[i][j] < preservedValue
						&& finalState[i][j] == preservedValue) {

					initialVersusFinalMap[i][j] = stateChanged;
					observedQtyIncorrectlyConverted++;

				}
			}
		}
		// depois, vamos comparar 2008 com simulado
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				if (ignoredLayers[i][j] == 1 || simulatedFinalState[i][j] < 0) {
					// ignorar
				} else if (simulatedFinalState[i][j] == preservedValue
						&& finalState[i][j] == preservedValue) {

					simulatedQtyCorrectlyPreserved++;
					valid++;
				} else if (simulatedFinalState[i][j] < preservedValue
						&& finalState[i][j] < preservedValue) {
					if (initialVersusFinalMap[i][j] == stateChanged) {
						type1++;
					}
					simulatedQtyCorrectlyConverted++;
					valid++;
				} else if (simulatedFinalState[i][j] == preservedValue
						&& finalState[i][j] < preservedValue) {
					type3++;
					simulatedQtyIncorrectlyPreserved++;valid++;
				} else if (simulatedFinalState[i][j] < preservedValue
						&& finalState[i][j] == preservedValue) {
					type4++;
					simulatedQtyIncorrectlyConverted++;valid++;
				}
			}
		}

		// agora vamos calcular o nullModel
		long totalObserved = observedQtyCorrectlyPreserved
				+ observedQtyCorrectlyConverted
				+ observedQtyIncorrectlyConverted
				+ observedQtyIncorrectlyPreserved;
		long errorsObserved = observedQtyIncorrectlyConverted
				+ observedQtyIncorrectlyPreserved;
		nullModel = (double) ((double) errorsObserved / (double) totalObserved);
		// calculando o simulatedNullModel
		long totalSimulated = simulatedQtyCorrectlyPreserved
				+ simulatedQtyCorrectlyConverted
				+ simulatedQtyIncorrectlyConverted
				+ simulatedQtyIncorrectlyPreserved;
		long errorsSimulated = simulatedQtyIncorrectlyConverted
				+ simulatedQtyIncorrectlyPreserved;
		simulatedNullModel = (double) ((double) errorsSimulated / (double) totalSimulated);

		// calculando figura de mérito
		double wrongchange = 100 * (type4 / valid);
		double rightchange = 100 * (type1 / valid);
		double wrongpersistance = 100 * (type3 / valid);
		figureOfMerit = 100 * (type1 / (type1 + type3 + type4));
		double producersaccuracy = 100 * (type1 / (type1 + type3));
		double usersaccuracy = 100 * (type1 / (type1 + type4));
		
		return new double[] { figureOfMerit, producersaccuracy, usersaccuracy,
				wrongchange, rightchange, wrongpersistance, nullModel,
				simulatedNullModel };
	}
	
}
