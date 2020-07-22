


# cellar-saga-diagram
## Introduction
The Saga execution controller (SEC) is the main component that orchestrates the entire execution of Cellar during the ingestion being responsible for the execution of the Saga transactions. All the steps in a given Saga are recorded in the Sagalog and SEC writes and interprets the records of the Sagalog. It also executes the compensating transactions when necessary, for example in case something goes wrong and Cellar must restore its databases. While the steps related to the Saga are recorded in the Sagalog, the orchestration logic (which can be represented as a directed acyclic graph) is part of SEC in the form of executable workflows that receive and send messages and interact with the Sagalog.

The most important requirements to fulfill are the following:
 - SEC must be able to scale horizontally, the workflows must,
   therefore, be stateless, each instance of the controller is independent and able to determine which messages to send to activate the next transaction.
 - It must be possible to set a timeout policy, in other words, in case the controller doesn't receive any confirmation for the current working transaction, Cellar must trigger a message of failure.
 - Each activation of transition must be persisted on a storage layer, every instance of SEC must access the same storage layer (Sagalog). The Sagalog must not scale horizontally.
 - The workflows are declaratively defined using a BPMN approach.  BPMN will provide SEC with standard formal notation for defining workflows, elasticity in case of modifications of the flows,  and will give organizations the ability to communicate these procedures in a standard manner. Furthermore, the graphical notation will facilitate internal communication and the general understanding of every involved organization of Cellar.

The controller can be viewed with the following 3-layer structure:
| Layer | Approach | Prototype |      
|--|--|--|
| Communication |  It must be asynchronous. The interface for each component involved in the comunication must be the message broker. | The prototype makes use of Rabbitmq as a message broker for implementing an asynchronous communication between the controller and the microservices, and REST messages among different instances of the controller. Each component involved in the comunication uses therefore a specific Rabbitmq client. |
| Business logic |  It consists of a Workflow engine for executing the BPMN workflows. | The prototype makes use of Camunda as a BPMN workflow engine. Other enterprise platforms are listed here: https://en.wikipedia.org/wiki/List_of_BPMN_2.0_engines |
| Persistence | It consists of a database, to implement every functionality properly, must be possible to set constraints and to perform stored procedures.  | The prototype makes use of Postgres for storing the Sagalog. |

