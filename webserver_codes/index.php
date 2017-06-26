<HTML>
<HEAD>
SENSORS
</HEAD>
<BODY>
<ul>

<li><a href="rainRT1.php">Rain Sensor 1</a></li>
<li><a href="rainRT2.php">Rain Sensor 2</a></li>
<li><a href="rainRT3.php">Rain Sensor 3</a></li>
</ul>

<ul>
<li><a href="downloadRT1csv.php">Download RT1 csv file</a></li>
<li><a href="downloadRT2csv.php">Download RT2 csv file</a></li>
<li><a href="downloadRT3csv.php">Download RT3 csv file</a></li>
</ul>

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