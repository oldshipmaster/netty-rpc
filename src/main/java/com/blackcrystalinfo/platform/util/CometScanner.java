package com.blackcrystalinfo.platform.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CometScanner {
	private static final Logger logger = LoggerFactory.getLogger(CometScanner.class);

	private final static float CPU_THRESHOLD = Float.valueOf(Constants.getProperty("cpu.threshold", "0.9"));
	private final static long MEM_THRESHOLD = Long.valueOf(Constants.getProperty("mem.threshold", "1024"));
	private final static long HANDLER_THRESHOLD = Long.valueOf(Constants.getProperty("handler.threshold", "65535"));
	private final static long TICTOK = Long.valueOf(Constants.getProperty("tiktok", "1000"));

	private final static String root = Constants.getProperty("root", "/");

	private final static BlockingQueue<String> q = new LinkedBlockingQueue<String>();
	private final static Map<String, String> PATH2URL = new HashMap<String, String>();
	private final static Map<String, String> IPMAP = new HashMap<String, String>();

	private static ZooKeeper zk = null;

	private final static Pattern p = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+");

	static {
		try {
			Properties p = new Properties();
			p.load(ClassLoader.getSystemResourceAsStream("ip.properties"));
			for (Object key : p.keySet()) {
				String v = p.getProperty((String) key);
				IPMAP.put((String) key, v);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void tiktok() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				scan();
			}
		}, 0, TICTOK);
	}

	private static boolean assignZk() {
		if (zk != null && zk.getState().isAlive() && zk.getState().isConnected())
			return true;
		String zookeepers = Constants.getProperty("zookeeper", "");
		logger.debug("connect...{} ", zookeepers);
		try {
			final CountDownLatch signal = new CountDownLatch(1);
			zk = new ZooKeeper(zookeepers, 1000, new Watcher() {
				public void process(WatchedEvent event) {
					signal.countDown();
				}
			});
			signal.await();
			return true;
		} catch (Exception e) {
			logger.error("", e);
		}
		return false;
	}

	private static void scan() {

		try {
			if (!assignZk()) {
				logger.debug("There is not single one available zookeeper. This turn have to terminate.");
				return;
			}
			searching(root);
		} catch (KeeperException e) {
			logger.error("scan:KeeperException-", e);
		} catch (InterruptedException e) {
			logger.error("scan:InterruptedException-", e);
		} catch (Exception e) {
			logger.error("scan:Exception-", e);
		} finally {

		}
	}

	private static void searching(String path) throws Exception {
		List<String> l = null;
		l = zk.getChildren(path, false);
		Set<String> children = new HashSet<String>();
		for (String s : l) {
			Stat st = new Stat();
			final String tmpPath = path.equals("/") ? "/" + s : path + "/" + s;
			children.add(tmpPath);
			String data = new String(zk.getData(tmpPath,null, st));
			String[] items = data.split(",");
			if (items.length == 5) {
				String url = items[0];
				PATH2URL.put(tmpPath, url);
				float cpu = Float.valueOf(items[1]);
				long tm = Long.valueOf(items[2]);
				long um = Long.valueOf(items[3]);
				long h = Long.valueOf(items[4]);
				if (cpu < CPU_THRESHOLD && tm - um > MEM_THRESHOLD && HANDLER_THRESHOLD > h) {
					if (!q.contains(url)) {
						q.put(url);
						logger.debug("adding url:{}|path:{}", url, tmpPath);
					}
				} else {
					if (q.contains(url)) {
						q.remove(url);
						logger.debug("remove url:{}|path:{}", url, tmpPath);
					}
				}
			}

			// int n = st.getNumChildren();
			// logger.info("znode:{} has {} children", path.equals("/") ? "/" + s : path + "/" + s, n);
			// if (n > 0) {
			// recurseZnode(zk, path.equals("/") ? "/" + s : path + "/" + s);
			// } else {
			// return;
			// }
		}
		Set<String> keys = PATH2URL.keySet();
		keys.removeAll(children);
		for(String child : keys){
			q.remove(PATH2URL.get(child));
			PATH2URL.remove(child);
		}
	}

	public static synchronized String take() {
		String url = q.poll();
		if (url != null)
			q.offer(url);
		else
			url = Constants.getProperty("websocket.addr", "");
		Matcher m = p.matcher(url);
		if (m.find()) {
			String key = m.group();
			if (IPMAP.get(key) != null)
				url = url.replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+", IPMAP.get(key));
			else
				logger.error("ip {} has no its mapping ip ", key);
		}
		logger.debug("return url:{}", url);
		return url;
	}

	public static void closing() {
		if (zk != null)
			try {
				zk.close();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public static void main2(String[] args) throws Exception {
		CometScanner.tiktok();
		Thread.sleep(1 * 1000);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				CometScanner.closing();
			}
		});
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				String url = CometScanner.take();
				System.out.println("--------------" + url);
			}
		}, 0, 3000);
	}
	
	public static void main(String[] args) {
		Set<String> a = new HashSet<String>();
		a.add("a");
		a.add("b");
		Set<String> b = new HashSet<String>();
		b.add("a");
		b.add("c");
		float f = Float.valueOf("NaN");
		System.out.println(f<CPU_THRESHOLD);
		System.out.println(Float.valueOf("NaN"));
	}
}