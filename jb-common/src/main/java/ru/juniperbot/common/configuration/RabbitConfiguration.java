/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.configuration;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    public final static String QUEUE_GUILD_INFO_REQUEST = "juniperbot.guild.info.request";

    public final static String QUEUE_RANKING_UPDATE_REQUEST = "juniperbot.ranking.update.request";

    public final static String QUEUE_COMMAND_LIST_REQUEST = "juniperbot.command.list.request";

    public final static String QUEUE_STATUS_REQUEST = "juniperbot.status.request";

    public final static String QUEUE_WEBHOOK_GET_REQUEST = "juniperbot.webhook.get.request";

    public final static String QUEUE_WEBHOOK_UPDATE_REQUEST = "juniperbot.webhook.update.request";

    public final static String QUEUE_WEBHOOK_DELETE_REQUEST = "juniperbot.webhook.delete.request";

    public final static String QUEUE_PATREON_WEBHOOK_REQUEST = "juniperbot.patreon.webhook.request";

    public final static String QUEUE_CHECK_OWNER_REQUEST = "juniperbot.check.owner.request";

    public final static String QUEUE_CACHE_EVICT_REQUEST = "juniperbot.cache.evict.request";

    @Autowired
    private CommonProperties commonProperties;

    @Bean
    public ConnectionFactory connectionFactory() {
        CommonProperties.RabbitMQ rabbitMQ = commonProperties.getRabbitMQ();
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitMQ.getHostname(), rabbitMQ.getPort());
        if (rabbitMQ.getUsername() != null && rabbitMQ.getPassword() != null) {
            factory.setUsername(rabbitMQ.getUsername());
            factory.setPassword(rabbitMQ.getPassword());
        }
        return factory;
    }

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setDefaultRequeueRejected(false);
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue guildInfoRequest() {
        return new Queue(QUEUE_GUILD_INFO_REQUEST);
    }

    @Bean
    public Queue rankingUpdateRewardsQueue() {
        return new Queue(QUEUE_RANKING_UPDATE_REQUEST);
    }

    @Bean
    public Queue commandListRequest() {
        return new Queue(QUEUE_COMMAND_LIST_REQUEST);
    }

    @Bean
    public Queue statusRequest() {
        return new Queue(QUEUE_STATUS_REQUEST);
    }

    @Bean
    public Queue webhookGetRequest() {
        return new Queue(QUEUE_WEBHOOK_GET_REQUEST);
    }

    @Bean
    public Queue webhookUpdateRequest() {
        return new Queue(QUEUE_WEBHOOK_UPDATE_REQUEST);
    }

    @Bean
    public Queue webhookDeleteRequest() {
        return new Queue(QUEUE_WEBHOOK_DELETE_REQUEST);
    }

    @Bean
    public Queue patreonWebhookRequest() {
        return new Queue(QUEUE_PATREON_WEBHOOK_REQUEST);
    }

    @Bean
    public Queue checkOwnerRequest() {
        return new Queue(QUEUE_CHECK_OWNER_REQUEST);
    }

    @Bean
    public Queue cacheEvictRequest() {
        return new Queue(QUEUE_CACHE_EVICT_REQUEST);
    }
}
