/*
 * Copyright (C) 2015 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var expandAlerts = false;

var vehicleId = getParameterByName("vehicleId", "");
var routeId = getParameterByName("routeId", "1");
var type = getType(vehicleId);
var count = 0;
var shapes ={};
var routeShapeIds;
var infoWindow;
var refreshPopupRequest = null;

var blockLocation = {lat:null, lng:null};
var apiKey = getParameterByName("key", "TEST");
var smurl = "/onebusaway-api-webapp/siri/vehicle-monitoring?key=" + apiKey + "&type=json";
{}
var shapeUrlPre = "/onebusaway-api-webapp/api/where/shape/";
var shapeUrlPost = ".json?key=" + apiKey + "&version=2";
var stopUrlPre = "/onebusaway-api-webapp/api/where/stop/";
var stopUrlPost = ".json?key=" + apiKey + "&version=2";

if(type == "vehicle"){
    var preUrl = "/onebusaway-api-webapp/api/where/vehicle-position-for-vehicle/";
    var postUrl = ".json?key=" + apiKey + "&version=2";
    var agencyId = vehicleId.split("_")[0];
    var apiId = vehicleId.split("_")[1];
    var agencyAndid = vehicleId;
} else {
    var preUrl = "/onebusaway-api-webapp/api/where/vehicle-positions-for-route/";
    var postUrl = ".json?key=" + apiKey + "&version=2";
    var agencyId = routeId.split("_")[0];
    var apiId = routeId.split("_")[1];
    var agencyAndid = routeId;
}

// drop the map marker arbitrarily
var errorLatLng = new google.maps.LatLng(38.905216, -77.06301);
var lengendInit = false;
var markerGtfsr ={};
var markerBlockLocation = {};
//var shape;
var markerStop;
var map;
var refreshSeconds = getParameterByName("refresh", "5");

var colorIndex = ["red","blue","yellow","green","orange","pink","purple","lightblue"]


function getParameterByName(name, defaultValue) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    if(results === null) {
        return defaultValue;
    } else {
        return decodeURIComponent(results[1].replace(/\+/g, " "));
    }
}

function getType(vehicleId){
    if (vehicleId == ""){
        return "route";
    }
    return "vehicle";
}

function initialize() {
    console.log("looking for vehicleId=" + vehicleId);
    map = new google.maps.Map(document.getElementById("map"), {
        zoom: 16,
    });
    update();
    updateDistance();
    updatePan();

    /*setTimeout(function() {
        update();
    }, (refreshSeconds * 1000));*/
    //popup();
};


function update() {
    var gtfsUrl;
    routeShapeIds = {};

    if (typeof(OBA) == "undefined" || typeof(OBA.Config) == "undefined" || typeof(OBA.Config.apiBaseUrl) == "undefined") {
        gtfsrUrl = "http://localhost:8080" + preUrl + agencyAndid + postUrl;
    } else {
        gtfsrUrl = OBA.Config.apiBaseUrl +  preUrl + agencyAndid + postUrl;
    }

    console.log("making raw call = " + gtfsrUrl);

    jQuery.getJSON(gtfsrUrl, {}, function(data) {
        var code = data.code;

        if (code == null || code == 404 || typeof data.data == "undefined") {
            console.log("no raw position");
            document.getElementById("age").innerHTML = "unavailable";
            document.getElementById("timestamp").innerHTML = "unavailable";
            return null;
        }

        var vehicleRawPositionList = getRawVehicleData(data);

        updateRawVehiclePostitionMarkers(vehicleRawPositionList);

        //updateShapes(Object.keys(routeShapeIds));

        var shapeCount = 0;
        for(var vehicleId in markerGtfsr){
           /* if(shapeCount == 0){
                jQuery.each(shapes, function(index, shape){
                    shape.setMap(null);
                });
            }*/
            var marker = markerGtfsr[vehicleId];
            updateCalcVehiclePositionMarkers(vehicleId, marker);
            shapeCount++;
        }

       /* jQuery.each(markerGtfsr, function(k,v){
            updateCalcVehiclePositionMarkers(vehicleId, data)
        });*/

    });

    //updateStop(activity.MonitoredVehicleJourney.MonitoredCall.StopPointRef);

    if (lengendInit == false) {
        createLegend(map);
    }

    setTimeout(function() {
        update();
        if(typeof(infoWindow) != "undefined" && infoWindow !== null && typeof infoWindow.refreshFn === 'function') {
            infoWindow.refreshFn();
        }
    }, (refreshSeconds * 1000));

}

