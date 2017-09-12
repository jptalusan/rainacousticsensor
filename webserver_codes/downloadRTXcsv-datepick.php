<?php
// connect to the database
include('connect.php');

//Getting post data
isset($_POST['transmitter']) ? $transmitter = $_POST['transmitter'] : $transmitter = $_GET['transmitter'];
$startDate = isset($_REQUEST["date5"]) ? $_REQUEST["date5"] : "";
$endDate = isset($_REQUEST["date6"]) ? $_REQUEST["date6"] : "";

$startDate = $startDate . " 00:00:00";
$endDate = $endDate . " 23:59:59";

echo $startDate . ' to ' . $endDate;

echo "<br>Converting to millis:";

$timezone = 'Asia/Manila';

$startMillis = strtotime($startDate . ' '. $timezone) * 1000;
$endMillis = strtotime($endDate . ' '. $timezone) * 1000;
echo "<br>Start: " . $startMillis . "<br>" . "End: " . $endMillis;
//End of getting post data

header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename=' . $transmitter . '_data.csv');

$output = fopen('php://output', 'w');

fputcsv($output, array(
'id',
'timestamp',
'soundLevel'));

//SELECT * FROM `RT3` WHERE `date_time` BETWEEN 1499494653 AND 1499494658
$queryString = "SELECT * FROM " . $transmitter . " WHERE (`date_time` * 1000) BETWEEN " . $startMillis . " AND " . $endMillis;

if ($result = $mysqli->query($queryString))
{
	if ($result->num_rows > 0)
	{
		while ($row = $result->fetch_assoc())
		{
			fputcsv($output, $row);
		}
	}
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