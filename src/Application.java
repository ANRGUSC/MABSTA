import java.io.PrintStream;
import java.util.Arrays;


public class Application {
	private int numTasks;
	private int numEdges;
	private int[][] dataSizes;
	
	public Application (int num) {
		numTasks = num;
		dataSizes = new int[num][num];
		for (int i = 0; i < num; i++) {
			for (int j = 0; j < num; j++) {
				dataSizes[i][j] = 0;
			}
		}
	}
	
	public void countEdges() {
		numEdges = 0;
		for (int i = 0; i < numTasks; i++) {
			for (int j = 0; j < numTasks; j++) {
				if (dataSizes[i][j] > 0) numEdges ++;
			}
		}
	}
	
	public void setDataSizes(int i, int j, int v) {
		dataSizes[i][j] = v;
		return;
	}
	
	public int getDataSizes(int i, int j) {
		return dataSizes[i][j];
	}
	
	public int getNumTasks() {
		return numTasks;
	}
	
	public int getNumEdges() {
		return numEdges;
	}
	
	public void print(PrintStream writer) {
		writer.println("numTasks = " + numTasks + ";");
		writer.println("numEdges = " + numEdges + ";");
		writer.println("dataSizes = " + Arrays.deepToString(dataSizes) + ";");
	}
}