function getRawVehicleData(data){
    var vehicleDataList = {};
    if(typeof(data.data.list) != "undefined"){
        console.log(data.data.list)
        var list = data.data.list;
        jQuery.each(list, function(index, value){
            vehicleDataList[value.vehicleId] = value;
        });
    }
    else{
        vehicleDataList[data.data.vehicleId] = data.data;
    }
    return vehicleDataList;
}

function updateRawVehiclePostitionMarkers(vehicleRawPositionList){

    jQuery.each(vehicleRawPositionList, function(index, data){
        console.log("have raw data= " + data.timeOfLocationUpdate);

        lat = data.currentLocation.lat;
        lng = data.currentLocation.lon;

        console.log("lat=" + lat + ", lon=" + lng);
        document.getElementById("age").innerHTML = round((new Date() / 1000) - data.timeOfLocationUpdate);

        var dateStr = new Date(data.timeOfLocationUpdate * 1000).toString();
        document.getElementById("timestamp").innerHTML = dateStr.substring(0, dateStr.length-14).split(" ")[4];

        console.log("record is " + document.getElementById("age").innerHTML + "s old");

        var gtfsrLocation = {lat: parseFloat(lat), lng: parseFloat(lng)};
        var vehicleId = data.vehicleId;
        var color = colorIndex[count % colorIndex.length];
        count++;
        if (markerGtfsr[vehicleId] == null) {
            var marker = new google.maps.Marker({
                vehicleId: vehicleId,
                icon: {
                    url: 'http://maps.google.com/mapfiles/ms/icons/' + color + '.png',
                    labelOrigin: new google.maps.Point(15, 48),
                    scaledSize: new google.maps.Size(40, 40)
                },
                position: gtfsrLocation,
                opacity: 1,
                map: map,
                title: 'Raw Position: ' + vehicleId,
                labelOrigin: new google.maps.Point(0, 0),
                color: color,
                label: {
                    text: vehicleId.split("_")[1],
                    fontWeight: 'bold',
                    fontSize: '12px',
                    fontFamily: 'Verdana,Courier,Monospace',
                    color: 'black'
                }

            });
            markerGtfsr[vehicleId] = marker;
        } else {
            markerGtfsr[vehicleId].setPosition(gtfsrLocation);
        }

        /*var marker = new google.maps.Marker({
            vehicleId: vehicleId,
            labelAnchor: new google.maps.Point(15, 65),
            icon: 'http://maps.google.com/mapfiles/ms/icons/' + color + '.png',
            position: gtfsrLocation,
            map: map,
            title: 'Raw Position: ' + vehicleId,
            color: color
        });
        markerGtfsr[vehicleId] = marker;*/
    });

    deleteStaleVehicles(vehicleRawPositionList, markerGtfsr);
}

function round(num) {
    return Math.round(num * 100) / 100;
}

