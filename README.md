# Spring Data Marklogic [![Build Status](https://travis-ci.org/stoussaint/spring-data-marklogic.svg?branch=master)](https://travis-ci.org/stoussaint/spring-data-marklogic)

The primary goal of the [Spring Data](http://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data Marklogic project aims a Spring-based programming model for Marklogic repositories

## Getting Help

If you are new to Spring as well as to Spring Data, look for information about [Spring projects](http://projects.spring.io/).

## A few words about Marklogic

> MarkLogic Server is an Enterprise NoSQL database.
> 
> It is a document-centric, transactional, search-centric, structure-aware, schema-agnostic, XQuery- and JavaScript-driven, high performance, clustered, database server.
> 
> MarkLogic fuses together database internals, search-style indexing, and application server behaviors into a unified system. It uses XML and JSON documents, along with RDF triples, as its data model, and stores the documents within a transactional repository. It indexes the words and values from each of the loaded documents, as well as the document structure. And, because of its unique Universal Index, MarkLogic doesn't require advance knowledge of the document structure (its "schema") nor complete adherence to a particular schema. Through its application server capabilities, it's programmable and extensible.
> 
> MarkLogic clusters on commodity hardware using a shared-nothing architecture and differentiates itself in the market by supporting massive scale and fantastic performance — customer deployments have scaled to hundreds of terabytes of source data while maintaining sub-second query response time.

This excerpt is from [An Introduction to MarkLogic Server and XQuery](https://developer.marklogic.com/learn/technical-overview)

## Quick Start

First of all, you will need Marklogic to be installed (note that Marklogic provide free licence for development).
Spring Data Marklogic connects to Marklogic using the XDBC client, so you need a [XDBC server](https://docs.marklogic.com/guide/admin/xdbc) up and ready.

Then add the Spring Data Marklogic dependency in your project.

### Maven configuration 

```xml
<dependency>
    <groupId>com.4dconcept.springframework.data</groupId>
    <artifactId>spring-data-marklogic</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle configuration

```
    compile ("com.4dconcept.springframework.data:spring-data-marklogic:${version}")
```


