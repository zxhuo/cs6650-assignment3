

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import models.PayLoad;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import redis.clients.jedis.JedisPoolConfig;

public class ResortConsumer {
    private final static String QUEUE_NAME = "SkierMQ";
    private final static int THREAD_POOL_SIZE = 3;
    private final static String RMQIP = "54.202.195.59";
    private final static int RMQPORT = 5672;
    private final static String RMQ_USER = "zhixiang";
    private final static String RMQ_PASSWORD = "123456";
    private static JedisPool pool = null;
    private final static String REDIS_HOST = "54.202.195.59";
    private final static int REDIS_PORT = 6379;

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        Gson gson = new Gson();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RMQIP);
        factory.setPort(RMQPORT);
        factory.setUsername(RMQ_USER);
        factory.setPassword(RMQ_PASSWORD);
        Connection connection = factory.newConnection();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(512);
        JedisPool pool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try(Jedis jedis = pool.getResource()) {
                    Channel channel = connection.createChannel();
                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        PayLoad payload = gson.fromJson(message, PayLoad.class);
                        String key = payload.getDayID();
                        String info = payload.getSkierID() +","+ payload.getResortID() +","+
                                payload.getSeasonID() + "," + payload.getDayID()+ "," +
                                payload.getLiftID()+ "," + payload.getTime();
                        //day - [skierID, resortID, seasonID, dayID, liftID, time]
                        jedis.rpush(key,info);

//                        “How many unique skiers visited resort X on day N?”

//                        String uniqueskier = payload.getDayID() + Integer.toString(payload.getResortID()) + "_uniqueskier";
//                        String day = payload.getDayID();
//                        int visiter = 1;
//                        if(jedis.exists(uniqueskier)){
//                            if(jedis.hexists(uniqueskier, day)){
//                                visiter = Integer.parseInt(jedis.hget(uniqueskier,day)) + 1;
//                            }
//                        }
//                        jedis.hset(uniqueskier, day, String.valueOf(visiter));

//                        “How many rides on lift N happened on day N?”

//                        String rides = payload.getDayID() + Integer.toString(payload.getLiftID()) + "_lift";
//                        String day = payload.getDayID();
//                        int totalrides = 1;
//                        if(jedis.exists(rides)){
//                            if(jedis.hexists(rides, day)){
//                                totalrides = Integer.parseInt(jedis.hget(rides,day)) + 1;
//                            }
//                        }
//                        jedis.hset(rides, day, String.valueOf(totalrides));
//                        “On day N, show me how many lift rides took place in each hour of the ski day
//                        String liftrides = payload.getDayID() + Integer.toString(payload.getLiftID()) + "_liftride";
//                        String hours = String.valueOf(payload.getTime() / 60);
//                        int hourrides = 1;
//                        if(jedis.exists(liftrides)){
//                            if(jedis.hexists(liftrides, hours)){
//                                hourrides = Integer.parseInt(jedis.hget(liftrides,hours)) + 1;
//                            }
//                        }
//                        jedis.hset(liftrides, hours, String.valueOf(hourrides));
//

                    };
                    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        List<Thread> threadPool = new ArrayList<>();
        for(int i = 0; i < THREAD_POOL_SIZE; i++){
            Thread thread = new Thread(runnable);
            thread.start();
            threadPool.add(thread);
        }

        System.out.println("[x] Connection is ready, " + THREAD_POOL_SIZE+
                " Thread waiting for messages. To exit press CTRL+C\"");
//        for(int i = 0; i < THREAD_POOL_SIZE; i++){
//            threadPool.get(i).join();
//        }
//        System.out.println("[x] Connection is close, pool will close");
//        pool.close();
//        System.out.println("[x] pool is close");
    }
}
