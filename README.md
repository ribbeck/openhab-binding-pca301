# openHAB binding for ELV PCA 301 radio power sockets.
This binding works with a JeeLink RF USB device and installed PCA301 sketch. The sketch (pcaSerial) can be downloaded from [FHEM sourceforge](http://sourceforge.net/p/fhem/code/HEAD/tree/trunk/fhem/contrib/arduino/).
Further information about the JeeLink device and sketch installation can be found in [FHEM wiki](http://www.fhemwiki.de/wiki/JeeLink).

## Installation
On linux distributions with dpkg you can just install the deb file. On other systems just copy the jar file into the addons folder of openHAB.
Be sure that the dependency org.openhab.io.transport.serial-1.x.x,jar is installed too.

## Configuration
The following configuration in openhab.cfg is required:

    pca301:port=<USB port of JeeLink device>	# e.q. /dev/ttyUSB0
    pca301:retryCount=<Number of retries>		# e.q. 5 (since 1.7.2)

## Binding
The binding configuration of a PCA301 item looks as follwing:

    {pca301="<key>=<value>,<key>=<value>"}

Available keys:

* **address**
A 3-bytes integer. Can be found in logging file when a new PCA301 device is noticed.
* **property**
The name of the property which should be read/written. Following properties are available:
	* consumption
	Total power consumption since last reset in kWh.
	* power
	Current power in Watt.
	* reset
	Resets the total power consumption.
	* state
	Current state of socket. Either on or off.

## Examples

    Switch Socket	"PCA301 Socket"	{pca301="address=178720,property=state"}
    Number Power	"PCA301 Power"	{pca301="address=178720,property=power"}
