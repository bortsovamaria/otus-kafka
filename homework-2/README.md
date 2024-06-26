### 1. Запускаем Kafka с Kraft

#### 1.1 Генерируем UUID кластера

    docker run --rm --name cp-kafka-1 confluentinc/cp-kafka:7.5.3 kafka-storage random-uuid

    D9XnfxvpRqadqdEWAI1zug

#### 1.2 Запуск Kafka кластера с Kraft

Используя UUID из предыдущего шага, формируем docker-compose.yml для поднятия кластера Kafka с Kraft:

     cd homework-2/plain/docker/
     docker-compose up -d
![image](plain\kafka-kraft.PNG)

### 2. Настройка аутентификацию SASL/PLAIN + авторизация
    Удаляем контейнер из предыдущего шага

#### 2.1 Создаем конфигурационные файлы
    Создаем файлы ".properties" для аутенификации пользователей "admin" "userA" "userB" "userC",
    а также "jaas" конфиг для Kafka:

        ./sasl_plain/client.admin.properties
        
        ./sasl_plain/client.userA.properties
        
        ./sasl_plain/client.userB.properties
        
        ./sasl_plain/client.userC.properties
        
        ./sasl_plain/kafka.jaas.conf

#### 2.3 Запускаем кластер Kafka c Kraft + SASL_PLAIN:
    cd sasl-plain/docker
    docker-compose up -d
![image](sasl_plain\kafka_kraft_sasl_plain.PNG)

### 3. Настройка авторизации

#### 3.2 Создаем топик
     docker run --rm --network=sasl_plain_default -v ./client.admin.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-topics --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --create --replication-factor 3 --partitions 5 --topic my_topic
        Created topic home.

    docker run --rm --network=sasl_plain_default -v ./client.admin.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-topics --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --list
        home

#### 3.3 Выдаем права "write" для userA, права "read" для userC
    docker run --rm --network=sasl_plain_default -v ./client.admin.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-acls --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --add --allow-principal User:userA --operation WRITE --topic my_topic

    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=my_topic, patternType=LITERAL)`: 
        (principal=User:userA, host=*, operation=WRITE, permissionType=ALLOW) 

    Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=my_topic, patternType=LITERAL)`: 
        (principal=User:userA, host=*, operation=WRITE, permissionType=ALLOW) 

    docker run --rm --network=sasl_plain_default -v ./client.admin.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-acls --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --add --allow-principal User:userB --operation READ --topic my_topic --group "my_group"

    Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=my_topic, patternType=LITERAL)`: 
        (principal=User:userB, host=*, operation=READ, permissionType=ALLOW) 
    
    Adding ACLs for resource `ResourcePattern(resourceType=GROUP, name=my_group, patternType=LITERAL)`: 
        (principal=User:userB, host=*, operation=READ, permissionType=ALLOW) 
    
    Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=my_topic, patternType=LITERAL)`: 
        (principal=User:userB, host=*, operation=READ, permissionType=ALLOW)
        (principal=User:userA, host=*, operation=WRITE, permissionType=ALLOW) 
    
    Current ACLs for resource `ResourcePattern(resourceType=GROUP, name=my_group, patternType=LITERAL)`: 
        (principal=User:userB, host=*, operation=READ, permissionType=ALLOW) 

### 4. Проверка авторизации для каждого топика

#### 4.1 Получение списка топикоп:
    docker run --rm --network=sasl_plain_default -v ./client.userA.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-topics --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --list
        my_topic
 
    docker run --rm --network=sasl_plain_default -v ./client.userB.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-topics --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --list
        my_topic
 
     docker run --rm --network=sasl_plain_default -v ./client.userC.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-topics --command-config /client.properties --bootstrap-server 192.168.0.61:39092 --list


