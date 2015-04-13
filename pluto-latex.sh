#/bin/sh

ARGS="pluto-latex build.pluto.buildlatex.Latex.factory build.pluto.buildlatex.Latex\$Input $@"

mvn compile exec:java -Dexec.args="$ARGS"
