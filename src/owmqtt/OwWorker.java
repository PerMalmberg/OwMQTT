// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import jowshell.Discovery;
import jowshell.Network;
import jowshell.items.OwData;
import jowshell.items.OwDevice;
import jowshell.logging.ILogger;
import jowshell.system.ICommandExecution;
import jowshell.system.IExecute;
import jowshell.system.ShellExecute;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.*;

public class OwWorker extends Thread implements ILogger, ICommandExecution {
	Discovery discovery;
	private final IExecute myExec = new ShellExecute(this);
	private final IOwToMqtt myMqtt;
	private Iterator<OwDevice> myDevIt = null;
	private final boolean myDebugLog;
	private final int myCmdTimeout;
	private String myNoValue;

	public OwWorker(String owserver, IOwToMqtt owToMqtt, boolean debugLog, int cmdTimeout, String noValue) throws MqttException {
		discovery = new Discovery(owserver, this, this);
		myMqtt = owToMqtt;
		myDebugLog = debugLog;
		myCmdTimeout = cmdTimeout;
		myNoValue = noValue;
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
					info( net.getAllDevices().size() + " devices found");

					subscribeToWritableProperties(net);
					while (!isInterrupted()) {
						publishProps(getNextDevice(net));
						handleMessage(myMqtt.getIncomingMessages().poll(), net);
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

	private void handleMessage(Map.Entry<String, MqttMessage> pair, Network net) {
		if (pair != null) {
			String topic = pair.getKey();
			MqttMessage msg = pair.getValue();

			String[] split = topic.split("/");
			if (split.length > 0) {
				String deviceName = split[0];

				OwDevice device = net.getAllDevices().get(deviceName);
				if (device != null) {
					HashMap<String, OwData> data = device.getData();
					String propName = topic.replace(deviceName, "");
					if (propName.startsWith("/")) {
						propName = propName.substring(1, propName.length());
					}
					OwData owData = data.get(propName);
					if (owData.isWritable(this)) {
						owData.write(this, new String(msg.getPayload()));
					}
				}
			}
		}
	}

	private void publishProps(OwDevice device) {
		device.getData().values().stream().filter(data -> data.isReadable(this)).forEach(data -> {
			String s = data.read(this);
			if (s == null) {
				if (myNoValue != null) {
					s = myNoValue;
				}
			} else {
				s = s.trim();
			}

			if( s == null) {
				error("Could not read data from " + data.getFullPath());
			}
			else {
				// If writable, we don't want the MQTT broker to retain the message because that will
				// result in us receiving it on connect and thus writing it to the device, which may have changed
				// since we last were connected
				boolean retain = !data.isWritable(this);

				myMqtt.publish(device.getName() + "/" + data.getFullPropertyName(), s, Qos.ExactlyOnce, retain);
			}
		});
	}

	private void subscribeToWritableProperties(Network net) {
		for (OwDevice dev : net.getAllDevices().values()) {
			dev.getData().values().stream().filter(data -> data.isWritable(this)).forEach(data -> {
				myMqtt.subscribe(dev.getName() + "/" + data.getFullPropertyName());
			});

		}
	}

	@Override
	public void debug(String msg) {
		if (myDebugLog) {
			System.out.println( prefixTime( msg ));
		}
	}

	private String prefixTime(String msg) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		return timeStamp + ": " + msg;

	}

	@Override
	public void error(String msg) {
		System.err.println(prefixTime( msg ));
	}

	public void info(String msg) {
		System.out.println(prefixTime( msg ));
	}

	@Override
	public void error(Exception ex) {
		error(ex.getMessage());
		error(Arrays.toString(ex.getStackTrace()));
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
