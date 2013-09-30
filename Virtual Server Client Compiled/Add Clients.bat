ECHO off
SET var=0
:START_CLIENT
START "Client %var%" "Start Client.bat" %var%
ECHO Client %var% started...
set /a var+=1
ECHO Starting Client %var%...
PAUSE
GOTO :START_CLIENT