import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


public class Tree {
	private int id; // identified by the end task of this problem
	private int precProb; // the preceding problem
	private List<LinkedList<Integer>> childList;
	public Vector<Integer> tasks; // the set of tasks in this tree
	public double[][][] weight; // the weights conditioned on precProb
	
	// constructor
	public Tree(int x) {
		tasks = new Vector<Integer>();
		precProb = -1;
		id = x;
		// add the first task
		tasks.add(x);
		childList = new ArrayList<LinkedList<Integer>>();
	}
	
	public int getID() {return id;}
	public int getPrecProb() {return precProb;}
	public void setPrecProb(int x) {precProb = x;}
	
	public void build(boolean[][] invGraph) {
		childList = new ArrayList<LinkedList<Integer>>();
		for (int i = 0; i < tasks.size(); i++) {
			childList.add(new LinkedList<Integer>());
			for (int j = i+1; j < tasks.size(); j++) {
				if (invGraph[tasks.get(i)][tasks.get(j)]) {
					childList.get(childList.size()-1).add(tasks.get(j));
				}
			}
		}
	}
	
	public boolean solve(double alpha, Estimate estimate, int[] devices) {
		weight = new double [estimate.numDevices][estimate.numTasks][estimate.numDevices];
		if (precProb >= 0) {
			if (devices[precProb] >= 0) {
				solveConditional(alpha,estimate,devices,devices[precProb]);
			} else {
				for (int prec_j = 0; prec_j < estimate.numDevices; prec_j++) {
					solveConditional(alpha,estimate,devices,prec_j);
				}
			}
		} else {
			solveConditional(alpha,estimate,devices,-1);
		}
		return true;
	}
	
	// solve the weight condition given precProb is assigned to precDevice, if precProb = -1 (no precProb),
	// solve the unconditional version and stored at weight[][][0]
	private void solveConditional(double alpha, Estimate estimate, int[] devices, int precDevice) {
		
		for (int i = tasks.size()-1; i >= 0; i--) {
			int curTask = tasks.get(i);
			if (childList.get(i).isEmpty()) {
				// leaf
				if (precDevice >= 0) {
					if (devices[curTask] >= 0) {
						weight[devices[curTask]][curTask][precDevice] = Math.exp(alpha*(estimate.serverEstimate[devices[curTask]][curTask]
								+estimate.channelEstimate[precDevice][devices[curTask]][precProb][curTask]));
					} else {
						for (int j = 0; j < estimate.numDevices; j++) {
							weight[j][curTask][precDevice] = Math.exp(alpha*(estimate.serverEstimate[j][curTask]
																	+estimate.channelEstimate[precDevice][j][precProb][curTask]));
						}	
					}	
				} else {
					if (devices[curTask] >= 0) {
						weight[devices[curTask]][curTask][0] = Math.exp(alpha*estimate.serverEstimate[devices[curTask]][curTask]);
					} else {
						for (int j = 0; j < estimate.numDevices; j++) {
							weight[j][curTask][0] = Math.exp(alpha*estimate.serverEstimate[j][curTask]);
						}	
					}
				}
			} else {
				if (devices[curTask] >= 0) {
					// current task is fixed on devices[curTask]
					solveTask(curTask,devices[curTask],childList.get(i),devices,estimate,alpha,precDevice);
				} else {
					for (int j = 0; j < estimate.numDevices; j++) {
						solveTask(curTask,j,childList.get(i),devices,estimate,alpha,precDevice);
					}
				}
			}
		}
	}

	private void solveTask(int curTask, int curDevice, LinkedList<Integer> childList,
			int[] devices, Estimate estimate, double alpha, int precDevice) {
		
		int assignIdx = (precDevice == -1)? 0:precDevice;
		
		weight[curDevice][curTask][assignIdx] = Math.exp(alpha*estimate.serverEstimate[curDevice][curTask]);
		for (int i = 0; i < childList.size(); i++) {
			int childTask = childList.get(i);
			double temp = 0;
			if (devices[childTask] >= 0) {
				temp = Math.exp(alpha*estimate.channelEstimate[devices[childTask]][curDevice][childTask][curTask])
						*weight[devices[childTask]][childTask][assignIdx];
			} else {
				for (int j = 0; j < estimate.numDevices; j++) {
					temp += (Math.exp(alpha*estimate.channelEstimate[j][curDevice][childTask][curTask])
							*weight[j][childTask][assignIdx]);
				}
			}
			weight[curDevice][curTask][assignIdx] *= temp;
		}
	}

	@SuppressWarnings("unused")
	private int[] findLocalDecision(int strategy, int M, int N) {
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
}
