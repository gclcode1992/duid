@echo off
chcp 65001 >nul
setlocal EnableExtensions
cd /d "%~dp0.."

rem Export duid-server image to a tar file for offline transfer: docker save
set "IMAGE_NAME=duid-server"
set "IMAGE_TAG=1.0.0"
if defined DUID_IMAGE_TAG set "IMAGE_TAG=%DUID_IMAGE_TAG%"
if exist .env (
  for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"DUID_IMAGE_TAG=" ".env" 2^>nul`) do (
    if not "%%B"=="" set "IMAGE_TAG=%%B"
  )
)

set "IMAGE=%IMAGE_NAME%:%IMAGE_TAG%"
set "OUT_DIR=dist"
set "OUT_FILE=%OUT_DIR%\%IMAGE_NAME%-%IMAGE_TAG%.tar"

if not "%~1"=="" set "OUT_FILE=%~1"

echo ========================================
echo  Export Docker image: %IMAGE%
echo  Output: %OUT_FILE%
echo ========================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] docker not found in PATH
  exit /b 1
)

docker image inspect %IMAGE% >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Image not found: %IMAGE%
  echo         Run docker\docker-build.bat first.
  exit /b 1
)

for %%I in ("%OUT_FILE%") do set "OUT_PARENT=%%~dpI"
if not exist "%OUT_PARENT%" mkdir "%OUT_PARENT%"
if errorlevel 1 (
  echo [ERROR] Cannot create output directory: %OUT_PARENT%
  exit /b 1
)

echo [1/2] Saving image to tar...
docker save -o "%OUT_FILE%" %IMAGE%
if errorlevel 1 (
  echo [ERROR] docker save failed
  exit /b 1
)

echo [2/2] Done.
for %%I in ("%OUT_FILE%") do echo       File: %%~fI  Size: %%~zI bytes
echo.
echo Load on another machine:
echo   docker load -i "%OUT_FILE%"
echo.
exit /b 0
