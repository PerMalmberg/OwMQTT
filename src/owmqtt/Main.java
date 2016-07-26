// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import cmdparser4j.CmdParser4J;
import cmdparser4j.SystemOutputParseResult;
import cmdparser4j.SystemOutputUsageFormatter;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Main {

	public static void main(String[] args) {
		SystemOutputParseResult result = new SystemOutputParseResult();

		CmdParser4J cmd = new CmdParser4J(result);

		cmd.accept("--owserver").asString(1).withAlias("-o").describedAs("The host where the owserver is running. e.g. 'localhost:4304'").setMandatory();
		cmd.accept("--broker").asString(1).withAlias("-b").describedAs("The address to the MQTT broker, i.e. tcp://yourserver.tld:1833").setMandatory();
		cmd.accept("--mqttPersistenceLocation").asString(1).withAlias("-p").describedAs("Path to where MQTT persistence database will be stored. Can be shared with other clients.");
		cmd.accept("--mqttClientId").asString(1).withAlias("-i").describedAs("MQTT client id").setMandatory();

		if (args.length > 0) {
			// Args provided
			if (!cmd.parse(args)) {
				System.out.println(result.getParseResult());
			} else {
				OwWorker owWorker;
				try {
					MQTTWorker mqtt = new MQTTWorker(cmd.getString("--broker"), cmd.getString("--mqttClientId" ), cmd.getString("--mqttPersistenceLocation"));
					mqtt.start();
					mqtt.publish("test", "test data");

					owWorker = new OwWorker(cmd.getString("--owserver") );
					owWorker.start();
					owWorker.join();
				} catch (MqttException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			SystemOutputUsageFormatter usage = new SystemOutputUsageFormatter("OwMQTT");
			cmd.getUsage(usage);
			System.out.println(usage);
		}

	}
}
