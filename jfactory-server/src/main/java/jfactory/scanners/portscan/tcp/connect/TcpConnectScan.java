package jfactory.scanners.portscan.tcp.connect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.net.util.SubnetUtils;

import lombok.Getter;

public class TcpConnectScan extends Thread {
	class Worker extends Thread {

		private String host;
		private Integer port;

		public Worker(String host, Integer port) {
			this.host = host;
			this.port = port;
		}

		@Override
		public void run() {
			Thread.currentThread().setName("TcpConnectScan-Worker-" + host + ":" + port);

			Proxy proxy = new Proxy(Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050));
			try (Socket socket = new Socket(proxy)) {
				socket.setSoTimeout(3000);
				socket.connect(new InetSocketAddress(host, port));
				System.err.println(host + ":" + port);
			} catch (IOException e1) {
				// IGNORE
			}
			workers.remove(this);
			this.getClass();
		}

	}

	public static void main(String[] args) {
		TcpConnectScan connectScan = new TcpConnectScan();
		connectScan.maxThreads = 4;
		// connectScan.getAddrs().add("109.198.0.0/16");
		connectScan.getPorts().add(25575);

		connectScan.start();
	}

	private CopyOnWriteArrayList<Worker> workers = new CopyOnWriteArrayList<>();
	private @Getter CopyOnWriteArrayList<String> addrs = new CopyOnWriteArrayList<>();
	private @Getter CopyOnWriteArrayList<Integer> ports = new CopyOnWriteArrayList<>();

	private int maxThreads;
	private Random random = new Random();

	public TcpConnectScan() {

	}

	private String getRandomHost() {
		StringBuilder host = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			int o = getRandomInt(0, 255);
			if (i < 3) {
				host.append(o).append('.');
			} else {
				host.append(o);

			}
		}
		return host.toString();
	}

	private int getRandomInt(int min, int max) {
		return random.ints(min, max).findFirst().getAsInt();
	}

	@Override
	public void run() {

		if (addrs.isEmpty()) {
			do {
				String host = getRandomHost();
				for (Integer port : ports) {
					while (workers.size() > maxThreads) {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					Worker worker = new Worker(host, port);
					workers.add(worker);
					worker.start();
				}
			} while (true);

		} else {
			for (String addrCIDR : addrs) {
				SubnetUtils addrCIDRa = new SubnetUtils(addrCIDR);
				for (String host : addrCIDRa.getInfo().getAllAddresses()) {
					for (Integer port : ports) {
						while (workers.size() > maxThreads) {
							try {
								Thread.sleep(30);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						Worker worker = new Worker(host, port);
						workers.add(worker);
						worker.start();
					}
				}
			}
		}

	}

}
