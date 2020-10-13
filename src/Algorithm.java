import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;


public class Algorithm {
	public int[] decision;
	public HashMap<Integer,int[]> decisionLookup;
	private Random rand;
	private DPdata dpdata;
	public Algorithm (int x) {
		decision = new int [x];
		Arrays.fill(decision, 0);
		decisionLookup = new HashMap<>();
		rand = new Random();
	}
	
	public void findLookupTable(int M, int N) {
		for (int i = 0; i < (int) Math.pow(M,N); i++) {
			decisionLookup.put(i, findDecision(i,M,N));
		}
	}
	
	public void simDesignatedGamma(int T, int M, Sequence seq, Application app, double gamma, boolean debug) {
		System.out.println("MABSTA_designated_gamma");
		int N = app.getNumTasks();
		int E = app.getNumEdges();
		
		Estimate estimate = new Estimate(M,N);
		
		dpdata = new DPdata(N,M);
		dpdata.inverseGraph(app);
		// group the tasks into subproblems, start from the end task
		if (!dpdata.group(N-1)) {
			System.out.println("GMoptimizer: invalid task graph (exit -1)");
		    System.exit(-1);
		}
		if(debug) dpdata.printGroupResult();
		dpdata.build();

		double alpha = gamma / M / (N+E*M);
		
		// start simulation
		int t = 0;
		while (t < T) {
			if (t % 10000 == 0) {
				System.out.println(t);
			}
			decision[t] = makeDecision(estimate,gamma,alpha);
			updateEstimate(decision[t],estimate,gamma,alpha,app,seq,t);
			t++;
		}
		return;
	}
	
	public void simVaryingGamma(int T, int M, Sequence seq, Application app, boolean debug) {
		System.out.println("MABSTA_varying_gamma");
		int N = app.getNumTasks();
		int E = app.getNumEdges();
		
		Estimate estimate = new Estimate(M,N);
		
		dpdata = new DPdata(N,M);
		dpdata.inverseGraph(app);
		// group the tasks into subproblems, start from the end task
		if (!dpdata.group(N-1)) {
			System.out.println("GMoptimizer: invalid task graph (exit -1)");
		    System.exit(-1);
		}
		if(debug) dpdata.printGroupResult();
		dpdata.build();
		
		double gamma = 1;
		double alpha = gamma / M / (N+E*M);
		
		// start simulation
		int t = 0;
		while (t < T) {
			gamma = Math.min( 1, Math.sqrt( (N+E*M)*N*M*Math.log(M)/(Math.E-1)/(N+E)/t ) );
			alpha = gamma / M / (N+E*M);
			if (t % 10000 == 0) {
				System.out.println(t);
			}
			decision[t] = makeDecision(estimate,gamma,alpha);
			updateEstimate(decision[t],estimate,gamma,alpha,app,seq,t);
			t++;
		}
		return;
	}
	
	public void sim(int T, int M, Sequence seq, Application app, boolean debug) {
		
		System.out.println("MABSTA");
		int N = app.getNumTasks();
		int E = app.getNumEdges();
		
		Estimate estimate = new Estimate(M,N);
		
		dpdata = new DPdata(N,M);
		dpdata.inverseGraph(app);
		// group the tasks into subproblems, start from the end task
		if (!dpdata.group(N-1)) {
			System.out.println("GMoptimizer: invalid task graph (exit -1)");
		    System.exit(-1);
		}
		if(debug) dpdata.printGroupResult();
		dpdata.build();
		
		double gamma = Math.min( 1, Math.sqrt( (N+E*M)*N*M*Math.log(M)/(Math.E-1)/(N+E)/T ) );
		double alpha = gamma / M / (N+E*M);
		
		// start simulation
		int t = 0;
		while (t < T) {
			if (t % 10000 == 0) {
				System.out.println(t);
			}
			decision[t] = makeDecision(estimate,gamma,alpha);
			updateEstimate(decision[t],estimate,gamma,alpha,app,seq,t);
			t++;
		}
		return;
	}
	
	private int makeDecision(Estimate estimate, double gamma, double alpha) {
		double randNum = rand.nextDouble();
		if (randNum > 1-gamma) {
			// choose an arm uniformly
			return (int)Math.floor(rand.nextDouble()*Math.pow(estimate.numDevices,estimate.numTasks));
		}
		int strategy = 0;
		int[] constraint = new int [estimate.numTasks];
		Arrays.fill(constraint,-1);
		for (int i = 0; i < estimate.numTasks; i++) {
			// assign task i given the assignment on the previous tasks
			double w = 0;
			double[] wVec = new double [estimate.numDevices];
			Arrays.fill(wVec,0);
			for (int j = 0; j < estimate.numDevices; j++) {
				constraint[i] = j;
				dpdata.solve(alpha, estimate, constraint);
				for (int k = 0; k < estimate.numDevices; k++) wVec[j] += dpdata.getWeight(k,estimate.numTasks-1);
				w += wVec[j];
			}
			randNum = rand.nextDouble();
			int singleDecision = 0;
			double prob = 0;
			while (singleDecision < estimate.numDevices) {
				prob += (wVec[singleDecision]/w);
				if (prob > randNum) {
					constraint[i] = singleDecision;
					strategy += singleDecision*Math.pow(estimate.numDevices,estimate.numTasks-i-1);
					break;
				}
				singleDecision ++;
			}
		}
		return strategy;
	}