### Prototype
This [git-repository](https://github.com/op-cellar-git/cellar-saga-diagram) contains the proposal for the implementation of a Saga controller for the orchestration of microservices using a declarative approach based on BPMN2 methodology, the execution is performed by Camunda. https://camunda.com/

## Communication
To specify properly the Asynchronous messaging API has been used the "AsyncAPI" specification language ([https://www.asyncapi.com/](https://www.asyncapi.com/)). This specification language can be considered the equivalent of [OpenAPI (fka Swagger)](https://github.com/OAI/OpenAPI-Specification) for specifying Asynchronous messaging API. The current version of the specification is available in the master branch of the following git repository: [https://github.com/op-cellar-git/cellar-transaction](https://github.com/op-cellar-git/cellar-transaction)

Each message must contain at least: 

 1. The name of the message, for example, "PrelockValidationBegin", "PrelockValidationEnd" and so on. In the startup phase SEC can use some special messages like "Startup" or "Start". 
 2. A Trace-ID to detect the Saga transaction. There could be - at the same time - different Saga transactions in running state. Since SEC must read on the Sagalog the current state for a given Saga transaction it needs this ID. In general, this field is mandatory with the only exception if the transaction is just started up (with a startup message), in this case, the Trace-ID is not mandatory because it will be computed by SEC itself. 
 3. And finally an Activity-ID. This ID is useful to detect properly the activity that has been triggered. This ID is mandatory because in case SEC has sent more than one "BeginX" message it must detect what is the "BeginX" related to the "EndX" or the "Fail" that it receives. 

In order to better clarify the usage of the Activity-ID follows two scenarios:
 - Scenario 1) In the first scenario SEC ends up in a Timeout, when SEC sends again the same message it is not more able to distinguish if the "End" message is related to the first sent message or the second one.  As a consequence, we have that it could proceed with the next steps without having completed the current transaction.  In case SEC used an Activity-ID, it would be able to discard the first "End" because it was related to a "Begin" ended in Timeout. 
 - Scenario 2) In this second scenario SEC is not able to distinguish if the Fail is related to the Transaction number one "t1" or the Transaction number two "t2". The difference is important because in case it was related to "t1" SEC must perform a retry, otherwise, in case it was related to "t2" SEC must perform a compensation chain.

![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/activity-id.png)
However, Activity-ID is also very useful in order to set up integrity checks through "stored procedures". For example it is possible to set as insertion precondition that there cannot be more than two records with the same pair <Trace-ID, Activity-ID>.  
In fact, it is reasonable to expect that for each start of an activity, for example, "LockBegin" there will be only one other record (or "Fail" or "LockEnd") with the same pair <Trace-ID, Activity-ID>. This part will be explained better in the context of Sagalog management.

### Prototype

The REST interface for activating activities in Camunda is the following:
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
The messages are not delivered via REST interface but via a Rabbitmq client working inside an instance of the Camunda workflow engine. In the prototype there is another Rabbitmq client to send messages: https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/sender/Send.java

## Business logic
In order to supply the business logic in the context of Cellar, the workflows need only a small subset of concepts from the BPMN2 notation. The workflows need :

**Start event:** it acts as a process trigger; can be a message or a simple start point.

**End event:** it represents the end of a process.

**Intermediate event:** it represents something that happens between the start and end events. In the context of Cellar, the purpose is catching temporal delays for the Timeout workflow. In fact, SEC must perform checks for Timeouts detecting at given time intervals.

**Activity:** it describes the kind of work which must be done. The only kind of activity Cellar needs is Service task. A Service task represents a single unit of work that is not or cannot be broken down to a further level of business process detail. It is an atomic activity. A Service task is the lowest level activity illustrated in a process diagram.  The service tasks Cellar needs are essentially software modules.

**Sequence Flow:** it is represented by a solid line and arrowhead, and shows in which order the activities are performed. The _sequence flow_ may also have a symbol at its start, a small diamond indicates one of a number of **conditional flows** from an activity, while a diagonal slash indicates the **default flow** from a decision or activity with conditional flows.

**Exclusive Gateway:** it is used to create alternative flows in a process. Because only one of the paths can be taken, it is called exclusive. 

In order to supply properly the business logic, SEC needs two different BPMN workflows, that will be called: 
 1. *Cellar-SEC-Workflow* 
 2. *Cellar-SEC-Timeout*

### Cellar-SEC-Workflow
 It is responsible for deciding the next message to send when SEC receives a message. Since SEC must be completely stateless and horizontally scalable each request can be processed by different instances, it is possible because each instance of SEC share the same Sagalog (database) and for each request, they recover the state from the Sagalog. 
![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/workflow1.png)
This workflow consists of:
  1. A started event triggered via an asynchronous message containing the name of the message, the Trace-ID, and the Activity-ID.
 2. A Service task called "State retrieving" that retrieves from the Sagalog the current state and the compensation state (true or false) using the pair <Trace-ID, Activity-ID> 
 3. Conditional flows triggered by the message that SEC receives and the current state that SEC finds into the Sagalog:  <Message, Current state, Compensation state>. For example, if SEC receives the message "EndActivity1" and the current state is "BeginActivity1" then the message to send is "BeginActivity2".
 4. Following the flow, SEC gets the last activity that still consists of a Service task, it sends a new message to the Message broker and modifies the current state in the Sagalog for the current Saga transaction. In this case, it writes on Sagalog "BeginActivity2" as the current state for that Saga transaction and it sends "BeginActivity2" to the message broker.
 5. The process can terminate, with this kind of implementation each request can be processed by a different instance of SEC. In fact, this BPMN does not represent the entire flow altogether, rather it describes a set of transitions to operating in an independent way each other. Each transition of state for a given Saga transaction is an independent execution for a given instance of the workflow engine.

### Cellar-SEC-Timeout
 It is responsible for activating the Cellar-SEC-Workflow in case occurs a "Timeout" via a Fail message. This workflow consists of a Service Task that checks into the Sagalog whether some current state is out of maximum time. 
![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/workflow2.png)In order to provide this business the Service task "Timeout checking" must take in input two variables:
 1. How often to perform a database check for a certain state.
 2. The maximum threshold allowed between a "Begin" message and an "End" message or a "Fail" for a certain state.
In case the threshold is exceeded, this workflow must trigger via a "Fail" message the other one (Cellar-SEC-Workflow).

### Prototype
The workflow engine in the prototype is Camunda.
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
> 
> **Activity-ID**: it represents the unique identifier for a  given Activity
> *format: there are no constraints but it must be such as to avoid conflicts*
> 
> **Current State**: it represents the current state of the workflow for a given Saga
> *i.e: Begin "Saga", Begin "ActivityX", Fail,  End "Saga", End "ActivityX"*
> 
> **Time Stamp**: it represents the time when the state changed to the current state
> *format: A data time format like YYYY-MM-DD HH:MM:SS*
> 
> **Compensation**: it says us whether the current transaction is in a compensation state
> *format: this information can be seen as a boolean flag*


### Sagalog integrity
In order to ensure the proper functioning of SEC, the implementation must take advantage of some precautions on the database side. Since each activity consists of a "Begin" record and an "End" or "Fail" record, SEC must exploit this information in order to avoid situations of inconsistency.
In the following image, there are two possible scenarios in which we can see that the horizontal scalability of the workflows could be problematic for the consistency of the Sagalog and therefore for the proper functioning of the SEC.


![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/img/integrity.png) 
- Scenario 1) In the first scenario SEC receives the End after the Timeout, this message must be discarded.
 - Scenario 2) Since more than one Timeout workflow can send a Timeout message, SEC must discard duplicates.

To ensure that each activity is managed correctly must be set the following constraints:

 1. The triple <Trace-ID, Activity-ID, State> must be unique
 2. Must not be more than two records with the same pair <Trace-ID, Activity-ID>

### Quality requirements
Since the Sagalog is a bottle-neck (single point of failure) for the entire Cellar architecture, it must be reliable. It means that the implementation must guarantee:
-   Integrity: the records represent the current state of the Sagas, the correctness is crucial for the right overall working of Cellar.
-   Safety: Access to the database must be authorized because any type of data corruption must be avoided. Since the logic is supplied by SEC, Sagalog is supposed to be accessed only in B2B mode by an instance of the Saga Execution controller.
-   Recoverability: Cellar needs effective procedures in case of any lost data or some other kind of failure data side. This means that Cellar handles a replica set of the Sagalog and in case of failure it can recover the right status via some recovery procedure.

### Prototype
The prototype uses a minimal approach (a table with a Primary key composed by Trace-ID, Activity-ID, and the current state) to store the log-records:
```
CREATE TABLE public.sagalog (
	trace_id text NOT NULL,
	state text NOT NULL,
	time_stamp timestamp NOT NULL,
	compensation bool NOT NULL,
	activity_id text NOT NULL,
	CONSTRAINT sagalog_pk PRIMARY KEY (trace_id, activity_id, state)
);
```
https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/sagalog/sagalog_table.sql

It follows some sample records:
| trace_id | state  | timestamp | compensation | activity_id |
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

