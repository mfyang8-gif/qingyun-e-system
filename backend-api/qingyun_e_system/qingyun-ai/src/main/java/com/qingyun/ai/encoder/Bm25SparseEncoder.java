package com.qingyun.ai.encoder;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class Bm25SparseEncoder {

    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    // BM25 算法参数
    private static final float K1 = 1.5f;
    private static final float B = 0.75f;

    // Redis Keys
    private static final String REDIS_DF_KEY = "bm25:df:table";
    private static final String REDIS_N_KEY = "bm25:total:docs";
    private static final String REDIS_TOKENS_KEY = "bm25:total:tokens";

    private final StringRedisTemplate redisTemplate;

    // 内存缓存层
    private final ConcurrentHashMap<Integer, AtomicInteger> documentFrequency = new ConcurrentHashMap<>();
    private final AtomicInteger totalDocuments = new AtomicInteger(0);
    private final AtomicLong totalTokenCount = new AtomicLong(0);

    @PostConstruct
    public void loadFromRedis() {
        try {
            String nStr = redisTemplate.opsForValue().get(REDIS_N_KEY);
            if (nStr != null) {
                totalDocuments.set(Integer.parseInt(nStr));
            }

            String tokensStr = redisTemplate.opsForValue().get(REDIS_TOKENS_KEY);
            if (tokensStr != null) {
                totalTokenCount.set(Long.parseLong(tokensStr));
            }

            Map<Object, Object> dfEntries = redisTemplate.opsForHash().entries(REDIS_DF_KEY);
            dfEntries.forEach((k, v) -> {
                int termIndex = Integer.parseInt(k.toString());
                int df = Integer.parseInt(v.toString());
                documentFrequency.put(termIndex, new AtomicInteger(df));
            });

            log.info("BM25 状态已恢复: docs={}, tokens={}, vocab_size={}",
                    totalDocuments.get(), totalTokenCount.get(), documentFrequency.size());
        } catch (Exception e) {
            log.warn("BM25 Redis 加载失败，将从空表开始构建", e);
        }
    }

    public SparseVector encodeDocument(String text) {
        if (text == null || text.isBlank()) {
            return SparseVector.empty();
        }

        List<SegToken> tokens = SEGMENTER.process(text, JiebaSegmenter.SegMode.SEARCH);

        Map<Integer, Integer> termFreqMap = new HashMap<>();
        int validTokenCount = 0;

        for (SegToken token : tokens) {
            String word = token.word.trim();
            if (word.length() < 2) {
                continue;
            }
            validTokenCount++;
            int termIndex = mapToIndex(word);
            termFreqMap.merge(termIndex, 1, Integer::sum);
        }

        if (validTokenCount == 0) {
            return SparseVector.empty();
        }

        // 1. 更新内存统计（原子操作）
        totalDocuments.incrementAndGet();
        totalTokenCount.addAndGet(validTokenCount);

        // 记录本次新增的 DF，用于增量更新 Redis
        Map<String, String> redisDfIncrements = new HashMap<>(termFreqMap.size());

        for (int termIndex : termFreqMap.keySet()) {
            documentFrequency
                    .computeIfAbsent(termIndex, k -> new AtomicInteger(0))
                    .incrementAndGet();
            redisDfIncrements.put(String.valueOf(termIndex), "1");
        }

        // 2. 异步增量更新 Redis，防止阻塞主业务线程
        asyncIncrementRedis(validTokenCount, redisDfIncrements);

        // 3. 计算当前的 BM25 向量
        float avgDocLength = getAverageDocLength();
        List<Integer> indices = new ArrayList<>(termFreqMap.size());
        List<Float> values = new ArrayList<>(termFreqMap.size());

        for (Map.Entry<Integer, Integer> entry : termFreqMap.entrySet()) {
            int termIndex = entry.getKey();
            int tf = entry.getValue();

            float bm25Weight = computeBm25(tf, validTokenCount, avgDocLength, termIndex);
            if (bm25Weight > 0) {
                indices.add(termIndex);
                values.add(bm25Weight);
            }
        }

        return new SparseVector(indices, values);
    }

    public SparseVector encodeQuery(String text) {
        if (text == null || text.isBlank()) {
            return SparseVector.empty();
        }

        List<SegToken> tokens = SEGMENTER.process(text, JiebaSegmenter.SegMode.SEARCH);

        Map<Integer, Integer> termFreqMap = new LinkedHashMap<>();
        for (SegToken token : tokens) {
            String word = token.word.trim();
            if (word.length() < 2) {
                continue;
            }
            int termIndex = mapToIndex(word);
            termFreqMap.merge(termIndex, 1, Integer::sum);
        }

        int queryLength = termFreqMap.values().stream().mapToInt(Integer::intValue).sum();
        boolean dfCold = totalDocuments.get() == 0;

        List<Integer> indices = new ArrayList<>();
        List<Float> values = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : termFreqMap.entrySet()) {
            int termIndex = entry.getKey();
            int tf = entry.getValue();

            AtomicInteger dfCounter = documentFrequency.get(termIndex);

            float bm25Weight;
            if (dfCounter == null && dfCold) {
                bm25Weight = 1.0f + (float) tf / queryLength;
            } else if (dfCounter == null) {
                continue;
            } else {
                bm25Weight = computeBm25(tf, queryLength, (float) queryLength, termIndex);
            }

            if (bm25Weight > 0) {
                indices.add(termIndex);
                values.add(bm25Weight);
            }
        }

        if (dfCold && !indices.isEmpty()) {
            log.debug("BM25 冷启动降级: DF 表为空，查询使用词重叠权重 (terms={})", indices.size());
        }

        return new SparseVector(indices, values);
    }

    public void reset() {
        documentFrequency.clear();
        totalDocuments.set(0);
        totalTokenCount.set(0);
        try {
            redisTemplate.delete(List.of(REDIS_DF_KEY, REDIS_N_KEY, REDIS_TOKENS_KEY));
        } catch (Exception e) {
            log.warn("BM25 Redis 清理失败", e);
        }
        log.info("BM25 统计已重置 (vocab={}, docs=0)", documentFrequency.size());
    }

    public int getTotalDocuments() {
        return totalDocuments.get();
    }

    public int getVocabularySize() {
        return documentFrequency.size();
    }

    private float computeBm25(int tf, int docLength, float avgDocLength, int termIndex) {
        float idf = getIdf(termIndex);
        float tfNorm = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * docLength / avgDocLength));
        return idf * tfNorm;
    }

    private float getIdf(int termIndex) {
        AtomicInteger dfCounter = documentFrequency.get(termIndex);
        int df = (dfCounter != null) ? dfCounter.get() : 0;
        int n = totalDocuments.get();
        if (n == 0 || df == 0) {
            return 0;
        }
        return (float) Math.log(1.0 + (n - df + 0.5) / (df + 0.5));
    }

    private float getAverageDocLength() {
        int n = totalDocuments.get();
        if (n == 0) {
            return 1.0f;
        }
        // 使用精确累加的 token 总数计算平均文档长度
        return (float) totalTokenCount.get() / n;
    }

    /**
     * 异步增量刷盘机制
     * 使用 HINCRBY 增量更新 DF，利用 Pipeline 批量提交，避免网络 I/O 拥堵
     */
    private void asyncIncrementRedis(int tokensAdded, Map<String, String> dfIncrements) {
        // 使用 Java 21 虚拟线程处理后台刷盘任务 (若使用较旧 JDK，可替换为 Spring 注入的 ThreadPoolTaskExecutor)
        Thread.startVirtualThread(() -> {
            try {
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    byte[] keyBytes = redisTemplate.getStringSerializer().serialize(REDIS_DF_KEY);
                    dfIncrements.forEach((k, v) -> {
                        byte[] field = redisTemplate.getStringSerializer().serialize(k);
                        connection.hashCommands().hIncrBy(keyBytes, field, 1);
                    });

                    connection.stringCommands().incr(redisTemplate.getStringSerializer().serialize(REDIS_N_KEY));
                    connection.stringCommands().incrBy(redisTemplate.getStringSerializer().serialize(REDIS_TOKENS_KEY), tokensAdded);
                    return null;
                });
            } catch (Exception e) {
                log.warn("BM25 Redis 增量更新失败", e);
            }
        });
    }

    /**
     * 去除 VOCAB_SIZE 取模限制，直接映射到正整数空间，大幅降低哈希碰撞
     */
    private static int mapToIndex(String word) {
        int h = word.hashCode();
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h & 0x7fffffff; // 保证返回正整数
    }

    public record SparseVector(List<Integer> indices, List<Float> values) {
        public static SparseVector empty() {
            return new SparseVector(List.of(), List.of());
        }
    }
}