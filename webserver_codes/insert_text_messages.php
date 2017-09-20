<?php
date_default_timezone_set('Asia/Manila');
// header('Content-Type: application/json');
include('connect.php');
// $params = $this->getService()->getRequest()->getActionParams();
// echo json_encode($params);

$transmitterId = $mysqli->real_escape_string($_POST['transmitterId']);
$phoneNumber = $mysqli->real_escape_string($_POST['phoneNumber']);
$message = $mysqli->real_escape_string($_POST['message']);
// echo $transmitterId . " " . $phoneNumber . " " . $message;
// echo $data;
// echo "\n";
$myObj = new \stdClass();
$myObj->name = "Transmitter " . $transmitterId;
$myObj->phoneNumber = $phoneNumber;
$myObj->message = $message;

//TODO: Process sndLevels
$tablename = "ReceivedTexts";

$resultQuery = $mysqli->query("INSERT INTO $tablename (
		transmitterId, 
		phoneNumber,
		message
		) VALUES (
		'$transmitterId', 
		'$phoneNumber',
		'$message'
		)");

if($resultQuery) {
	$myObj->result = "Success";
} else {
	$myObj->result = "Failure";
}

$myJSON = json_encode($myObj);

echo $myJSON;

?>