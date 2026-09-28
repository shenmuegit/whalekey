#include <jni.h>
#include <llama.h>

#include <algorithm>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace {
constexpr int32_t kContextTokens = 2048;
constexpr int32_t kOutputTokens = 512;
constexpr int32_t kBatchTokens = 256;
constexpr char kInstruction[] =
    "请改写用户提供的中文，使表达更清晰自然，保持原意。必须改变措辞，只输出改写后的文本，不要解释。";

std::once_flag backend_once;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_fcitx_fcitx5_android_input_LocalRewriter_rewriteNative(
    JNIEnv *env, jobject, jstring model_path, jbyteArray text) {
    if (!model_path || !text) return nullptr;

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) return nullptr;
    std::string path_copy(path);
    env->ReleaseStringUTFChars(model_path, path);

    std::string input(env->GetArrayLength(text), '\0');
    env->GetByteArrayRegion(text, 0, static_cast<jsize>(input.size()),
                            reinterpret_cast<jbyte *>(input.data()));
    if (env->ExceptionCheck()) return nullptr;

    std::call_once(backend_once, [] {
        llama_log_set([](ggml_log_level, const char *, void *) {}, nullptr);
        llama_backend_init();
    });

    auto model = std::unique_ptr<llama_model, decltype(&llama_model_free)>(
        llama_model_load_from_file(path_copy.c_str(), llama_model_default_params()),
        llama_model_free);
    if (!model) return nullptr;

    const llama_chat_message messages[] = {{"system", kInstruction}, {"user", input.c_str()}};
    const char *chat_template = llama_model_chat_template(model.get(), nullptr);
    const int32_t prompt_size = llama_chat_apply_template(chat_template, messages, 2, true, nullptr, 0);
    if (prompt_size <= 0) return nullptr;
    std::string prompt(prompt_size, '\0');
    if (llama_chat_apply_template(chat_template, messages, 2, true, prompt.data(), prompt_size)
        != prompt_size) return nullptr;

    const llama_vocab *vocab = llama_model_get_vocab(model.get());
    std::vector<llama_token> prompt_tokens(kContextTokens);
    const int32_t token_count = llama_tokenize(vocab, prompt.data(), prompt_size,
                                               prompt_tokens.data(), kContextTokens, true, true);
    if (token_count <= 0 || token_count > kContextTokens - kOutputTokens) return nullptr;

    llama_context_params params = llama_context_default_params();
    params.n_ctx = kContextTokens;
    params.n_batch = kBatchTokens;
    params.n_threads = 4;
    params.n_threads_batch = 4;
    auto context = std::unique_ptr<llama_context, decltype(&llama_free)>(
        llama_init_from_model(model.get(), params), llama_free);
    if (!context) return nullptr;

    for (int32_t offset = 0; offset < token_count; offset += kBatchTokens) {
        const int32_t count = std::min(kBatchTokens, token_count - offset);
        if (llama_decode(context.get(), llama_batch_get_one(prompt_tokens.data() + offset, count)))
            return nullptr;
    }

    auto sampler = std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)>(
        llama_sampler_init_greedy(), llama_sampler_free);
    if (!sampler) return nullptr;
    std::vector<llama_token> output_tokens;
    bool complete = false;
    for (int32_t i = 0; i < kOutputTokens; ++i) {
        llama_token token = llama_sampler_sample(sampler.get(), context.get(), -1);
        if (llama_vocab_is_eog(vocab, token)) {
            complete = true;
            break;
        }
        output_tokens.push_back(token);
        if (llama_decode(context.get(), llama_batch_get_one(&token, 1))) return nullptr;
    }
    if (!complete || output_tokens.empty()) return nullptr;

    int32_t bytes = llama_detokenize(vocab, output_tokens.data(), output_tokens.size(),
                                      nullptr, 0, false, false);
    if (bytes >= 0) return nullptr;
    std::string output(-bytes, '\0');
    bytes = llama_detokenize(vocab, output_tokens.data(), output_tokens.size(),
                             output.data(), output.size(), false, false);
    if (bytes <= 0) return nullptr;

    jbyteArray result = env->NewByteArray(bytes);
    if (result) env->SetByteArrayRegion(result, 0, bytes,
                                        reinterpret_cast<const jbyte *>(output.data()));
    return result;
}
