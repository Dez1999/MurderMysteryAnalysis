const express           = require('express');
const path              = require('path');
const fs                = require('fs');
const mongoose          = require('mongoose');

//mongoose.connect('mongodb://localhost/yardAndGarage', { useNewUrlParser: true, useCreateIndex: true, });

const app = express();

// Set Static Folder
app.use(express.static(path.join(__dirname, '../Client')));

function getMainPage(req, res) {
  fs.readFile('../Client/index.html', 'utf8', function(err, data) {
    if(err) {
      console.log("/Client/index.html is missing")
      return
    }
    res.type('.html')
    res.send(data)
  })
}

app.get('/', function (req, res) {
  getMainPage(req, res);
});
app.get('', function (req, res) {
  getMainPage(req, res);
});

app.get('/data', function (req, res) {
  res.send("kill me")
});
app.listen(3000, function () {
  console.log('Example app listening on port 3000!')
});
