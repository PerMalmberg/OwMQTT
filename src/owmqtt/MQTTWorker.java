// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;


public class MQTTWorker implements MqttCallback {
	private final MqttClient mqtt;

	public MQTTWorker(String mqttBroker, String mqttClientId, String persistenceLocation) throws MqttException {
		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistenceLocation);
		mqtt = new MqttClient(mqttBroker, mqttClientId, persistence);
	}

	public void start() throws MqttException {
		mqtt.setCallback(this);
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		connOpts.setAutomaticReconnect(true);
		mqtt.connect(connOpts);
	}

	public void publish(String topic, String value)
	{
		try {
			mqtt.publish( topic, value.getBytes(), 2, false);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable throwable) {
		System.out.println("Lost connection");
	}

	@Override
	public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
		System.out.println("Message:" + s + mqttMessage.toString());
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
		System.out.println("Delivered: " + iMqttDeliveryToken.toString());
	}
}
