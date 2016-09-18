package p2pReplica;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Peer {
	 static volatile ArrayList<FileDetails> replicaFiles = new ArrayList<FileDetails>();
		
	public static void main(String args[]) {

		Peer peerInstance = new Peer();
		peerInstance.peerOperations(args);
	}

	public void peerOperations(String args[]) {
		// Function to handle the peer Operations.
		//
		String masterDir;
		String replicaDir;
		ArrayList<FileDetails> masterFiles = new ArrayList<FileDetails>();
		List<Thread> threadInstancesList = new ArrayList<Thread>();
		int port;
		int peerid;
		int PUSHCount = 0;
		int choice;
		Boolean bExit = false;
		ArrayList<NeighborPeers> neighborPeers = new ArrayList<NeighborPeers>();
		ArrayList<PeerDetails> searchResult_Peers = new ArrayList<PeerDetails>();
		ArrayList<NeighborConnectionThread> neighborConnThreadList = new ArrayList<NeighborConnectionThread>();
		Properties prop = new Properties();
		InputStream input = null;
		
		//
		try {
			input = new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			//
			// Collect peerid and port from the config.
			System.out.println("Enter the peerid");
			peerid = Integer.parseInt(br.readLine());
			port = Integer.parseInt( prop.getProperty("peerid."+peerid+".port"));
			System.out.println("Session for peer id: " + peerid + " started...");
			//
			// Get the directory to share with other peers.
			masterDir = prop.getProperty("peerid."+peerid+".Master");
			replicaDir = prop.getProperty("peerid."+peerid+".Replica");
			
			//
			// Get the filenames that existed locally
			getLocalFiles(masterDir, masterFiles,peerid);
			// Run peer as a server on a specific port
			runPeerAsServer(peerid, port, masterDir,replicaDir, masterFiles,replicaFiles);

			// User Menu
			while (true) {
				System.out.println("************ Main Menu ***************");
				System.out.println("1. Push(Send Master copy to all replicas)");
				System.out.println("2. Pull(Update replica on TTR expriy or version change)");
				System.out.println("3. Modify File(Without PUSH - To test PULL)");
				System.out.println("4. Modify File(With PUSH");
				System.out.println("**************************************");
				System.out.println("Select your choice");
				choice = Integer.parseInt(br.readLine());
				switch (choice) {
				case 5:
					System.out.println(replicaFiles.size());
					break;
				case 1:
					// PUSH operation
					//
					// Clear the previous PUSH operation contents
					neighborPeers.clear();
					threadInstancesList.clear();
					neighborConnThreadList.clear();
					searchResult_Peers.clear();
					//
					// Get Neighbor peers
					getNeighborPeers(prop,neighborPeers, peerid);
					//
					// Generate unique message id
					++PUSHCount;
					//
					for(int j=0;j<masterFiles.size();j++){			
						String msgId = "Peer"+peerid+".Iteration" +PUSHCount+".Version"+ masterFiles.get(j).version+"."+masterFiles.get(j).fileName;
						System.out.println("Message id for search: " + msgId);
						
					for (int i = 0; i < neighborPeers.size(); i++) {
						System.out.println("Sending PUSH request to " + neighborPeers.get(i).peerId + " "
								+ neighborPeers.get(i).portno);
						NeighborConnectionThread connectionThread = new NeighborConnectionThread(
								neighborPeers.get(i).ip, neighborPeers.get(i).portno, masterFiles.get(j).fileName, msgId, peerid,
								neighborPeers.get(i).peerId,peerid,1,masterFiles.get(i).modifiedTime);
						Thread threadInstance = new Thread(connectionThread);
						threadInstance.start();
						//
						// Save connection thread instances
						threadInstancesList.add(threadInstance);
						neighborConnThreadList.add(connectionThread);

					}
					}
					//
					// Wait until child threads finished execution
					for (int i = 0; i < threadInstancesList.size(); i++)
						((Thread) threadInstancesList.get(i)).join();
					//
									break;
				case 2:
					//PULL - Poll to get the status of current replica files and update if there is miss match
					for(int i=0;i<replicaFiles.size();i++){
						
						PollFileStatus(replicaFiles.get(i).originPeerId,replicaDir,replicaFiles.get(i).fileName,replicaFiles.get(i).version, i,replicaFiles.get(i).modifiedTime);
					}
					break;
				case 3:
					//Updates the version of the file.
					int version=0;
					long modifiedTiming=0;
					modifiedTiming=System.currentTimeMillis();
					System.out.println("Enter the file name to indicate file modification (just updates version - NO PUSH)");
					String filename=br.readLine();
					for (int j=0;j<masterFiles.size();j++){
						if(masterFiles.get(j).fileName.equals(filename)){
							 version=masterFiles.get(j).version;
							++version;
							masterFiles.get(j).version=version;
							masterFiles.get(j).modifiedTime=modifiedTiming;
							UpdateVersionInServer(prop,peerid,filename,version,modifiedTiming);
						}
					}
					break;
				case 4:
					// Updates the version of file and PUSH the files to all the replica servers
					int versionid=0;
					long modifiedTime=0;
					System.out.println("Enter the file name that to trigger the modify function and PUSH");
					String filename_modify=br.readLine();
					for (int j=0;j<masterFiles.size();j++){
						if(masterFiles.get(j).fileName.equals(filename_modify)){
							 versionid=masterFiles.get(j).version;
							++versionid;
							masterFiles.get(j).version=versionid;
							modifiedTime=System.currentTimeMillis();
							masterFiles.get(j).modifiedTime=modifiedTime;
							UpdateVersionInServer(prop,peerid,filename_modify,versionid,modifiedTime);
						}
					}
					// Clear the previous search contents
					neighborPeers.clear();
					threadInstancesList.clear();
					neighborConnThreadList.clear();
					searchResult_Peers.clear();
					//
					// Get Neighbor peers
					getNeighborPeers(prop,neighborPeers, peerid);
					//
					// Generate unique message id
					++PUSHCount;
					//

						
					// Loop through all the neighbor peers
					String msgId = "Peer"+peerid+".Iteration" +PUSHCount+".Version"+ versionid+"."+filename_modify;
					System.out.println("Message id for search: " + msgId);
						
					for (int i = 0; i < neighborPeers.size(); i++) {
						System.out.println("Sending request to " + neighborPeers.get(i).peerId + " "
								+ neighborPeers.get(i).portno);
						NeighborConnectionThread connectionThread = new NeighborConnectionThread(
								neighborPeers.get(i).ip, neighborPeers.get(i).portno, filename_modify, msgId, peerid,
								neighborPeers.get(i).peerId,peerid,versionid,modifiedTime);
						Thread threadInstance = new Thread(connectionThread);
						threadInstance.start();
						//
						// Save connection thread instances
						threadInstancesList.add(threadInstance);
						neighborConnThreadList.add(connectionThread);

					}
					// Wait until child threads finished execution
					for (int i = 0; i < threadInstancesList.size(); i++)
						((Thread) threadInstancesList.get(i)).join();
					//
					break;
				default:
					bExit = true;
				}
				if (bExit) {
					// End of the client session
					System.exit(1);
					break;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void UpdateVersionInServer(Properties prop, int peerid, String filename,int version,long modifiedTime) {
		// TODO Auto-generated method stub
		int port= Integer.parseInt(prop.getProperty("peerid."+peerid+".port"));
		PeerInterface peer=null;
		 try {
			 // Establish the connection
			peer=(PeerInterface) Naming.lookup("rmi://localhost:"+port+"/peerServer");
			// Call remote method updateVersion
			peer.updateVersion(filename,version,modifiedTime);			
		 } catch (MalformedURLException | RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			 System.out.println("Unable to connect to " + peerid +" : "+e.getMessage());
		}
	
	}

	public void getLocalFiles(String sharedDir, ArrayList<FileDetails> masterFiles,int peerid) {
		// Get the local files that existed in the local directory
		File directoryObj = new File(sharedDir);
		File newfind;
		String filename;
		String[] filesList = directoryObj.list();
		for (int i = 0; i < filesList.length; i++) {
			FileDetails temp=new FileDetails();
			newfind = new File(filesList[i]);
			temp.fileName = newfind.getName();
			temp.version=1;
			temp.originPeerId=peerid;
			temp.modifiedTime=System.currentTimeMillis();
			// Store the file name in arrayList
			masterFiles.add(temp);

		}

	}

	public void runPeerAsServer(int peerId, int port, String sharedDir,String replicaDir, ArrayList<FileDetails> masterFiles,ArrayList<FileDetails> replicaFiles) {
		// Run peer remote methods on provided port
		try {
			LocateRegistry.createRegistry(port);
			PeerInterface stub = new PeerInterfaceRemote(sharedDir,replicaDir, peerId, port, masterFiles,replicaFiles);
			Naming.rebind("rmi://localhost:" + port + "/peerServer", stub);
			System.out.println("Peer " + peerId + " acting as server on 127.0.0.1:" + port);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void getNeighborPeers(Properties prop, ArrayList<NeighborPeers> neighborPeers, int peerId) {
		// Get the Neighbor peers for the provided peer id
		String property = null;
		
		try {

			property = "peerid." + peerId + ".neighbors";
			// get the property value and print it out
			String[] strNeighbors = prop.getProperty(property).split(",");
			for (int i = 0; i < strNeighbors.length; i++) {
				NeighborPeers tempPeer = new NeighborPeers();
				tempPeer.peerId = strNeighbors[i];
				tempPeer.ip = prop.getProperty(strNeighbors[i] + ".ip");
				tempPeer.portno = Integer.parseInt(prop.getProperty(strNeighbors[i] + ".port"));
				neighborPeers.add(tempPeer);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			
		}
	}



	public void download(ArrayList<PeerDetails> searchResult_Peers, int peerId, String fileName, String Path)
			throws IOException {
		// Download functionality
		int totalPeers, iCount = 0;
		int port = 0;
		String Host = null;
		totalPeers = searchResult_Peers.size();
		while (iCount < totalPeers) {
			if (peerId == searchResult_Peers.get(iCount).peerId) {
				port = searchResult_Peers.get(iCount).port;
				Host = searchResult_Peers.get(iCount).hostIp;
				break;
			}
			iCount++;
		}

		System.out.println("Downloading from " + Host + ":" + port);
		//
		// Get an object for peer server to download the file.
		PeerInterface PeerServer = null;
		try {
			PeerServer = (PeerInterface) Naming.lookup("rmi://localhost:" + port + "/peerServer");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Call remote method, obtain, to download the file
		byte[] fileData = null;
		try {
			fileData = PeerServer.obtain(fileName);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		// Create new file for current peer with the downloaded bytes of
		// data
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(Path + "//" + fileName));
		try {
			output.write(fileData, 0, fileData.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output.flush();
		output.close();
		System.out.println("\"" + fileName + "\" downloaded to path: " + Path);

	}

	public void PollFileStatus(int originPeerId,String replicaDir,String filename,int version,int index,long lastModifiedTime){
		Properties prop = new Properties();
		InputStream input = null;
			PeerInterface PeerServer = null;
		BufferedOutputStream output=null;
		try {
			input = new FileInputStream("config.properties");
			// load a properties file
						prop.load(input);
			int port=Integer.parseInt(prop.getProperty("peerid."+originPeerId+".port"));

			PeerServer = (PeerInterface) Naming.lookup("rmi://localhost:" + port + "/peerServer");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int latestVersion=0;
		try {
			latestVersion = PeerServer.checkFileStatus(filename,version,lastModifiedTime);
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Call remote method, obtain, to download the file
		if(latestVersion>0){
		byte[] fileData = null;
		try {
			fileData = PeerServer.obtain(filename);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		// Create new file for current peer with the downloaded bytes of
		try{
		// data
		 output = new BufferedOutputStream(new FileOutputStream(replicaDir + "//" + filename));
	
			output.write(fileData, 0, fileData.length);
			output.flush();
			output.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\"" + filename + "\" downloaded to path: " + replicaDir);
		replicaFiles.get(index).version=latestVersion;
		long currentModifiedTime=System.currentTimeMillis();
		replicaFiles.get(index).modifiedTime=currentModifiedTime;
		}
	}

}

