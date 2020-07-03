package lu.op.cellar.sec.saga.logic;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class ProcessRequestDelegate implements JavaDelegate {

  private final static Logger LOGGER = Logger.getLogger("LOAN-REQUESTS");

  public void execute(DelegateExecution execution) throws Exception {
    LOGGER.info("----------------------------------------------------------");
    LOGGER.info("execID               :"+execution.getId());
    LOGGER.info("activityName         :"+execution.getCurrentActivityName());
    LOGGER.info("businessKey          :"+execution.getBusinessKey());
    LOGGER.info("variable             :"+execution.getVariable("last"));
    LOGGER.info("activityId           :"+execution.getCurrentActivityId());
    LOGGER.info("variableScopeKey     :"+execution.getVariableScopeKey());
    LOGGER.info("----------------------------------------------------------");
    //execution.setProcessBusinessKey(execution.getId());
    
    
    
  }

}
