var staticFiles = {name : 'hashes', value : $STATIC_FILES};

var IDB_VERSION = 14;
var db = undefined;

function closeDB() {
	console.log("Closing indexeddb");
	try {
		if (db)
			db.close();
		db = undefined;
	} catch(e) {
		console.log("Error closing indexddb", e);
	}
}
function getIndexDB(name)
{
	var promise = new Promise(function(resolve, reject) {
		console.log("Open indexdb");
		if (db) {
			resolve(db);
			return;
		}

		var indexedDBOpenRequest = indexedDB.open(name, IDB_VERSION);
		console.log("Open request has been sent");

		indexedDBOpenRequest.onblocked = function(error) {
			console.error("IndexedDB BLOCKED: " + error);
			reject(error);
		}
		indexedDBOpenRequest.onerror = function(error) {
			console.error('IndexedDB error:', error);
			reject(error);
		};
		indexedDBOpenRequest.onupgradeneeded = function() {
			console.log("Creating object store: KEYVALUE.name");
			try {
				this.result.deleteObjectStore('KEYVALUE');
			} catch(error) { }
			var req = this.result.createObjectStore('KEYVALUE', {keyPath: 'name'});
		};
		indexedDBOpenRequest.onsuccess = function() {
			console.log("got db");
			db = this.result;
			resolve(db);
		};
	});
	return promise;
}

//Helper method to get the object store that we care about.
function getObjectStore(storeName, mode) {
	return idbDatabase.transaction(storeName, mode).objectStore(storeName);
}

function getValueFromObjectStore(objectStore, objectName)
{
	var promise = new Promise(function(resolve, reject) {
		console.log("Get Value from object store");
		var req = objectStore.get(objectName);
		req.onsuccess = function() { console.log("got onsuccess");console.log(req);resolve(req.result); }
		req.onerror = function(error) {console.log("got onerror");console.log(error); reject(error); }
	});

	return promise;
}

var myClients = [];

function broadcast(data) {
	console.log("Broadcasting", data);

	self.clients.matchAll({includeUncontrolled: true, type: 'window'}).then(function(clients) {
		clients.forEach(function(client) {
			client.postMessage(data);
		});
	});
}

self.addEventListener("message", function(e) {
	console.log("Adding client: ", e.source);
	console.log("Clients is: ", myClients);
	myClients.push(e.source);
});

function getChangedOrNewFiles(db) {
	var objStore = db.transaction('KEYVALUE', 'readwrite').objectStore('KEYVALUE');
	return getValueFromObjectStore(objStore, 'hashes').then(function(o) {
		var newFiles = [];
		if (!o) {
			for(var k in staticFiles.value)
				newFiles.push(k);

			console.log("Adding missing hashes");
		} else {
			console.log("Verifying old hashes");
			for(var k in staticFiles.value) {
				var same = staticFiles.value[k] == o.value[k]; 
				if (!same)
					newFiles.push(k);
			}
		}
		return newFiles;
	});
}
function updateStaticFileHash(db) {
	 var objStore = db.transaction('KEYVALUE', 'readwrite').objectStore('KEYVALUE');
	 objStore.put(staticFiles);
}

self.addEventListener('install', function(event) {
	console.log(event);

	event.waitUntil(
			Promise.all([
			             caches.open('static'),
			             getIndexDB('jeller')
			             ])
			             .then(function(promises) {
			            	 var cache = promises[0], db = promises[1];
			            	 console.log("Opened the cache(" + cache + ") and the database (" + db + ")");
			            	 return getChangedOrNewFiles(db)
			            	 .then(files => {broadcast({action : "downloading-files", payload: files}); return files;})
			            	 .then(files => {cache.addAll(files); return files; })
			            	 .then(files => {broadcast({action : "new-files", payload: files}); return files;})
			            	 .then( _ => updateStaticFileHash(db))
			            	 .then( _ => closeDB());

			            	 /*
			            	 var objStore = db.transaction('KEYVALUE', 'readwrite').objectStore('KEYVALUE');
			            	 return getValueFromObjectStore(objStore, 'hashes').then(function(o) {
			            		 console.log("Got response from getValueFromObjectStore");
			            		 console.log(o);
			            		 var newFiles = [];
			            		 if (!o) {
			            			 for(var k in staticFiles.value)
			            				 newFiles.push(k);

			            			 console.log("Adding missing hashes");
			            		 } else {
			            			 console.log("Verifying old hashes");
			            			 for(var k in staticFiles.value) {
			            				 var same = staticFiles.value[k] == o.value[k]; 
			            				 if (!same)
			            					 newFiles.push(k);
			            			 }
			            		 }
			            		 broadcast({action : "downloading-files",  payload: newFiles});
			            		 return cache.addAll(newFiles).then(function() {
			            			 var objStore = db.transaction('KEYVALUE', 'readwrite').objectStore('KEYVALUE');
			            			 objStore.put(staticFiles);
			            			 closeDB();
			            			 broadcast({action : "new-files",  payload: newFiles});
			            		 }).catch(function(o) {
			            			 console.log("inside catch: ", o);
			            			 closeDB();
			            		 });
			            	 }).catch(function(o) {
			            		 console.log("Got error from getValueFromObjectStore"); 
			            		 console.log(o);
			            	 });
			            	  */
			             })
	);
});

self.addEventListener('fetch', function(event) {
	console.log(event);

	if (event.request.url.includes("/api/getDevices")) {
		event.respondWith(
				caches.open('jeller-dynamic').then(function(cache) {
					return fetch(event.request).then(function(response) {
						console.log("getDevices from network");
						cache.put(event.request, response.clone());
						return response;
					}).catch(function() {
						console.log("getDevices from cache");
						return cache.match(event.request);
					});
				}));
	}
	else {
		event.respondWith(
				caches.match(event.request).then(function(response) {
					return response || fetch(event.request);
				})
		);
	}
});
