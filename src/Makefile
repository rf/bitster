.PHONY: test

TESTFILES=$(subst .java,,$(wildcard test/Test*.java))

all: run

libbitster:
	javac libbitster/*.java

bitstercli:
	javac bitstercli/*.java

test: $(TESTFILES)

run: libbitster bitstercli
	java bitstercli/RUBTClient ../project1.torrent out.jpg

$(TESTFILES): 
	javac $@.java
	java -ea $@
