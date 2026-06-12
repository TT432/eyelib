@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

set "PROJECT_ROOT=E:\_ideaProjects\qylEyelib"

if not exist "%PROJECT_ROOT%" (
    echo ERROR: project not found at %PROJECT_ROOT%
    exit /b 1
)

set "OUTPUT=%~dp0eyelib-all-main.md"
if not "%~1"=="" set "OUTPUT=%~1"

cd /d "%PROJECT_ROOT%"
python "%~dp0repomix_main_only.py" "%OUTPUT%"
endlocal
