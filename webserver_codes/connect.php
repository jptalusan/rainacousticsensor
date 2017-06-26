<?php
// server info https://x10hosting.com/wiki/MySQL_Information
$server = 'localhost';
$user = 'excthack_user';
$pass = 'br00dling!';
$db = 'excthack_rainsensor';
// connect to the database
$mysqli = new mysqli($server, $user, $pass, $db);
// show errors (remove this line if on a live site)
mysqli_report(MYSQLI_REPORT_ERROR);
?>