# cellar-saga-diagram

Approaches for implementing the Saga execution controller and for modeling the Saga diagram

## XPDL serialization
https://www.wfmc.org/standards/xpdl


## Camunda controller as a SEC
https://camunda.com/
With Camunda we have at our disposal a workflow-engine. The notation used by Camunda is BPMN-compatible
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
https://docs.camunda.org/manual/7.9/reference/rest/message/

## Open points for Camunda

 - Scalability for processes into the context of an instance of Camunda with Spring Boot
 - Service activity for sending messages?
 - Recovering from crashes: we need an extension of the Saga diagram. A superstructure?
 - Not clear the implementation of the interaction between Controller and Saga Log. Perhaps we need a custom Java activity to perform it properly

