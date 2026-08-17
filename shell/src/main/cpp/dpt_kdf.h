#ifndef DPT_DPT_KDF_H
#define DPT_DPT_KDF_H

#include <cstdint>

// Keep these constexpr functions bit-identical with DexUtils.  The static
// assertions make every native build verify the same boundary vectors as the
// Java unit test instead of relying on a one-off comparison script.
constexpr uint64_t splitmix64(uint64_t s) {
    s += 0x9E3779B97F4A7C15ULL;
    uint64_t z = s;
    z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9ULL;
    z = (z ^ (z >> 27)) * 0x94D049BB133111EBULL;
    return z ^ (z >> 31);
}

constexpr uint64_t derive_insns_key_stream(uint32_t master_key,
                                            int dex_index,
                                            uint32_t method_index) {
    const uint64_t seed = static_cast<uint64_t>(master_key)
            ^ ((static_cast<uint64_t>(dex_index) + 1ULL) << 32U)
            ^ (static_cast<uint64_t>(method_index) * 0x9E3779B97F4A7C15ULL);
    return splitmix64(seed);
}

static_assert(derive_insns_key_stream(0U, 0, 0U) == 0xC42C5A1AA3820138ULL);
static_assert(derive_insns_key_stream(1U, 0, 0U) == 0x204391A6FD59956FULL);
static_assert(derive_insns_key_stream(0xFFFFFFFFU, 0, 0U) == 0xB0CAED0A7BF9C2C8ULL);
static_assert(derive_insns_key_stream(0x80000000U, 1, 1U) == 0xF37B00A54C74820FULL);
static_assert(derive_insns_key_stream(0x12345678U, 7, 42U) == 0xEFA52F7ECD9E690AULL);
static_assert(derive_insns_key_stream(0xDEADBEEFU, 31, 0x7FFFFFFFU) == 0x73150763AB0415DFULL);

#endif // DPT_DPT_KDF_H
