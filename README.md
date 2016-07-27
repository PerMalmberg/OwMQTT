# OwMQTT
OneWire to MQTT bridge

## Features
  * Uses ow-shell commands to read all properties of all devices attached to all 1-Wire adapters.
  * Publishes all properties to an MQTT broker.
  * Can write data to 1-Wire devices via MQTT-messages.
  * Supports the alias-feature of OWFS (devices are named 'MyName' instead of the default <family>.<address>)
 
## Reqirements
  * Java 8
  * An instance of a working [owserver](http://owfs.org/index.php?page=owserver) at a local or remote computer.
  * An MQTT broker, such as [Mosquitto](https://mosquitto.org/)
   
## Usage
1. Compile the project using IntelliJ, this will create a OwMQTT.jar file.
2. Run "java -jar OwMQTT.jar" to display the help.

### Usage example:
    java -jar OwMQTT.jar -o localhost -b tcp://192.168.10.245 -i Foo2 -r /read/ -w /write/ -p . --debug
    
