
# cellar-saga-diagram
This REPO contains the proposal for the implementation of a saga controller for the orchestration of microservices using a declarative approach based on BPMN terminology.

The most important requirements we must fulfill are the following:
 - The controller must be able to scale horizontally, the workflow must,
   therefore, be stateless, each instance of the controller is
   independent and able to determine which messages to send in order to
   activate the transactions.
 - We want to be able to set a timeout, in case the controller doesn't  
   receive any confirmation for the current working transaction.
 - Each activation of transition must be persisted on a storage layer.
 - We want to be able to define declaratively the workflow.

In this prototype we have structured the controller with the following architecture:
| Layer | Technology/approach |      
|--|--|
| Communication | asynchronous communication via Rabbitmq among microservices and the Saga controller, REST interface in Camunda|
| Business logic | Camunda as workflow engine for our BPMN flows (Spring boot is embedded)|
| Persistence | postgres for storing the sagalog |

## Communication

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
The business logic is supplied by Camunda, with Camunda we have at our disposal a workflow-engine. 
![overview](https://raw.githubusercontent.com/op-cellar-git/cellar-saga-diagram/dev/abstract.png)

#### Cellar-SEC-Workflow

#### Cellar-SEC-Timeout

##  Sagalog

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
https://www.rabbitmq.com/
https://camunda.com/
https://www.postgresql.org/

