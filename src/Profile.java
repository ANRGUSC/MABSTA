import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class Profile {
	private int numTasks;
	private int numDevices;
	private int numFrames;
	public Application app; // application profile read from the application file
	public Resource resource; // resource profile read from the resource file
	
	public Profile(int num) {
		numFrames = num;
	}
	
	public int getNumTasks() {
		return numTasks;
	}
	
	public int getNumDevices() {
		return numDevices;
	}
	
	public int getNumFrames() {
		return numFrames;
	}
	
	public boolean buildFromFile(String appFile, String resourceFile, boolean debug) {
		BufferedReader reader = null;

		// read from app file
		try {
			reader = new BufferedReader(new FileReader(appFile));
			String line;
			while ((line = reader.readLine()) != null) {
				// assign profile data
				if (!processString(line)) {
					reader.close();
					return false;
				}
			}
			reader.close();
		} catch (IOException ex) {
			System.out.println("Profile: cannot find file: " + appFile);
			return false;
		}

		// read from resource file
		reader = null;
		try {
			reader = new BufferedReader(new FileReader(resourceFile));
			String line;
			while ((line = reader.readLine()) != null) {
				// assign profile data
				if (!processString(line)) return false;
			}
			reader.close();
		} catch (IOException ex) {
			System.out.println("Profile: cannot find file: " + resourceFile);
			return false;
		}
		
		app.countEdges();
		
		// print result
		if (debug) {
			System.out.println("Profile: print buildFromFile result:");
			app.print(System.out);
			resource.print(System.out);
		}
		
		return true;
	}
	
	private boolean processString(String oriStr) {
		String str = oriStr.trim(); // omit white spaces at the beginning and the end
		int end = str.indexOf('/');
		int numChar = (end == -1)? str.length() : end;
		int idx;
		int oldIdx = 0;
		String fieldName = "";
		double number;
		int elementIdx = 0;
		int row;
		int col;
		String[] part;

		while ((idx=str.indexOf(' ', oldIdx)) != -1) {
			if (idx >= numChar) break;
			if (oldIdx == 0) {
				// read the first substring as a field name
				fieldName = str.substring(0, idx);
				elementIdx = 0; 
				oldIdx = idx + 1;
			} else {
				if (fieldName.equals("dataSizes")) {
					part = str.substring(oldIdx, idx).split(":");
					app.setDataSizes(Integer.parseInt(part[0])-1, Integer.parseInt(part[1])-1, Integer.parseInt(part[2]));
				} else {
					// read the following substrings as a number
					number = Double.parseDouble(str.substring(oldIdx, idx));
					switch (fieldName) {
						case "numTasks":
							numTasks = (int)number;
							app = new Application((numTasks));
							break;
						case "numDevices":
							numDevices = (int)number;
							resource = new Resource(numDevices,numTasks);
							break;
						case "unitTaskRangeLow":
							row = (int) elementIdx / numTasks;
							col = elementIdx % numTasks;
							resource.setServTime(row,col,0,number);
							elementIdx++;
							break;
						case "unitTaskRangeHigh":
							row = (int) elementIdx / numTasks;
							col = elementIdx % numTasks;
							resource.setServTime(row,col,1,number);
							elementIdx++;
							break;
						case "unitDataRangeLow":
							row = (int) elementIdx / numDevices;
							col = elementIdx % numDevices;
							resource.setTransTime(row,col,0,number);
							elementIdx ++;
							break;
						case "unitDataRangeHigh":
							row = (int) elementIdx / numDevices;
							col = elementIdx % numDevices;
							resource.setTransTime(row,col,1,number);
							elementIdx ++;
							break;
						case "marProbServerLow":
							row = (int) elementIdx / numTasks;
							col = elementIdx % numTasks;
							resource.setServProb(row,col,0,number);
							elementIdx++;
							break;
						case "marProbServerHigh":
							row = (int) elementIdx / numTasks;
							col = elementIdx % numTasks;
							resource.setServProb(row,col,1,number);
							elementIdx++;
							break;
						case "marProbChannelLow":
							row = (int) elementIdx / numDevices;
							col = elementIdx % numDevices;
							resource.setChannelProb(row,col,0,number);
							elementIdx ++;
							break;
						case "marProbChannelHigh":
							row = (int) elementIdx / numDevices;
							col = elementIdx % numDevices;
							resource.setChannelProb(row,col,1,number);
							elementIdx ++;
							break;
						default:
							System.out.println(fieldName); 
							System.out.println("Profile: invalid line");
							return false;
					}
				}
			}
			oldIdx = idx + 1;
		}
		return true;
	}
}
