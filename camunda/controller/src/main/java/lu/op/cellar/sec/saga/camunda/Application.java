package lu.op.cellar.sec.saga.camunda;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

@SpringBootApplication
public class Application {

	private final static String QUEUE_NAME = "hello";
	private final static Logger LOGGER = Logger.getLogger("MESSAGE HANDLER");

	public static void start(ProcessEngine processEngine, String corpus) {
		Map<String, Object> mapVars=new HashMap<String, Object>();
		String[] corp = corpus.split(" ");
		mapVars.put("msg",corp[0]);
		mapVars.put("trace",corp[1]);
		mapVars.put("activity",corp[2]);
		processEngine.getRuntimeService().startProcessInstanceByMessage("start", mapVars);
	}

	public static void main(String[] args) throws IOException, TimeoutException {
		SpringApplication.run(Application.class);
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		LOGGER.info("Rabbitmq: Waiting for messages");

		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			String corpus = new String(delivery.getBody(), "UTF-8");
			LOGGER.info("Rabbitmq: Message received '" + corpus + "'");
			try {
				Application.start(processEngine, corpus);
			} catch (Exception e ) { e.printStackTrace(); }
		};
		channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
	}

}