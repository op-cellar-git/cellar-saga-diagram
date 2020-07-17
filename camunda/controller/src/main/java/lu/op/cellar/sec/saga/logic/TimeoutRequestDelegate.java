package lu.op.cellar.sec.saga.logic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import lu.op.cellar.sec.saga.utilities.SagalogUtilities;

//https://docs.camunda.org/get-started/java-process-app/service-task/
public class TimeoutRequestDelegate implements JavaDelegate {

	private final static Logger LOGGER = Logger.getLogger("TIMEOUT REQUEST DELEGATE:");

	public void execute(DelegateExecution execution) throws Exception {

		LOGGER.info("TIMEOUT REQUEST DELEGATE:");
		LOGGER.info("execID               :"+execution.getId());
		LOGGER.info("activityName         :"+execution.getCurrentActivityName());
		LOGGER.info("businessKey          :"+execution.getBusinessKey());
		LOGGER.info("activityId           :"+execution.getCurrentActivityId());
		LOGGER.info("var#threshold        :"+execution.getVariable("threshold").toString());
		List<String> list = SagalogUtilities.timeouts("'"+execution.getCurrentActivityName()+"%'", "'"+execution.getVariable("threshold").toString()+"'");
		LOGGER.info("result               :"+list);
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		for (String result:list) {
			String r[]=result.split("\\|");
			Map<String, Object> mapVars=new HashMap<String, Object>();
			mapVars.put("msg","Fail");
			//LOGGER.info(r[0]+"|"+r[1]);
			mapVars.put("trace",r[0]);
			mapVars.put("activity",r[1]);
			processEngine.getRuntimeService().startProcessInstanceByMessage("start", mapVars);
			//in case we can use rabbitmq here insted of sending messages via the Camunda interface
		}
	}
}