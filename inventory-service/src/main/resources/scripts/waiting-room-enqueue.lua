-- Atomically enqueue a user into a waiting room (idempotent per userId).
-- KEYS[1] = seqKey        (INCR counter)
-- KEYS[2] = queueKey      (ZSET: token -> seq)
-- KEYS[3] = userIndexKey  (HASH: userId -> token)
-- KEYS[4] = roomsKey      (SET: registry of active rooms)
-- ARGV[1] = tokenKeyPrefix (string prefix for per-token hash, e.g. "wr:{r}:tok:")
-- ARGV[2] = resourceId    (added to rooms registry)
-- ARGV[3] = newToken      (caller-generated UUID)
-- ARGV[4] = userId
-- ARGV[5] = nowMs
-- ARGV[6] = tokenTtlMs
--
-- Returns: { token(String), position(Long, 1-based), flag(String "NEW"|"EXISTING") }

local seqKey        = KEYS[1]
local queueKey      = KEYS[2]
local userIndexKey  = KEYS[3]
local roomsKey      = KEYS[4]

local tokenKeyPrefix = ARGV[1]
local resourceId     = ARGV[2]
local newToken       = ARGV[3]
local userId         = ARGV[4]
local nowMs          = tonumber(ARGV[5])
local tokenTtlMs     = tonumber(ARGV[6])

-- Reuse existing token if the user already holds one that is still alive.
local existing = redis.call('HGET', userIndexKey, userId)
if existing and existing ~= false and existing ~= '' then
    if redis.call('EXISTS', tokenKeyPrefix .. existing) == 1 then
        local rank = redis.call('ZRANK', queueKey, existing)
        local pos
        if rank == false then
            pos = 0 -- already admitted (or elsewhere); caller should use /status
        else
            pos = rank + 1
        end
        return { existing, pos, 'EXISTING' }
    else
        -- Stale index; fall through and issue a new token.
        redis.call('HDEL', userIndexKey, userId)
    end
end

local seq = redis.call('INCR', seqKey)
redis.call('ZADD', queueKey, seq, newToken)
redis.call('HSET', tokenKeyPrefix .. newToken,
    'userId', userId,
    'status', 'WAITING',
    'seq', seq,
    'enqueuedAt', nowMs)
redis.call('PEXPIRE', tokenKeyPrefix .. newToken, tokenTtlMs)
redis.call('HSET', userIndexKey, userId, newToken)
redis.call('PEXPIRE', userIndexKey, tokenTtlMs)

redis.call('SADD', roomsKey, resourceId)

local position = redis.call('ZRANK', queueKey, newToken) + 1
return { newToken, position, 'NEW' }
