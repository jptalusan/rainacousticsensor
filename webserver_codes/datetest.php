<head>
<script language="javascript" src="calendar/calendar.js"></script>
</head>
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