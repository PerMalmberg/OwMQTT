// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import jowshell.Discovery;
import jowshell.Network;
import jowshell.items.OwDevice;
import jowshell.logging.ILogger;
import jowshell.system.ICommandExecution;
import jowshell.system.IExecute;
import jowshell.system.ShellExecute;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class OwWorker extends Thread implements ILogger, ICommandExecution {
	Discovery discovery;
	private final IExecute myExec = new ShellExecute(this);
	private final IOwToMqtt myMqtt;
	private Iterator<OwDevice> myDevIt = null;
	private final boolean myDebugLog;
	private final int myCmdTimeout;

	public OwWorker(String owserver, IOwToMqtt owToMqtt, boolean debugLog, int cmdTimeout) throws MqttException {
		discovery = new Discovery(owserver, this, this);
		myMqtt = owToMqtt;
		myDebugLog = debugLog;
		myCmdTimeout = cmdTimeout;
	}

	@Override
	public void run() {

		do {
			info("OW worker started");
			myExec.setTimeout(myCmdTimeout);

			try {
				// Discover devices
				if (discovery.discoverTree()) {
					// Subscribe to writable properties.
					Network net = discovery.getNetwork();
					subscribeToWritableProperties(net);
					while (!isInterrupted()) {
						publishProps(getNextDevice(net));
						handleMessage(myMqtt.getIncomingMessages().poll());
					}
				} else {
					error("Failed to discover network, sleeping for a minute");
					sleep(1000 * 60);
				}
			} catch (InterruptedException e) {
				// Nothing
			}
		}
		while (!isInterrupted());
	}

	private void handleMessage(Map.Entry<String, MqttMessage> pair) {
		if (pair != null) {
			int foo = 0;//qqq
		}
	}

	private void publishProps(OwDevice device) {
		device.getData().values().stream().filter(data -> data.isReadable(this)).forEach(data -> {
			String s = data.read(this);
			s = s == null ? "" : s.trim();
			myMqtt.publish(device.getName() + "/" + data.getFullPropertyName(), s, Qos.ExactlyOnce);
		});
	}

	private void subscribeToWritableProperties(Network net) {
		for (OwDevice dev : net.getAllDevices().values()) {
			dev.getData().values().stream().filter(data -> data.isWritable(this)).forEach(data -> {
				myMqtt.subscribe(dev.getName() + "/" + data.getFullPropertyName(), Qos.ExactlyOnce);
			});

		}
	}

	@Override
	public void debug(String msg) {
		if (myDebugLog) {
			System.out.println(msg);
		}
	}

	@Override
	public void error(String msg) {
		System.err.println(msg);
	}

	public void info(String msg) {
		System.out.println(msg);
	}

	@Override
	public void error(Exception ex) {
		System.out.println(ex.getMessage());
		System.out.println(Arrays.toString(ex.getStackTrace()));
	}

	@Override
	public String getOwRead() {
		return "owread";
	}

	@Override
	public String getOwDir() {
		return "owdir";
	}

	@Override
	public String getOwWrite() {
		return "owwrite";
	}

	@Override
	public IExecute getExec() {
		return myExec;
	}

	private OwDevice getNextDevice(Network net) {
		if (myDevIt == null || !myDevIt.hasNext()) {
			myDevIt = net.getAllDevices().values().iterator();
		}

		return myDevIt.next();
	}
}
