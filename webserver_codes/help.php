<html>
<title>
How to:
</title>
<body>
1. Based from the code these are the requirements and the steps needed to be done: <br>
Requirements<br>
1 Transmitter mobile phone (with SMS capability)<br>
1 Receiver mobile phone (with SMS and access to the internet)<br>
1 Monitor mobile phone (your own phone will work, this just needs to text a code to the <br>transmitter/receiver)<br>
In transmitter phone (one installed with transmitter.apk) launch the app and...<br>
Enter the monitor phone number<br>
Enter the receiver/server phone number<br>
In receiver phone (one installed with receiver.apk) launch the app and ...<br>
Enter monitor phone number<br>
Enter the first transmitter's phone number (you can skip the 2nd and 3rd transmitter info for now)<br>
Enter the website for uploading: http://admurainsensor.comxa.com/<br>
Using the monitor mobile phone, send an SMS to the transmitter phone to begin, here are the keywords:<br>
[Start, Stop]-[GSM, WIFI, Buffer, Backup, Test]<br>
example: Start-GSM: to start transmission. The labels on the transmitter activity should change to reflect this.<br>
Stop-GSM<br>
Start-WIFI: This doesn't send the data to receiver via SMS, will only store it in .csv file inside transmitter.<br>
[Truncate]-[Buffer,Backup]<br>
example: Truncate-Buffer: to truncate/delete the current DBs inside the mobile phones<br>
The transmission has a 2 minute window, so data may not appear immediately. For testing purpose, try sending Start-WIFI to transmitter first then wait maybe 10 minutes and check if there is a new folder created with a .csv file inside with the data.<br>
<br>
If everything should work, when you use Start-GSM, the transmitter should send data via GSM to receiver phone and then receiver should upload the data to the site i provided. Since you are only using one transmitter, the data should appear here: http://admurainsensor.comxa.com/rainRT1.php every time data is sent to the receiver.<br>

<br>
You can download this program to view the sqlite files on your computer:
<a href="http://sqlitebrowser.org">sqlitebrowser</a>
</body>