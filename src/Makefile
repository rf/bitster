.PHONY: test libbitster bitstercli

TESTFILES=$(subst .java,,$(wildcard test/Test*.java))
TORRENT="../project2.torrent"
OUT="out.mp3"

all: run

libbitster:
	javac libbitster/*.java

bitstercli:
	javac bitstercli/*.java

test: $(TESTFILES)

run: libbitster bitstercli
	java -Xmx1024m bitstercli/RUBTClient $(TORRENT) $(OUT)

$(TESTFILES): 
	javac $@.java
	java -ea $@

clean:
	rm -f libbitster/*.class
	rm -f bitstercli/*.class
	rm -f out.jpg
