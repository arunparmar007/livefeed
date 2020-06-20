package livefeeddisplay;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * @author arun.parmar 20-Jun-2020 livefeeddisplay
 */
public class RedisHelper {

	private static Logger logger = LoggerFactory.getLogger(RedisHelper.class);
	private static RedisHelper redisPool;
	private JedisPool jedisPool;
	private static String host;
	private HashMap<String, String> redisValues = new HashMap<>();
	private static String[] dataChangedCasesHandled;
	private static String counterKeyPattern;

	private RedisHelper() {
		initPool();
	}

	public static RedisHelper getInstance() {
		if (redisPool == null) {
			redisPool = new RedisHelper();
		}
		return redisPool;
	}

	private void initPool() {
		host = "127.0.0.1";
		counterKeyPattern = "counter_";
		dataChangedCasesHandled = "TestCounter1,TestCounter2".split(",");
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(20);
		poolConfig.setMaxIdle(10);
		poolConfig.setMinIdle(2);
		jedisPool = new JedisPool(poolConfig, host);
		new WebSocketHelper();
		for (String key : dataChangedCasesHandled) {
			key = counterKeyPattern + key;
			String value = get(key);
			value = (value == null) ? "0" : value;
			redisValues.put(key, value);
		}
		
		String processedKey = counterKeyPattern;
		String processedNumber = get(processedKey);
		processedNumber = (processedNumber == null) ? "0" : processedNumber;
		redisValues.put(processedKey, processedNumber);
	}

	public String get(String key) {
		Jedis rc = jedisPool.getResource();
		try {

			return rc.get(key);
		} catch (Exception ex) {
			logger.error("Redis Error : " + ex.getLocalizedMessage());
			return "";
		} finally {
			if (rc != null)
				rc.close();
		}
	}

	public void subscribe(String subPattern) {
		Jedis rc = jedisPool.getResource();
		try {
			rc.configSet("notify-keyspace-events", "AKE");
			rc.psubscribe(new JedisPubSub() {

				@Override
				public void onPMessage(String pattern, String channel, String message) {
					String value = get(channel);
					redisValues.put(channel, value);
					WebSocketHelper.broadcastMessage(generateBroadcastMessage());
				}

				@Override
				public void onPSubscribe(String pattern, int subscribedChannels) {
					System.out.println();
					super.onPSubscribe(pattern, subscribedChannels);
				}

			}, subPattern);
		} catch (Exception ex) {
			logger.error("Redis Error : " + ex.getLocalizedMessage());
		} finally {
			if (rc != null)
				rc.close();
		}
	}

	public void publish(String channel, String data) {
		Jedis pub = jedisPool.getResource();
		try {
			pub.select(5);
			pub.set(channel, data);
			pub.publish(channel, data);

		} catch (Exception ex) {

			System.out.println("Exception : " + ex.getMessage());
		} finally {

			if (pub != null) {
				pub.close();
			}
		}

	}

	public String generateBroadcastMessage() {
		StringBuilder message = new StringBuilder();
		String processedKey = counterKeyPattern;
		for (Map.Entry<String, String> entry : redisValues.entrySet()) {
			String key = entry.getKey().replace(counterKeyPattern, "");
			
			message.append(key).append(": ").append(entry.getValue()).append("<br />");
		}
		return message.toString();
	}

	public HashMap<String, String> getCurrentRedisCounterValues() {
		return redisValues;
	}

	public boolean closeConnection() {
		try {
			jedisPool.close();
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

}
