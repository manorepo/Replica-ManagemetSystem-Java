package p2pReplica;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class NeighborConnectionThread extends Thread{
	// Class to handle the connection with neighbor peers
	
int port;
String ip;
String fileName;
String msgId;
int fromPeerId;
int fromPeerPort;
ArrayList<PeerDetails> filesFoundat = new ArrayList<PeerDetails>();
HitQuery hitQueryResult = new HitQuery();
String toPeerId;
int originPeerId;
int version;
long modifiedTime;
NeighborConnectionThread(String ip,int port, String fileName, String msgId,int fromPeerId,String toPeerId,int originPeerId,int version,long modifiedTime){
	this.ip=ip;
	this.port=port;
	this.fromPeerId=fromPeerId;
	this.fileName=fileName;;
	this.msgId=msgId;
	this.toPeerId=toPeerId;
	this.originPeerId=originPeerId;
	this.version=version;
	this.modifiedTime=modifiedTime;
}
	@Override
	public void run() {
		PeerInterface peer=null;
			 try {
				 // Establish the connection
				peer=(PeerInterface) Naming.lookup("rmi://"+ip+":"+port+"/peerServer");
				// Call remote method query
				peer.invalidation(fromPeerId,msgId, fileName,version,originPeerId,modifiedTime);			
			 } catch (MalformedURLException | RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				 System.out.println("Unable to connect to " + toPeerId +" : "+e.getMessage());
			}
		
	}
	public HitQuery getValue(){
		// Method to return the hit query result
		return hitQueryResult;
	}

}
