package lu.op.cellar.sec.saga.logic;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import lu.op.cellar.sec.saga.utilities.SagalogUtilities;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class DatabaseRequestDelegate implements JavaDelegate {

  private final static Logger LOGGER = Logger.getLogger("DATABASE REQUEST DELEGATE");

  public void execute(DelegateExecution execution) throws Exception {


    LOGGER.info("DATABASE REQUEST DELEGATE:");
    LOGGER.info("execID               :"+execution.getId());
    LOGGER.info("activityName         :"+execution.getCurrentActivityName());
    LOGGER.info("businessKey          :"+execution.getBusinessKey());
    LOGGER.info("activityId           :"+execution.getCurrentActivityId());
    LOGGER.info("trace                :"+execution.getVariable("trace").toString()+"|"+execution.getVariable("activity").toString());
    
    
    String msg = execution.getVariable("msg").toString();
    if(msg.equals("Fail")) {
    	String[] r = SagalogUtilities.retrieveState(execution.getVariable("trace").toString(), execution.getVariable("activity").toString());
    	execution.setVariableLocal("state", r[0]);
    	execution.setVariableLocal("compensation", true); 
    }
    else if(msg.equals("Startup")) {
    	execution.setVariable("state", "");
    	execution.setVariable("trace", execution.getId().toString());
    	execution.setVariable("activity", execution.getId().toString());
    	execution.setVariable("compensation", false); 
    }
    else {
    	String[] r = SagalogUtilities.retrieveState(execution.getVariable("trace").toString(), execution.getVariable("activity").toString());
    	execution.setVariableLocal("state", r[0]);
    	String c=r[1];
    	if(c.equals("t")) c = "true";
    	else if(c.equals("f")) c = "false";
    	execution.setVariableLocal("compensation", new Boolean(c));  
    	execution.setVariableLocal("state_params", r[2]);
    	LOGGER.info("var#state_params        :"+execution.getVariable("state_params").toString());
    }

    LOGGER.info("var#msg           :"+execution.getVariable("msg").toString());
    LOGGER.info("var#msg_params    :"+execution.getVariable("msg_params").toString());
    LOGGER.info("var#state         :"+execution.getVariable("state").toString());
    LOGGER.info("var#compensation  :"+execution.getVariable("compensation").toString());
    LOGGER.info("var#trace         :"+execution.getVariable("trace").toString());
    LOGGER.info("var#activity      :"+execution.getVariable("activity").toString());
    SagalogUtilities.writeRecord(execution.getVariable("trace").toString(), execution.getVariable("msg").toString(), new Boolean(execution.getVariable("compensation").toString()), execution.getVariable("activity").toString(), execution.getVariable("msg_params").toString());
    
  }
}
