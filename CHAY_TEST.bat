@echo off
chcp 65001 > nul
echo ===================================================
echo     DANG CHAY TEST SUITE CSD201 - NHOM 1 (FA26)
echo ===================================================
echo.
cd /d "%~dp0HashTableProject"
java -cp "build/classes;build/test/classes" hashtable.test.TestRunner
echo.
pause
