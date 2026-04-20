-- Extend the active expiry for an admitted token, atomically.
-- KEYS[1] = activeKey  (ZSET)
-- KEYS[2] = tokenKey   (HASH)
-- ARGV[1] = token
-- ARGV[2] = nowMs
-- ARGV[3] = passTtlMs
--
-- Returns: newExpiresAtMs if updated, 0 if not active.

local activeKey = KEYS[1]
local tokenKey  = KEYS[2]

local token     = ARGV[1]
local nowMs     = tonumber(ARGV[2])
local passTtlMs = tonumber(ARGV[3])

local expiresAt = nowMs + passTtlMs

-- XX: only update if member already exists in the active set.
local updated = redis.call('ZADD', activeKey, 'XX', 'CH', expiresAt, token)
if updated == 0 then
    return 0
end

redis.call('HSET', tokenKey, 'expiresAt', expiresAt)
redis.call('PEXPIRE', tokenKey, passTtlMs + 60000)
return expiresAt
