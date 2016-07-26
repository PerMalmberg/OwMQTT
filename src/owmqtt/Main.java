package owmqtt;

import cmdparser4j.CmdParser4J;
import cmdparser4j.SystemOutputParseResult;
import cmdparser4j.SystemOutputUsageFormatter;

public class Main {

	public static void main(String[] args) {
		SystemOutputParseResult result = new SystemOutputParseResult();

		CmdParser4J cmd = new CmdParser4J(result);

		cmd.accept("--owserver").asString(1).withAlias("-o").describedAs("The host where the owserver is running. e.g. 'localhost:4304'").setMandatory();
		cmd.accept("--broker").asString(1).withAlias("-b").describedAs("The address to the MQTT broker, i.e. tcp://yourserver.tld:1833").setMandatory();

		if (args.length > 0) {
			// Args provided
			if (!cmd.parse(args)) {
				System.out.println(result.getParseResult());
			}
		}
		else {
			SystemOutputUsageFormatter usage = new SystemOutputUsageFormatter("OwMQTT");
			cmd.getUsage(usage);
			System.out.println( usage );
		}

	}
}
