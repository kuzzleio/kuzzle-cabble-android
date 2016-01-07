# Kuzzle - Cabble for android

Cabble is an app enabling customers to find a cab, and cabs to find customers.

## Table of contents

* [Connection to kuzzle](#kuzzle-connection)
* [Create index](#create-index)
* [Create mapping](#create-mapping)
* [Create collection](#create-collection)
* [Create user](#create-user)
* [Handling users connection and disconnection](#handling-users-connection-and-disconnection)
* [Set up our vicinity](#set-up-our-vicinity)
* [Change our availability](#change-our-availability)
* [Subscribtion to rides proposal](#subscribtion-to-rides-proposal)

## Kuzzle connection

First we need to establish a connection to our Kuzzle [(MapActivity:297)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L297):
```java
kuzzle = new Kuzzle(kuzzle_host, "cabble", options, new ResponseListener() {
  @Override
  public void onSuccess(JSONObject object) {
    prepareMapping(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        createMySelf();
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble", "An error occured during the connection to kuzzle: " + error.toString());
      }
    });
  }
  ...
```

## Create index

Once we've connected to Kuzzle, the onSuccess callback will be executed and then we will create our collection mapping [(MapActivity.java:332)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L332)
To do that we check if our index exists by verifying if our collections exist with kuzzle.listCollections, if they don't the onError callback will be triggered [(MapActivity.java:343)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L343)
and then we create our index with kuzzle.query [(MapActivity.java:348)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L348)
```java
kuzzle.query("", "admin", "createIndex", query, new ResponseListener() { ... });
```

## Create mapping

Now that our index is created we need to create our mapping (This is not mandatory, we only need to create our 'pos' field to specify that its type is 'geo_point' [(MapActivity.java:321)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L321))
```java
KuzzleDataMapping mapping = new KuzzleDataMapping(userCollection);
  mapping.set("pos", new JSONObject().put("type", "geo_point"));
  mapping.apply();
```

## Create collection

Then we can finally create our 2 collections, which are rideCollection and userCollection (you can create a collection by subscribing to it without creating it before) [(MapActivity.java:351)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L351)
```java
userCollection = kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_users));
rideCollection = kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_rides));
userCollection.create(...);
rideCollection.create(...);
```

## Create user

We will now create our user depending on its type (cab or customer) [(MapActivity.java:391)](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L391)
The user is represented by a KuzzleDocument, which is stored into elasticsearch:
```java
self = new KuzzleDocument(userCollection);
self.setContent("type", userType.toString().toLowerCase())
    .setContent("pos", pos)
    .setContent("status", Status.IDLE.toString())
    .setContent("sibling", "none");
userCollection.createDocument(self, ...);
```

## Handling users connection and disconnection

Connections and disconnections handling are not related to documents.
These events are fired whenever someone enters or leaves a room you're listening to.
Be aware that a room is in fact a set of filters, so to receive one of these notifications, you need to use the same filters than the application entering or leaving the room.
To do that we subscribe to the userCollection without any filters and with the option of listening to all user event and adding the user id to the subscription's metadata ([MapActivity.java:586](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L586))

```java
KuzzleRoomOptions options = new KuzzleRoomOptions();
options.setUsers(Users.OUT);
JSONObject meta = new JSONObject();
meta.put("_id", self.getString("_id"));
options.setMetadata(meta);
options.setSubscribeToSelf(false);
usersScope = userCollection.subscribe(null, options, new ResponseListener() {
  @Override
  public void onSuccess(JSONObject object) {
    try {
      if (object.getString("action").equals("off")) {
        MapController.getSingleton(MapActivity.this.getBaseContext()).removeCandidate(object.getJSONObject("metadata").getString("_id"));
        userList.remove(object.getJSONObject("metadata").getString("_id"));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
...
```

## Set up our vicinity

We don't want to see the cabs and customers of the whole world so we set a vicinity.

To do that we also use the scope to know when it will be in or out of our vicinity ([MapActivity.java:493](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L493))
```java
userRoom = userCollection.subscribe(userSubscribeFilter, options, new ResponseListener() {
  @Override
  public void onSuccess(JSONObject object) {
    Log.e("cabble", "# user: " + object.toString());
    try {
      if (!object.getString("scope").equals("out")) {
        // user out of vicinity
        ...
      } else {
        // user in vicinity
        ...
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  ...
```

We also need to set a geo spatial filter ([MapActivity.java:427](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L427)), you can relate to [this tutorial](https://github.com/kuzzleio/demo/blob/master/cabble/tutorial.geospacial.md) to know more.
```java
JSONObject geo_distance = new JSONObject();
JSONObject filter = new JSONObject();
geo_distance.put("pos", self.getContent("pos"));
geo_distance.put("distance", getResources().getString(R.string.distance_filter) + "m");
filter.put("geo_distance", geo_distance);
```

## Change our availability

When a cab wants to look for a customer or a customer wants a ride we simply update the KuzzleDocument, then all the other subscriptions will be notified and will be able to propose a ride or an hire.
([MapActivity.java:139](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L139))
```java
...
self.setContent("status", Status.TOHIRE.toString());
...
self.save();
```

## Subscribtion to rides proposal

Here we subscribe to the ride collection with the filter matching status, type and position according to our type ([MapActivity.java:524](https://github.com/kuzzleio/kuzzle-cabble-android/blob/master/src/main/java/io/kuzzle/demo/demo_android/MapActivity.java#L524))
```java
rideRoom = rideCollection.subscribe(initRideSubscribeFilter(), options, new ResponseListener() {
  @Override
  public void onSuccess(JSONObject object) {
    try {
      currentRide = new KuzzleDocument(rideCollection, object);
      manageRideProposal();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  ...
```
