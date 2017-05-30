package masex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.settings.ISettingsService;
import jadex.commons.Tuple;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ITuple2Future;
import jadex.commons.future.ThreadSuspendable;
import jadex.commons.future.TupleResult;

public class Starter {
	private static IComponentManagementService cms;
	private static ISettingsService settings;
	public static long time;
	public static String[] args;
	public static IExternalAccess platform;
	public static String platformName;
	public static boolean starting = true;
	private static boolean debug = true;

	public static void main(String[] args){
		print("Initializing Belief database");
		Starter.args = args;
		start();
	}

	public static void start(){
		HashMap<String, String> map = new HashMap<String, String>();
		try {
			map = BeliefDB.initParameters();
			BeliefDB.initCommonBeliefs(map, starting);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		map = new HashMap<String, String>();
		ArrayList<String> argumentosJADEX = new ArrayList<String>();

		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].contains("MASE")) {
					StringTokenizer tokens = new StringTokenizer(args[i].replace("-MASE", ""), "=");
					do {
						map.put(tokens.nextToken(), tokens.nextToken());
					} while (tokens.hasMoreTokens());
				} else {
					argumentosJADEX.add(args[i]);
				}
			}

			if (!map.isEmpty()) {
				try {
					BeliefDB.initCommonBeliefs(map, starting);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		try {
			BeliefDB.initMatrices();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		starting = false;
		time = System.currentTimeMillis();
		// Creating parameters for the platform
		String[] defargs = new String[] { "-gui", "false", "-welcome", "false", "-cli", "false", "-printpass",
				"false", "-awareness", "false" };
		String[] argumentosJadexString = new String[argumentosJADEX.size()];
		for (int i = 0; i < argumentosJADEX.size(); i++) {
			argumentosJadexString[i] = argumentosJADEX.get(i);
		}
		String[] newargs = new String[defargs.length + argumentosJadexString.length];
		System.arraycopy(defargs, 0, newargs, 0, defargs.length);
		System.arraycopy(argumentosJadexString, 0, newargs, defargs.length, argumentosJadexString.length);
		// Starting platform
		IFuture<IExternalAccess> plataform = jadex.base.Starter.createPlatform(newargs);
		// creating auxiliary thread
		ThreadSuspendable sus = new ThreadSuspendable();
		// waiting for object that accesses the platform
		platform = plataform.get(sus);

		Starter.print("Started platform: " + platform.getComponentIdentifier());
		platformName = platform.getComponentIdentifier().toString();
		// obtaining the object that provides component management services, so
		// we can start our first agent
		cms = SServiceProvider.getService(platform.getServiceProvider(), IComponentManagementService.class,
				RequiredServiceInfo.SCOPE_PLATFORM).get(sus);
		settings = SServiceProvider
				.getService(platform.getServiceProvider(), ISettingsService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.get(sus);

		settings.setSaveOnExit(false);
		// creating parameters for our agent
		Map<String, Object> gridParam = new HashMap<String, Object>();
		// creating our agent
		final ITuple2Future<IComponentIdentifier, Map<String, Object>> grid = cms.createComponent("GRID",
				"bin/masex/GRIDBDI.class", new CreationInfo(gridParam));
		grid.addResultListener(new IResultListener<Collection<TupleResult>>() {

			public void resultAvailable(Collection<TupleResult> resultado) {

				restart();
			}

			public void exceptionOccurred(Exception arg0) {
				long current = System.currentTimeMillis();
				long simulationTime = (current - Starter.time);
				double resultados[] = { -1, -1, -1, -1, -1, -1, -1, -1 };
				int currentStep = -1;
				String imageName = "ERRO" + Starter.platformName + "agentes"
						+ (BeliefDB.getTransformationAgentQty()[0] + BeliefDB.getTransformationAgentQty()[1])
						+ "porcentagem" + (BeliefDB.getTransformationAgentGroupPercentage()[0]);
				try {
					String csvName = imageName + ".csv";
					GRIDBDI.criaCSV(csvName, resultados, currentStep, simulationTime);
					String arqName = imageName+ "erro.txt";
					
					Starter.criaDumpErro(arqName, arg0);
					try {
						String zipname = ZipAndSend.zip("ERRO"+imageName, new String[] { csvName, arqName });
						ZipAndSend.upload(zipname);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				Starter.print("Simulation aborted!");
				System.exit(1);
			}
		});
	}

	public static void restart(){
		if ((!BeliefDB.increaseAgents)) {
			BeliefDB.qtyOfExecutions -= 1;
		}

		if (BeliefDB.qtyOfExecutions <= 0) {
			Starter.print("finalizado!");
			System.exit(0);

		}
		Starter.print("restarting!");

		int qtyOfAgentsType1 = BeliefDB.getTransformationAgentQty()[0];
		int percentageOfGroupAgentsType1 = BeliefDB.getTransformationAgentGroupPercentage()[0];

		int qtyOfAgentsType2 = BeliefDB.getTransformationAgentQty()[0];
		int percentageOfGroupAgentsType2 = BeliefDB.getTransformationAgentGroupPercentage()[0];

		BeliefDB.setTransformationAgentQty(new int[] { qtyOfAgentsType1 + BeliefDB.amountOfIncrease,
				qtyOfAgentsType2 + BeliefDB.amountOfIncrease });
		BeliefDB.setTransformationAgentGroupPercentage(
				new int[] { percentageOfGroupAgentsType1 + BeliefDB.amountOfPercentageIncrease,
						percentageOfGroupAgentsType2 + BeliefDB.amountOfPercentageIncrease });

		if (BeliefDB.percentageIncrement >= 50) {
			System.exit(0);
		}
		platform.killComponent();
		System.gc();
		try{
			start();
		}catch(Exception e){}
	}

	public static void print(String message) {
		if (debug) {
			System.out.println(message);
		}
	}

	public static void criaDumpErro(String arqName, String erro) throws IOException {

		File reseval = new File(arqName);
		if (!reseval.exists()) {
			reseval.createNewFile();
		}
		FileWriter fw = new FileWriter(reseval);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(erro);
		bw.flush();
		bw.close();
		fw.close();

	}
	
	public static void criaDumpErro(String arqName, Exception erro) throws IOException {

		File reseval = new File(arqName);
		if (!reseval.exists()) {
			reseval.createNewFile();
		}
		PrintWriter pw = new PrintWriter(reseval);
		erro.printStackTrace(pw);
		pw.append("\nStack:\n");
		for(StackTraceElement a :erro.getStackTrace()){
			pw.append(a.toString()+"\n");
		}
		
		pw.flush();
		pw.close();

	}

}
