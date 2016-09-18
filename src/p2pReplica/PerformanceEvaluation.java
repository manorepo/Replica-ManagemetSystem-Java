package p2pReplica;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PerformanceEvaluation {
	// static volatile ArrayList<FileDetails> replicaFiles = new ArrayList<FileDetails>();
		
	int peerid;
	int port;
	String sharedDir;
	String searchFileName;
	boolean bPerformSearch=false;
	public PerformanceEvaluation(int peerid,boolean bPerformSearch) {
		// TODO Auto-generated constructor stub
		this.peerid=peerid;
		//this.port=port;
		//this.sharedDir=sharedDir;
		//this.searchFileName=searchFileName;
		this.bPerformSearch=bPerformSearch;
	}
public long startProcess(){

		// Function to handle the peer Operations.
		//
	 ArrayList<FileDetails> replicaFiles = new ArrayList<FileDetails>();
		
	String masterDir;
	long startTime=0;
	long endTime=0;
	String replicaDir;
	ArrayList<FileDetails> masterFiles = new ArrayList<FileDetails>();
	List<Thread> threadInstancesList = new ArrayList<Thread>();
	int port;
	//int peerid = 0;
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
Peer p =new Peer();
		// load a properties file
		prop.load(input);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//
		// Collect peerid and port from the config.
	//	System.out.println("Enter the peerid");
		//peerid = Integer.parseInt(br.readLine());
		port = Integer.parseInt( prop.getProperty("peerid."+peerid+".port"));
		System.out.println("Session for peer id: " + peerid + " started...");
		//
		// Get the directory to share with other peers.
		masterDir = prop.getProperty("peerid."+peerid+".Master");
		replicaDir = prop.getProperty("peerid."+peerid+".Replica");
		
		//
		// Get the filenames that existed locally
		p.getLocalFiles(masterDir, masterFiles,peerid);
		// Run peer as a server on a specific port
		p.runPeerAsServer(peerid, port, masterDir,replicaDir, masterFiles,replicaFiles);

if(bPerformSearch)
{
	startTime=System.currentTimeMillis();
				// PUSH operation
				//
				// Clear the previous PUSH operation contents
				neighborPeers.clear();
				threadInstancesList.clear();
				neighborConnThreadList.clear();
				searchResult_Peers.clear();
				//
				// Get Neighbor peers
				p.getNeighborPeers(prop,neighborPeers, peerid);
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
				endTime=System.currentTimeMillis();
}

	 }
					
	  catch (Exception e) {
			System.out.println(e);
		}
	return (endTime-startTime);

}
public void selectPeerToDownload(Peer p, ArrayList<PeerDetails> searchResult_Peers, String fileName,
		String Path) {
	// Functionality to download the file from the peers
	int peerId;
	try {
			peerId = 4;
			p.download(searchResult_Peers, peerId, fileName, Path);
		}
	 catch (Exception e) {
		System.out.println(e.getMessage());
	
}
}
}