function updateCalcVehiclePositionMarkers(vehicleId, marker){
    var smParams = {
        OperatorRef: agencyId,
        VehicleRef: vehicleId,
        MaximumNumberOfCallsOnwards: "1",
        VehicleMonitoringDetailLevel: "calls",
        Color: marker.color
    }
    console.log("smurl=" + smurl);

    var contentBlock = "unavailable",
        contentTrip = "unavailable",
        contentNextStopId = "unavailable",
        contentNextStop = "unavailable",
        contentScheduled = "unavailable",
        contentPredicted = "unavailable",
        contentDeviation = "unavailable",
        contentLatitude = "unavailable",
        contentLongitude = "unavailable";

    jQuery.getJSON(smurl, smParams, function (data, status) {
        var activity = data.Siri.ServiceDelivery.VehicleMonitoringDelivery[0].VehicleActivity[0];
        if (typeof(activity) == "undefined" || activity === null || activity.MonitoredVehicleJourney === null) {
            console.log("no activity for vehicle " + vehicleId);

            /* document.getElementById("block").innerHTML = "unavailable";
             document.getElementById("trip").innerHTML = "unavailable";
             document.getElementById("nextStopId").innerHTML = "unavailable";
             document.getElementById("nextStop").innerHTML = "unavailable";
             document.getElementById("scheduled").innerHTML = "unavailable";
             document.getElementById("predicted").innerHTML = "unavailable";
             document.getElementById("deviation").innerHTML = "unavailable";*/
            return null;
        } else {
            if (!(activity.MonitoredVehicleJourney.JourneyPatternRef in shapes)) {
                updateShapes(activity.MonitoredVehicleJourney.JourneyPatternRef);
            }
            console.log("have activity for vehicle " + smParams.VehicleRef);

            var latitude = activity.MonitoredVehicleJourney.VehicleLocation.Latitude;
            var longitude = activity.MonitoredVehicleJourney.VehicleLocation.Longitude;
            var extensions = activity.MonitoredVehicleJourney.MonitoredCall.Extensions;

            if (typeof(extensions) !== "undefined") {
                contentDeviation = extensions.Deviation;
                //document.getElementById("deviation").innerHTML = extensions.Deviation;
                console.log("deviation=" + deviation.value);
            }
            contentBlock = activity.MonitoredVehicleJourney.BlockRef;
            contentTrip = activity.MonitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef;
            contentNextStopId = activity.MonitoredVehicleJourney.MonitoredCall.StopPointRef;
            contentNextStop = activity.MonitoredVehicleJourney.MonitoredCall.StopPointName;
            if (typeof(activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime) !== "undefined") {
                console.log("aimed=" + activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime);
                contentScheduled
                    = activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime.split("T")[1].split("-")[0].split(".")[0];
            }
            if (typeof(activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime) !== "undefined") {
                contentPredicted
                    = activity.MonitoredVehicleJourney.MonitoredCall.ExpectedArrivalTime.split("T")[1].split("-")[0].split(".")[0];
            }

            /*document.getElementById("block").innerHTML = activity.MonitoredVehicleJourney.BlockRef;
            document.getElementById("trip").innerHTML = activity.MonitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef;
            document.getElementById("nextStopId").innerHTML = activity.MonitoredVehicleJourney.MonitoredCall.StopPointRef;
            document.getElementById("nextStop").innerHTML = activity.MonitoredVehicleJourney.MonitoredCall.StopPointName;
            if (typeof(activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime) !== "undefined") {
                console.log("aimed=" + activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime);
                document.getElementById("scheduled").innerHTML
                    = activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime.split("T")[1].split("-")[0].split(".")[0];
            } else {
                document.getElementById("scheduled").innerHTML = "unavailable";
            }
            if (typeof(activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime) !== "undefined") {
                document.getElementById("predicted").innerHTML
                    = activity.MonitoredVehicleJourney.MonitoredCall.ExpectedArrivalTime.split("T")[1].split("-")[0].split(".")[0];
            } else {
                document.getElementById("predicted").innerHTML = "unavailable";
            }*/

            blockLocation.lat = latitude;
            blockLocation.lng = longitude;
            console.log("lat/lng set");

            var contentString =
                '<div id="popup">Deviation: ' + contentDeviation + ' min</div>' +
                '<div id="popup">Block: ' + contentBlock + '</div>' +
                '<div><span>Trip:</span> ' + contentTrip + '</div>' +
                '<div><span>Next Stop Id:</span> ' + contentNextStopId + '</div>' +
                '<div><span>Next Stop:</span> ' + contentNextStop + '</div>' +
                '<div><span>Scheduled:</span> ' + contentScheduled + '</div>' +
                '<div><span>Predicted:</span> ' + contentPredicted + '</div>';

            var color = smParams.Color;

            if (smParams.Color == 'lightblue'){
                color = 'ltblue';
            }

            if (markerBlockLocation[vehicleId] == null) {
                markerBlockLocation[vehicleId] = new google.maps.Marker({
                    position: blockLocation,
                    icon: {
                        url: 'http://maps.google.com/mapfiles/ms/icons/' + color + '-dot.png',
                        //url: '/img/debug/green.png',
                        labelOrigin: new google.maps.Point(15, 48),
                        //scaledSize: new google.maps.Size(40, 40)
                    },
                    map: map,
                    title: 'Calculated Position: ' + vehicleId,

                    label: {
                        text: vehicleId.split("_")[1],
                            fontWeight: 'bold',
                            fontSize: '12px',
                            fontFamily: 'Verdana,Courier,Monospace',
                            color: 'black'
                    }
                });

                markerBlockLocation[vehicleId].addListener('click', function() {
                    showPopupWithContentFromRequest(markerBlockLocation[vehicleId], smurl, smParams);
                });

            } else {
                markerBlockLocation[vehicleId].setPosition(blockLocation);
                /*markerBlockLocation[vehicleId].addListener('click', function() {
                    showPopupWithContent(markerBlockLocation[vehicleId], contentString);
                });*/
                //infoWindow.setContent(contentString);
            }


           /* var infowindow = new google.maps.InfoWindow({
                content: contentString
            });

            markerBlockLocation[vehicleId].addListener('click', function() {
                infowindow.open(map,  markerBlockLocation[vehicleId]);
            });
*/

            /*  var marker = new google.maps.Marker({
                  position: blockLocation,
                  icon: 'http://maps.google.com/mapfiles/ms/icons/' + smParams.Color + '-dot.png',
                  map: map,
                  title: 'Calculated Position: ' + vehicleId
              });
              markerBlockLocation.push(marker);*/



            //updateShape(activity.MonitoredVehicleJourney.JourneyPatternRef);

            deleteStaleVehicles(markerGtfsr, markerBlockLocation);
        }

        //updateShape(activity.MonitoredVehicleJourney.JourneyPatternRef)
    });


}

