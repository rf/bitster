var net = require('net');
var sock = net.createServer(function (c) {
  console.log('got connection');
  c.on('data', function (buf) {
    console.log(buf.toString());
  });
  var len = new Buffer([19]);
  var proto = new Buffer('BitTorrent Protocol');

  console.log(len);
  console.log(proto);

  c.write(len);
  c.write(proto);
  setTimeout(function () {
    setTimeout(function () {
      c.write(new Buffer(8));
      c.write(new Buffer('asdfasdfasdfasdfasdf'));
      setTimeout(function () {
        c.write(new Buffer(20));

        setTimeout(function () {
          var keepalive = new Buffer([0, 0, 0, 0]);
          console.log(keepalive);
          c.write(new Buffer([0, 0, 0, 0]));

          setTimeout(function () {
            c.write(new Buffer([0, 0, 0, 1, 0]));

            setTimeout(function () {
              c.write(new Buffer([0, 0, 0, 1, 2, 0, 0, 0, 1, 2, 0, 0, 0, 1, 2]));
            }, 1000);

          }, 1000);

        }, 1000);

      }, 1000);
    }, 1000);
  }, 1000);
}).listen(4000);
