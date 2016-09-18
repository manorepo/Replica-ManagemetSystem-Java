package p2pReplica;

public class PerformanceLauncher {
	public static void main(String args[]) {
	//	int noofPeersToSearch = Integer.parseInt(args[0]);
		int noofPeersToSearch=0;
		int count = 0;
		boolean bSearch = false;
		if (++count <= noofPeersToSearch) {
			bSearch = true;
		} else {
			bSearch = false;
		}
	
		if (++count <= noofPeersToSearch) {
			bSearch = true;
		} else {
			bSearch = false;
		}

		
		if (++count <= noofPeersToSearch) {
			bSearch = true;
		} else {
			bSearch = false;
		}

	
		if (++count <= noofPeersToSearch) {
			bSearch = true;
		} else {
			bSearch = false;
		}
		
		PerformancePeerClass peer2 = new PerformancePeerClass(2,false);
		peer2.start();
			PerformancePeerClass peer3 = new PerformancePeerClass(3,false);
		peer3.start();
		PerformancePeerClass peer4 = new PerformancePeerClass(4,bSearch);
		peer4.start();
		if (++count <= noofPeersToSearch) {
			bSearch = true;
		} else {
			bSearch = false;
		}

		PerformancePeerClass peer5 = new PerformancePeerClass(5, bSearch);
		peer5.start();
		PerformancePeerClass peer6 = new PerformancePeerClass(6, bSearch);
		peer6.start();
		PerformancePeerClass peer7 = new PerformancePeerClass(7, bSearch);
		peer7.start();
		PerformancePeerClass peer8 = new PerformancePeerClass(8, bSearch);
		peer8.start();
		PerformancePeerClass peer9 = new PerformancePeerClass(9, bSearch);
		peer9.start();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PerformancePeerClass peer1 = new PerformancePeerClass(1,true);
		peer1.start();
		
		
		
		
	}
}

class PerformancePeerClass extends Thread {
	int peerid;
	int port;
	String sharedDir;
	String searchFileName;
	boolean bPerformSearch = false;

	public PerformancePeerClass(int peerid, boolean bPerformSearch) {
		this.peerid = peerid;
		this.port = port;
		this.sharedDir = sharedDir;
		this.searchFileName = searchFileName;
		this.bPerformSearch = bPerformSearch;
	}

	public void run() {

		PerformanceEvaluation p = new PerformanceEvaluation(peerid, bPerformSearch);
		long time = p.startProcess();
		System.out.println("peerid "+peerid+" PUSH running time is "+time);

	}
}
