package lu.op.cellar.sec.saga.logic;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class TimeoutRequestDelegate implements JavaDelegate {

	private final static Logger LOGGER = Logger.getLogger("TIMEOUT REQUEST DELEGATE:");

	public void execute(DelegateExecution execution) throws Exception {

		LOGGER.info("TIMEOUT REQUEST DELEGATE:");
		LOGGER.info("execID               :"+execution.getId());
		LOGGER.info("activityName         :"+execution.getCurrentActivityName());
		LOGGER.info("businessKey          :"+execution.getBusinessKey());
		LOGGER.info("activityId           :"+execution.getCurrentActivityId());
		LOGGER.info("var#threshold          :"+execution.getVariable("threshold").toString());

		LOGGER.info("SENDING MESSAGE VIA RABBITMQ");
		
	}

}