	private void updateEstimate(int decision, Estimate estimate, double gamma, double alpha, Application app, Sequence seq, int t) {
		double[] nodeUpdate = new double[estimate.numTasks];
		double[][] edgeUpdate = new double[estimate.numTasks][estimate.numTasks];
		int[] strategy = decisionLookup.get(decision);
		
		// prevent overflow
		double maxExponent = Math.log(Double.MAX_VALUE)/alpha/(app.getNumTasks()+app.getNumEdges());
		
		double max = 0;
		for (int i = 0; i < estimate.numDevices; i++) {
			for (int j = 0; j < estimate.numTasks; j++) {
				if (estimate.serverEstimate[i][j] > max) max = estimate.serverEstimate[i][j];
			}
		}
		for (int i = 0; i < estimate.numDevices; i++) {
			for (int j = 0; j < estimate.numDevices; j++) {
				for (int m = 0; m < estimate.numTasks; m++) {
					for (int n = 0; n < estimate.numTasks; n++) {
						if (estimate.channelEstimate[i][j][m][n] > max) max = estimate.channelEstimate[i][j][m][n];
					}
				}
			}
		}
		
		if (max > maxExponent) {
			for (int i = 0; i < estimate.numDevices; i++) {
				for (int j = 0; j < estimate.numTasks; j++) {
					estimate.serverEstimate[i][j] -= max;
				}
			}
			for (int i = 0; i < estimate.numDevices; i++) {
				for (int j = 0; j < estimate.numDevices; j++) {
					for (int m = 0; m < estimate.numTasks; m++) {
						for (int n = 0; n < estimate.numTasks; n++) {
							estimate.channelEstimate[i][j][m][n] -= max;
						}
					}
				}
			}
		}
		
		
		double prob;
		for (int i = 0; i < estimate.numTasks; i++) {
			prob = findMarginalNode(strategy[i],i,estimate,gamma,alpha);
			nodeUpdate[i] = seq.getServer(strategy[i],i,t)/prob;
			for (int j = 0; j < estimate.numTasks; j++) {
				if (app.getDataSizes(i,j) > 0) {
					prob = findMarginalEdge(strategy[i],strategy[j],i,j,estimate,gamma,alpha);
					edgeUpdate[i][j] = app.getDataSizes(i,j)*seq.getChannel(strategy[i],strategy[j],t)/prob;
				}
			}
		}
		
		for (int i = 0; i < estimate.numTasks; i++) {
			estimate.serverEstimate[strategy[i]][i] += nodeUpdate[i];
			for (int j = 0; j < estimate.numTasks; j++) {
				if (app.getDataSizes(i,j) > 0) {
					estimate.channelEstimate[strategy[i]][strategy[j]][i][j] += edgeUpdate[i][j];
				}
			}
		}
	}
	
	private double findMarginalEdge(int device1, int device2, int task1, int task2,
			Estimate estimate, double gamma, double alpha) {
		double uniProb = gamma / Math.pow(estimate.numDevices,2);
		double prob;
		int[] constraint = new int [estimate.numTasks];
		Arrays.fill(constraint,-1);
		if (task2 == estimate.numTasks-1) {
			double w = 0;
			double num = 0;
			for (int i = 0; i < estimate.numDevices; i++) {
				constraint[task1] = i;
				dpdata.solve(alpha,estimate,constraint);
				for (int j = 0; j < estimate.numDevices; j++) w += dpdata.getWeight(j,task2);
				if (i == device1) num = dpdata.getWeight(device2,task2);
			}
			prob = (1-gamma)*num/w;
		} else {
			double[][] wVec = new double [estimate.numDevices][estimate.numDevices];
			double w = 0;
			for (int i = 0; i < estimate.numDevices; i++) {
				for (int j = 0; j < estimate.numDevices; j++) {
					wVec[i][j] = 0;
					constraint[task1] = i;
					constraint[task2] = j;
					dpdata.solve(alpha,estimate,constraint);
					for (int k = 0; k < estimate.numDevices; k++) wVec[i][j] += dpdata.getWeight(k,estimate.numTasks-1);
					w += wVec[i][j];
				}
			}
			prob = (1-gamma)*wVec[device1][device2]/w;
		}
		return prob + uniProb;
	}

	private double findMarginalNode(int device, int task, Estimate estimate,
			double gamma, double alpha) {
		double uniProb = gamma / estimate.numDevices;
		double prob;
		int[] constraint = new int [estimate.numTasks];
		Arrays.fill(constraint,-1);
		if (task == estimate.numTasks-1) {
			// solve for the final task
			dpdata.solve(alpha,estimate,constraint);
			double w = 0;
			for (int i = 0; i < estimate.numDevices; i++) {
				w += dpdata.getWeight(i,task);
			}
			prob = (1-gamma)*dpdata.getWeight(device,task)/w;
		} else {
			double[] wVec = new double [estimate.numDevices];
			Arrays.fill(wVec,0);
			double w = 0;
			for (int i = 0; i < estimate.numDevices; i++) {
				constraint[task] = i;
				dpdata.solve(alpha,estimate,constraint);
				for (int j = 0; j < estimate.numDevices; j++) {
					wVec[i] += dpdata.getWeight(j,estimate.numTasks-1);
				}
				w += wVec[i];
			}
			prob = (1-gamma)*wVec[device]/w;
		}
		return prob + uniProb;
	}

