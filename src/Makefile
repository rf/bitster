.PHONY: test

TESTFILES=$(subst .java,,$(wildcard test/Test*.java))

libbitster:
	javac libbitster/*.java

test: $(TESTFILES)

$(TESTFILES): 
	javac $@.java
	java -ea $@
