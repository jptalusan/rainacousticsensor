<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>  
<title>View Records</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
</head>
<body>
<?php
// connect to the database
include('connect.php');

//Getting post data
isset($_POST['transmitter']) ? $transmitter = $_POST['transmitter'] : $transmitter = $_GET['transmitter'];
$startDate = isset($_REQUEST["date3"]) ? $_REQUEST["date3"] : "";
$endDate = isset($_REQUEST["date4"]) ? $_REQUEST["date4"] : "";

$startDate = $startDate . " 00:00:00";
$endDate = $endDate . " 23:59:59";

echo $startDate . ' to ' . $endDate;

echo "<br>Converting to millis:";

$timezone = 'Asia/Manila';

$startMillis = strtotime($startDate . ' '. $timezone) * 1000;
$endMillis = strtotime($endDate . ' '. $timezone) * 1000;
echo "<br>Start: " . $startMillis . "<br>" . "End: " . $endMillis;
//End of getting post data

echo "<br>Data for " . $transmitter . "\r\n";
//SELECT * FROM `RT3` WHERE `date_time` BETWEEN 1499494653 AND 1499494658
$queryString = "SELECT * FROM " . $transmitter . " WHERE (`date_time` * 1000) BETWEEN " . $startMillis . " AND " . $endMillis;
echo "<br>" . $queryString . "<br>";

if ($result = $mysqli->query($queryString))
{
	// display records if there are records to display
	if ($result->num_rows > 0)
	{
		// display records in a table
		echo "<table border='1' cellpadding='10'>";

		// set table headers
		echo "<tr>
				<th>DATE_TIME DD-MM-YYYY</th>
				<th>ADDED SOUNDLEVEL dB (20 samples/second)</th>
			  </tr>";

		while ($row = $result->fetch_object())
		{
			$currentTimeInMillis = $row->date_time;
			$offset = 43200;
			$seconds = $currentTimeInMillis + $offset;
			$timeStamp = date("d/m/Y H:i:s", $seconds);
			// set up a row for each record
			echo "<tr>";
			echo "<td>" . $timeStamp . "</td>";
			echo "<td>" . $row->soundLevel . "</td>";
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