package org.redis;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class RedisMapTest {

    private RedisMap redisMap;

    @Container
    private static final GenericContainer<?> REDIS_CONTAINERS = new GenericContainer<>("redis:7.4")
            .withExposedPorts(6379);

    private static String redisHost;
    private static int redisPort;

    @BeforeAll
    static void setUpContainer() {
        redisHost = REDIS_CONTAINERS.getHost();
        redisPort = REDIS_CONTAINERS.getFirstMappedPort();
    }

    @BeforeEach
    void setUp() {
        redisMap = new RedisMap(redisHost, redisPort);
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.flushDB();
        }
    }

    @AfterEach
    void tearDown() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.flushDB();
        }
    }

    @Test
    @DisplayName("A test for the size of a database that does not contain a value")
    void testSize_Empty() {
        assertEquals(0, redisMap.size());
    }

    @Test
    @DisplayName("A test for the size of a database containing values")
    void testSize_NonEmpty() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
            jedis.set("key2", "value2");
        }
        assertEquals(2, redisMap.size());
    }

    @Test
    @DisplayName("The database is empty")
    void testIsEmpty_Empty() {
        assertTrue(redisMap.isEmpty());
    }

    @Test
    @DisplayName("The database is not empty")
    void testIsEmpty_NonEmpty() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
        }
        assertFalse(redisMap.isEmpty());
    }

    @Test
    @DisplayName("The database contains the transmitted key")
    void testContainsKey_ExistingKey() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
        }
        assertTrue(redisMap.containsKey("key1"));
    }

    @Test
    @DisplayName("The database contains a non-transferable key")
    void testContainsKey_NonExistingKey() {
        assertFalse(redisMap.containsKey("key1"));
    }

    @Test
    @DisplayName("Null values are passed to verify the existence of the key")
    void testContainsKey_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> redisMap.containsKey(null));
    }

    @Test
    @DisplayName("Checking the existence of a value by key")
    void testContainsValue_ExistingValue() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
        }
        assertTrue(redisMap.containsValue("value1"));
    }

    @Test
    @DisplayName("Checking the existence of a value for a non-existent key")
    void testContainsValue_NonExistingValue() {
        assertFalse(redisMap.containsValue("value2"));
    }

    @Test
    @DisplayName("Checking the existence of a value by a null key")
    void testContainsValue_NullValue() {
        assertFalse(redisMap.containsValue(null));
    }

    @Test
    @DisplayName("Getting a value using an existing key")
    void testGet_ExistingKey() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
        }
        assertEquals("value1", redisMap.get("key1"));
    }

    @Test
    @DisplayName("Getting a value using a non-existing key")
    void testGet_NonExistingKey() {
        assertNull(redisMap.get("key1"));
    }

    @Test
    @DisplayName("Getting a value using a null key")
    void testGet_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> redisMap.get(null));
    }

    @Test
    @DisplayName("Inserting a new key-value pair")
    void testPut_NewKey() {
        assertNull(redisMap.put("key1", "value1"));
        String value = "";
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            value = jedis.get("key1");
        }
        assertEquals("value1", value);
    }

    @Test
    @DisplayName("Inserting a key-value pair with an existing key")
    void testPut_ExistingKey() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
        }
        assertEquals("value1", redisMap.put("key1", "value2"));

        String value = "";
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            value = jedis.get("key1");
        }
        assertEquals("value2", value);
    }

    @Test
    @DisplayName("Inserting a key-value pair with a null key")
    void testPut_NullKey() {
        assertThrows(IllegalArgumentException.class, () -> redisMap.put(null, "value1"));
    }

    @Test
    @DisplayName("Inserting a key-value pair with a null value")
    void testPut_NullValue() {
        assertThrows(IllegalArgumentException.class, () -> redisMap.put("key1", null));
    }

    @Test
    @DisplayName("Deleting an existing record")
    void testRemove_ExistingKey() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
        }
        assertEquals("value1", redisMap.remove("key1"));

        boolean isExists = true;
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            isExists = jedis.exists("key1");
        }
        assertFalse(isExists);
    }

    @Test
    @DisplayName("Deleting a non-existing record")
    void testRemove_NonExistingKey() {
        assertNull(redisMap.remove("key1"));
    }

    @Test
    @DisplayName("Deleting an entry with a null key")
    void testRemove_nullKey() {
        assertThrows(IllegalArgumentException.class, () -> redisMap.remove(null));
    }

    @Test
    @DisplayName("Inserting multiple records")
    void testPutAll_Success() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        redisMap.putAll(map);

        int size = 0;
        String value1 = "";
        String value2 = "";
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            size = (int) jedis.dbSize();
            value1 = jedis.get("key1");
            value2 = jedis.get("key2");
        }

        assertEquals(2, size);
        assertEquals("value1", value1);
        assertEquals("value2", value2);
    }

    @Test
    @DisplayName("Attempt to insert null")
    void testPutAll_NullMap() {
        assertThrows(IllegalArgumentException.class, () -> redisMap.putAll(null));
    }

    @Test
    @DisplayName("An attempt to insert a record with the passed null in the key or value")
    void testPutAll_NullKeyOrValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", null);
        assertThrows(IllegalArgumentException.class, () -> redisMap.putAll(map));
    }

    @Test
    @DisplayName("Clearing the database")
    void testClear_Success() {
        int currSize = 0;
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
            jedis.set("key2", "value2");
            currSize = (int) jedis.dbSize();
        }
        assertEquals(2, currSize);

        redisMap.clear();

        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            currSize = (int) jedis.dbSize();
        }
        assertEquals(0, currSize);
    }

    @Test
    @DisplayName("Get a list of all keys")
    void testKeySet_Success() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
            jedis.set("key2", "value2");
        }

        Set<String> keySet = redisMap.keySet();

        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("key1"));
        assertTrue(keySet.contains("key2"));
    }

    @Test
    @DisplayName("Get a list of all values")
    void testValues_Success() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
            jedis.set("key2", "value2");
        }

        Collection<String> values = redisMap.values();

        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    @DisplayName("Get the contents of the database")
    void testEntrySet_Success() {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            jedis.set("key1", "value1");
            jedis.set("key2", "value2");
        }

        Set<Map.Entry<String, String>> entrySet = redisMap.entrySet();
        assertEquals(2, entrySet.size());

        Map<String, String> entries = entrySet.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("value1", entries.get("key1"));
        assertEquals("value2", entries.get("key2"));
    }

    @Test
    @DisplayName("Error when establishing a database connection")
    void testConnectionFailure() {
        RedisMap badRedisMap = new RedisMap("invalid-host", 9999);
        assertThrows(JedisException.class, badRedisMap::size);
    }
}
