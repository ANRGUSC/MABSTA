import java.util.Arrays;


public class Metric {
	public double[] prob;
	public double[] weight;
	
	public Metric(int x) {
		// initialize probability and weight arrays of every n-horizon decision
		prob = new double[x];
		weight = new double[x];
		Arrays.fill(weight, 1);
	}
}
