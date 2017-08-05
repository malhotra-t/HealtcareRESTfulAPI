# Insurance Portal – (RESTful Web Service/CQRS Architecture)

This repository hosts an academic project that I build as part of my masters at Northeastern University, Boston.

It's a RESTful web service API for a hypothetical companies Insurance Portal where admin can set up the insurance options and the employees can pick from a variety of insurance options.

The RESTful web service is implemented using Spring Boot.

The project employs the notion of json-schema for defining a schema (json-schema.org) and then creating entities against it for CRUD operations in an Insurance Portal. 

JSON schema validation on business model entities ensures data integrity while allowing the ubiquitous json to flow through all the tiers of the application.

Additionally, the project performs real time updates to ElasticSearch to support Lucene query syntax.
