//
// Created by luoyesiqiu on 2025/11/26.
//

#include "dpt_crypto.h"
#include <cstring>
#include <mbedtls/gcm.h>
#include <mbedtls/platform_util.h>
#ifndef DPT_HOST_CRYPTO
#include "common/dpt_log.h"
#else
#define DLOGE(...)
#define DLOGD(...)
#endif

// 与 Java CryptoUtils 的 DPC1 容器逐字节一致，认证失败绝不交给指令解析器。
std::vector<uint8_t> decrypt_code_item_blob(const std::string &root_key_hex,
                                          const uint8_t *blob, size_t len) {
    if (!blob || len <= 32 || root_key_hex.size() != 64 || memcmp(blob, "DPC1", 4) != 0) return {};
    uint8_t root_key[32];
    auto digit = [](char c) -> int {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    };
    for (size_t i = 0; i < 32; ++i) {
        int hi = digit(root_key_hex[2 * i]), lo = digit(root_key_hex[2 * i + 1]);
        if (hi < 0 || lo < 0) { mbedtls_platform_zeroize(root_key, sizeof(root_key)); return {}; }
        root_key[i] = static_cast<uint8_t>((hi << 4) | lo);
    }
    const char domain[] = "qy-dpt-code-item/v1";
    auto key = hmac_sha256(root_key, sizeof(root_key),
                           reinterpret_cast<const uint8_t *>(domain), sizeof(domain) - 1);
    mbedtls_platform_zeroize(root_key, sizeof(root_key));
    if (key.size() != 32) return {};
    std::vector<uint8_t> plain(len - 32);
    mbedtls_gcm_context ctx;
    mbedtls_gcm_init(&ctx);
    int ret = mbedtls_gcm_setkey(&ctx, MBEDTLS_CIPHER_ID_AES, key.data(), 256);
    if (ret == 0) ret = mbedtls_gcm_auth_decrypt(&ctx, plain.size(), blob + 4, 12,
                                               blob, 4, blob + len - 16, 16, blob + 16, plain.data());
    mbedtls_gcm_free(&ctx);
    mbedtls_platform_zeroize(key.data(), key.size());
    if (ret != 0) { mbedtls_platform_zeroize(plain.data(), plain.size()); return {}; }
    return plain;
}

std::vector<uint8_t> hmac_sha256(const uint8_t *key,
                                 size_t key_len,
                                 const uint8_t *input,
                                 size_t input_len) {
    if (key == nullptr || key_len == 0 || input == nullptr || input_len == 0) {
        DLOGE("invalid hmac input");
        return {};
    }

    const mbedtls_md_info_t *md_info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    if (md_info == nullptr) {
        DLOGE("mbedtls sha256 unavailable");
        return {};
    }

    std::vector<uint8_t> out(32);
    int ret = mbedtls_md_hmac(md_info, key, key_len, input, input_len, out.data());
    if (ret != 0) {
        DLOGE("hmac-sha256 failed: %d", ret);
        return {};
    }
    return out;
}

std::vector<uint8_t> aes_cbc_decrypt(const uint8_t *key,
                                     size_t key_bits,
                                     const uint8_t *iv,
                                     const uint8_t *in,
                                     size_t inlen) {
    if (key == nullptr || iv == nullptr || in == nullptr || inlen == 0 || (inlen % 16) != 0) {
        DLOGE("invalid aes cbc input");
        return {};
    }
    if (key_bits != 128 && key_bits != 192 && key_bits != 256) {
        DLOGE("unsupported aes key bits: %zu", key_bits);
        return {};
    }

    std::vector<uint8_t> out_vec(inlen);

    mbedtls_aes_context ctx;
    mbedtls_aes_init(&ctx);

    int setkey_ret = mbedtls_aes_setkey_dec(&ctx, key, static_cast<unsigned int>(key_bits));

    if(setkey_ret == 0) {
        DLOGD("set key success");
    }
    else {
        DLOGE("set key fail");
        mbedtls_aes_free(&ctx);
        return {};
    }

    uint8_t new_iv[16] = {0};
    memcpy(new_iv, iv, 16);

    int ret = mbedtls_aes_crypt_cbc(&ctx, MBEDTLS_AES_DECRYPT, inlen, new_iv, in, out_vec.data());

    if(ret == 0) {
        DLOGD("decrypt ret: %d", ret);
    }
    else {
        DLOGE("decrypt fail");
        mbedtls_aes_free(&ctx);
        return {};
    }

    if (!out_vec.empty()) {
        uint8_t pad = out_vec.back();
        DLOGD("padding: %d", pad);
        if (pad == 0 || pad > 16 || pad > out_vec.size()) {
            DLOGE("invalid padding");
            mbedtls_aes_free(&ctx);
            return {};
        }
        for (size_t i = 0; i < pad; ++i) {
            if (out_vec[out_vec.size() - 1 - i] != pad) {
                DLOGE("invalid padding bytes");
                mbedtls_aes_free(&ctx);
                return {};
            }
        }
        out_vec.resize(out_vec.size() - pad);
    }

    mbedtls_aes_free(&ctx);

    return out_vec;
}
