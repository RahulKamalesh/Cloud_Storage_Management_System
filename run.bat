@echo off
if not exist out mkdir out
javac -cp "lib/*" -d out src\*.java && java -cp "out;lib/*" LoginMain
