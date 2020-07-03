
# cellar-saga-diagram

Approaches for implementing the Saga execution controller and for modeling the Saga diagram

## XPDL serialization
https://www.wfmc.org/standards/xpdl


## Camunda controller as a SEC
https://camunda.com/
With Camunda we have at our disposal a workflow-engine. The notation used by Camunda is BPMN-compatible,
for sending messages:
```
{
  "messageName" : "aMessage",
  "businessKey" : "aBusinessKey",
  "correlationKeys" : {
    "aVariable" : {"value" : "aValue", "type": "String"}
  },
  "processVariables" : {
    "aVariable" : {"value" : "aNewValue", "type": "String", 
                    "valueInfo" : { "transient" : true } },
    "anotherVariable" : {"value" : true, "type": "Boolean"}
  }
}
```
or
```
{
  "messageName" : "aMessage",
  "businessKey" : "traceID",
}
```
we are free to use the instanceId of the workflow instance instead of the specific BusinessId (that it is not mandatory)
```
{
  "messageName" : "CleanUpEnd",
  "instanceId" : "a1e0b61e-bd3b-11ea-9536-d89c67c87228"
}
```
https://docs.camunda.org/manual/7.9/reference/rest/message/

## Infrastructural open points for the Camunda workflow-engine

 - Scalability for processes into the context of an instance of Camunda with Spring Boot
 - General architecture for handling a pool of Camunda workflow-engines 
 - ~~Recovering from crashes: we need an extension of the Saga diagram. A superstructure?~~
 - Who is responsible for executing instances of the BPMN-workflow

## Implementation open points for the Camunda workflow-engine

 - Interaction with the Saga Log (implementation of a persistence layer)
 - sync APIs vs async APIs
