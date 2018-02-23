#!/usr/bin/env sh

# This magic tells our jvm instance to use some experimental settings to control RAM usage in a docker container.
# Only works on jdk8u131+ or jdk9

set -e
if [ "$1" = 'java' ]; then
    shift
    java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap $@
else
    exec "$@"
fi
