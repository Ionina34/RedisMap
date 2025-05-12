package org.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс {@code RedisMap} реализует интерфейс {@link Map<String, String>} для взаимодействия с Redis.
 * Все операции с ключами и значениями выполняются через библиотеку Jedis, обеспечивая хранение данных
 * в Redis как пар строкового ключа и значения.
 *
 * <p>
 * Класс предоставляет потокобезопасный доступ к Redis через подключение, создаваемое для каждой операции,
 * и автоматически закрывает его после выполнения. Поддерживает все стандартные операции {@code Map},
 * включая добавление, удаление, получение и итерацию по ключам и значениям.
 * </p>
 *
 * @since 1.0
 */
public class RedisMap implements Map<String, String> {

    private final String host;
    private final int port;

    /**
     * Конструктор, инициализирующий подключение к Redis по указанным хосту и порту.
     *
     * @param host хост Redis-сервера
     * @param port порт Redis-сервера
     */
    public RedisMap(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Создает и возвращает подключение к Redis.
     *
     * @return объект {@link Jedis} для взаимодействия с Redis
     * @throws JedisException если не удалось установить соединение
     */
    private Jedis getJedis() {
        try {
            return new Jedis(host, port);
        } catch (Exception e) {
            throw new JedisException("Couldn`t connect to the Redis: " + e.getMessage());
        }
    }

    /**
     * Возвращает количество пар ключ-значение в Redis.
     *
     * @return количество ключей
     */
    @Override
    public int size() {
        try (Jedis jedis = getJedis()) {
            return (int) jedis.dbSize();
        }
    }

    /**
     * Проверяет, является ли Redis пустым (не содержит ключей).
     *
     * @return {@code true}, если Redis не содержит ключей, иначе {@code false}
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Проверяет, существует ли указанный ключ в Redis.
     *
     * @param key ключ для проверки
     * @return {@code true}, если ключ существует, иначе {@code false}
     * @throws IllegalArgumentException если ключ равен {@code null}
     */
    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null");
        }

        try (Jedis jedis = getJedis()) {
            return jedis.exists(key.toString());
        }
    }

    /**
     * Проверяет, существует ли указанное значение в Redis.
     *
     * @param value значение для проверки
     * @return {@code true}, если значение существует, иначе {@code false}
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }

        try (Jedis jedis = getJedis()) {
            Set<String> keys = jedis.keys("*");
            for (String key : keys) {
                if (value.toString().equals(jedis.get(key))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Получает значение, связанное с указанным ключом.
     *
     * @param key ключ, значение которого нужно получить
     * @return значение, связанное с ключом, или {@code null}, если ключ не существует
     * @throws IllegalArgumentException если ключ равен {@code null}
     */
    @Override
    public String get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null");
        }

        try (Jedis jedis = getJedis()) {
            return jedis.get(key.toString());
        }
    }

    /**
     * Добавляет или обновляет пару ключ-значение в Redis.
     *
     * @param key   ключ
     * @param value значение
     * @return предыдущее значение, связанное с ключом, или {@code null}, если ключа не существовало
     * @throws IllegalArgumentException если ключ или значение равны {@code null}
     */
    @Override
    public String put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("The key or value cannot be null");
        }

        try (Jedis jedis = getJedis()) {
            String oldValue = jedis.get(key);
            jedis.set(key, value);
            return oldValue;
        }
    }

    /**
     * Удаляет пару ключ-значение из Redis.
     *
     * @param key ключ для удаления
     * @return значение, связанное с ключом, или {@code null}, если ключ не существовал
     * @throws IllegalArgumentException если ключ равен {@code null}
     */
    @Override
    public String remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null");
        }

        try (Jedis jedis = getJedis()) {
            String value = jedis.get(key.toString());
            jedis.del(key.toString());
            return value;
        }
    }

    /**
     * Добавляет все пары ключ-значение из указанной карты в Redis.
     *
     * @param m карта, содержащая пары ключ-значение
     * @throws IllegalArgumentException если карта содержит {@code null} ключи или значения
     */
    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        if (m == null) {
            throw new IllegalArgumentException("The card cannot be null");
        }

        try (Jedis jedis = getJedis()) {
            for (Entry<? extends String, ? extends String> entry : m.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("The key or value cannot be null");
                }
                jedis.set(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Удаляет все ключи и значения из Redis.
     */
    @Override
    public void clear() {
        try (Jedis jedis = getJedis()) {
            jedis.flushDB();
        }
    }

    /**
     * Возвращает множество всех ключей в Redis.
     *
     * @return множество ключей
     */
    @Override
    public Set<String> keySet() {
        try (Jedis jedis = getJedis()) {
            return jedis.keys("*");
        }
    }

    /**
     * Возвращает коллекцию всех значений в Redis.
     *
     * @return коллекция значений
     */
    @Override
    public Collection<String> values() {
        try (Jedis jedis = getJedis()) {
            Set<String> keys = jedis.keys("*");
            return keys.stream().map(jedis::get).toList();
        }
    }

    /**
     * Возвращает множество всех пар ключ-значение в Redis.
     *
     * @return множество пар ключ-значение
     */
    @Override
    public Set<Entry<String, String>> entrySet() {
        try (Jedis jedis = getJedis()) {
            Set<String> keys = jedis.keys("*");
            return keys.stream()
                    .map(key -> new AbstractMap.SimpleEntry<>(key, jedis.get(key)))
                    .collect(Collectors.toSet());
        }
    }
}
