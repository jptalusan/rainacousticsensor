<?php
$startDate = isset($_REQUEST["date3"]) ? $_REQUEST["date3"] : "";
$endDate = isset($_REQUEST["date4"]) ? $_REQUEST["date4"] : "";
$transmitter = isset($_POST['transmitter']) ? $transmitter = $_POST['transmitter'] : $transmitter = $_GET['transmitter'];

$startDate = $startDate . " 00:00:00";
$endDate = $endDate . " 23:59:59";

echo "For: " . $transmitter . "<br>";

echo $startDate . ' to ' . $endDate;

echo "<br>Converting to millis:";

$timezone = 'Asia/Manila';

$startMillis = strtotime($startDate . ' '. $timezone) * 1000;
$endMillis = strtotime($endDate . ' '. $timezone) * 1000;
echo "<br>Start: " . $startMillis . "<br>" . "End: " . $endMillis;
?>