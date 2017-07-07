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
isset($_POST['transmitter']) ? $transmitter = $_POST['transmitter'] : $transmitter = $_GET['transmitter'];

echo "Data for " . $transmitter . "\r\n";
$queryString = "SELECT * FROM " . $transmitter;
if ($result = $mysqli->query($queryString))
{
	// display records if there are records to display
	if ($result->num_rows > 0)
	{
		// display records in a table
		echo "<table border='1' cellpadding='10'>";

		// set table headers
		echo "<tr>
				<th>DATE_TIME IN SECONDS</th>
				<th>ADDED SOUNDLEVEL dB (20 samples/second)</th>
			  </tr>";

		while ($row = $result->fetch_object())
		{
			// set up a row for each record
			echo "<tr>";
			echo "<td>" . $row->date_time . "</td>";
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