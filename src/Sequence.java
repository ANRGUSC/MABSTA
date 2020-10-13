import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.Random;


public class Sequence {
	private double [][][] channel;
	private double [][][] server;
	private Random rand;
	private int numSamplesInFile;
	
	public Sequence(int numFrames, int numTasks, int numDevices) {
		channel = new double [numDevices][numDevices][numFrames];
		server = new double [numDevices][numTasks][numFrames];
		rand = new Random();
	}
	
	public void setChannel(int i, int j, int k, double v) {
		channel[i][j][k] = v;
	}
	
	public double getChannel(int i, int j, int k) {
		return channel[i][j][k];
	}
	
	public void setServer(int i, int j, int k, double v) {
		server[i][j][k] = v;
	}
	
	public double getServer(int i, int j, int k) {
		return server[i][j][k];
	}
	
	public void build(Profile profile, boolean fixServer, boolean fixChannel, String serverFolder, String channelFolder) {
		if (fixServer) {
			LinkedList<Integer> map = readMAP(serverFolder + "/map.txt");
			if (map.size() != server.length) {
				System.out.println("MABSTA_new: inconsistent profile");
				System.exit(-1);
			}
			for (int i = 0; i < map.size(); i++) {
				readServer(serverFolder + "/task_" + Integer.toString(map.get(i)) + ".txt", i);
			}
		} else {
			buildMarkovServer(profile);
		}
		if (fixChannel) {
			LinkedList<Integer> map = readMAP(serverFolder + "/map.txt");
			if (map.size() != server.length) {
				System.out.println("MABSTA_new: inconsistent profile");
				System.exit(-1);
			}
			for (int i = 0; i < map.size(); i++) {
				for (int j = 0; j < map.size(); j++) {
					if (i != j) {
						readChannel(channelFolder + "/channel_"+Integer.toString(map.get(i))+"_"+Integer.toString(map.get(j))+".txt",i,j);
					}
				}
			}
		} else {
			buildMarkovChannel(profile);
		}
	}

