package p2pReplica;
import java.rmi.*;
//
// Interface for Remote method invocations
public interface PeerInterface extends Remote{
	public byte[] obtain(String filename)throws RemoteException;
	public void updateVersion(String fileName,int version,long modifiedTime)throws RemoteException;
	public int checkFileStatus(String fileName,int version,long lastModifiedTime)throws RemoteException;
	public void invalidation(int fromPeerId,String msgId,String fileName,int version, int originPeerId,long modifiedTime)throws RemoteException;
}
