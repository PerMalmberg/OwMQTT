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
	private final String myTopicRoot;
	private final boolean myDebugLogging;

	public MQTTWorker(String mqttBroker, String mqttClientId, String persistenceLocation, String topicRoot, boolean debugLog) throws MqttException {
		myDebugLogging = debugLog;

		if (!topicRoot.endsWith("/")) {
			myTopicRoot = topicRoot + "/";
		} else {
			myTopicRoot = topicRoot;
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
		myIncoming.add(new AbstractMap.SimpleEntry<>(s, mqttMessage));
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

	}

	@Override
	public void subscribe(String topic, Qos qos) {
		try {
			String fullTopic = myTopicRoot + topic;
			debug("Subscribe to: " + fullTopic);
			mqtt.subscribe(fullTopic, qos.getValue());
		} catch (MqttException e) {
			error(e);
		}
	}

	@Override
	public void publish(String topic, String content, Qos qos) {
		try {
			debug("Publish: " + topic + ":" + content);

			// Publish with retain flag enabled.
			mqtt.publish(myTopicRoot + topic, content == null ? "".getBytes() : content.getBytes(), qos.getValue(), true);
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
