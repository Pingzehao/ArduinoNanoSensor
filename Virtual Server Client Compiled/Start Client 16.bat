ECHO off
SET port=8080
SET /a port+=%1
ECHO on
java -jar VirtualSensorClient16.jar 127.0.0.1 %port% client%port% 0
PAUSE