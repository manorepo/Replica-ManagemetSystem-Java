package p2pReplica;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class NeighborPeers
{
	String peerId;
	int portno;
	String ip;
}
public class PeerInterfaceRemote extends UnicastRemoteObject implements PeerInterface {
	private static final long serialVersionUID = 1L;
	String masterDir;
	String replicaDir;
	ArrayList<FileDetails> masterFiles=new ArrayList<FileDetails>();
	ArrayList<String> processdMsgIds;
	int peerId;
	int currentPeerPort;
	ArrayList<FileDetails> replicaFiles;

	PeerInterfaceRemote(String sharedDir,String replicaDir,int peerId,int currentPeerPort,ArrayList<FileDetails> masterFiles,ArrayList<FileDetails> replicaFiles) throws RemoteException {
		super();
		this.masterDir = sharedDir;
		this.replicaDir=replicaDir;
		this.peerId=peerId;
		this.masterFiles=masterFiles;
		this.currentPeerPort=currentPeerPort;
		processdMsgIds=new ArrayList<String>();
		this.replicaFiles=replicaFiles;
	}
	public int checkFileStatus(String fileName,int version,long lastModifiedTime) throws RemoteException{
		long TTR=Long.parseLong(getProperty("TTR"));
		long currentTime=System.currentTimeMillis();
		for(int i=0;i<masterFiles.size();i++){
			if(masterFiles.get(i).fileName.equals(fileName) &&(( masterFiles.get(i).version != version) || (currentTime-lastModifiedTime>=TTR))){
				return masterFiles.get(i).version;
			}
		}
		return 0;
	}
	public synchronized byte[] obtain(String filename) throws RemoteException {
		byte[] fileBytes = null;
		String fullFileName = masterDir + "/" + filename;
		try {
			File myFile = new File(fullFileName);
			fileBytes = new byte[(int) myFile.length()];
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(fullFileName));
			input.read(fileBytes, 0, fileBytes.length);
			input.close();
			return fileBytes;
		} catch (Exception e) {

		}
		return fileBytes;

	}
	public void updateVersion(String fileName,int version,long modifiedTime){
		int iIndex=-1;
		for(int i=0;i<this.masterFiles.size();i++){
			if(this.masterFiles.get(i).fileName.equals(fileName)){
				iIndex=i;
				break;
			}
		}
		if(iIndex>-1){
			this.masterFiles.get(iIndex).version=version;
			this.masterFiles.get(iIndex).modifiedTime=modifiedTime;
		}
	}
	private String getProperty(String string) {
		// TODO Auto-generated method stub
		String res="";
		try{
		Properties prop = new Properties();
		InputStream input = null;
		input = new FileInputStream("config.properties");
		// load a properties file
		prop.load(input);
		res=prop.getProperty(string);
		}catch(Exception e){
			
		}
			return res;
	}
	public void invalidation(int fromPeerId,String msgId,String filename,int version, int originPeerId,long modifiedTime) throws RemoteException {
		ArrayList<NeighborPeers> neighborPeers=new ArrayList<NeighborPeers>();
		ArrayList<PeerDetails> findAt = new ArrayList<PeerDetails>();
		ArrayList<String> pathTrace=new ArrayList<String>();
		HitQuery hitqueryResult=new HitQuery();
		Boolean bDuplicate=false;
		ArrayList<NeighborConnectionThread> peerThreadsList = new ArrayList<NeighborConnectionThread>();
		synchronized(this){
		if (this.processdMsgIds.contains(msgId)){
			System.out.println("Incoming Request to peer "+peerId+": From - "+fromPeerId+" Duplicate Request - Already searched in this peer- with message id - " + String.valueOf(msgId));
			bDuplicate=true;	
		}
					    
				
		else{
			// Store the messaged id to avoid duplicate search
			
			this.processdMsgIds.add(msgId);

		}
		} 
		if(bDuplicate==false){
			System.out.println("Incoming Request to peer "+peerId+": From - "+fromPeerId+" Search locally and send request to neighbours for msg id- " + String.valueOf(msgId));
			
		List<Thread> threads = new ArrayList<Thread>();
		//
		boolean bUpToDate=false;
		// Search the filename among the local files of current peer
		for(int j=0;j<replicaFiles.size();j++){
			if((replicaFiles.get(j).fileName.equals(filename)) && replicaFiles.get(j).version==version){
				bUpToDate=true;
				break;
			}
		}
		if(bUpToDate==false)
		{
			new DownloadFiles(originPeerId,replicaDir,filename).start();
			FileDetails temp=new FileDetails();
			temp.fileName=filename;
			temp.originPeerId=originPeerId;
			temp.version=version;
			temp.modifiedTime=modifiedTime;
			replicaFiles.add(temp);
		}
		//
		// Read the config file to get the neighbor peers details.
		getNeighborPeers(neighborPeers,peerId);
		if (neighborPeers.size()==0){
			pathTrace.add(Integer.toString(peerId));
		}
		
		for(int i=0;i<neighborPeers.size();i++){
			String currentPeer="peerid."+fromPeerId;
			if (neighborPeers.get(i).peerId.equals(currentPeer)){
			continue;
			}
			System.out.println("Outgoing Request from peer "+peerId+": Sending request to "+neighborPeers.get(i).peerId + " "+ neighborPeers.get(i).portno);
			NeighborConnectionThread ths = new NeighborConnectionThread(neighborPeers.get(i).ip, neighborPeers.get(i).portno,filename,msgId,peerId,neighborPeers.get(i).peerId,originPeerId,version,modifiedTime);  
			Thread ts = new Thread(ths);
			ts.start();
			threads.add(ts);
			peerThreadsList.add(ths);
	
		}
		for (int i = 0; i < threads.size(); i++)
			try {
				((Thread) threads.get(i)).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		for (int i = 0; i < threads.size(); i++)
			try {
				((Thread) threads.get(i)).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			System.out.println("File \""+filename+"\" is uptodate in peer "+ peerId);
		}
		/*for (int i = 0; i < peerThreadsList.size(); i++){
		//	ArrayList<PeerDetails> threadResult = new ArrayList<PeerDetails>();
			HitQuery temp = new HitQuery();
			temp=  (HitQuery) peerThreadsList.get(i).getValue();
			   if(temp.foundPeers.size()>0){
			//  System.out.println("return value of thread "+i+" is "+threadResult.toArray());
			  findAt.addAll(temp.foundPeers);
			   }
			   for (int count=0;count<temp.paths.size();count++){
				   String path=peerId+temp.paths.get(count);
							   pathTrace.add(path);
			   }
			   }
		if (pathTrace.size()==0)
		{
			pathTrace.add(Integer.toString(peerId));
		}
		 System.out.println("HitQuery: Send following result back to "+fromPeerId);
		 for (int i = 0; i < findAt.size(); i++){
			 System.out.println("--Found at Peer"+findAt.get(i).peerId+" on localhost:"+findAt.get(i).port);
			  
			}
		 hitqueryResult.foundPeers.addAll(findAt);
			hitqueryResult.paths.addAll(pathTrace);
			
		}
		
		return hitqueryResult;

*/
	}

	private Boolean searchInCurrentPeer(ArrayList<String> localFiles2, String filename) {
		// TODO Auto-generated method stub
		 int index = masterFiles.indexOf(filename);
		    if (index == -1)
		      return false;
		    else
		      return true;
	}

	private void getNeighborPeers(ArrayList<NeighborPeers> neighborPeers, int peerId) {
		// Get the Neighbor peers for the provided peer id
		String property=null;
	//	NeighborPeers tempPeer=new NeighborPeers();
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);
			property="peerid."+peerId+".neighbors";
			// get the property value and print it out
			String value=prop.getProperty(property);
			if(value!=null){
			String[] strNeighbors=value.split(",");
			
			for(int i=0;i<strNeighbors.length;i++){
				NeighborPeers tempPeer=new NeighborPeers();
				
				tempPeer.peerId=strNeighbors[i];
				tempPeer.ip=prop.getProperty(strNeighbors[i]+".ip");
				tempPeer.portno=Integer.parseInt(prop.getProperty(strNeighbors[i]+".port"));
				neighborPeers.add(tempPeer);
			}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}	
	}

}
class DownloadFiles extends Thread
{
	int originPeerId;
	String replicaDir;
	String filename;
	
	DownloadFiles(int originPeerId,String replicaDir,String filename){
		this.originPeerId=originPeerId;
		this.replicaDir=replicaDir;
		this.filename=filename;
	}
	public void run(){
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
		// Call remote method, obtain, to download the file
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


	}
}