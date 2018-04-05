all: clean_build prepare_build build_app

build_app:
	-javac -Xdiags:verbose -g -d build/ *.java

prepare_build:
	-mkdir build

clean_build:
	-rm -rf build
