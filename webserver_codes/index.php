<HTML>
<HEAD>
SENSORS
<script language="javascript" src="calendar/calendar.js"></script>
</HEAD>
<BODY>
<h2>Received Texts</h2>
<a href="receivedTexts.php">Received Texts</a></li>
<br>
<hr>

Select Transmitter and date to show data:
<br>
<form action="rainRTX-datepick.php" method="post">

<select name="transmitter">
    <option value="RT1">RT1</option>
    <option value="RT2">RT2</option>
    <option value="RT3">RT3</option>
    <option value="RT4">RT4</option>
    <option value="RT5">RT5</option>
    <option value="RT6">RT6</option>
    <option value="RT7">RT7</option>
    <option value="RT8">RT8</option>
    <option value="RT1TxDirect">RT1 Direct</option>
    <option value="RT2TxDirect">RT2 Direct</option>
    <option value="RT3TxDirect">RT3 Direct</option>
    <option value="RT4TxDirect">RT4 Direct</option>
    <option value="RT5TxDirect">RT5 Direct</option>
    <option value="RT6TxDirect">RT6 Direct</option>
    <option value="RT7TxDirect">RT7 Direct</option>
    <option value="RT8TxDirect">RT8 Direct</option>
  </select>

<?php
	require_once('calendar/classes/tc_calendar.php');					
	//TODO: Change this to the date yesterday and then today
	$date3_default = "2017-09-11";
	$date4_default = "2017-09-17";

	echo 'From: ';
	$myCalendar = new tc_calendar("date3", true, false);
	$myCalendar->setIcon("calendar/images/iconCalendar.gif");
	$myCalendar->setDate(date('d', strtotime($date3_default))
	    , date('m', strtotime($date3_default))
	    , date('Y', strtotime($date3_default)));
	$myCalendar->setPath("calendar/");
	$myCalendar->setYearInterval(1970, 2020);
	$myCalendar->setAlignment('left', 'bottom');
	$myCalendar->setDatePair('date3', 'date4', $date4_default);
	$myCalendar->writeScript();	  

	echo "\r\n";

	echo 'To: ';

	$myCalendar = new tc_calendar("date4", true, false);
	$myCalendar->setIcon("calendar/images/iconCalendar.gif");
	$myCalendar->setDate(date('d', strtotime($date4_default))
	   , date('m', strtotime($date4_default))
	   , date('Y', strtotime($date4_default)));
	$myCalendar->setPath("calendar/");
	$myCalendar->setYearInterval(1970, 2020);
	$myCalendar->setAlignment('left', 'bottom');
	$myCalendar->setDatePair('date3', 'date4', $date3_default);
	$myCalendar->writeScript();	  
?>

<input type="submit" name="check">
</form>

<br><hr>

Select Transmitter and date to download corresponding CSV:
<br>
<form action="downloadRTXcsv-datepick.php" method="post">

<select name="transmitter">
    <option value="RT1">RT1</option>
    <option value="RT2">RT2</option>
    <option value="RT3">RT3</option>
    <option value="RT4">RT4</option>
    <option value="RT5">RT5</option>
    <option value="RT6">RT6</option>
    <option value="RT7">RT7</option>
    <option value="RT8">RT8</option>
    <option value="RT1TxDirect">RT1 Direct</option>
    <option value="RT2TxDirect">RT2 Direct</option>
    <option value="RT3TxDirect">RT3 Direct</option>
    <option value="RT4TxDirect">RT4 Direct</option>
    <option value="RT5TxDirect">RT5 Direct</option>
    <option value="RT6TxDirect">RT6 Direct</option>
    <option value="RT7TxDirect">RT7 Direct</option>
    <option value="RT8TxDirect">RT8 Direct</option>
  </select>

<?php
	//require_once('calendar/classes/tc_calendar.php');	
	//TODO: Change this to the date yesterday and then today				
	$date5_default = "2017-09-11";
	$date6_default = "2017-09-17";

	echo 'From: ';
	$myCalendar = new tc_calendar("date5", true, false);
	$myCalendar->setIcon("calendar/images/iconCalendar.gif");
	$myCalendar->setDate(date('d', strtotime($date5_default))
	    , date('m', strtotime($date5_default))
	    , date('Y', strtotime($date5_default)));
	$myCalendar->setPath("calendar/");
	$myCalendar->setYearInterval(1970, 2020);
	$myCalendar->setAlignment('left', 'bottom');
	$myCalendar->setDatePair('date5', 'date6', $date5_default);
	$myCalendar->writeScript();	  

	echo "\r\n";

	echo 'To: ';

	$myCalendar = new tc_calendar("date6", true, false);
	$myCalendar->setIcon("calendar/images/iconCalendar.gif");
	$myCalendar->setDate(date('d', strtotime($date6_default))
	   , date('m', strtotime($date6_default))
	   , date('Y', strtotime($date6_default)));
	$myCalendar->setPath("calendar/");
	$myCalendar->setYearInterval(1970, 2020);
	$myCalendar->setAlignment('left', 'bottom');
	$myCalendar->setDatePair('date5', 'date6', $date6_default);
	$myCalendar->writeScript();	  
?>

<input type="submit" name="check">
</form>

<hr>
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
<br>
<li><a href="rainRTX.php?transmitter=RT9TxDirect">Rain Sensor 9 Debug</a></li>
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
</HTML>