#### 4.2 Запись сообщений в топик:
    1) Пользователь userA:
    docker run -it --rm --network=sasl_plain_default -v ./client.userA.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-console-producer --producer.config /client.properties --bootstrap-server 192.168.0.61:39091 --topic=my_topic
        >m1
        >m2
        >m3
        >m4
        >m5

    2) Пользователь userB:
    docker run -it --rm --network=sasl_plain_default -v ./client.userB.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-console-producer --producer.config /client.properties --bootstrap-server 192.168.0.61:39091 --topic=my_topic
    >b1
    [2024-06-26 20:23:20,117] ERROR [Producer clientId=console-producer] Aborting producer batches due to fatal error (org.apache.kafka.clients.producer.internals.Sender)
    org.apache.kafka.common.errors.ClusterAuthorizationException: Cluster authorization failed.
    [2024-06-26 20:23:20,119] ERROR Error when sending message to topic my_topic with key: null, value: 2 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
    org.apache.kafka.common.errors.ClusterAuthorizationException: Cluster authorization failed.
    org.apache.kafka.common.KafkaException: Cannot execute transactional method because we are in an error state
        at org.apache.kafka.clients.producer.internals.TransactionManager.maybeFailWithError(TransactionManager.java:1010)
        at org.apache.kafka.clients.producer.internals.TransactionManager.maybeAddPartition(TransactionManager.java:328)
        at org.apache.kafka.clients.producer.KafkaProducer.doSend(KafkaProducer.java:1061)
        at org.apache.kafka.clients.producer.KafkaProducer.send(KafkaProducer.java:962)
        at kafka.tools.ConsoleProducer$.send(ConsoleProducer.scala:117)
        at kafka.tools.ConsoleProducer$.loopReader(ConsoleProducer.scala:90)
        at kafka.tools.ConsoleProducer$.main(ConsoleProducer.scala:99)
        at kafka.tools.ConsoleProducer.main(ConsoleProducer.scala)
    Caused by: org.apache.kafka.common.errors.ClusterAuthorizationException: Cluster authorization failed.

    3) Пользователь userC:
    docker run -it --rm --network=sasl_plain_default -v ./client.userC.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-console-producer --producer.config /client.properties --bootstrap-server 192.168.0.61:39091 --topic=my_topic
    >c1
    [2024-06-26 20:23:36,996] WARN [Producer clientId=console-producer] Error while fetching metadata with correlation id 4 : {my_topic=TOPIC_AUTHORIZATION_FAILED} (org.apache.kafka.clients.NetworkClient)
    [2024-06-26 20:23:37,003] ERROR [Producer clientId=console-producer] Topic authorization failed for topics [my_topic] (org.apache.kafka.clients.Metadata)
    [2024-06-26 20:23:37,006] ERROR Error when sending message to topic my_topic with key: null, value: 2 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
    org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [my_topic]


#### 4.3 Чтение сообщений из топика:
    1) Пользователь userA:
        docker run -it --rm --network=sasl_plain_default -v ./client.userA.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-console-consumer --consumer.config /client.properties --bootstrap-server 192.168.0.61:39092 --topic=hmy_topic --group "my_group" --from-beginning
        [2024-06-26 20:32:27,434] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
        org.apache.kafka.common.errors.GroupAuthorizationException: Not authorized to access group: my_group
        Processed a total of 0 messages
    2) Пользователь userB:
        docker run -it --rm --network=sasl_plain_default -v ./client.userB.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-console-consumer --consumer.config /client.properties --bootstrap-server 192.168.0.61:39092 --topic=my_topic --group "my_group" --from-beginning
        m1
        m2
        m3
        m4
        m5
    3) Пользователь userC:
        docker run -it --rm --network=sasl_plain_default -v ./client.userC.properties:/client.properties confluentinc/cp-kafka:7.5.3 kafka-console-consumer --consumer.config /client.properties --bootstrap-server 192.168.0.61:39092 --topic=my_topic --group "my_group" --from-beginning
        [2024-06-26 20:32:57,399] WARN [Consumer clientId=console-consumer, groupId=my_group] Error while fetching metadata with correlation id 2 : {my_topic=TOPIC_AUTHORIZATION_FAILED} (org.apache.kafka.clients.NetworkClient)
        [2024-06-26 20:32:57,400] ERROR [Consumer clientId=console-consumer, groupId=my_group] Topic authorization failed for topics [my_topic] (org.apache.kafka.clients.Metadata)
        [2024-06-26 20:32:57,401] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
        org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [my_topic]
        Processed a total of 0 messages