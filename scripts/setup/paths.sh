#!/bin/bash

if [[ -z "$SERENADE_SOURCE_ROOT" ]] ; then
  SERENADE_SOURCE_ROOT="$HOME/serenade"
fi

if [[ -z "$SERENADE_LIBRARY_ROOT" ]] ; then
  SERENADE_LIBRARY_ROOT="$HOME/libserenade"
fi

mkdir -p $SERENADE_SOURCE_ROOT
mkdir -p $SERENADE_LIBRARY_ROOT

# docker doesn't use sudo
if [[ "$EUID" == 0 ]] ; then
  sudo-non-docker () {
    "$@"
  }
  else
  sudo-non-docker () {
    sudo "$@"
  }
fi