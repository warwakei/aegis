@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
set "DONE_DIR=%ROOT_DIR%DonePacks"

echo ===== Aegis Neo Build =====
echo.

REM 1 -- Main clean compileJava
echo [1/5] Main: clean compileJava...
cd /d "%ROOT_DIR%"
call gradlew clean compileJava
if %errorlevel% neq 0 (
    echo [FAIL] Main compileJava failed!
    pause
    exit /b 1
)
echo [OK]
echo.

REM 2 -- Main build
echo [2/5] Main: build...
call gradlew build
if %errorlevel% neq 0 (
    echo [FAIL] Main build failed!
    pause
    exit /b 1
)
echo [OK]
echo.

REM 3 -- Create DonePacks
if not exist "%DONE_DIR%" mkdir "%DONE_DIR%"

REM 4 -- Copy main jar (skip -sources)
echo [3/5] Copy main jar...
for %%f in ("%ROOT_DIR%build\libs\AegisNeo-*.jar") do (
    echo %%f | findstr /C:"-sources" >nul
    if errorlevel 1 (
        copy /Y "%%f" "%DONE_DIR%" >nul
        echo      %%~nxf
        goto :main_done
    )
)
:main_done
echo [OK]
echo.

REM 5 -- Launcher clean compileJava
echo [4/5] Launcher: clean compileJava...
cd /d "%ROOT_DIR%launcher"
call gradlew clean compileJava
if %errorlevel% neq 0 (
    echo [FAIL] Launcher compileJava failed!
    pause
    exit /b 1
)
echo [OK]
echo.

REM 6 -- Launcher build
echo [5/5] Launcher: build...
call gradlew build
if %errorlevel% neq 0 (
    echo [FAIL] Launcher build failed!
    pause
    exit /b 1
)
echo [OK]
echo.

REM 7 -- Copy launcher jar
echo Copy launcher jar...
for %%f in ("%ROOT_DIR%launcher\build\libs\AegisUpdater*.jar") do (
    echo %%f | findstr /C:"-sources" >nul
    if errorlevel 1 (
        copy /Y "%%f" "%DONE_DIR%" >nul
        echo      %%~nxf
    )
)
echo [OK]
echo.

echo ===== Done =====
dir "%DONE_DIR%"
echo.
