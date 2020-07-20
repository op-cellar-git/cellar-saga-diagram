
# cellar-saga-diagram
## Introduction
The Saga execution controller (SEC) is the main component that orchestrates the entire logic of Cellar being responsible for the execution of the Saga transaction. All the steps in a given Saga are recorded in the Sagalog and the SEC writes and interprets the records of the Saga log. It also executes the sub-transactions/operations and the corresponding compensating transactions when necessary. While the steps related to the Saga are recorded in the Sagalog, the orchestration logic (which can be represented as a directed acyclic graph) is part of the SEC process.

The most important requirements we must fulfill are the following:
 - The controller must be able to scale horizontally, the workflow must,
   therefore, be stateless, each instance of the controller is independent and able to determine which messages to send to activate the transactions.
 - It must be possible to set a timeout policy, in other words, in case the controller doesn't receive any confirmation for the current working transaction, Cellar must trigger a message of failure.
 - Each activation of transition must be persisted on a storage layer, every instance of the controller must access the same storage layer (Sagalog).
 - The workflows are declaratively defined using a BPMN approach.  BPMN will provide us with standard formal notation for defining workflows, elasticity in case of modifications of the flows,  and will give organizations the ability to communicate these procedures in a standard manner. Furthermore, the graphical notation will facilitate internal communication and the general understanding of every involved organization of Cellar. 

The controller is supposed to be composed with the following architecture:
| Layer | Approach | Prototype |      
|--|--|--|
| Communication |  As has already been said in the general requirements, all communication must be asynchronous | In the context of our prototype we have made used of Rabbitmq as message broker for implementing an asynchronous communication between our controller and the microservices, and REST messages among different instances of the controller |
| Business logic |  Camunda as workflow engine for our BPMN flows (Spring boot is embedded)| In the context of our prototype we have made used of Camunda as BPMN workflow engine. |
| Persistence |  | In the context of our prototype we have made used of postgres for storing the sagalog |

### Prototype
This [git-repository](https://github.com/op-cellar-git/cellar-saga-diagram) contains the proposal for the implementation of a saga controller for the orchestration of microservices using a declarative approach based on BPMN methodology.

## Communication

In order to specify properly the Asynchronous messaging API we have used the "AsyncAPI" specification language ([https://www.asyncapi.com/](https://www.asyncapi.com/)). This specification language can be considered the equivalent of [OpenAPI (fka Swagger)](https://github.com/OAI/OpenAPI-Specification) for specifying Asynchronous messaging API. The current version of the specification is available in the master branch of the following git repository: [https://github.com/op-cellar-git/cellar-transaction](https://github.com/op-cellar-git/cellar-transaction)

### Prototype

The REST interface for activating activities in Camunda is the is the following:
```
{
  "messageName" : "start",
  "processVariables" : {
    "msg" : {"value" : "PrelockValidationEnd", "type": "String"},
    "trace" : {"value" : "abc9cc6c-c802-11ea-975f-d89c67c87228", "type": "String"},
    "activity" : {"value" : "aa89eb4e-67e3-4ac5-8281-8344911668c1", "type": "String"}
  }
}
```
https://docs.camunda.org/manual/7.9/reference/rest/message/


## Business logic
In order to supply properly our business logic we need two different BPMN workflows:
one responsible to decide the next 

### Cellar-SEC-Workflow

### Cellar-SEC-Timeout

### Prototype
The workflow engine in our prototype is Camunda, with Camunda. There are several other different BPMN workflow engines: https://en.wikipedia.org/wiki/List_of_BPMN_2.0_engines
![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/abstract.png)
##  Sagalog
The Sagalog is the log that’s used to persist every transaction/operation during the execution of Sagas. At a high level, the Saga log contains various state-changing operations, such as

> Begin Saga, End Saga, Abort Saga, Begin T-i, End T-i, Begin C-i, and End C-i

Since Saga log is a bottle-neck (single point of failure) for the entire Cellar architecture, it must be reliable. It means that the implementation must guarantee:
-   Integrity: the records represent the current state of the Sagas, the correctness is crucial for the right overall working of Cellar.
-   Safety: Access to the database must be authorized because any type of data corruption must be avoid. Since the logic is supplied by Saga execution controller, Saga log is supposed to be accessed only in B2B mode by an instance of the Saga Execution controller.
-   Recoverability: Cellar needs effective procedures in case of any lost data or some other kind of failure data side. This means that Cellar handles a replica set of the Saga log and in case of failure it is able to recover the right status via some recovery procedure.

### Prototype

| saga_id | state  | timestamp | compensation | activity_id |
|--|--|--|--|--|
| e5c8e197-c75b-11ea-a1f8-d89c67c87228 | startup | 2020-07-16 14:00:10 | false | e5c8e197-c75b-11ea-a1f8-d89c67c87228 |
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | PrelockValidationBegin | 2020-07-16 14:00:10 | false | f3cc5dbd-2cac-4228-99ca-abb1bb673de5
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | PrelockValidationEnd | 2020-07-16 14:23:24 | false | f3cc5dbd-2cac-4228-99ca-abb1bb673de5
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | LockBegin | 2020-07-16 14:23:24 | false | 83f8b2c4-e464-4c03-8f22-018f3be341f0
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | LockEnd | 2020-07-16 15:10:57 | false | 83f8b2c4-e464-4c03-8f22-018f3be341f0
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | CleanUpBegin | 2020-07-16 15:10:57 | false | a90fecca-3e04-49bd-8d1e-248b3f629278
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | LockEnd | 2020-07-16 15:13:02 | false | a90fecca-3e04-49bd-8d1e-248b3f629278
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | CleanUpEnd | 2020-07-16 15:16:02 | false | a90fecca-3e04-49bd-8d1e-248b3f629278
e5c8e197-c75b-11ea-a1f8-d89c67c87228 | end | 2020-07-16 15:16:02 | false | e5c8e197-c75b-11ea-a1f8-d89c67c87228

##  References
 - https://www.rabbitmq.com/
 - https://camunda.com/
 - https://www.postgresql.org/
 - http://www.bpmn.org/
 - https://github.com/op-cellar-git/cellar-transaction
 - https://en.wikipedia.org/wiki/List_of_BPMN_2.0_engines
 - https://www.asyncapi.com/

