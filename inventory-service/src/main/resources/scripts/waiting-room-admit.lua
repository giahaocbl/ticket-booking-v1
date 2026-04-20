-- Promote up to `freeSlots` users from the queue into the active set.
-- Also sweeps expired admissions.
-- KEYS[1] = activeKey   (ZSET: token -> expiresAtMs)
-- KEYS[2] = queueKey    (ZSET: token -> seq)
-- ARGV[1] = tokenKeyPrefix (string prefix for per-token hash)
-- ARGV[2] = capacity       (int)
-- ARGV[3] = nowMs          (long)
-- ARGV[4] = passTtlMs      (long)
--
-- Returns: admittedCount (Long)

local activeKey = KEYS[1]
local queueKey  = KEYS[2]

local tokenKeyPrefix = ARGV[1]
local capacity       = tonumber(ARGV[2])
local nowMs          = tonumber(ARGV[3])
local passTtlMs      = tonumber(ARGV[4])

-- 1) Sweep expired admissions so their slots are freed.
redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', nowMs)

local active = redis.call('ZCARD', activeKey)
local free = capacity - active
if free <= 0 then
    return 0
end

local popped = redis.call('ZPOPMIN', queueKey, free)
if (not popped) or #popped == 0 then
    return 0
end

local expiresAt = nowMs + passTtlMs
local admitted = 0
-- ZPOPMIN returns flat array: {member1, score1, member2, score2, ...}
for i = 1, #popped, 2 do
    local token = popped[i]
    redis.call('ZADD', activeKey, expiresAt, token)
    redis.call('HSET', tokenKeyPrefix .. token,
        'status', 'ADMITTED',
        'admittedAt', nowMs,
        'expiresAt', expiresAt)
    -- Keep hash around a bit beyond the pass so clients can still look up state.
    redis.call('PEXPIRE', tokenKeyPrefix .. token, passTtlMs + 60000)
    admitted = admitted + 1
end

return admitted
