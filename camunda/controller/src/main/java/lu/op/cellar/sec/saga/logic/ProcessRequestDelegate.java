package lu.op.cellar.sec.saga.logic;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class ProcessRequestDelegate implements JavaDelegate {

	private final static Logger LOGGER = Logger.getLogger("PROCESS REQUEST DELEGATE");

	public void execute(DelegateExecution execution) throws Exception {

		//	    URL url = new URL("http://localhost:3000/message");
		//	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
		//	        for (String line; (line = reader.readLine()) != null;) {
		//	            System.out.println(line);
		//	        }
		//	    }
		LOGGER.info("PROCESS REQUEST DELEGATE:");
		LOGGER.info("execID               :"+execution.getId());
		LOGGER.info("activityName         :"+execution.getCurrentActivityName());
		LOGGER.info("businessKey          :"+execution.getBusinessKey());
		LOGGER.info("activityId           :"+execution.getCurrentActivityId());


		LOGGER.info("var#msg           :"+execution.getVariable("msg").toString());
		LOGGER.info("var#state         :"+execution.getVariable("state").toString());
		LOGGER.info("var#compensation  :"+execution.getVariable("compensation").toString());
		LOGGER.info("var#trace         :"+execution.getVariable("trace").toString());
		
		LOGGER.info("SENDING MESSAGE VIA RABBITMQ");
		SagalogUtilities.writeRecord(execution.getVariable("trace").toString(), execution.getCurrentActivityName(), new Boolean(execution.getVariable("compensation").toString()));
	}

}
