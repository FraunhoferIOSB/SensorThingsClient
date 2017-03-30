SensorThingsClient [![Build Status](https://travis-ci.org/FraunhoferIOSB/SensorThingsClient.svg?branch=master)](https://travis-ci.org/FraunhoferIOSB/SensorThingsClient)
==================

This library provides a Java-based client library for the [SensorThingsAPI](https://github.com/opengeospatial/sensorthings) and aims to simplify development of SensorThings enabled client applications.

**Note:** This project is still under development and therefore lacks complete support of the SensorThingsAPI.

## Features

* CRUD operations
* Queries on entity sets
* Loading of referenced entities

## Unsupported

* Batch requests
* *$select*
* dataArray
* MQTT

## API

The `SensorThingsService` class is central to the library. An instance of it represents a SensorThings service and is identified by an URI.

### CRUD operations

The source code below demonstrates the CRUD operations for Thing objects. Operations for other entities work similarly.

```java
URI serviceEndpoint = URI.create("http://example.org/v1.0/");
SensorThingsService service = new SensorThingsService(serviceEndpoint);
```

```java
Thing thing = new Thing();
thing.setDescription("I'm a thing!");
service.things().create(thing);

thing = service.things().find(1l);

thing.setDescription("Things change...");
service.things().update(thing);

service.things().delete(thing);
```

### Entity Sets

Entity Sets are represented by instances of `EntityList<>`. The query parameters specified by the SensorThingsAPI standard can be applied to queries.

```java
EntityList<Thing> things = service.things()
							.query()
							.count()
							.orderBy("description")
							.filter("")
							.skip(5)
							.top(10)
							.list();

for (Thing thing : things) {
	System.out.println("So many things!");
}
```


Entity sets only load so many entities at a time. If you want to get *all* entities,
and there are more entities than the $top parameter allows you get in one request, you can
use the `EntityList.fullIterator();` Iterator.

```java
EntityList<Observations> observations = service.observations()
							.query()
							.count()
							.top(1000)
							.list();

Iterator<Observation> i = observations.fullIterator();
while (i.hasNext()) {
    Observation obs = i.next();
	System.out.println("Observation " + obs.getId() + " has result " + obs.getResult());
}
```


Related entity sets can also be queried.
```java
thing = service.things().find(1l);

EntityList<Datastream> dataStreams = thing.datastreams().query().list();
for (Datastream dataStream : dataStreams) {
	Sensor sensor = dataStream.getSensor();
    System.out.println("dataStream " + dataStream.getId() + " has Sensor " + sensor.getId());
}

```

However, `$expand` does not work on queries yet.

### Loading referenced objects

Loading referenced objects in one operation (and therefore in one request) is supported. The *$expand* option of the SensorThingsAPI standard is used internally.

```java
Thing thing = service.things().find(1l,
				Expansion.of(EntityType.THING)
				.with(ExpandedEntity.from(EntityType.LOCATIONS)));
EntityList<Location> locations = thing.getLocations();
```

### DataArray for Observation creation

Using DataArrays for creating Observations is more efficient, since only one http request
 is done, and the observations are more efficiently encoded in this request, so the request
 is smaller than the sum of the separate, normal requests.

```java
Set<DataArrayValue.Property> properties = new HashSet<>();
properties.add(DataArrayValue.Property.Result);
properties.add(DataArrayValue.Property.PhenomenonTime);

DataArrayValue dav1 = new DataArrayValue(datastream1, properties);
dav1.addObservation(observation1);
dav1.addObservation(observation2);
dav1.addObservation(observation3);

DataArrayValue dav2 = new DataArrayValue(multiDatastream1, properties);
dav2.addObservation(observation4);
dav2.addObservation(observation5);
dav2.addObservation(observation6);

DataArrayDocument dad = new DataArrayDocument();
dad.addDataArrayValue(dav1);
dad.addDataArrayValue(dav2);

service.create(dad);

```

## Background

This library emerged from a practical work for a lecture at [KIT](http://www.kit.edu) in collaboration with the [Fraunhofer IOSB](http://iosb.fraunhofer.de). A [server implementation](https://github.com/FraunhoferIOSB/SensorThingsServer) of the SensorThingsAPI, developed by the Fraunhofer IOSB, is available on GitHub as well.

## Contributing

Contributions are welcome!

1. Fork this repository
2. Commit your changes
3. Create a pull request

## License

The code and the documentation of this work is available under the MIT license.
