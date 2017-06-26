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

// if($_SERVER['SERVER_ADDR'] == "::1") {
// $address = "localhost";
// } else {
// $address = $_SERVER['SERVER_ADDR'];
// }
// $address = "http://" . $address . "/usbong";
// $address = "http://shrimptalusan.hostei.com/usbong";
// get the records from the database
if ($result = $mysqli->query("SELECT * FROM RT3"))
{
	// display records if there are records to display
	if ($result->num_rows > 0)
	{
		// display records in a table
		echo "<table border='1' cellpadding='10'>";

		// set table headers
		echo "<tr>
				<th>DATE_TIME IN SECONDS</th>
				<th>ADDED SOUNDLEVEL (10 samples/second)</th>
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