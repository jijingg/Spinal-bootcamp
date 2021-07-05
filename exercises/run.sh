#! /bin/sh

set -o errexit
set -o nounset
set -o xtrace

MILL_VERSION=0.9.7

if [ ! -f mill ]; then
  curl -L https://github.com/com-lihaoyi/mill/releases/download/$MILL_VERSION/$MILL_VERSION > mill && chmod +x mill
fi

./mill version
cd exercises

# Check format and lint
../mill Examples.reformat
../mill Examples.fix --check

# Run test and simulation
../mill Examples.runMain exercises.BoothSim
../mill Examples.test.testOnly exercises.ApbArbiterTest
../mill Examples.test.testOnly exercises.ApbBridgeTest
../mill Examples.test.testOnly exercises.FifoCCTest
../mill Examples.test.testOnly exercises.HandShakePipeTest
# ../mill Examples.test # Not work yet