	private void readChannel(String file, int id1, int id2) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			for (int t = 0; t < channel[0][0].length; t++) {
				if (t < numSamplesInFile) {
					line = reader.readLine();
					line = line.trim();
					if (isNumeric(line)) channel[id1][id2][t] = Double.parseDouble(line);
					else channel[id1][id2][t] = -1;
				} else {
					channel[id1][id2][t] = channel[id1][id2][t%numSamplesInFile];
				}
			}
			reader.close();
		} catch (Exception e) {
			System.err.format("Exception occurred trying to read '%s'.", file);
			e.printStackTrace();
			return;
		}
	}

	private boolean isNumeric(String str) {
		try  
		  {  
		    @SuppressWarnings("unused")
			double d = Double.parseDouble(str);  
		  }  
		  catch(NumberFormatException nfe)  
		  {  
		    return false;
		  }  
		  return true;
	}

	private void readServer(String file, int id) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			for (int t = 0; t < server[0][0].length; t++) {
				if (t < numSamplesInFile) {
					line = reader.readLine();
					line = line.trim();
					for (int i = 0; i < server[0].length; i++) {
						server[id][i][t] = Double.parseDouble(line);
					}
				} else {
					for (int i = 0; i < server[0].length; i++) {
						server[id][i][t] = server[id][i][t%numSamplesInFile];
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			System.err.format("Exception occurred trying to read '%s'.", file);
			e.printStackTrace();
			return;
		}
	}

	private LinkedList<Integer> readMAP(String file) {
		// read the map.txt file
		LinkedList<Integer> map = new LinkedList<Integer>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
		    String line;
		    while ((line = reader.readLine()) != null) {
		    	processString(line,map);
		    }
		    reader.close();
		} catch (Exception e){
			System.err.format("Exception occurred trying to read '%s'.", file);
			e.printStackTrace();
			return null;
		}
		return map;
	}

	private void processString(String line, LinkedList<Integer> map) {
		String str = line.trim(); // omit white spaces at the beginning and the end
		int end = str.indexOf('/');
		int numChar = (end == -1)? str.length() : end;
		int idx;
		int oldIdx = 0;
		String fieldName = "";
		int number;

		while ((idx=str.indexOf(' ', oldIdx)) != -1) {
			if (idx >= numChar) break;
			if (oldIdx == 0) {
				// read the first substring as a field name
				fieldName = str.substring(0, idx);
			} else {
				// read the following substrings as a number
				number = Integer.parseInt(str.substring(oldIdx, idx));
				switch (fieldName) {
					case "numSamples":
						numSamplesInFile = number;
						break;
					case "map":
						map.add(number);
						break;
					default:
						System.out.println(fieldName); 
						System.out.println("Profile: invalid line");
						return;
				}
			}
			oldIdx = idx + 1;
		}
	}

	private void buildMarkovChannel(Profile profile) {
		double delta = 0.1;
		Random rand = new Random();
		int M = profile.getNumDevices();
		int T = channel[0][0].length;
		boolean[][] channelState = new boolean [M][M];
		
		for (int t = 0; t < T; t++) {
			for (int i = 0; i < M; i++) {
				for (int j = 0; j < M; j++) {
					double randNum = delta*(rand.nextDouble()-0.5);
					channel[i][j][t] = profile.resource.getTransTime(i, j, channelState[i][j]?1:0) + randNum;
					channelState[i][j] = isChanged(profile.resource.getChannelProb(i, j, channelState[i][j]?1:0))? !channelState[i][j]:channelState[i][j];
				}
			}
		}
		return;
	}

	private void buildMarkovServer(Profile profile) {
		double delta = 0.1;
		Random rand = new Random();
		int M = profile.getNumDevices();
		int N = profile.getNumTasks();
		int T = server[0][0].length;
		boolean[][] serverState = new boolean [M][N];
		
		for (int t = 0; t < T; t++) {
			for (int j = 0; j < M; j++) {
				for (int i = 0; i < N; i++) {
					double randNum = delta*(rand.nextDouble()-0.5);
					server[j][i][t] = profile.resource.getServTime(j, i, (serverState[j][i])?1:0) + randNum;
					serverState[j][i] = isChanged(profile.resource.getServProb(j, i, serverState[j][i]?1:0))? !serverState[j][i]:serverState[j][i];
				}
			}
		}
		return;
	}
	
	private boolean isChanged(double prob) {
		return (rand.nextDouble() > prob);
	}

	public void reverseAsReward(Application app) {
		double max = 0;
		for (int t = 0; t < server[0][0].length; t++) {
			for (int j = 0; j < server.length; j++) {
				for (int i = 0; i < server[0].length; i++) {
					if (server[j][i][t] > max) max = server[j][i][t];
				}
			}
		}
		// normalize the server latency (between 0 and 1) and flip
		for (int t = 0; t < server[0][0].length; t++) {
			for (int j = 0; j < server.length; j++) {
				for (int i = 0; i < server[0].length; i++) {
					server[j][i][t] = 1 - server[j][i][t]/max;
				}
			}
		}
		
		max = 0;
		double maxWeight = 0;
		for (int t = 0; t < channel[0][0].length; t++) {
			for (int i = 0; i < channel.length; i++) {
				for (int j = 0; j < channel[0].length; j++) {
					if (channel[i][j][t] > max) max = channel[i][j][t];
				}
			}
		}
		for (int i = 0; i < app.getNumTasks(); i++) {
			for (int j = 0; j < app.getNumTasks(); j++) {
				if (app.getDataSizes(i, j) > maxWeight) maxWeight = app.getDataSizes(i, j);
			}
		}
		// normalize the channel latency (between 0 and 1) and flip
		for (int t = 0; t < channel[0][0].length; t++) {
			for (int i = 0; i < channel.length; i++) {
				for (int j = 0; j < channel[0].length; j++) {
					if (channel[i][j][t] == -1) channel[i][j][t] = 0;
					else channel[i][j][t] = 1 - channel[i][j][t]/(max*maxWeight);
				}
			}
		}
	}

	public void swapServer() {
		double temp;
		for (int i = 0; i < server[0].length; i++) {
			for (int j = 0; j < server[0][0].length; j++) {
				temp = server[0][i][j];
				server[0][i][j] = server[1][i][j];
				server[1][i][j] = temp;
			}
		}
	}

	public Sequence concatinate(Sequence seq2, int length) {
		Sequence seq = new Sequence(server[0][0].length+length,server[0].length,server.length);
		int offset = server[0][0].length;
		for (int t = 0; t < offset; t++) {
			for (int i = 0; i < server.length; i++) {
				for (int j = 0; j < server[0].length; j++) {
					seq.setServer(i, j, t, server[i][j][t]);
				}
			}
			
			for (int i = 0; i < channel.length; i++) {
				for (int j = 0; j < channel[0].length; j++) {
					seq.setChannel(i, j, t, channel[i][j][t]);
				}
			}
		}
		for (int t = 0; t < length; t++) {
			for (int i = 0; i < server.length; i++) {
				for (int j = 0; j < server[0].length; j++) {
					seq.setServer(i, j, t+offset, seq2.getServer(i, j, t));
				}
			}
			for (int i = 0; i < channel.length; i++) {
				for (int j = 0; j < channel[0].length; j++) {
					seq.setChannel(i, j, t+offset, seq2.getChannel(i, j, t));
				}
			}
		}
		return seq;
	}
}