	private int[] findDecision(int strategy, int M, int N) {
		int[] arr = new int [N];
		int remainder = strategy;
		int i = 0;
		while (i < N) {
			arr[i] = (int)remainder/(int)Math.pow(M, N-1-i);
			remainder = (int)(remainder - arr[i]*Math.pow(M, N-1-i));
			i++;
			if (remainder == 0) break;
		}
		return arr;
	}

	@SuppressWarnings("unused")
	private void fillZeros(double[][] estimateServer,
			double[][][][] estimateChannel) {
		for (int i = 0; i < estimateServer.length; i++) {
			for (int j = 0; j < estimateServer[0].length; j++) {
				estimateServer[i][j] = 0;
			}
		}
		for (int i = 0; i < estimateChannel.length; i++) {
			for (int j = 0; j < estimateChannel[0].length; j++) {
				for (int k = 0; k < estimateChannel[0][0].length; k++) {
					for (int l = 0; l < estimateChannel[0][0][0].length; l++) {
						estimateChannel[i][j][k][l] = 0;
					}
				}
			}
		}
	}

	public void simExp3(int numFrames, int numDevices, Sequence seq, Application app) {
		
		
		System.out.println("Exp3");
		int N = app.getNumTasks();
		int E = app.getNumEdges();
		int M = numDevices;
		int T = numFrames;
		int numArms = (int)Math.pow(numDevices,app.getNumTasks());
		Metric metric = new Metric(numArms);
//		double g = (app.getNumTasks()+app.getNumEdges())*numFrames;
		double gamma = Math.min( 1, Math.sqrt( (N+E*M)*N*M*Math.log(M)/(Math.E-1)/(N+E)/T ) );
//		double gamma = Math.min(1,Math.sqrt(numArms*Math.log(numArms)/(Math.E-1)/g));
		
		double estimate;
		for (int t = 0; t < numFrames; t++) {
			if (t % 10000 == 0) {
				System.out.println(t);
			}
			updateProb(metric,gamma);
			makeDecisionExp3(t,metric.prob,gamma);
			estimate = getReward(decisionLookup.get(decision[t]),t,app,seq)/metric.prob[decision[t]]/(app.getNumTasks()+app.getNumEdges());
			metric.weight[decision[t]] *= Math.exp(gamma*estimate/numArms);
			
		}
		System.out.println();
	}
	
	private void updateProb(Metric metric, double gamma) {
		double sum = 0;
		for (int i = 0; i < metric.weight.length; i++) {
			sum += metric.weight[i];
		}
		for (int i = 0; i < metric.prob.length; i++) {
			metric.prob[i] = (1-gamma)*metric.weight[i]/sum + gamma/metric.weight.length;
		}
	}

	private double getReward(int[] strategy, int t, Application app, Sequence seq) {
		double reward = 0;
		for (int i = 0; i < app.getNumTasks(); i++) {
			reward += seq.getServer(strategy[i], i, t);
			for (int j = 0; j < app.getNumTasks(); j++) {
				if (app.getDataSizes(i, j) > 0) reward += app.getDataSizes(i, j)*seq.getChannel(strategy[i], strategy[j], t);
			}
		}
		return reward;
	}

	private void makeDecisionExp3(int t, double[] prob, double gamma) {
		double randNum = rand.nextDouble();
		double value = 0;
		
		for (int i = 0; i < prob.length; i++) {
			value += prob[i];
			if (value > randNum) {
				decision[t] = i;
				return;
			}
		}
	}

	public void sim_myopic(int numFrames, int numDevices, Sequence seq,
			Resource resource) {
		double[] beliefVec = new double [numDevices];
		Arrays.fill(beliefVec, 0);
		
		double max;
		double temp;
		int bestDev;
		
		for (int i = 0; i < numFrames; i++) {
			max = 0;
			bestDev = 0;
			for (int j = 0; j < numDevices; j++) {
				temp = beliefVec[j]*resource.getServProb(j,0,0) + (1-beliefVec[j])*(1-resource.getServProb(j,0,1));
				if (temp > max) {
					max = temp;
					bestDev = j;
				}
			}
			decision[i] = bestDev;
			// update beliefVec
			for (int j = 0; j < numDevices; j++) {
				if (j == bestDev) {
					if (seq.getServer(j,0,i) == 1) {
						beliefVec[j] = 1;
					} else {
						beliefVec[j] = 0;
					}
				} else {
					beliefVec[j] = beliefVec[j]*resource.getServProb(j,0,0) + (1-beliefVec[j])*(1-resource.getServProb(j,0,1));
				}
			}
		}
	}
	
}
