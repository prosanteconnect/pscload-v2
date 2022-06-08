/*
 * Copyright A.N.S 2021
 */

package fr.ans.psc.pscload.service;

import com.google.gson.Gson;
import fr.ans.psc.pscload.model.entities.Professionnel;
import fr.ans.psc.pscload.model.operations.OperationType;
import static fr.ans.psc.rabbitmq.conf.PscRabbitMqConfiguration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPsMessage(Professionnel professionnel, OperationType operation) {
        log.debug("Sending message for Ps {}", professionnel.getNationalId());

        String routingKey;
        switch (operation) {
            case CREATE:
                routingKey = PS_CREATE_MESSAGES_QUEUE_ROUTING_KEY;
                break;
            case DELETE:
                routingKey = PS_DELETE_MESSAGES_QUEUE_ROUTING_KEY;
                break;
            case UPDATE:
                routingKey = PS_UPDATE_MESSAGES_QUEUE_ROUTING_KEY;
                break;
            default:
                routingKey = "";
                break;
        }

        Gson json = new Gson();
        try {
            rabbitTemplate.convertAndSend(EXCHANGE_MESSAGES, routingKey, json.toJson(professionnel));
        } catch (AmqpException e) {
            log.error("Error occurred when sending Ps {} informations to queue manager", professionnel.getNationalId());
            e.printStackTrace();
        }
    }
}
