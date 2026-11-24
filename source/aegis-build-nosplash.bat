@echo off
setlocal enabledelayedexpansion

REM ===== Aegis Neo: сборка БЕЗ СПЛЕШСКРИНА =====
REM Сплеш выключен намертво (ни Swing, ни aegis-splash.exe),
REM апдейтер (aegis_updater) не требуется.
REM Джарка: AegisNeo-<version>-nosplash.jar

set "ROOT_DIR=%~dp0"
set "DONE_DIR=%ROOT_DIR%..\DonePacks"

echo ===== Aegis Neo Build (nosplash) =====
echo.

echo [1/3] Main: clean compileJava (-Pnosplash)...
cd /d "%ROOT_DIR%"
call gradlew.bat clean compileJava -Pnosplash
if %errorlevel% neq 0 (
    echo [FAIL] Main compileJava failed!
    pause
    exit /b 1
)
echo [OK]
echo.

echo [2/3] Main: remapJar (-Pnosplash)...
call gradlew.bat remapJar -Pnosplash
if %errorlevel% neq 0 (
    echo [FAIL] Main remapJar failed!
    pause
    exit /b 1
)
echo [OK]
echo.

echo [3/3] Copy nosplash jar...
if not exist "%DONE_DIR%" mkdir "%DONE_DIR%"
for %%f in ("%ROOT_DIR%build\libs\AegisNeo-*-nosplash.jar") do (
    copy /Y "%%f" "%DONE_DIR%" >nul
    echo      %%~nxf
)
echo [OK]
echo.

echo ===== Done (nosplash) =====
dir "%DONE_DIR%"
echo.
exit /b 0