function showPopupWithContent(marker, content) {
    closeInfoWindow();

    infoWindow = new google.maps.InfoWindow({
        //pixelOffset: new google.maps.Size(0, (marker.getIcon().size.height / 2)),
        disableAutoPan: false
    });

    google.maps.event.addListener(infoWindow, "closeclick", closeInfoWindow);

    marker.addListener('click', function() {
        infoWindow.open(map, marker);
    });

    infoWindow.setContent(content);
}

function showPopupWithContentFromRequest(marker, url, params) {
    closeInfoWindow();

    infoWindow = new google.maps.InfoWindow({
        disableAutoPan: false,
    });

    google.maps.event.addListener(infoWindow, "closeclick", closeInfoWindow);

    var refreshFn = function(openBubble) {

        if(refreshPopupRequest !== null) {
            refreshPopupRequest.abort();
            openBubble = true;
        }
        refreshPopupRequest = jQuery.getJSON(url, params, function(json) {
            if(infoWindow === null) {
                return;
            }

            var contentBlock = "unavailable",
                contentTrip = "unavailable",
                contentNextStopId = "unavailable",
                contentNextStop = "unavailable",
                contentScheduled = "unavailable",
                contentPredicted = "unavailable",
                contentDeviation = "unavailable",
                contentLatitude = "unavailable",
                contentLongitude = "unavailable";

            var activity = json.Siri.ServiceDelivery.VehicleMonitoringDelivery[0].VehicleActivity[0];
            if (typeof(activity) == "undefined" || activity === null || activity.MonitoredVehicleJourney === null) {
                console.log("no activity for vehicle " + vehicleId);
                return;
            } else {

                var extensions = activity.MonitoredVehicleJourney.MonitoredCall.Extensions;

                if (typeof(extensions) !== "undefined") {
                    contentDeviation = extensions.Deviation;
                    //document.getElementById("deviation").innerHTML = extensions.Deviation;
                    console.log("deviation=" + deviation.value);
                }
                contentBlock = activity.MonitoredVehicleJourney.BlockRef;
                contentTrip = activity.MonitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef;
                contentNextStopId = activity.MonitoredVehicleJourney.MonitoredCall.StopPointRef;
                contentNextStop = activity.MonitoredVehicleJourney.MonitoredCall.StopPointName;
                if (typeof(activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime) !== "undefined") {
                    console.log("aimed=" + activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime);
                    contentScheduled
                        = activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime.split("T")[1].split("-")[0].split(".")[0];
                }
                if (typeof(activity.MonitoredVehicleJourney.MonitoredCall.AimedArrivalTime) !== "undefined") {
                    contentPredicted
                        = activity.MonitoredVehicleJourney.MonitoredCall.ExpectedArrivalTime.split("T")[1].split("-")[0].split(".")[0];
                }


                var contentString =
                    '<div id="popup">Deviation: ' + contentDeviation + ' min</div>' +
                    '<div id="popup">Block: ' + contentBlock + '</div>' +
                    '<div><span>Trip:</span> ' + contentTrip + '</div>' +
                    '<div><span>Next Stop Id:</span> ' + contentNextStopId + '</div>' +
                    '<div><span>Next Stop:</span> ' + contentNextStop + '</div>' +
                    '<div><span>Scheduled:</span> ' + contentScheduled + '</div>' +
                    '<div><span>Predicted:</span> ' + contentPredicted + '</div>';


            }

            infoWindow.setContent(contentString);

            if(openBubble === true) {
                infoWindow.open(map, marker);
            }
        });
    };
    refreshFn(true);
    infoWindow.refreshFn = refreshFn;
}

function closeInfoWindow() {
    if(typeof(infoWindow) != "undefined" && infoWindow !== null) {
        infoWindow.close();
    }
    infoWindow = null;
}

