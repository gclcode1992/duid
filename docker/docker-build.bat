@echo off
chcp 65001 >nul
setlocal EnableExtensions
cd /d "%~dp0.."

rem Build duid-server image via docker/docker-compose.yml (mvn package then compose build)
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"
set "COMPOSE_FILE=docker\docker-compose.yml"
set "IMAGE_NAME=duid-server"
set "IMAGE_TAG=1.0.0"
if defined DUID_IMAGE_TAG set "IMAGE_TAG=%DUID_IMAGE_TAG%"
if exist .env (
  for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"DUID_IMAGE_TAG=" ".env" 2^>nul`) do (
    if not "%%B"=="" set "IMAGE_TAG=%%B"
  )
)

set "IMAGE=%IMAGE_NAME%:%IMAGE_TAG%"
set "PULL_BASE=0"
if /i "%~1"=="--pull" set "PULL_BASE=1"
if /i "%~1"=="pull" set "PULL_BASE=1"

echo ========================================
echo  Build Docker image: %IMAGE%
echo  Compose: %COMPOSE_FILE%
echo ========================================
echo.

where mvn >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Maven/mvn not found in PATH
  exit /b 1
)

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] docker not found in PATH
  exit /b 1
)

echo [1/2] Maven package: mvn clean package -DskipTests -pl duid-server -am
call mvn clean package -DskipTests -pl duid-server -am
if errorlevel 1 (
  echo [ERROR] Maven package failed
  exit /b 1
)

dir /b "duid-server\target\duid-server-*.jar" >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Jar not found: duid-server\target\duid-server-*.jar
  exit /b 1
)

echo [2/2] Docker build: docker compose -f %COMPOSE_FILE% build
if "%PULL_BASE%"=="1" (
  echo       Also pulling latest base image --pull
  if exist .env (
    docker compose -f %COMPOSE_FILE% --env-file .env build --pull --build-arg APP_VERSION=%IMAGE_TAG%
  ) else (
    docker compose -f %COMPOSE_FILE% build --pull --build-arg APP_VERSION=%IMAGE_TAG%
  )
) else (
  if exist .env (
    docker compose -f %COMPOSE_FILE% --env-file .env build --build-arg APP_VERSION=%IMAGE_TAG%
  ) else (
    docker compose -f %COMPOSE_FILE% build --build-arg APP_VERSION=%IMAGE_TAG%
  )
)
if errorlevel 1 (
  echo [ERROR] Docker image build failed
  exit /b 1
)

echo.
echo Build finished: %IMAGE%
docker image ls %IMAGE_NAME%
echo.
echo Start single replica:
echo   docker\docker-run.bat
exit /b 0
