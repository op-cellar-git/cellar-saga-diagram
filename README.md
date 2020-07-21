

# cellar-saga-diagram
## Introduction
The Saga execution controller (SEC) is the main component that orchestrates the entire logic of Cellar being responsible for the execution of the Saga transaction. All the steps in a given Saga are recorded in the Sagalog and the SEC writes and interprets the records of the Sagalog. It also executes the sub-transactions/operations and the corresponding compensating transactions when necessary. While the steps related to the Saga are recorded in the Sagalog, the orchestration logic (which can be represented as a directed acyclic graph) is part of the SEC process.

The most important requirements we must fulfill are the following:
 - The controller must be able to scale horizontally, the workflow must,
   therefore, be stateless, each instance of the controller is independent and able to determine which messages to send to activate the transactions.
 - It must be possible to set a timeout policy, in other words, in case the controller doesn't receive any confirmation for the current working transaction, Cellar must trigger a message of failure.
 - Each activation of transition must be persisted on a storage layer, every instance of the controller must access the same storage layer (Sagalog). The Sagalog must not scale horizontally.
 - The workflows are declaratively defined using a BPMN approach.  BPMN will provide us with standard formal notation for defining workflows, elasticity in case of modifications of the flows,  and will give organizations the ability to communicate these procedures in a standard manner. Furthermore, the graphical notation will facilitate internal communication and the general understanding of every involved organization of Cellar. 

The controller can be viewed with the following 3-layer structure:
| Layer | Approach | Prototype |      
|--|--|--|
| Communication |  As has already been said in the general requirements, all communication must be asynchronous. | In the context of our prototype we have made use of Rabbitmq as a message broker for implementing an asynchronous communication between our controller and the microservices, and REST messages among different instances of the controller. |
| Business logic |  It consists of a Workflow engine for executing our BPMN workflows. | In the context of our prototype we have made use of Camunda as a BPMN workflow engine. Other enterprise platforms are listed here: https://en.wikipedia.org/wiki/List_of_BPMN_2.0_engines |
| Persistence | It consists of a database, to implement every functionality properly, must be possible to set constraints and performing the stored procedure.  | In the context of our prototype we have made use of Postgres for storing the Sagalog. |

