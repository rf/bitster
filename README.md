# Bitster

## Group Members

 * Martin Miralles-Cordal
 * Russell Frank
 * Theodore Surgent

## Usage

```
$ java bitstercli/RUBTClient [torrent file] [output file]
```

For example:

```
$ java bitstercli/RUBTClient ../project1.torrent out.jpg
```

## Overview

Bitster uses several `Actors` running in threads and non-blocking io for
concurrency.  We have the following `Actors` in threads:

1. `Deputy`: communicates with the tracker.
2. `Manager`: determines what to download and instructs its pool of `Brokers`
   to download pieces from peers.
3. `Funnel`: receives pieces and 'funnels' them into a buffer if they pass a
   hash check.
4. `Timeout`: provides a simple interface for scheduling events in the future.

The `Actors` communicate with `Memo`s, simple objects with a string type and
`Object` payload.  Not all `Actor`s are run in their own thread, however;
`Broker`s handle communication with peers, and are `tick()`ed by their
`Manager`.  Each time they are `tick()`ed, they also inform their `Protocol`
instance to `communicate()`.  The `Protocol` class will then poll the socket to
see if there is any data to be read or if data can be written.  If so, it will
perform the necessary io.  If a message is available from a peer, it will place
it onto an `inbox` queue for processing by the `Broker`.

## Classes

### Actors

#### Deputy

*Runs in a thread.* Communicates with the tracker.  It does not use non blocking io; since it's
running in its own thread, and HTTP is a request / response protocol, we
decided to use traditional blocking io here.  It accepts a `list` memo,
indicating that we'd like a peer list, and a `done` memo, indicating that it
should inform the tracker that we're done.

#### Manager

*Runs in a thread.* Handles the pool of `Broker`s, `tick()`ing them regularly, figures out what to
download and instructs the `Broker`s to download.

#### Funnel

*Runs in a thread.* Receives pieces from the `Manager`, verifies them, and
places them into some buffer.  Can also write that buffer to disk.  Runs in its
own thread to offload the hashing elsewhere; also, we plan to replace the
ByteBuffer with an `mmap()`ed file, so the io it is doing *may* be blocking.

#### Timeout

*Runs in a thread.* Allows `Memo`s to be scheduled to be "returned to sender"
after a period of time.

#### Broker

*Does not run in a thread.* Handles communication with the peer. Does not deal
with the low-level protocol mumbo-jumbo; contains a high-level representation
of the peer's state, handles protocol messages, forwards completed data
off to the manager.

### Other Classes

#### Actor

Base class for actors. Implements `Runnable` and can be `start()`ed in its own
thread.  Has a `tick()` method which will call the `idle()` function once and
the `receive()` function with any `Memo`s on the queue.

#### Handshake

Verifies and creates peer protocol handshakes.  Used in `Protocol`.

#### Memo

Represents an internal message that is passed between `Actor`s (as opposed to 
a BT peer protocol `Message` which is passed between peers via TCP).  Has a string
type and `Object` payload.

#### Message

Represents an external message that is passed between peers in the BT peer
protocol. Has factory methods for creating `Message`s and can deserialize a
`Message` from a `ByteBuffer`.

#### Piece

Represents a piece of a file. Used by the `Manager` class to send pieces that
have been received from the `Broker` to the `Funnel` to be merged into a completed
file. Has methods to add blocks of data to a piece and perform SHA-1 validation.

#### Protocol

Handles all of the low-level protocol detail. Polls the socket and performs io
when necessary. Parses out the length of messages and hands off a `ByteBuffer`
to `Message` (or `Handshake`) when it holds a complete message.

#### Util

Contains the `Timeout` Actor, also contains a few other utility methods.
