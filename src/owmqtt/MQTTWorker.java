// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MQTTWorker implements MqttCallback, IOwToMqtt {
	private final MqttClient mqtt;
	private final ConcurrentLinkedQueue<Map.Entry<String, MqttMessage>> myIncoming = new ConcurrentLinkedQueue<>();
	private final String myReadTopicRoot;
	private final String myWriteTopicRoot;
	private final boolean myDebugLogging;

	public MQTTWorker(String mqttBroker, String mqttClientId, String persistenceLocation, String readTopicRoot, String writeTopicRoot, boolean debugLog) throws MqttException {
		myDebugLogging = debugLog;

		if (!readTopicRoot.endsWith("/")) {
			myReadTopicRoot = readTopicRoot + "/";
		} else {
			myReadTopicRoot = readTopicRoot;
		}

		if (!writeTopicRoot.endsWith("/")) {
			myWriteTopicRoot = writeTopicRoot + "/";
		} else {
			myWriteTopicRoot = writeTopicRoot;
		}

		mqtt = new MqttClient(mqttBroker, mqttClientId, new MqttDefaultFilePersistence(persistenceLocation));
	}

	public void start() throws MqttException {
		mqtt.setCallback(this);
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		connOpts.setAutomaticReconnect(true);
		mqtt.connect(connOpts);
		info("MQTT worker started");
	}

	@Override
	public void connectionLost(Throwable throwable) {
		error("Lost connection");
	}

	@Override
	public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
		debug("Incoming message:" + s + mqttMessage.toString());
		// We're only interested in messages targeting our writable root
		if( s.startsWith( myWriteTopicRoot)) {
			s = s.replace(myWriteTopicRoot, "");
			myIncoming.add(new AbstractMap.SimpleEntry<>(s, mqttMessage));
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

	}

	@Override
	public void subscribe(String topic) {
		try {
			// Subscribe to the writable root
			String fullTopic = myWriteTopicRoot + topic;
			debug("Subscribe to: " + fullTopic);
			mqtt.subscribe(fullTopic, Qos.ExactlyOnce.getValue());
		} catch (MqttException e) {
			error(e);
		}
	}

	@Override
	public void publish(String topic, String content, Qos qos, boolean retain) {
		try {
			debug("Publish: " + topic + ":" + content);
			mqtt.publish(myReadTopicRoot + topic, content == null ? "".getBytes() : content.getBytes(), qos.getValue(), retain);
		} catch (MqttException e) {
			error(e);
		}
	}

	@Override
	public ConcurrentLinkedQueue<Map.Entry<String, MqttMessage>> getIncomingMessages() {
		return myIncoming;
	}

	public void stop() {
		try {
			mqtt.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	public void debug(String msg) {
		if (myDebugLogging) {
			System.out.println(msg);
		}
	}

	public void error(String msg) {
		System.err.println(msg);
	}

	public void error(Exception ex) {
		System.out.println(ex.getMessage());
		System.out.println(Arrays.toString(ex.getStackTrace()));
	}

	public void info(String msg) {
		System.out.println(msg);
	}
}
