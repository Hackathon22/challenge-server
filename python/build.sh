#!/bin/bash

cd ..
./gradlew shadowJar
cp build/libs/challenge.jar python/

