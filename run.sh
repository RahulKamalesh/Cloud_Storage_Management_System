#!/bin/bash
mkdir -p out
javac -cp "lib/*" -d out src/*.java && java -cp "out:lib/*" LoginMain
