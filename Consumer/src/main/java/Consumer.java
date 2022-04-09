

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import models.PayLoad;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Consumer {
    private final static String QUEUE_NAME = "SkierMQ";
    private final static int THREAD_POOL_SIZE = 3;
    //private final static String RMQIP = "34.212.223.13";
    private final static String RMQIP = "54.202.195.59";
    private final static int RMQPORT = 5672;
    private final static String RMQ_USER = "zhixiang";
    private final static String RMQ_PASSWORD = "123456";
    private static JedisPool pool = null;
    private final static String REDIS_HOST = "54.202.195.59";
    private final static int REDIS_PORT = 6379;

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        Gson gson = new Gson();
        //ConcurrentHashMap<Integer, CopyOnWriteArrayList<PayLoad>> map = new ConcurrentHashMap<>();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RMQIP);
        factory.setPort(RMQPORT);
        factory.setUsername(RMQ_USER);
        factory.setPassword(RMQ_PASSWORD);
        Connection connection = factory.newConnection();
//        JedisPoolConfig poolConfig = new JedisPoolConfig();
//        poolConfig.setMaxTotal(512);
//        pool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
        pool = new JedisPool(REDIS_HOST, REDIS_PORT);


        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try(Jedis jedis = pool.getResource()) {
                    Channel channel = connection.createChannel();
                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        PayLoad payload = gson.fromJson(message, PayLoad.class);
                        String key = Integer.toString(payload.getSkierID());
                        String info = payload.getSkierID() + "," + payload.getResortID() + "," +
                                payload.getSeasonID() + "," + payload.getDayID() + "," +
                                payload.getLiftID() + "," + payload.getTime();
                        // SkierID - [skierID, resortID, seasonID, dayID, liftID, time]
                        jedis.rpush(key,info);
                        //SkierID_totalday - totalday
//                      String totalday_skier = Integer.toString(payload.getSkierID()) + "_totalday";
//                      int totalday = 1;
//                      if(!jedis.exists(totalday_skier)){
//                          totalday = Integer.parseInt(jedis.get(totalday_skier)) + 1;
//                      }
//                      jedis.set(totalday_skier,String.valueOf(totalday));
//
//                      //SkierID_totalvertical,dayID - totalVerticalNumber
//                      String totalVertical =  Integer.toString(payload.getSkierID()) + "_totalvertical";
//                      String fields = payload.getDayID();
//                      int total_num = 1;
//                      if(jedis.exists(totalVertical)){
//                          if(jedis.hexists(totalVertical, fields)){
//                              total_num = Integer.parseInt(jedis.hget(totalVertical,fields)) + payload.getLiftID() * 10;
//                          }
//                      }
//                      jedis.hset(totalVertical, fields, String.valueOf(total_num));
//                      //SkierID_totalvertical,dayID - totalVerticalNumber
//                      String totallifts =  Integer.toString(payload.getSkierID()) + "_totallifts";
//                      String lift_fields = payload.getDayID();
//                      int total_lift = 1;
//                      if(jedis.exists(totallifts)){
//                          if(jedis.hexists(totallifts, lift_fields)){
//                              total_lift = Integer.parseInt(jedis.hget(totallifts,lift_fields)) + 1;
//                          }
//                      }
//                      jedis.hset(totallifts, lift_fields, String.valueOf(total_lift));
                    };
                    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
                    });

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
//
//    public static void saveData(PayLoad payload){
//        // SkierID - [resortID, seasonID, dayID, liftID, time]
//        String key =  Integer.toString(payload.getSkierID());
//
//        String info = payload.getSkierID() +","+ payload.getResortID() +","+
//                payload.getSeasonID() + "," + payload.getDayID()+ "," +
//                payload.getLiftID()+ "," + payload.getTime();
//        System.out.println(info);
//        try (Jedis jedis = pool.getResource()) {
//
//        }
//        }catch (Exception e) {
////        try {
////            jedis = pool.getResource();
//////            jedis.lpush(key,info);
//////            System.out.println(jedis.get(key));
////        }catch (Exception e) {
//            e.printStackTrace();
//            if (null != jedis) {
//                pool.returnBrokenResource(jedis);
//                jedis = null;
//            }
//        } finally {
//            if (jedis != null) {
//                jedis.close();
//            }
//        }
    public static void savedata(String key, String info){
        Jedis jedis = pool.getResource();
        try{
            jedis.rpush(key,info);
        }catch (JedisException e) {
            //if something wrong happen, return it back to the pool
            if (null != jedis) {
                pool.returnBrokenResource(jedis);
                jedis = null;
            }
        } finally {
            ///it's important to return the Jedis instance to the pool once you've finished using it
            if (null != jedis)
                pool.returnResource(jedis);
        }
    }
}
