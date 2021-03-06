// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface IOwToMqtt {
	void subscribe(String topic);

	void publish(String topic, String content, Qos qos, boolean retain);

	ConcurrentLinkedQueue<Map.Entry<String, MqttMessage>> getIncomingMessages();
}
