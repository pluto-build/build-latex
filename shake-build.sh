#!/bin/sh
BIN=bin-shake
mkdir -p $BIN
ghc --make src-shake/Build.hs -rtsopts -with-rtsopts=-I0 -outputdir=$BIN -o $BIN/build && $BIN/build "$@"
