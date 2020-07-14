package lu.op.cellar.sec.saga.logic;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class DatabaseRequestDelegate implements JavaDelegate {

  private final static Logger LOGGER = Logger.getLogger("DATABASE REQUEST DELEGATE");

  public void execute(DelegateExecution execution) throws Exception {


    LOGGER.info("DATABASE REQUEST DELEGATE:");
    LOGGER.info("execID               :"+execution.getId());
    LOGGER.info("activityName         :"+execution.getCurrentActivityName());
    LOGGER.info("businessKey          :"+execution.getBusinessKey());
    LOGGER.info("activityId           :"+execution.getCurrentActivityId());
    
    
    String msg = execution.getVariable("msg").toString();
    if(msg.equals("startup")) {
    	execution.setVariableLocal("state", "");
    	execution.setVariableLocal("trace", execution.getId().toString());
    	execution.setVariableLocal("compensation", false); 
    }
    else {
    	String[] r = SagalogUtilities.retrieveState(execution.getVariable("trace").toString());
    	execution.setVariableLocal("state", r[0]);
    	execution.setVariableLocal("compensation", new Boolean(r[1]));    	
    }

    LOGGER.info("var#msg           :"+execution.getVariable("msg").toString());
    LOGGER.info("var#state         :"+execution.getVariable("state").toString());
    LOGGER.info("var#compensation  :"+execution.getVariable("compensation").toString());
    LOGGER.info("var#trace         :"+execution.getVariable("trace").toString());
    SagalogUtilities.writeRecord(execution.getVariable("trace").toString(), execution.getVariable("msg").toString(), new Boolean(execution.getVariable("compensation").toString()));
    
    
  }

}
