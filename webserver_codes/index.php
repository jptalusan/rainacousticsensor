<HTML>
<HEAD>
SENSORS
</HEAD>
<BODY>

<h2>Direct upload from Tx via SMS</h2>
<ul>
<li><a href="rainRTX.php?transmitter=RT1">Rain Sensor 1</a></li>
<li><a href="rainRTX.php?transmitter=RT2">Rain Sensor 2</a></li>
<li><a href="rainRTX.php?transmitter=RT3">Rain Sensor 3</a></li>
<br>
<li><a href="rainRTX.php?transmitter=RT4">Rain Sensor 4</a></li>
<li><a href="rainRTX.php?transmitter=RT5">Rain Sensor 5</a></li>
<li><a href="rainRTX.php?transmitter=RT6">Rain Sensor 6</a></li>
<br>
<li><a href="rainRTX.php?transmitter=RT7">Rain Sensor 7</a></li>
<li><a href="rainRTX.php?transmitter=RT8">Rain Sensor 8</a></li>
</ul>

<ul>
<li><a href="downloadRTXcsv.php?transmitter=RT1">Download Rain Sensor 1 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT2">Download Rain Sensor 2 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT3">Download Rain Sensor 3 csv file</a></li>
<br>
<li><a href="downloadRTXcsv.php?transmitter=RT4">Download Rain Sensor 4 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT5">Download Rain Sensor 5 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT6">Download Rain Sensor 6 csv file</a></li>
<br>
<li><a href="downloadRTXcsv.php?transmitter=RT7">Download Rain Sensor 7 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT8">Download Rain Sensor 8 csv file</a></li>
</ul>
<hr>

<h2>Direct upload from Tx via wifi or mobile data</h2>
<ul>
<li><a href="rainRTX.php?transmitter=RT1TxDirect">Rain Sensor 1</a></li>
<li><a href="rainRTX.php?transmitter=RT2TxDirect">Rain Sensor 2</a></li>
<li><a href="rainRTX.php?transmitter=RT3TxDirect">Rain Sensor 3</a></li>
<br>
<li><a href="rainRTX.php?transmitter=RT4TxDirect">Rain Sensor 4</a></li>
<li><a href="rainRTX.php?transmitter=RT5TxDirect">Rain Sensor 5</a></li>
<li><a href="rainRTX.php?transmitter=RT6TxDirect">Rain Sensor 6</a></li>
<br>
<li><a href="rainRTX.php?transmitter=RT7TxDirect">Rain Sensor 7</a></li>
<li><a href="rainRTX.php?transmitter=RT8TxDirect">Rain Sensor 8</a></li>
</ul>

<ul>
<li><a href="downloadRTXcsv.php?transmitter=RT1TxDirect">Download Rain Sensor 1 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT2TxDirect">Download Rain Sensor 2 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT3TxDirect">Download Rain Sensor 3 csv file</a></li>
<br>
<li><a href="downloadRTXcsv.php?transmitter=RT4TxDirect">Download Rain Sensor 4 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT5TxDirect">Download Rain Sensor 5 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT6TxDirect">Download Rain Sensor 6 csv file</a></li>
<br>
<li><a href="downloadRTXcsv.php?transmitter=RT7TxDirect">Download Rain Sensor 7 csv file</a></li>
<li><a href="downloadRTXcsv.php?transmitter=RT8TxDirect">Download Rain Sensor 8 csv file</a></li>
</ul>

<hr>
<br>
<a href="help.php">How To</a>
<br><br>
Use "http://rainsensor.excthackathon.x10host.com/insert.php" to insert data to DB<br>
Use "http://rainsensor.excthackathon.x10host.com" as server address in receiver phone.<br>
<br>
Monitor is the phone used to control both the receiver and transmitter.<br>
Transmitter is RT, the one used to record rain acoustics.<br>
Server is the "receiver".<br>
Enter the needed information ie. number in the following format: "+639989118888" <br>
<br>
Don't hesitate to email me when needed.<br>
jptalusan@gmail.com<br>
<br>
Right now there is only RT1 set in the receiver, <br>
So you'll only be able to use 1 device as transmiter. <br>
i'll update it when you need more devices.<br>
The sensor name "RT1" is currently hardcoded.<br>
<br>
I haven't been able to test this comepletely but they should<br>
still have the same functionality as the previous code. <br>
I haven't change logic here, just added the edit text boxes.<br>

<br>
Results Notation:
<ul>
<li>snd: sound level: split into 6 so each snd1...6 is an average of the 20 samples (dB)</li>
<li>sig: signal strength of the phone: during the same time the sound data is called (in dBm), same 120 samples divided into 6 with averages of the 20</li>
<li>dis: (my bad, i thought it was display) distribution of sound, this is just a histogram of the data sound data received divided into 10 slices starting from -20dB to 0dB at 1dB increments</li>
</ul>
</BODY>