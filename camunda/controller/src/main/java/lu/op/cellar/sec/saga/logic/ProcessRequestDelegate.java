package lu.op.cellar.sec.saga.logic;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class ProcessRequestDelegate implements JavaDelegate {

  private final static Logger LOGGER = Logger.getLogger("LOAN-REQUESTS");

  public void execute(DelegateExecution execution) throws Exception {

	    URL url = new URL("http://localhost:3000/message");
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
	        for (String line; (line = reader.readLine()) != null;) {
	            System.out.println(line);
	        }
	    }
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
