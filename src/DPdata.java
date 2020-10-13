import java.util.Vector;


public class DPdata {
	private int numTasks;
	private int numDevices;
	private boolean[][] invGraph;
	private Vector<Tree> trees;
	private Vector<Integer> bfsQueue;
	private boolean[] visited;
	private double[][] weight;
	
	public DPdata (int N, int M) {
		numTasks = N;
		numDevices = M;
		invGraph = new boolean [N][N];
		trees = new Vector<Tree>();
		bfsQueue = new Vector<Integer>();
		visited = new boolean [N];
		for (int i = 0; i < N; i++) {
			visited[i] = false;
			for (int j = 0; j < N; j++) invGraph[i][j] = false;
		}
	}
	
	public double getWeight(int i, int j) {
		return weight[i][j];
	}
	
	public void inverseGraph(Application app) {
		for (int i = 0; i < numTasks; i++) {
			for (int j = 0; j < numTasks; j++) {
				if (app.getDataSizes(i,j) > 0) invGraph[j][i] = true;
			}
		}
	}
	
	public boolean group (int id) {
		
		if (id == numTasks - 1) {
			// initialize a tree rooted at the last task
			trees.add(new Tree(id));
			visited[id] = true;
			for (int i = 0; i < numTasks; i++) {
				if (invGraph[id][i] && !visited[i]) {
					bfsQueue.add(i);
					visited[i] = true;
				}
			}
		} else if (!singleOut(id)) {
			// reach a split point
			trees.lastElement().setPrecProb(id);
			// initialize a new tree
			trees.add(new Tree(id));
			for (int i = 0; i < numTasks; i++) {
				if (invGraph[id][i] && !visited[i]) {
					bfsQueue.add(i);
					visited[i] = true;
				}
			}
		} else {
			// a child with 1 out-degree
			trees.lastElement().tasks.add(id);
			for (int i = 0; i < numTasks; i++) {
				if (invGraph[id][i] && !visited[i]) {
					bfsQueue.add(i);
					visited[i] = true;
				}
			}
		}
		
		if (bfsQueue.isEmpty() && allVisited()) return true;
		else if (bfsQueue.isEmpty()) return false;
		else {
			int next = bfsQueue.firstElement();
			bfsQueue.remove(0);
			return group(next);
		}
	}
	
	private boolean allVisited() {
		for (int i = 0; i < numTasks; i++) {
			if (!visited[i]) return false;
		}
		return true;
	}
	
	private boolean singleOut(int id) {
		int outDegree = 0;
		for (int i = 0; i < numTasks; ++i) {
			outDegree += (invGraph[i][id])? 1 : 0;
			if (outDegree > 1) return false;
		}
		return true;
	}

	public void printGroupResult() {
		System.out.println("");
		System.out.println("Print Group Result:");
		for (int i = 0; i < (int)trees.size(); i++) {
			System.out.println("tree " + i + ":");
			System.out.println("	tasks: " + trees.get(i).tasks.toString());
			System.out.println("	precProb: " + trees.get(i).getPrecProb());
		}		
	}

	public void build() {
		for (int i = 0; i < trees.size(); i++) {
			trees.get(i).build(invGraph);
		}
	}

	public void solve(double alpha, Estimate estimate, int[] constraint) {
		weight = new double [numDevices][numTasks];
		
		// solve the trees in order
		trees.lastElement().solve(alpha, estimate, constraint);
		if (constraint[trees.lastElement().getID()] >= 0) {
			weight[constraint[trees.lastElement().getID()]][trees.lastElement().getID()] 
					= trees.lastElement().weight[constraint[trees.lastElement().getID()]][trees.lastElement().getID()][0];
		} else {
			for (int j = 0; j < numDevices; j++) {
				weight[j][trees.lastElement().getID()]
						= trees.lastElement().weight[j][trees.lastElement().getID()][0];
			}
		}
		for (int i = trees.size()-2; i >= 0; i--) {
			trees.get(i).solve(alpha,estimate,constraint);
			int curTask = trees.get(i).getID();
			int curDevice = constraint[curTask];
			int precTask = trees.get(i).getPrecProb();
			int precDevice = (trees.get(i).getPrecProb() >= 0)? constraint[trees.get(i).getPrecProb()]:-1;
			if (curDevice >= 0 && precDevice >= 0) {
				weight[curDevice][curTask] = trees.get(i).weight[curDevice][curTask][precDevice]*weight[precDevice][precTask];
			} else if (curDevice >= 0 && precDevice < 0) {
				for (int j = 0; j < numDevices; j++) {
					weight[curDevice][curTask] += trees.get(i).weight[curDevice][curTask][j]*weight[j][precTask];
				}
			} else if (curDevice < 0 && precDevice >= 0) {
				for (int j = 0; j < numDevices; j++) {
					weight[j][curTask] = trees.get(i).weight[j][curTask][precDevice]*weight[precDevice][precTask];
				}
			} else {
				for (int j = 0; j < numDevices; j++) {
					for (int k = 0; k < numDevices; k++) {
						weight[j][curTask] += trees.get(i).weight[j][curTask][k]*weight[k][precTask];
					}
				}
			}
		}
	}
}
