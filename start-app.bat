@echo off
setlocal
rem Starts MySQL (when it isn't running yet) and the Stock Replenishment System at http://localhost:8473.
rem Double-click this file or run it from a terminal. Close the window or press Ctrl+C to stop the app.

set "APP_PORT=8473"
set "MYSQL_SERVICE=MySQL80"
set "URL=http://localhost:%APP_PORT%"
title Stock Replenishment System - %URL%
cd /d "%~dp0"

if not exist ".env" (
    echo Missing the .env file with DB_USERNAME and DB_PASSWORD in %CD%
    pause
    exit /b 1
)

rem 1. MySQL. Starting a Windows service needs administrator rights, so ask for them only when needed.
sc query "%MYSQL_SERVICE%" >nul 2>&1
if errorlevel 1 (
    echo The MySQL service %MYSQL_SERVICE% was not found. Is MySQL installed?
    pause
    exit /b 1
)
sc query "%MYSQL_SERVICE%" | findstr /c:"RUNNING" >nul
if errorlevel 1 (
    echo Starting MySQL...
    net start "%MYSQL_SERVICE%" >nul 2>&1
    if errorlevel 1 (
        echo Starting MySQL needs administrator rights. Accept the Windows prompt.
        powershell -NoProfile -Command "Start-Process net -ArgumentList 'start','%MYSQL_SERVICE%' -Verb RunAs -Wait -WindowStyle Hidden"
    )
)
sc query "%MYSQL_SERVICE%" | findstr /c:"RUNNING" >nul
if errorlevel 1 (
    echo MySQL could not be started.
    pause
    exit /b 1
)
echo MySQL is running.

rem 2. An app instance that is still running keeps the port busy.
netstat -ano | findstr /r /c:":%APP_PORT% .*LISTENING" >nul || goto start_app
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":%APP_PORT% .*LISTENING"') do set "OLD_PID=%%p"
echo Port %APP_PORT% is already in use by this process, probably an app instance that is still running:
tasklist /fi "PID eq %OLD_PID%" /nh
choice /c YN /m "Stop it and start the app again"
if errorlevel 2 (
    start "" "%URL%"
    exit /b 0
)
taskkill /f /pid %OLD_PID% >nul

:start_app
rem 3. Open the browser as soon as the app answers (gives up after 3 minutes).
start "" /b powershell -NoProfile -Command "for ($i = 0; $i -lt 180; $i++) { try { (New-Object Net.Sockets.TcpClient('localhost', %APP_PORT%)).Close(); Start-Process '%URL%'; break } catch { Start-Sleep -Seconds 1 } }"

rem 4. The app with MySQL (the default profile). This window shows its log.
rem    .\ because Windows can be set to not run programs from the current folder by name alone.
echo Starting the app at %URL% ...
call .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=%APP_PORT%"
echo The app has stopped.
pause
