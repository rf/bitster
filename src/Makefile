.PHONY: test libbitster bitstercli

TESTFILES=$(subst .java,,$(wildcard test/Test*.java))
TORRENT=../project2.torrent
OUT=out.mp3
SWITCHES=

all: run

libbitster:
	javac libbitster/*.java

bitstercli:
	javac bitstercli/*.java

test: $(TESTFILES)

run: libbitster bitstercli
	java -Xmx1024m bitstercli/RUBTClient $(SWITCHES) $(TORRENT) $(OUT) -no-cli

$(TESTFILES): 
	javac $@.java
	java -ea $@

clean:
	rm -f libbitster/*.class
	rm -f bitstercli/*.class
	rm -f $(OUT)
	rm -f metadata.bitster.dat
	rm -f bitster.log
