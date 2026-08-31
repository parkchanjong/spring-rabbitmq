// 비디오 알림용 RabbitMQ Exchange, Queue, 재시도 처리를 구성한다.
package dev.backend.rabbitmq_notification.notification;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class NotificationRabbitConfig {

	public static final String VIDEO_EVENT_EXCHANGE = "video.events";
	public static final String VIDEO_CREATED_ROUTING_KEY = "video.created";
	public static final String VIDEO_NOTIFICATION_QUEUE = "video.notification";
	public static final String VIDEO_EVENT_DLX = "video.events.dlx";
	public static final String VIDEO_NOTIFICATION_DLQ = "video.notification.dlq";
	public static final String VIDEO_NOTIFICATION_DLQ_ROUTING_KEY = "video.notification.failed";

	@Bean
	DirectExchange videoEventExchange() {
		return new DirectExchange(VIDEO_EVENT_EXCHANGE, true, false);
	}

	@Bean
	Queue videoNotificationQueue() {
		return new Queue(VIDEO_NOTIFICATION_QUEUE, true);
	}

	@Bean
	Binding videoCreatedBinding(Queue videoNotificationQueue, DirectExchange videoEventExchange) {
		return BindingBuilder.bind(videoNotificationQueue)
				.to(videoEventExchange)
				.with(VIDEO_CREATED_ROUTING_KEY);
	}

	@Bean
	DirectExchange videoEventDeadLetterExchange() {
		return new DirectExchange(VIDEO_EVENT_DLX, true, false);
	}

	@Bean
	Queue videoNotificationDeadLetterQueue() {
		return new Queue(VIDEO_NOTIFICATION_DLQ, true);
	}

	@Bean
	Binding videoNotificationDeadLetterBinding(
			Queue videoNotificationDeadLetterQueue,
			DirectExchange videoEventDeadLetterExchange
	) {
		return BindingBuilder.bind(videoNotificationDeadLetterQueue)
				.to(videoEventDeadLetterExchange)
				.with(VIDEO_NOTIFICATION_DLQ_ROUTING_KEY);
	}

	@Bean
	MessageConverter rabbitMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	SimpleRabbitListenerContainerFactory notificationRabbitListenerContainerFactory(
			ConnectionFactory connectionFactory,
			MessageConverter rabbitMessageConverter,
			RabbitTemplate rabbitTemplate
	) {
		RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
				rabbitTemplate,
				VIDEO_EVENT_DLX,
				VIDEO_NOTIFICATION_DLQ_ROUTING_KEY
		);
		Advice retryInterceptor = RetryInterceptorBuilder.stateless()
				.maxRetries(2)
				.backOffOptions(1_000, 2, 4_000)
				.recoverer(recoverer)
				.build();
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(rabbitMessageConverter);
		factory.setConcurrentConsumers(3);
		factory.setDefaultRequeueRejected(false);
		factory.setAdviceChain(retryInterceptor);
		return factory;
	}
}
