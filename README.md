

# cellar-saga-diagram
## Introduction
The Saga execution controller (SEC) is the main component that orchestrates the entire logic of Cellar being responsible for the execution of the Saga transaction. All the steps in a given Saga are recorded in the Sagalog and the SEC writes and interprets the records of the Saga log. It also executes the sub-transactions/operations and the corresponding compensating transactions when necessary. While the steps related to the Saga are recorded in the Sagalog, the orchestration logic (which can be represented as a directed acyclic graph) is part of the SEC process.

The most important requirements we must fulfill are the following:
 - The controller must be able to scale horizontally, the workflow must,
   therefore, be stateless, each instance of the controller is independent and able to determine which messages to send to activate the transactions.
 - It must be possible to set a timeout policy, in other words, in case the controller doesn't receive any confirmation for the current working transaction, Cellar must trigger a message of failure.
 - Each activation of transition must be persisted on a storage layer, every instance of the controller must access the same storage layer (Sagalog). The Sagalog must not scale horizontally.
 - The workflows are declaratively defined using a BPMN approach.  BPMN will provide us with standard formal notation for defining workflows, elasticity in case of modifications of the flows,  and will give organizations the ability to communicate these procedures in a standard manner. Furthermore, the graphical notation will facilitate internal communication and the general understanding of every involved organization of Cellar. 

The controller is supposed to be composed with the following architecture:
| Layer | Approach | Prototype |      
|--|--|--|
| Communication |  As has already been said in the general requirements, all communication must be asynchronous. | In the context of our prototype we have made use of Rabbitmq as a message broker for implementing an asynchronous communication between our controller and the microservices, and REST messages among different instances of the controller. |
| Business logic |  It consists of a Workflow engine for executing our BPMN workflows. | In the context of our prototype, we have made use of Camunda as a BPMN workflow engine. Other enterprise platforms are listed here: https://en.wikipedia.org/wiki/List_of_BPMN_2.0_engines |
| Persistence | It consists of a database, to implement every functionality properly, must be possible to set constraints and performing the stored procedure.  | In the context of our prototype, we have made use of Postgres for storing the Sagalog. |

### Prototype
This [git-repository](https://github.com/op-cellar-git/cellar-saga-diagram) contains the proposal for the implementation of a Saga controller for the orchestration of microservices using a declarative approach based on BPMN2 methodology, implemented by Camunda.

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
In order to supply our business logic, our workflows need only a small subset of concepts from the BPMN2 notation, at least initially. The workflows need :
**Start event:** it acts as a process trigger; indicated by a single narrow border, and can only be _Catch_, so is shown with an open (outline) icon.
**End event:** it represents the result of a process; indicated by a single thick or bold border, and can only _Throw_, so is shown with a solid icon.
**Intermediate event:** it represents something that happens between the start and end events; is indicated by a double border, and can _Throw_ or _Catch_ (using solid or open icons as appropriate). In our context, we are interested in catching delays, especially for our Timeout workflow. In fact, the SEC must perform a check every 5 minutes (for example).
**Activity:** it is represented with a rounded-corner rectangle and describes the kind of work which must be done. An activity is a generic term for work that a company. The only kind of activity Cellar needs is Service tasks. A Service task represents a single unit of work that is not or cannot be broken down to a further level of business process detail. It is referred to as an atomic activity. A Service task is the lowest level activity illustrated in a process diagram.  The service tasks Cellar needs are essentially software modules, these tasks will be described below.
**Sequence Flow:** it is represented with a solid line and arrowhead, and shows in which order the activities are performed. The _sequence flow_ may also have a symbol at its start, a small diamond indicates one of a number of **conditional flows** from an activity, while a diagonal slash indicates the **default flow** from a decision or activity with conditional flows.
**Exclusive Gateway:** it is used to create alternative flows in a process. Because only one of the paths can be taken, it is called exclusive. Represents the result of a process; indicated by a single thick or bold border, and can only _Throw_, so is shown with a solid icon.

In order to supply properly our business logic, we need two different BPMN workflows: Cellar-SEC-Workflow and Cellar-SEC-Timeout.
### Cellar-SEC-Workflow
 It is responsible for deciding the next message to send. Since SEC must be completely horizontally scalable each request (the messages in input could be the Begin's and the Fail) can be processed by different instances. This workflow consists of:
  1. A started event triggered via asynchronous message
 2. A Service task called "State retrieving" that retrieves the current state for the pair <trace_id, activity_id> 
 3. Conditional flows triggered by the message we have received and the current state we found  <msg, state, compensation>
 4. Following the flow, we get the other activity that still consists in a Service task, that, simply, sends the message for activating the next computation  

### Cellar-SEC-Timeout
 It is responsible for activating the Cellar-SEC-Workflow in case occurs a "Time out" via a Fail message. This workflow consists of a set of Service Task that checks into the Sagalog whether some current state is out of maximum time. In order to provide this business the Service task via two temporal variables must take in input two temporal variables:
 1. How often to perform a database check for a certain state
 2. What is the maximum threshold allowed between a "Begin" message and an "End" message.


### Prototype
The workflow engine in our prototype is Camunda.
![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/abstract.png)Follow two drafts of workflow, for Cellar-SEC-Workflow: 
https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/controller/src/main/resources/process.bpmn
and  for Cellar-SEC-Timeout: 
https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/master/camunda/controller/src/main/resources/timeout.bpmn
##  Sagalog
The Sagalog is the log that is used to persist in every activity during the execution of Sagas. At a high level, the Sagalog contains various state-changing operations, such as

> Begin Saga, End Saga, Abort Saga, Begin T-i, End T-i, Begin C-i, and End C-i
### Sagalog structure

### Quality requirements
Since the Sagalog is a bottle-neck (single point of failure) for the entire Cellar architecture, it must be reliable. It means that the implementation must guarantee:
-   Integrity: the records represent the current state of the Sagas, the correctness is crucial for the right overall working of Cellar.
-   Safety: Access to the database must be authorized because any type of data corruption must be avoided. Since the logic is supplied by Saga execution controller, Saga log is supposed to be accessed only in B2B mode by an instance of the Saga Execution controller.
-   Recoverability: Cellar needs effective procedures in case of any lost data or some other kind of failure data side. This means that Cellar handles a replica set of the Saga log and in case of failure it can recover the right status via some recovery procedure.

### Prototype
In our prototype, the values are valorized as follows:



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

