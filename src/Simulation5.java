import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
// simulate the performance of MABSTA for different time horizons and compare with Exp3 and randomized baseline
public class Simulation5 {
	private static final String VERSION = "v01";
	private static final String APP_FILE = "files/app1"; // define number of tasks and their relation
	private static final String RESOURCE_FILE = "files/resource1"; // define number of devices and the channel quality
	private static final boolean DEBUG = false;
	private static final boolean SERVER_FROM_FILE = true;
	private static final String SERVER_SAMPLE_FOLDER = "../task-allocation.git/data/20150426_2300";
	//private static final String SERVER_SAMPLE_FOLDER = "../task-allocation.git/data/20150412_1900";
	//private static final String SERVER_SAMPLE_FOLDER = "../task-allocation.git/data/20150406_1800";
	private static final boolean CHANNEL_FROM_FILE = true;
	private static final String CHANNEL_SAMPLE_FOLDER = "../task-allocation.git/data/20150426_2300";
	//private static final String CHANNEL_SAMPLE_FOLDER = "../task-allocation.git/data/20150412_1900";
	//private static final String CHANNEL_SAMPLE_FOLDER = "../task-allocation.git/data/20150325_1330";
	private static final int[] NUM_PACKET = {100,500,1000,2000,3000,4000,5000,10000,20000,30000,40000,50000,100000,500000};
	public static void main(String[] args) {
		System.out.println("");
		System.out.println("");
		System.out.println("MABSTA_new_Sim5 " + VERSION);
		Date curDate = new Date();
		final String OUTPUT_FILE = "files/output/SIM5_" + curDate.toString().replaceAll("[ :]", "_");
		
		
		HashMap<Integer,int[]> decisionLookup = new HashMap<>();
		double[] regretMABSTA = new double[NUM_PACKET.length];
		double[] regretExp3 = new double[NUM_PACKET.length];
		double[] regretBase = new double[NUM_PACKET.length];
		for (int idx = 0; idx < NUM_PACKET.length; idx++) {
			System.out.println("Experiment: " + NUM_PACKET[idx] + " frames...");
			// build profile from the files
			Profile profileData = new Profile(NUM_PACKET[idx]);
			if (!profileData.buildFromFile(APP_FILE, RESOURCE_FILE, DEBUG)) {
				// fail to build from file
				System.out.println("MABSTA_new_Sim1: fail to build from file (exit -1)");
				System.exit(-1);	
			}
			// generate reward sequence
			Sequence seq = new Sequence(NUM_PACKET[idx],profileData.getNumTasks(),profileData.getNumDevices());
			seq.build(profileData, SERVER_FROM_FILE, CHANNEL_FROM_FILE, SERVER_SAMPLE_FOLDER, CHANNEL_SAMPLE_FOLDER);
			seq.reverseAsReward(profileData.app);
			
			// new MABSTA
			Algorithm alg_MABSTA = new Algorithm(profileData.getNumFrames());
			if (idx == 0) {
				// build lookup table
				alg_MABSTA.findLookupTable(profileData.getNumDevices(),profileData.getNumTasks());
				decisionLookup = alg_MABSTA.decisionLookup;
			} else {
				alg_MABSTA.decisionLookup = decisionLookup;
			}
			alg_MABSTA.sim(profileData.getNumFrames(),profileData.getNumDevices(),seq,profileData.app,DEBUG);
			
			// Exp3
			Algorithm alg_Exp3 = new Algorithm(profileData.getNumFrames());
			alg_Exp3.decisionLookup = decisionLookup;
			//alg_Exp3.simExp3(profileData.getNumFrames(),profileData.getNumDevices(),seq,profileData.app);
			
			// calculate regret
			calculateRegret(alg_MABSTA,alg_Exp3,seq,profileData,regretMABSTA,regretExp3,regretBase,idx);
		}
		
		outputFile(regretMABSTA,regretExp3,regretBase,OUTPUT_FILE,APP_FILE,RESOURCE_FILE);
		
		System.out.println("MABSTA_new_Sim5 " + VERSION + " ends");

	}
	