function updateShapes(shapeId) {
        var shapeUrl = shapeUrlPre + shapeId + shapeUrlPost;
        console.log("making shape call = " + shapeUrl);
        jQuery.getJSON(shapeUrl, {}, function(data) {
            var encodedShape = data.data.entry.points;

           /* if (shape != null) {
                shape.setMap(null);
            }*/
            var points = OBA.Util.decodePolyline(encodedShape);

            var latlngs = jQuery.map(points, function(x) {
                return new google.maps.LatLng(x[0], x[1]);
            });

            var color = "0000FF";
            var options = {
                path: latlngs,
                strokeColor: "#" + color,
                strokeOpacity: .5,
                strokeWeight: 4,
                clickable: false,
                map: map,
                zIndex: 2
            };

            shape = new google.maps.Polyline(options);
            shape.setMap(map);
            shapes[shapeId] = shape;

        });
}

function updateDistance() {
    map.setZoom(14);
    /*for(x=0; x < markerGtfsr.length; x++) {
        if (typeof(markerBlockLocation) !== "undefined"
            && typeof(markerGtfsr[x].getPosition().lat()) !== "undefined"
            && typeof(markerBlockLocation[x].getPosition().lat() !== "undefined")) {
            var delta = distance(markerGtfsr[x].getPosition().lat(),
                markerGtfsr[x].getPosition().lng(),
                markerBlockLocation[x].getPosition().lat(),
                markerBlockLocation[x].getPosition().lng());
            document.getElementById("distance").innerHTML = delta;
            if (delta > 1000) {
                map.setZoom(14);
            } else {
                map.setZoom(16);
            }

        } else {
            console.log("skipping distance");
            document.getElementById("distance").innerHTML = "unavailable";
        }
    }*/
}

function updatePan() {
    map.panTo({lat: errorLatLng.lat(), lng: errorLatLng.lng()});
    /*if (typeof(markerGtfsr) !== "undefined"
        && typeof(markerBlockLocation) !== "undefined"
        &&  typeof(markerGtfsr.getPosition().lat()) !== "undefined"
        && typeof(markerBlockLocation.getPosition().lat() !== "undefined")) {
        console.log("case 1: both");
        var mp = middlePoint(markerGtfsr.getPosition().lat(),
            markerGtfsr.getPosition().lng(),
            markerBlockLocation.getPosition().lat(),
            markerBlockLocation.getPosition().lng());
        console.log("middelpoint " + mp[0] + "," + mp[1] + " from "
            + markerGtfsr.getPosition().lat() + ","
            + markerGtfsr.getPosition().lng() + " and "
            + markerBlockLocation.getPosition().lat() + ","
            + markerBlockLocation.getPosition().lng());

        map.panTo({lat: parseFloat(mp[0]), lng: parseFloat(mp[1])});
    } else if (typeof(markerGtfsr) !== "undefined") {
        console.log("case 2: raw");
        map.panTo({lat: markerGtfsr.getPosition().lat(), lng: markerGtfsr.getPosition().lng()});
    } else if (typeof(markerBlockLocation) !== "undefined") {
        console.log("case 3: block");
        map.panTo({lat: markerBlockLocation.getPosition().lat(), lng: markerBlockLocation.getPosition().lng()});
    } else {
        console.log("case 4: error");
        map.panTo({lat: errorLatLng.lat(), lng: errorLatLng.lng()});
    }*/
}


/*function popup() {
    var contentString = '<div id="content">'+
        '<div id="siteNotice">'+
        '</div>'+
        '<h2 id="firstHeading" class="firstHeading">Uluru</h2>'+
        '</div>';

    var infowindow = new google.maps.InfoWindow({
        content: contentString
    });

    for(i=0;  i < markerGtfsr.lenth; i++) {
        infowindow.open(map,markerGtfsr[i]);
    }
}*/

function deleteStaleVehicles(latestVehiclesList, markersList){
    var markersKeys = Object.keys(markersList);

    for (var i = 0; i < markersKeys.length; i++) {
        if (!latestVehiclesList[markersKeys[i]]) {
            markersList[markersKeys[i]].setMap(null);
            delete markersList[markersKeys[i]];
        }
    }
}

function deleteStaleShapes(latestShapesList, shapesList){
    var shapeKeys = Object.keys(shapesList);

    for (var i = 0; i < shapeKeys.length; i++) {
        if (!latestVehiclesList[markersKeys[i]]) {
            markersList[markersKeys[i]].setMap(null);
            delete markersList[markersKeys[i]];
        }
    }
}

function createLegend(map) {
    var legend = document.getElementById('legend');
    if (typeof(legend) == "undefined" || legend == null || typeof(legend.style.display) == "undefined") {
        console.log("delaying legend init");
    } else {
        console.log("legend init successful");
        legend.style.display = "";
        map.controls[google.maps.ControlPosition.RIGHT_TOP].push(legend);
        lengendInit = true;
    }

}

createLegend(map);
google.maps.event.addDomListener(window, 'load', initialize);

//update();


 