#!/bin/bash

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd $HERE/../..

if ! curl "http://localhost:17200/api/status" &> /dev/null ; then
  echo "Start services before running this script."
  exit 1
fi

path=client/src/tutorials
files=offline/tutorials/*.json
if [[ $# -gt 0 ]] ; then
    files=$1
else
    rm -rf $path
fi

mkdir -p $path
for i in $files ; do
    echo "Generating $i"
    ./offline/build/install/offline/bin/offline generate-tutorial $i $path/$(basename $i)
done