### Prototype
This [git-repository](https://github.com/op-cellar-git/cellar-saga-diagram) contains the proposal for the implementation of a Saga controller for the orchestration of microservices using a declarative approach based on BPMN2 methodology, the execution is performed by Camunda. https://camunda.com/

## Communication

In order to specify properly the Asynchronous messaging API we have used the "AsyncAPI" specification language ([https://www.asyncapi.com/](https://www.asyncapi.com/)). This specification language can be considered the equivalent of [OpenAPI (fka Swagger)](https://github.com/OAI/OpenAPI-Specification) for specifying Asynchronous messaging API. The current version of the specification is available in the master branch of the following git repository: [https://github.com/op-cellar-git/cellar-transaction](https://github.com/op-cellar-git/cellar-transaction)

Each message must contain at least: 

 1. The name of the message, for example "PrelockValidationBegin", "PrelockValidationEnd" and so on. In startup phase SEC can use some special message like "Startup" or "Start". 
 2. A trace ID to detect the Saga transaction. In fact we can have at the same time different Saga transactions in running state. Since we have to read in the Sagalog the current state for a given Saga transaction we need this ID. In general this field is mandatory with the only exception if the transaction is  just started up (with a startup message), in this case the trace ID is not mandatory because it will be computed by SEC itself. 
 3. And finally an activity ID. This ID is useful to detect properly the activity that has been triggered. This ID is mandatory because in case we have sent more than one "BeginX" message SEC must detect what is the "BeginX" related to the "EndX" or the "Fail" that SEC receives. In order to clarify  

In order to better clarify the usage of the activity ID follow two scenarios:

 - Scenario 1) In the first scenario SEC ends up in a Timeout, when SEC sends again the message is not more able to distiguish if the "End" message is related to the first sent message or the second one.  As a consequence we have that we could proceed with the next steps without having successfully completed the current transaction.  In case SEC used an activity ID, it would be able to discard the first "End" because it was related to a "Begin" ended in Timeout. 
 - Scenario 2) In this second scenario SEC is not able to distinguish if the Fail is related to the Transaction number one "t1" or to the Transaction number two "t2". The difference is important because in case it was related with "t1" SEC must perform a retry, in case it was related to "t2" SEC must perform a compensation chain, otherwise it must only retry "t1".

![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/activity-id.png)
However this ID is also very useful in order to set up integrity checks through "stored procedures". for example we can set as insertion precondition that there cannot be more than two records with the same trace-id activity-id pair.  
In fact, we expect that for each start of activity, for example LockBegin there will be only one other record (or a fail or a LockEnd). This part will be explained in the context of Sagalog management.

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
The messages are not delivered via REST interface but via a Rabbitmq client working inside an instance of the Camunda workflow engine. In our prototype we have setup another Rabbitmq client to send messages: https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/sender/Send.java

## Business logic
In order to supply the business logic, the workflows need only a small subset of concepts from the BPMN2 notation. The workflows need :

**Start event:** it acts as a process trigger; can be a message or a simple start point.

**End event:** it represents the end of a process.

**Intermediate event:** it represents something that happens between the start and end events. In our context, we are interested in catching temporal delays for the Timeout workflow. In fact, SEC must perform checks for Timeout at given time intervals.

**Activity:** it describes the kind of work which must be done. The only kind of activity Cellar needs is Service task. A Service task represents a single unit of work that is not or cannot be broken down to a further level of business process detail. It is an atomic activity. A Service task is the lowest level activity illustrated in a process diagram.  The service tasks Cellar needs are essentially software modules.

**Sequence Flow:** it is represented by a solid line and arrowhead, and shows in which order the activities are performed. The _sequence flow_ may also have a symbol at its start, a small diamond indicates one of a number of **conditional flows** from an activity, while a diagonal slash indicates the **default flow** from a decision or activity with conditional flows.

**Exclusive Gateway:** it is used to create alternative flows in a process. Because only one of the paths can be taken, it is called exclusive. 

In order to supply properly the business logic, we need two different BPMN workflows: Cellar-SEC-Workflow and Cellar-SEC-Timeout.
### Cellar-SEC-Workflow
 It is responsible for deciding the next message to send when SEC receives a message. Since SEC must be completely horizontally scalable each request can be processed by different instances, it is possible because each instance of SEC share the same Sagalog (database). This workflow consists of:
  1. A started event triggered via asynchronous message containing the name of the message, the trace ID and the activity ID.
 2. A Service task called "State retrieving" that retrieves the current state for the pair <trace_id, activity_id> 
 3. Conditional flows triggered by the message we have received and the current state we found  <msg, state, compensation>
 4. Following the flow, we get the other activity that still consists in a Service task, that, simply, sends the message for activating the next computation  

### Cellar-SEC-Timeout
 It is responsible for activating the Cellar-SEC-Workflow in case occurs a "Time out" via a Fail message. This workflow consists of a set of Service Task that checks into the Sagalog whether some current state is out of maximum time. In order to provide this business the Service task via two temporal variables must take in input two temporal variables:
 1. How often to perform a database check for a certain state
 2. What is the maximum threshold allowed between a "Begin" message and an "End" message.


### Prototype
The workflow engine in our prototype is Camunda.
![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/abstract.png)Follow two drafts of workflow, for Cellar-SEC-Workflow: 
https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/controller/src/main/resources/process.bpmn
and  for Cellar-SEC-Timeout: 
https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/controller/src/main/resources/timeout.bpmn
##  Sagalog
The Sagalog is the database used to persist every activity during the execution of Sagas.  It is a key component for the entire architecture because it contains the history of the logic performed and allows the workflows to recover properly the current state for the running Sagas.
### Sagalog structure
At a high level, the Sagalog contains in each record various information about the state-changing operations, such as

> **Trace-ID**: it represents the unique identifier for a  given Saga transaction
> *format: there are no constraints but it must be such as to avoid conflicts*
> **Activity-ID**: it represents the unique identifier for a  given Activity
> *format: there are no constraints but it must be such as to avoid conflicts*
> **Current State**: it represents the current state of the workflow for a given Saga
> *i.e: Begin "Saga", Begin "ActivityX", Fail,  End "Saga", End "ActivityX"*
> **Time Stamp**: it represents the time when the state changed to the current state
> *format: A data time format like YYYY-MM-DD HH:MM:SS*
> **Compensation**: it says us whether the current transaction is in a compensation state
> *format: this information can be seen as a boolean flag*


### Sagalog integrity
In order to ensure proper functioning of the SEC, we can take advantage of some precautions on the database side. Since we know that an activity consists of a Begin record and an End or Fail record, we can exploit this information in order to avoid situations of inconsistency.
In the following image there are two possible scenarios in which we can see that the horizontal scalability of the workflows could be problematic for the consistency of the Sagalog and therefore for the proper functioning of the SEC.


![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/integrity.png) 
- Scenario 1) In the first scenario SEC receives the End after the Timeout, this message must be discarded.
 - Scenario 2) Since more than one Timeout workflow can send a Timeout message, SEC must discard duplicates.

To ensure that each activity is managed correctly we must set the following constraints:

 1. The triple <Trace-ID, Activity-ID, State> must be unique
 2. Must not be more than two records with the same pair <Trace-ID, Activity-ID>

### Quality requirements
Since the Sagalog is a bottle-neck (single point of failure) for the entire Cellar architecture, it must be reliable. It means that the implementation must guarantee:
-   Integrity: the records represent the current state of the Sagas, the correctness is crucial for the right overall working of Cellar.
-   Safety: Access to the database must be authorized because any type of data corruption must be avoided. Since the logic is supplied by Saga execution controller, Sagalog is supposed to be accessed only in B2B mode by an instance of the Saga Execution controller.
-   Recoverability: Cellar needs effective procedures in case of any lost data or some other kind of failure data side. This means that Cellar handles a replica set of the Sagalog and in case of failure it can recover the right status via some recovery procedure.

### Prototype
In our prototype we use a minimal approach (a table with a Primary key composed by Saga-ID, Activity-ID and current State) to store our records:
```
CREATE TABLE public.sagalog (
	saga_id text NOT NULL,
	state text NOT NULL,
	time_stamp timestamp NOT NULL,
	compensation bool NOT NULL,
	activity_id text NOT NULL,
	CONSTRAINT sagalog_pk PRIMARY KEY (saga_id, activity_id, state)
);
```
https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/sagalog/sagalog_table.sql

It follows some sample records:
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
 - https://en.wikipedia.org/wiki/Business_Process_Model_and_Notation
 - https://en.wikipedia.org/wiki/List_of_BPMN_2.0_engines
 - https://www.asyncapi.com/
 - https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/controller/src/main/resources/process.bpmn
 - https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/controller/src/main/resources/timeout.bpmn

