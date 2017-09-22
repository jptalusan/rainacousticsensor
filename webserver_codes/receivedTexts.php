<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>  
<title>View Received Texts</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
</head>
<body>
<?php
// connect to the database
include('connect.php');

echo "Data for " . $transmitter . "\r\n";
$queryString = "SELECT * FROM ReceivedTexts";
if ($result = $mysqli->query($queryString))
{
	// display records if there are records to display
	if ($result->num_rows > 0)
	{
		// display records in a table
		echo "<table border='1' cellpadding='10'>";

		// set table headers
		echo "<tr>
				<th>TRANSMITTER ID</th>
				<th>TIMESTAMP</th>
				<th>PHONE NUMBER</th>
				<th>MESSAGE</th>
			  </tr>";

		while ($row = $result->fetch_object())
		{
			$messageArray = explode(';', $row->message); //0 tId, 1 timestamp, 2 message

			$currentTimeInMillis = $messageArray[1];
			$offset = 43200;
			$seconds = $currentTimeInMillis + $offset;
			$timeStamp = date("d/m/Y H:i:s", $seconds);
			// set up a row for each record
			echo "<tr>";
			echo "<td>" . $messageArray[0] . "</td>";
			echo "<td>" . $timeStamp . "</td>";
			echo "<td>" . $row->phoneNumber . "</td>";
			// echo "<td>" . $row->message . "</td>"; //Until changes are made to apk, use this first 
			echo "<td>" . $messageArray[2]. "</td>"; //use when apk have been updated
			echo "</tr>";
		}
		echo "</table>";
	}
	// if there are no records in the database, display an alert message
	else
	{
		echo "No results to display!";
	}
}
// show an error if there is an issue with the database query
else
{
	echo "Error: " . $mysqli->error;
}

// close database connection
$mysqli->close();

?>

</body>
</html>