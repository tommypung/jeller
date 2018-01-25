angular.module('jeller', ['ngRoute'])
.config(['$routeProvider', function($routeProvider) {
	$routeProvider
	.when("/",                      			 		{templateUrl: 'startPage.html'})
	.when("/allDevices",                    			{templateUrl: 'allDevices.html'})
	.when("/addNewDevice",          			 		{templateUrl: 'editDevice.html', controller: 'AddNewDeviceController'})
	.when("/editDevice/:deviceId",          			{templateUrl: 'editDevice.html', controller: 'AddNewDeviceController'})
	.when("/addNewDeviceScheduleEntry/:deviceId",		{templateUrl: 'addNewDeviceScheduleEntry.html', controller: 'AddNewDeviceScheduleEntryController'})
	.otherwise({
		redirectTo: "/"
	})
}])
.controller('RootController', function($scope, $http, $location, $window, $timeout) {
	$scope.notifications = [];
	function addNotification(msg, ttl) {
		console.log("Adding notification: " + msg);
		$scope.notifications.push({ msg : msg, ttl: ttl });
	}

	navigator.serviceWorker.addEventListener('message', function handler (event) {
		console.log("Incoming data", event.data);
		if (event.data.action == "downloading-files") {
			$scope.$apply(function() {
				addNotification("Uppdaterar " + event.data.payload.length + " filer");
			});
		}

		if (event.data.action == "new-files")
		{
			console.log("switch working - payload.length = " + event.data.payload.length);
			$scope.$apply(function() {
				addNotification("En ny version av jeller finns tillgänglig, ladda om sidan.", 5000);
			}); 
		}
	});

	$scope.devices = [];
	$scope.icons = ["icons/airballoon.svg", "icons/balloon.svg", "icons/bed.svg","icons/candle.svg","icons/car.svg","icons/christmastree.svg","icons/computer.svg","icons/desklamp.svg","icons/door.svg","icons/lantern.svg","icons/leaf.svg","icons/lightbulb.svg","icons/moon.svg","icons/nightstand.svg","icons/owl.svg","icons/sofa.svg","icons/stove.svg","icons/tub.svg","icons/tv.svg"];
	$scope.vibrate = function() {
		navigator.vibrate = navigator.vibrate || navigator.webkitVibrate || navigator.mozVibrate || navigator.msVibrate;
		if (navigator.vibrate)
			navigator.vibrate(200);
	}
	var selectedDevice = undefined;
	$scope.getSelectedDevice = function() {
		return selectedDevice;
	}
	$scope.setSelectedDevice = function(device) {
		selectedDevice = device;
	}

	function randomIcon() {
		return $scope.icons[Math.floor(Math.random() * $scope.icons.length)];
	}

	$scope.$on('$locationChangeStart', function(event, next, current){
		if (selectedDevice && !next.includes("/editDevice")) {
			console.log("next = " + next);
			event.preventDefault();
		}

		$scope.moreAlternatives();
	});

	$scope.moreAlternatives = function(device) {
		$scope.vibrate();
		if ($scope.getSelectedDevice()) {
			$scope.getSelectedDevice().moreAlternatives = false;
			$scope.setSelectedDevice();
		}

		if (!device)
			return;

		console.log("more alternatives");
		device.moreAlternatives = !device.moreAlternatives;
		if (device.moreAlternatives)
			$scope.setSelectedDevice(device);
		else
			$scope.setSelectedDevice();
	}

	$scope.bookmarkToggle = function(device) {
		$scope.vibrate();
		device.bookmarked = !device.bookmarked;
	}

	$scope.touchEnd = function(device) {
		console.log("touch end");
	}

	$scope.gotoAllUnits = function() {
		$scope.vibrate();
		$location.path("/allDevices");
	}

	$scope.back = function() {
		$scope.vibrate();
		$window.history.back();
	}

	$scope.gotoStartPage = function() {
		$location.path("/");
	}

	$scope.gotoEditDevice= function(device) {
		$location.path("/editDevice/" + device.id);
	}

	$scope.gotoAddNewDevice= function(device) {
		$location.path("/addNewDevice");
	}

	$scope.reverseLearnClose = function() {
		$scope.reverseLearnModal = false;
	}

	function reverseLearnUpdate() {
		if (!$scope.reverseLearnModal)
			return; // the reverseLearnModal is not up - break free

		$http.get("/api/getLastUsedDevices/10").then(function(json) {
			$scope.reverseLearnDevices = json.data.devices;
			$timeout(reverseLearnUpdate, 2000);
		}, function() {
			$timeout(reverseLearnUpdate, 2000);
		});
	}

	$scope.reverseLearn = function() {
		$scope.reverseLearnModal = true;
		reverseLearnUpdate();
	}

	$scope.toggle = function(device) {
		device.loading = true;
		$scope.vibrate();

		$http.get("/api/toggleDevice/" + device.id).then(function(json) {
			device.loading = false;
			for(var i=0;i<$scope.devices.length;i++) {
				var d = $scope.devices[i];
				if (d.id == json.data.device.id) {
					angular.copy(json.data.device, d);
					if (!d.icon)
						d.icon = randomIcon();
				}
			}
		}, function() {
			device.loading = false;
		});
	}

	function fetchDevices() {
		$http.get("/api/getDevices").then(function(json) {
			$scope.devices = json.data.devices;
			for(var i=0;i<$scope.devices.length;i++) {
				var d = $scope.devices[i];
				if (!d.icon)
					d.icon = randomIcon();
			}
		}, function() {
		});
	}
	fetchDevices();
})
.controller('AddNewDeviceScheduleEntryController', function($scope, $http, $location, $routeParams) {
	$scope.entry = {
			command : 1, // POWER
			device : {
				id : $routeParams.deviceId
			}
	};
	$scope.sending = false;
	$scope.state = "add";

	$scope.save = function() {
		$scope.sending = true;
		$http.post("/api/addNewDeviceScheduleEntry", $scope.entry).then(function(json) {
			console.log("Got response: ");
			console.log(json.data);
			if (json.data.status == 'ok') {
				$scope.device = json.data;
				$scope.state = "done";
			}
			$scope.sending = false;
		}, function() {
			$scope.sending = false;
		})
	}
})
.controller('AddNewDeviceController', function($scope, $http, $location, $routeParams) {
	$scope.device = {
			id : $routeParams.deviceId,
			protocol : 'arctech',
			model : 'selflearning-switch',
			house : Math.floor((Math.random() * 10000) + 1),
			unit : Math.floor((Math.random() * 255) + 1),
			hwretries : 0,
			swretries : 0
	};
	if (!$scope.device.id) {
		$scope.information = $scope.technicalData = $scope.icon = true;
	}
	$scope.sending = false;
	$scope.state = "add";

	$scope.load = function() {
		$scope.device.name = 'Loading device';
		$http.post("/api/getDevice/"  + $scope.device.id).then(function(json) {
			angular.copy(json.data.device, $scope.device);
		}, function() {
		});
	}

	if ($scope.device.id)
		$scope.load();

	$scope.reverseLearnReturn = function(device) {
		$scope.device.protocol = device.protocol;
		$scope.device.model = device.model;
		$scope.device.house = device.house;
		$scope.device.unit = device.unit;
	}

	$scope.save = function() {
		$scope.sending = true;
		var url;
		if ($scope.device.id)
			url = "/api/updateDevice/" + $scope.device.id;
		else
			url = "/api/addNewDevice";

		$http.post(url, $scope.device).then(function(json) {
			console.log("Got response: ");
			console.log(json.data);
			$scope.device = json.data;
			if (json.data.status == 'ok') {
				$scope.state = "learn";				
			}
			$scope.sending = false;
		}, function() {
			$scope.sending = false;
		})
	}

	$scope.learn = function() {
		$scope.sending = true;
		$http.post("/api/learnDevice", $scope.device).then(function(json) {
			console.log("Got response: ");
			console.log(json.data);

			$scope.device = json.data;
			$scope.sending = false;
			$scope.state = "learn";
		}, function() {
			$scope.sending = false;
		})
	}
})
.directive('onLongPress', function($timeout) {
	return {
		restrict: 'A',
		link: function($scope, $elm, $attrs) {
			$elm.unselectable = "on";
			$elm.addClass("unselectable");
			$elm.bind('touchstart', function(evt) {
				// Locally scoped variable that will keep track of the long press
				$scope.longPress = true;

				// We'll set a timeout for 600 ms for a long press
				$timeout(function() {
					if ($scope.longPress) {
						// If the touchend event hasn't fired,
						// apply the function given in on the element's on-long-press attribute
						$scope.$apply(function() {
							$scope.$eval($attrs.onLongPress)
						});
					}
				}, 600);
			});

			$elm.bind('touchend', function(evt) {
				// Prevent the onLongPress event from firing
				$scope.longPress = false;
				// If there is an on-touch-end function attached to this element, apply it
				if ($attrs.onTouchEnd) {
					$scope.$apply(function() {
						$scope.$eval($attrs.onTouchEnd)
					});
				}
			});
		}
	};
});
