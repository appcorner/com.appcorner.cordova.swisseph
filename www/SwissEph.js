/*global cordova, module*/

module.exports = {
    greet: function (name, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "SwissEph", "greet", [name]);
    },
    computeChart: function (ephePath, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "SwissEph", "computeChart", [ephePath]);
    }
};