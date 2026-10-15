#include "dpt_crypto.h"
#include <fstream>
#include <iterator>
#include <iostream>
#include <stdexcept>

int main(int argc, char **argv) {
    if (argc != 2) return 2;
    std::ifstream input(argv[1], std::ios::binary);
    std::vector<uint8_t> blob((std::istreambuf_iterator<char>(input)), {});
    const std::string key = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    const std::vector<uint8_t> expected = {2,0,1,0,8,0,0,0,0,0};
    if (decrypt_code_item_blob(key, blob.data(), blob.size()) != expected) throw std::runtime_error("Java/native 向量不一致");
    for (size_t offset : {size_t(0), size_t(4), size_t(16), blob.size()-1}) {
        auto changed = blob; changed[offset] ^= 1;
        if (!decrypt_code_item_blob(key, changed.data(), changed.size()).empty()) throw std::runtime_error("篡改未拒绝");
    }
    for (size_t len : {size_t(0), size_t(16), size_t(32), blob.size()-1}) {
        if (!decrypt_code_item_blob(key, blob.data(), len).empty()) throw std::runtime_error("截断未拒绝");
    }
    for (const std::string bad : {std::string(64, '0'), std::string(64, 'z'), std::string()}) {
        if (!decrypt_code_item_blob(bad, blob.data(), blob.size()).empty()) throw std::runtime_error("错误密钥未拒绝");
    }
    std::cout << "PASS Java/native AES-GCM：正常向量、4 类篡改、4 类截断、3 类错误密钥\n";
}
