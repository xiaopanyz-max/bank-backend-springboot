@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem ============================================================
rem Start local Windows MySQL for the bank K8S lab.
rem
rem This script is tailored for your current local MySQL layout:
rem   MySQL service : MySQL80
rem   MySQL binary  : D:\MySQL\Server\bin\mysqld.exe
rem   MySQL config  : D:\MySQL\my.ini
rem   MySQL port    : 3306
rem
rem It also exposes Windows MySQL to the VMware/K8S VM:
rem   192.168.30.1:3306 -> 127.0.0.1:3306
rem ============================================================

set "MYSQL_SERVICE=MySQL80"
set "MYSQLD_EXE=D:\MySQL\Server\bin\mysqld.exe"
set "MYSQL_INI=D:\MySQL\my.ini"
set "VMWARE_HOST_IP=192.168.30.1"
set "MYSQL_PORT=3306"

echo.
echo [1/6] Checking administrator permission...
net session >nul 2>&1
if not "%errorlevel%"=="0" (
    echo Need Administrator permission. Re-opening this script as Administrator...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)
echo OK: Running as Administrator.

echo.
echo [2/6] Checking local MySQL files...
if not exist "%MYSQLD_EXE%" (
    echo ERROR: mysqld.exe not found:
    echo   %MYSQLD_EXE%
    pause
    exit /b 1
)
if not exist "%MYSQL_INI%" (
    echo ERROR: my.ini not found:
    echo   %MYSQL_INI%
    pause
    exit /b 1
)
echo OK: Found mysqld.exe and my.ini.

echo.
echo [3/6] Current MySQL service config:
sc.exe qc "%MYSQL_SERVICE%"

echo.
echo [4/6] Starting MySQL service...
net start "%MYSQL_SERVICE%"
if "%errorlevel%"=="0" goto MYSQL_STARTED

echo.
echo WARNING: Failed to start %MYSQL_SERVICE%.
echo Trying to repair the Windows service registration...
echo.
echo Reason:
echo   Your service may not be registered with --defaults-file=%MYSQL_INI%
echo   Without my.ini, MySQL may not know basedir/datadir/port.
echo.

sc.exe query "%MYSQL_SERVICE%" >nul 2>&1
if "%errorlevel%"=="0" (
    echo Removing old service: %MYSQL_SERVICE%
    "%MYSQLD_EXE%" --remove "%MYSQL_SERVICE%"
)

echo Installing service with config file:
echo   "%MYSQLD_EXE%" --install "%MYSQL_SERVICE%" --defaults-file="%MYSQL_INI%"
"%MYSQLD_EXE%" --install "%MYSQL_SERVICE%" --defaults-file="%MYSQL_INI%"
if not "%errorlevel%"=="0" (
    echo ERROR: Failed to install MySQL service.
    pause
    exit /b 1
)

echo Starting repaired service...
net start "%MYSQL_SERVICE%"
if not "%errorlevel%"=="0" (
    echo.
    echo ERROR: MySQL still failed to start.
    echo Last 80 lines from MySQL error log:
    powershell -NoProfile -Command "if (Test-Path 'D:\MySQL\data\pan.err') { Get-Content 'D:\MySQL\data\pan.err' -Tail 80 } else { Write-Host 'D:\MySQL\data\pan.err not found.' }"
    pause
    exit /b 1
)

:MYSQL_STARTED
echo OK: MySQL service is running.

echo.
echo [5/6] Checking MySQL listener on port %MYSQL_PORT%...
netstat -ano | findstr ":3306"
if not "%errorlevel%"=="0" (
    echo ERROR: MySQL service started, but port %MYSQL_PORT% is not listening.
    pause
    exit /b 1
)

echo.
echo [6/6] Refreshing VMware/K8S access portproxy...
echo Mapping:
echo   %VMWARE_HOST_IP%:%MYSQL_PORT% -^> 127.0.0.1:%MYSQL_PORT%
netsh interface portproxy delete v4tov4 listenaddress=%VMWARE_HOST_IP% listenport=%MYSQL_PORT% >nul 2>&1
netsh interface portproxy add v4tov4 listenaddress=%VMWARE_HOST_IP% listenport=%MYSQL_PORT% connectaddress=127.0.0.1 connectport=%MYSQL_PORT%
if not "%errorlevel%"=="0" (
    echo ERROR: Failed to add portproxy.
    pause
    exit /b 1
)

echo.
echo Current portproxy:
netsh interface portproxy show all

echo.
echo Done. Now test inside Ubuntu:
echo   nc -vz %VMWARE_HOST_IP% %MYSQL_PORT%
echo.
echo If success, restart K8S apps:
echo   kubectl rollout restart deployment/customer-service deployment/account-service -n bank
echo.
pause

