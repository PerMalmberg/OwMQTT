// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import cmdparser4j.CmdParser4J;
import cmdparser4j.SystemOutputParseResult;
import cmdparser4j.SystemOutputUsageFormatter;
import cmdparser4j.limits.NumericLimit;
import javafx.util.Pair;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

	public static void main(String[] args) {
		SystemOutputParseResult result = new SystemOutputParseResult();

		CmdParser4J cmd = new CmdParser4J(result);

		cmd.accept("--owserver").asString(1).withAlias("-o").describedAs("The host where the owserver is running. e.g. 'localhost:4304'").setMandatory();
		cmd.accept("--broker").asString(1).withAlias("-b").describedAs("The address to the MQTT broker, i.e. tcp://yourserver.tld:1833").setMandatory();
		cmd.accept("--mqttPersistenceLocation").asString(1).withAlias("-p").describedAs("Path to where MQTT persistence database will be stored. Can be shared with other clients.").setMandatory();
		cmd.accept("--mqttClientId").asString(1).withAlias("-i").describedAs("MQTT client id").setMandatory();
		cmd.accept("--owTopicRoot").asString(1).withAlias("-t").describedAs("The root topic used to publish 1-Wire data. e.g. '/MyRoot/'").setMandatory();
		cmd.accept("--debug").asSingleBoolean().describedAs("If specified, debug logging is enabled");
		cmd.accept("--owTimeout").asInteger(1, 1, new NumericLimit<Integer>(1500, 5000)).describedAs("1-Wire command timeout. 1500-5000ms");

		int owTimeout =  cmd.getInteger("--owTimeout", 0, 2000);

		if (args.length > 0) {
			// Args provided
			if (!cmd.parse(args)) {
				System.out.println(result.getParseResult());
			} else {
				OwWorker owWorker;
				MQTTWorker mqtt = null;
				try {
					mqtt = new MQTTWorker(cmd.getString("--broker"), cmd.getString("--mqttClientId" ), cmd.getString("--mqttPersistenceLocation"), cmd.getString("--owTopicRoot"), cmd.getBool("--debug"));
					mqtt.start();

					owWorker = new OwWorker(cmd.getString("--owserver"), mqtt, cmd.getBool("--debug"), owTimeout );
					owWorker.start();
					owWorker.join();
				} catch (MqttException | InterruptedException e) {
					e.printStackTrace();
				}
				finally {
					if( mqtt != null ) {
						mqtt.stop();
					}
				}
			}
		} else {
			SystemOutputUsageFormatter usage = new SystemOutputUsageFormatter("OwMQTT");
			cmd.getUsage(usage);
			System.out.println(usage);
		}

	}
}
