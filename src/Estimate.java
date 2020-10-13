
public class Estimate {
	public int numTasks;
	public int numDevices;
	double[][] serverEstimate;
	double[][][][] channelEstimate;
	
	public Estimate (int M, int N) {
		numTasks = N;
		numDevices = M;
		serverEstimate = new double [M][N];
		channelEstimate = new double [M][M][N][N];
		for (int i = 0; i < serverEstimate.length; i++) {
			for (int j = 0; j < serverEstimate[0].length; j++) {
				serverEstimate[i][j] = 0;
			}
		}
		for (int i = 0; i < channelEstimate.length; i++) {
			for (int j = 0; j < channelEstimate[0].length; j++) {
				for (int k = 0; k < channelEstimate[0][0].length; k++) {
					for (int l = 0; l < channelEstimate[0][0][0].length; l++) {
						channelEstimate[i][j][k][l] = 0;
					}
				}
			}
		}
	}
}
