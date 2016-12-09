#!/bin/bash

#create a jar called ndn-over-wifid.jar
BASE_DIR=app/src/main/java/ag/ndn/ndnoverwifidirect
jar -cfv ndn-over-wifid.jar $BASE_DIR/callback/* $BASE_DIR/model/* $BASE_DIR/runnable/* $BASE_DIR/service/* \
$BASE_DIR/task/* $BASE_DIR/utils/*