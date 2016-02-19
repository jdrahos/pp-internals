var reqJson;
var testReqJson = [];
var reqNativeJson;
var respDemoEdJson;
var respDemoEdNativeJson;
var reqAssets;
var props;


$( document ).ready(function() {
    init()
});


function init() {
    $.getJSON( "r/req1.json", function( data ) {
        reqJson = data;
    });
    $.getJSON( "r/req-n1.json", function( data ) {
        reqNativeJson = data;
    });
    $.getJSON( "r/resp-demo-ed1.json", function( data ) {
        respDemoEdJson = data;
    });
    $.getJSON( "r/resp-demo-ed-n1.json", function( data ) {
        respDemoEdNativeJson = data;
    });
    $.getJSON("do/props.json", function(data) {
        props = data;
    });

    ['adn1.txt', 'adn2.txt', 'adn3.txt', 'gg1.txt', 'gg2.txt', 'gg3.txt', 'gg4.txt', 'gg5.txt'].forEach(function(name){
        $.getJSON( "r/" + name, function( data ) {
            testReqJson.push(data);
        });
    });

    /*
     console.log("1Props read. ");
     $.ajax({
     dataType: "json",
     url: "do/props.json"
     })
     .done(function (data, textStatus, jqXHR) {
     console.log("done");
     console.log(data);
     console.log(textStatus);
     console.log(jqXHR);
     })
     .fail(function (jqXHR, textStatus, errorThrown) {
     console.log("fail");
     console.log(jqXHR);
     console.log(textStatus);
     console.log(errorThrown);
     })
     ;
     */

}


function setupDemoEd() {
    var url = props.demoEd;
    var respNativeString = JSON.stringify(respDemoEdNativeJson);
    respDemoEdJson.r.c = respNativeString;
    sendJson(url, respDemoEdJson);
}

function resetDemoEd() {
    var url = props.demoEd + "/reset";
    sendUrl({
        url: createEndpointUrl(url)
    });
}


function sendOpenRtbDemo() {
    sendOpenRtb(props.endpoints.testSsp);
}

function sendOpenRtbTest() {
    $("#responseTest").empty();
    testReqJson.forEach(function(req) {
        sendJson(props.endpoints.testQa2Ssp, req)
            .done(processOpenRtbResponse)
            .done(function() {
                $("#responseTest").append("#");
            })
            .fail(function() {
                $("#responseTest").append("-");
            });
    });
}

function sendOpenRtbBidtellect() {
    sendOpenRtb(props.endpoints.bidtellectEndpoint);
}

function sendOpenRtb(endpoint, test) {
    reqAssets = {};
    for (var i = 0; i < reqNativeJson.assets.length; i++) {
        var asset = reqNativeJson.assets[i];
        reqAssets[asset.id] = asset;
    }

    var reqNativeString = JSON.stringify(reqNativeJson);
    reqJson.imp[0].native.request = reqNativeString;
    reqJson.test = typeof test == 'undefined' ? 1 : test;

    sendJson(endpoint, reqJson)
        .done(processOpenRtbResponse);
}

function sendJson(url, data) {
    return sendUrl({
        url: createEndpointUrl(url),
        type: 'POST',
        data: JSON.stringify(data),
        contentType: 'application/json; charset=utf-8',
        dataType: 'json'
    })
}

function sendUrl(data) {
    $("#responseError").empty();
    $("#responseCode").empty();
    $("#responseText").empty();
    $("#responseObjects").empty();


    /*
     headers: {
     'Accept': 'application/json',
     'Content-Type': 'application/json'
     },
     */

    return $.ajax(data)
        .error(onResponseError)
        .fail(onResponseError)
        .done(function (resp, textStatus, jqXHR) {
            $("#responseCode").text(textStatus + '/' + jqXHR.status);
            if (jqXHR.status == 200) {
                $("#responseText").text(JSON.stringify(resp, null, '    '));
            }
        });
}

function createEndpointUrl(url) {
    var proxyUrl =
        window.location.protocol
        + '//'
        + window.location.host
        + '/my-web/do/proxy?url='
        +  encodeURIComponent(url);

    return proxyUrl;
}

function onResponseError(jqXHR, textStatus, errorThrown) {
    $("#responseCode").text(textStatus + '/' + jqXHR.status);
    $("#responseError").text("Error: " + errorThrown);
}


function processOpenRtbResponse(resp, textStatus, jqXHR) {
    //$("#responseCode").text(textStatus + '/' + jqXHR.status);
    if (jqXHR.status == 200) {
        //$("#responseText").text(JSON.stringify(resp, null, '    '));
        $('#responseObjects').empty();

        if (!resp.seatbid[0].bid[0].adm) return;
        var nativeResp = JSON.parse(resp.seatbid[0].bid[0].adm);

        $("#responseText").text($("#responseText").text()
            + '\n------------------------------------ Native\n'
            + JSON.stringify(nativeResp, null, '    '));

        nativeResp = nativeResp.native;
        $('#responseObjects').append('Main link:<a href="' + nativeResp.link.url + '">' + nativeResp.link.url + '</a><br>');


        var respAssets = nativeResp.assets;
        console.log(respAssets);

        for (var i = 0; i < respAssets.length; i++) {
            var outAsset = respAssets[i];

            var id = outAsset.id;
            var inAsset = reqAssets[id];


            $('#responseObjects').append('Asset [' + id + '] ');
            if (outAsset.title) {
                $('#responseObjects').append('Title: <span>' + outAsset.title.text + '</span><br>');
            }
            if (outAsset.img) {
                $('#responseObjects').append('Image: [' + (inAsset && inAsset.img.type) + '] <img src="' + outAsset.img.url + '" ><br>');
            }
            if (outAsset.data) {
                $('#responseObjects').append('Data: [' + (inAsset && inAsset.data.type) + '] <span>' + outAsset.data.value + '</span><br>');
            }
        }
    }
}
