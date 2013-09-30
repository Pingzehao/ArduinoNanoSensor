ECHO off
SET port=8080
SET /a port+=%1
ECHO on
java -jar VirtualSensorClient.jar 169.235.182.182 %port% client%port% 0
PAUSE