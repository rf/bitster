.PHONY: test libbitster bitstercli

TESTFILES=$(subst .java,,$(wildcard test/Test*.java))

all: run

libbitster:
	javac libbitster/*.java

bitstercli:
	javac bitstercli/*.java

test: $(TESTFILES)

run: libbitster bitstercli
	java bitstercli/RUBTClient ../project2.torrent out.jpg

$(TESTFILES): 
	javac $@.java
	java -ea $@

clean:
	rm -f libbitster/*.class
	rm -f bitstercli/*.class
	rm -f out.jpg
