var util = require('util');
var xml = require("node-xml-lite");

exports.parseBody = function(body) {
    if (typeof(body) == "string" && body !== "") {
        try {
            parsed = JSON.parse(body);
        }
        catch(err) {
            console.log("Parsing error : " + err.message + ", body= \n " + body);
            parsed = body;
        }
    } else {
        parsed = body;
    }
    return parsed;
};

function xmlToResult(x) {
    var parsed = {};
    try {
        for (var i = 0; i < x.childs.length; i++) {
            var child = x.childs[i];
            //console.log("child = " + JSON.stringify(child));
            parsed[child.name] = child.childs[0];
        }
    } catch(err) {
        console.log("Parsing error : " + err.message + ", xml= \n " + JSON.stringify(x));
    }
    return parsed;
}

exports.parseXmlBody = function(body) {
    if (typeof(body) == "string" && body !== "") {
        try {
            var x = xml.parseString(body);
            parsed = xmlToResult(x);
        } catch(err) {
            console.log("Parsing error : " + err.message + ", body= \n " + body);
        }
    } else {
        parsed = body;
    }
    return parsed;
};

exports.log = function(message) {
    var now = new Date();
    var h = now.getHours(); if (h < 10) h = '0' + h;
    var m = now.getMinutes(); if (m < 10) m = '0' + m;
    var s = now.getSeconds(); if (s < 10) s = '0' + s;
    var ms = now.getMilliseconds(); if (ms < 10) ms = '00' + ms; else if (ms < 100) ms = '0' + ms;
    console.log(h + ':' + m + ':' + s + '.' + ms + ' - ' + message);
};
