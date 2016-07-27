// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package owmqtt;

public enum Qos {
	AtMostOnce(0),
	AtLeastOnce(1),
	ExactlyOnce(2);

	private final int myValue;

	Qos(int value){
		myValue = value;
	}

	public int getValue() {
		return myValue;
	}

}
