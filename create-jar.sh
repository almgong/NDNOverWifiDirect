#!/bin/bash

###
#  MOVE THIS FILE to app/build/intermediates/debug/classes and then execute.
###

#create a jar called ndn-over-wifid.jar
#BASE_DIR=app/src/main/java/ag/ndn/ndnoverwifidirect
BASE_DIR=ag/ndn/ndnoverwifidirect
jar -cfv ndn-over-wifid.jar $BASE_DIR/callback/* $BASE_DIR/model/* $BASE_DIR/runnable/* $BASE_DIR/service/* \
$BASE_DIR/task/* $BASE_DIR/utils/*