	private static void calculateRegret(Algorithm alg_MABSTA,
			Algorithm alg_Exp3, Sequence seq, Profile profile,
			double[] regretMABSTA, double[] regretExp3, double[] regretBase,
			int idx) {
		int N = profile.getNumTasks();
		int M = profile.getNumDevices();
		int T = profile.getNumFrames();
		
		// calculate the offline optimal reward by playing single arm
		int bestARM = 0;
		double bestReward = 0;
		double curReward;
		double rewardOPT1 = 0;
		int[] strategy = new int [N];
		for (int i = 0; i < M; i++) {
			curReward = 0;
			Arrays.fill(strategy,i);
			for (int t = 0; t < T; t++) curReward += getReward(strategy,t,profile.app,seq);
			if (curReward > bestReward) {
				bestARM = i;
				bestReward = curReward;
			}
		}
		Arrays.fill(strategy,bestARM);
		
		for (int t = 0; t < T; t++) {
			rewardOPT1 += getReward(strategy,t,profile.app,seq);
		}
		
		// calculate MABSTA's reward
		double rewardMABSTA = 0;
		for (int t = 0; t < T; t++) {
			strategy = alg_MABSTA.decisionLookup.get(alg_MABSTA.decision[t]);
			rewardMABSTA += getReward(strategy,t,profile.app,seq);
		}
		
		// calculate Exp3's reward
		double rewardExp3 = 0;
		for (int t = 0; t < T; t++) {
			strategy = alg_Exp3.decisionLookup.get(alg_Exp3.decision[t]);
			rewardExp3 += getReward(strategy,t,profile.app,seq);
		}
		
		// calculate Randomized baseline reward
		Random rand = new Random();
		double rewardBase = 0;
		for (int t = 0; t < T; t++) {
			strategy = alg_Exp3.decisionLookup.get((int)Math.floor(rand.nextDouble()*Math.pow(M,N)));
			rewardBase += getReward(strategy,t,profile.app,seq);
		}
		
		// calculate regrets
		regretMABSTA[idx] = rewardOPT1 - rewardMABSTA;
		regretExp3[idx] = rewardOPT1 - rewardExp3;
		regretBase[idx] = rewardOPT1 - rewardBase;
		
		return;
	}

	private static void outputFile(double[] regretMABSTA,double[] regretExp3,double[] regretBase,
			String file, String appFile, String resourceFile) {

		try {
			PrintStream writer = new PrintStream(file);
			writer.println(new Date().toString());
			writer.println("");
			writer.println("Application: " + appFile);
			writer.println("Resource: " + resourceFile);
			writer.println("numPacket =  " + Arrays.toString(NUM_PACKET));
			if (SERVER_FROM_FILE) writer.println(SERVER_SAMPLE_FOLDER);
			if (CHANNEL_FROM_FILE) writer.println(CHANNEL_SAMPLE_FOLDER);
			writer.println("===============================================");
			writer.println("");
			writer.println("regretMABSTA = " + Arrays.toString(regretMABSTA) + ";");
			writer.println("regretExp3 = " + Arrays.toString(regretExp3) + ";");
			writer.println("regretBase = " + Arrays.toString(regretBase) + ";");
			writer.println("");
			writer.println("");
			writer.println("This is the end of the file.");
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static double getReward(int[] strategy, int t, Application app, Sequence seq) {
		double reward = 0;
		for (int i = 0; i < app.getNumTasks(); i++) {
			reward += seq.getServer(strategy[i], i, t);
			for (int j = 0; j < app.getNumTasks(); j++) {
				if (app.getDataSizes(i, j) > 0) reward += app.getDataSizes(i, j)*seq.getChannel(strategy[i], strategy[j], t);
			}
		}
		return reward;
	}


}
