import java.util.Arrays;
import java.io.*;

public class Resource {
	private int numDevices;
	private int numTasks;
	private double [][][] unitServiceTime; // MxNx2(upper and lower)
	private double [][][] unitTransTime; // MxMx2
	private double [][][] marProbChannel;
	private double [][][] marProbServer;
	
	
	public void setServTime(int i, int j, int k, double num) {
		unitServiceTime[i][j][k] = num;
	}
	public double getServTime(int i, int j, int k) {
		return unitServiceTime[i][j][k];
	}
	public void setTransTime(int row, int col, int idx, double num) {
		unitTransTime[row][col][idx] = num;
	}
	public double getTransTime(int x, int y, int z) {
		return unitTransTime[x][y][z];
	}
	public void setServProb(int i, int j, int k, double num) {
		marProbServer[i][j][k] = num;
	}
	public double getServProb(int i, int j, int k) {
		return marProbServer[i][j][k];
	}
	public void setChannelProb(int row, int col, int idx, double num) {
		marProbChannel[row][col][idx] = num;
	}
	public double getChannelProb(int x, int y, int z) {
		return marProbChannel[x][y][z];
	}
	public void print(PrintStream writer) {
		writer.println("unitServiceTime = " + Arrays.deepToString(unitServiceTime) + ";");
		writer.println("unitTransTime = " + Arrays.deepToString(unitTransTime) + ";");
		writer.println("marProbServer = " + Arrays.deepToString(marProbServer) + ";");
		writer.println("marProbChannel = " + Arrays.deepToString(marProbChannel) + ";");
	}
	
	public Resource (int x, int y) {
			numDevices = x;
			numTasks = y;
			unitServiceTime = new double [numDevices][numTasks][2];
			marProbServer = new double [numDevices][numTasks][2];
			unitTransTime = new double [numDevices][numDevices][2];
			marProbChannel = new double [numDevices][numDevices][2];
	}
}
