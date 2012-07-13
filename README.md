# Bitster

Our implementation is based on `Actor`s running in several threads.  Most 
`Actor`s run in their own thread; `Broker`s, however, are `tick()`ed manually
by their parent thread, the `Manager`.  `Actor`s which have their own threads
include:

1. `Deputy`: handles communication with the tracker.
2. `Manager`: handles the collection of `Broker` objects which communicate with 
   peers.  Also manages which pieces are being downloaded, etc.
3. `Funnel`: Receives pieces and 'funnels' them into some buffer.

The peer protocol code resides in the class `Protocol`.  This code utilizes the
java nio library for non blocking IO operations.  It selects on the socket and
performs io when necessary. It buffers incoming messages, then parses them out
into `Message` objects when received and places them into a queue for processing
by the `Broker` object which owns this `Protocol` object.

