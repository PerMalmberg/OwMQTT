// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

import jowshell.Discovery;
import jowshell.logging.ILogger;
import jowshell.system.ICommandExecution;
import jowshell.system.IExecute;
import jowshell.system.ShellExecute;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

public class OwWorker extends Thread implements ILogger, ICommandExecution {
	private final Discovery discovery;
	private final IExecute myExec = new ShellExecute(this);

	public OwWorker(String owserver) throws MqttException {
		discovery = new Discovery(owserver, this, this);
	}

	@Override
	public void run() {


		do {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			debug(".");
		}
		while (!isInterrupted());
	}

	@Override
	public void debug(String msg) {
		System.out.println(msg);
	}

	@Override
	public void error(String msg) {
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
}
