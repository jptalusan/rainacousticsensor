<?php
// connect to the database
include('connect.php');
header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename=RT3_data.csv');

$output = fopen('php://output', 'w');

fputcsv($output, array(
'id',
'timestamp',
'soundLevel'));

if ($result = $mysqli->query("SELECT * FROM RT3"))
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