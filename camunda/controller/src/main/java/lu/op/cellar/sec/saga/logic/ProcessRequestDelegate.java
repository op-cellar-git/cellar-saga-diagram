package lu.op.cellar.sec.saga.logic;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import lu.op.cellar.sec.saga.utilities.SagalogUtilities;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class ProcessRequestDelegate implements JavaDelegate {

	private final static Logger LOGGER = Logger.getLogger("PROCESS REQUEST DELEGATE");

	public void execute(DelegateExecution execution) throws Exception {

		LOGGER.info("PROCESS REQUEST DELEGATE:");
		LOGGER.info("execID               :"+execution.getId());
		LOGGER.info("activityName         :"+execution.getCurrentActivityName());
		LOGGER.info("businessKey          :"+execution.getBusinessKey());
		LOGGER.info("activityId           :"+execution.getCurrentActivityId());
		LOGGER.info("var#msg              :"+execution.getVariable("msg").toString());
		LOGGER.info("var#msg              :"+execution.getVariable("msg_params").toString());
		LOGGER.info("var#state            :"+execution.getVariable("state").toString());
		LOGGER.info("var#compensation     :"+execution.getVariable("compensation").toString());
		LOGGER.info("var#trace            :"+execution.getVariable("trace").toString());
		LOGGER.info("var#new            :"+execution.getVariable("new").toString());

		String activity= execution.getVariable("trace").toString();
		if(!(execution.getCurrentActivityName().contentEquals("end"))) {
			activity= UUID.randomUUID().toString();
			TimeUnit.MILLISECONDS.sleep(25);
			LOGGER.info("SENDING MESSAGE VIA RABBITMQ");
		}
		LOGGER.info("var#activity         :"+activity);
		
		String params = "";
		try { 
			params=execution.getVariable("params").toString();
		} catch(java.lang.NullPointerException e) { params = "null"; }
		
		
		SagalogUtilities.writeRecord(execution.getVariable("trace").toString(), execution.getVariable("new").toString(), new Boolean(execution.getVariable("compensation").toString()), activity, params);

	}

}
