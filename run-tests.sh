#!/bin/bash
#
# E.g. run the following in your jamm directory to kick off testing (for more info on EXECUTION_ID check the README toolchains section)
# sudo EXECUTION_ID=test ./run-tests.sh
# 
# Prerequisites
[ "x$EXECUTION_ID" != "x" ] || { echo "Variable EXECUTION_ID must be defined";  exit 1; }

mvn "${EXECUTION_ID}"
mvn "${EXECUTION_ID}" -DjvmArgs="-Xmx16G -XX:ObjectAlignmentInBytes=16"

case "$EXECUTION_ID" in 
  surefire:test@test-default|surefire:test@test-jdk11-64|surefire:test@test-jdk17-64)
    mvn "${EXECUTION_ID}" -DjvmArgs="-XX:+UseCompressedClassPointers -XX:+UseCompressedOops"
    mvn "${EXECUTION_ID}" -DjvmArgs="-XX:-UseCompressedClassPointers -XX:-UseCompressedOops"
    mvn "${EXECUTION_ID}" -DjvmArgs="-XX:+UseCompressedClassPointers -XX:-UseCompressedOops"
    ;;
  *)
    # do nothing
    ;;
esac


case "$EXECUTION_ID" in
  surefire:test@test-jdk17-32|surefire:test@test-jdk17-64)
    # mvn "${EXECUTION_ID}" -DjvmArgs="-XX:-UseEmptySlotsInSupers" This test currently fails, though it is commented out. Please check the README for more info
    mvn "${EXECUTION_ID}" -DjvmArgs="-XX:+UseEmptySlotsInSupers"
    ;;
  *)
    # do nothing
    ;;
esac

mvn "${EXECUTION_ID}" -DjvmArgs="-XX:+EnableContended -XX:+RestrictContended"
mvn "${EXECUTION_ID}" -DjvmArgs="-XX:+EnableContended -XX:-RestrictContended"
case "$EXECUTION_ID" in
  surefire:test@test-default|surefire:test@test-jdk8-32|surefire:test@test-jdk11-64|surefire:test@test-jdk11-32)
    mvn "${EXECUTION_ID}" -DjvmArgs="-XX:-EnableContended -XX:+RestrictContended"
    mvn "${EXECUTION_ID}" -DjvmArgs="-XX:+EnableContended -XX:+RestrictContended -XX:ContendedPaddingWidth=64"
    ;;
  *)
    # do nothing
    ;;
esac
