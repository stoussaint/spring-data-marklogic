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

First of all, you will need Marklogic to be installed (note that Marklogic provides free license for development).
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

### Use Spring Data Marklogic

Spring Data Marklogic comes with two level of support for Marklogic : MarklogicTemplate and Spring Data repositories

#### MarklogicTemplate

MarklogicTemplate is the central support for Marklogic database operations. It provides :

* A set of CRUD operations over entities (insert, save, remove, find)
* A convenient way to call xquery scripts (invokeAdhocQuery, invokeModule)
* Entity to xml document converter (JAXB support by default)
* Exception translation into Spring's 

MarklogicTemplate is the low level API that provide abstraction over Marklogic Java client (ContentSource and provided Session).

#### Spring Data repositories

To simplify the creation of data repositories, Spring Data Marklogic provides a generic repository programming model.
It will automatically create a repository proxy for you that adds implementations of methods you specify on an interface.

For exemple, given a `Person` class with first and last name properties, a `PersonRepository` interface that can query for `Person`
 by last name is show below :
 
```java
public interface PersonRepository extends MarklogicRepository<Person, String> {
    List<Person> findByLastname(String lastname);
}
```

The queries issued on execution will be derived from the method name.
Extending MarklogicRepository causes CRUD methods being pulled into the interface so that you can easily save and find single entities and collections of them.
It also provides Pagination and by Example queries.

You can have full Spring Data Marklogic up and set by using the following JavaConfig :

```java
@Configuration
@EnableMarklogicRepositories
public class ApplicationConfigConfig extends AbstractMarklogicConfiguration  {

    @Override
    protected URI getMarklogicUri() {
        return URI.create("xdbc://[login]:[password]@[host]:[port]");
    }

}
```

This sets up a connection to your Marklogic xdbc server using providing url and enables the detection of Spring Data repositories
 (through `@EnableMarklogicRepositories` annotation).
 
This will find the repository interface and register a proxy object in the container. You can use it as shown below :

```java
@Service
public class MyService  {
    
    private PersonRepository repository;

    public MyService(PersonRepository repository) {
        this.repository = repository;
    }
    
    public void testRepository() {
        repository.deleteAll();
        
        Person person = new Person();
        person.setFistname("Stephane");
        person.setLastname("Toussaint");
        person = repository.save(person);
        
        List<Person> myFamilly = repository.findByLastname("Toussaint");
        Person me = repository.findOne(person.getId());
    }

}
```

#### Basic information for how entity are stored

Spring Data will handle entities (Pojos) as xml documents. This required a few information :

- A uri : Document in a Marklogic database are stored at a defined uri. This uri will be computed using the default "/content/[entityClass.simpleName.lowerCase]/[id].xml" pattern.
You can provide a specific uri through `@Document` annotation.
- A collection (optional) : Marklogic used 'collection' to regroups documents in a logical way. Spring Data can use `collection` to regroups entities. By default no collection is applied, 
but it is recommended to define one through `@Document` annotation.
- An xml content : The pojo entities has to be marshall (serialized) into xml. A default converter will handle this conversion using Jaxb. Specific converters can be register if the default is not sufficient.

### Advanced configuration and usage and help

You will be able to find more information in the asciidoctor documentation coming with the source.  