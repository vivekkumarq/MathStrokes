@echo off
REM ---------------------------------------------------------------------------------------
REM  Double-click this to keep asking Oracle for an Ampere A1 instance until one appears.
REM
REM  Free A1 capacity in India South (Hyderabad) is scarce and there is only one availability
REM  domain there, so there is no second pool to fall back on. The wait can be minutes or
REM  days, and Oracle publishes nothing about it. Retrying harder does not help - it trips a
REM  429 throttle and hides whether a machine was ever available.
REM
REM  So: leave this window open, overnight if need be. It is safe to close and re-run; the
REM  network is created once and reused, and it stops as soon as an instance is running.
REM
REM  Run this here rather than inside an assistant session: a session ends, and on a machine
REM  with little free memory its background tasks get reclaimed. This window survives both.
REM ---------------------------------------------------------------------------------------

set "REPO=E:\Vivek2026Project\MathStrokesTestPlatform"
set "BASH=C:\Program Files\Git\bin\bash.exe"

REM The OCI CLI installs into the per-user Python scripts directory, which is not on PATH.
set "PATH=%APPDATA%\Python\Python310\Scripts;%PATH%"

REM The tenancy root doubles as the compartment - this is a single-application tenancy.
set "COMPARTMENT_OCID=ocid1.tenancy.oc1..aaaaaaaaawl6v7v5qvh2tz2ahxu4ctoen3ec7mhonseetu7v3clmeo2zufsq"
set "SSH_PUB=%USERPROFILE%/.ssh/iota_oracle.pub"

REM The private key carries an OCI_API_KEY label in some setups; we do not use one, and this
REM stops the CLI printing a warning on every single call.
set "SUPPRESS_LABEL_WARNING=True"

if not exist "%BASH%" (
    echo Could not find Git Bash at "%BASH%".
    echo Install Git for Windows, or edit the BASH line in this file.
    pause
    exit /b 1
)

cd /d "%REPO%" || exit /b 1

echo.
REM  The shape is deliberately not named here. It lives in 00-provision.sh, and a copy in
REM  this banner went stale the first time it changed - saying "4 OCPU / 24 GB" while the
REM  script asked for something else entirely. The script prints what it is actually
REM  requesting a few lines down; that line is the one to trust.
echo  Asking Oracle for an Ampere A1 instance, Ubuntu 24.04 aarch64.
echo  Leave this window open. Ctrl-C stops it; re-running is safe.
echo.

"%BASH%" scripts/oracle/00-provision.sh

echo.
echo  The loop has exited. If an instance was created, the SSH command is printed above.
pause
