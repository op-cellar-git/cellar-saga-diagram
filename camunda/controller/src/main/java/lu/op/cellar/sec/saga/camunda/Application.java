package lu.op.cellar.sec.saga.camunda;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
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

	public static void start(ProcessEngine processEngine, String msg) {
		Map<String, Object> mapVars=new HashMap<String, Object>();
		mapVars.put("msg",msg);
		processEngine.getRuntimeService().startProcessInstanceByKey("Process_0neapjn", "123", mapVars);
	}

	public static void main(String[] args) throws IOException, TimeoutException {
		SpringApplication.run(Application.class);
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

		
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
            Application.start(processEngine, "PrelockValidationEnd");
        };
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
		

		


	}

}