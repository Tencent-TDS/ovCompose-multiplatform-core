/*
 * Copyright (c) 2021-2024 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @addtogroup ArkTS_Napi_NativeModule
 * @{
 *
 *
 * @brief Provides native api of ArkTS native module.
 *
 * @since 10
 */

/**
 * @file native_api.h
 *
 * @brief Defines native api of ArkTS native module.
 *
 * @kit ArkTS
 * @library libace_napi.z.so
 * @syscap SystemCapability.ArkUI.ArkUI.Napi
 * @since 10
 * @version 1.0
 */

#ifndef FOUNDATION_ACE_NAPI_INTERFACES_KITS_NAPI_NATIVE_API_H
#define FOUNDATION_ACE_NAPI_INTERFACES_KITS_NAPI_NATIVE_API_H

#ifndef NAPI_VERSION
#define NAPI_VERSION 8
#endif // NAPI_VERSION

#ifndef NAPI_EXPERIMENTAL
#define NAPI_EXPERIMENTAL
#endif // NAPI_EXPERIMENTAL

#include "common.h"
#include "node_api.h"

#ifdef NAPI_TEST
#ifdef _WIN32 // WIN32
#define NAPI_INNER_EXTERN __declspec(dllexport)
#else // NON-WIN32
#define NAPI_INNER_EXTERN __attribute__((visibility("default")))
#endif // _WIN32

#else // NAPI_TEST
#ifdef _WIN32
#define NAPI_INNER_EXTERN __declspec(deprecated)
#else
#define NAPI_INNER_EXTERN __attribute__((__deprecated__))
#endif // __WIN32
#endif

#ifdef __cplusplus
extern "C" {
#endif

NAPI_EXTERN napi_status napi_create_string_utf16(napi_env env,
                                                 const char16_t* str,
                                                 size_t length,
                                                 napi_value* result);

NAPI_EXTERN napi_status napi_get_value_string_utf16(napi_env env,
                                                    napi_value value,
                                                    char16_t* buf,
                                                    size_t bufsize,
                                                    size_t* result);

NAPI_EXTERN napi_status napi_type_tag_object(napi_env env,
                                             napi_value value,
                                             const napi_type_tag* type_tag);

NAPI_EXTERN napi_status napi_check_object_type_tag(napi_env env,
                                                   napi_value value,
                                                   const napi_type_tag* type_tag,
                                                   bool* result);

NAPI_INNER_EXTERN napi_status napi_adjust_external_memory(napi_env env,
                                                          int64_t change_in_bytes,
                                                          int64_t* adjusted_value);

/**
 * @brief Native detach callback of napi_coerce_to_native_binding_object that can be used to
 *        detach the ArkTS object and the native object.
 *
 * @since 11
 */
typedef void* (*napi_native_binding_detach_callback)(napi_env env, void* native_object, void* hint);

/**
 * @brief Native attach callback of napi_coerce_to_native_binding_object that can be used to
 *        bind the ArkTS object and the native object.
 *
 * @since 11
 */
typedef napi_value (*napi_native_binding_attach_callback)(napi_env env, void* native_object, void* hint);

NAPI_EXTERN napi_status napi_run_script_path(napi_env env, const char* path, napi_value* result);
NAPI_EXTERN napi_status napi_queue_async_work_with_qos(napi_env env, napi_async_work work, napi_qos_t qos);

/**
 * @brief Loads an .abc file as a module. This API returns the namespace of the module.
 * @param env Current running virtual machine context.
 * @param path Path of the .abc file or name of the module to load.
 * @param result Result of the module object.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_load_module(napi_env env,
                                         const char* path,
                                         napi_value* result);

/**
 * @brief Associates data with the currently running environment.
 *
 * @param env Current running virtual machine context.
 * @param data Data item to bind with the 'env'.
 * @param finalize_cb Optional native callback that will be triggered when 'env' is destroyed or this interface
 *                    repeatedly calls.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_set_instance_data(napi_env env,
                                               void* data,
                                               napi_finalize finalize_cb,
                                               void* finalize_hint);

/**
 * @brief Retrieves the data that was previously associated with the currently running environment.
 *
 * @param env Current running virtual machine context.
 * @param data Data item is bound with the 'env'.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_get_instance_data(napi_env env,
                                               void** data);

/**
 * @brief Registers a clean-up hook for releasing resources when the environment exits.
 *
 * @param env Current running virtual machine context.
 * @param fun Function pointer which will be triggered when environment is destroy.
 * @param arg The argument is passed to the function pointer 'fun'.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_add_env_cleanup_hook(napi_env env,
                                                  void (*fun)(void* arg),
                                                  void* arg);

/**
 * @brief Unregisters the clean-up hook.
 *
 * @param env Current running virtual machine context.
 * @param fun Function pointer which will be triggered when environment is destroy.
 * @param arg The argument is passed to the function pointer 'fun'.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_remove_env_cleanup_hook(napi_env env,
                                                     void (*fun)(void* arg),
                                                     void* arg);

/**
 * @brief Registers an asynchronous clean-up hook for releasing resources when the environment exits.
 *
 * @param env Current running virtual machine context.
 * @param hook The function pointer to call at environment teardown.
 * @param arg The pointer to pass to `hook` when it gets called.
 * @param remove_handle Optional handle that refers to the asynchronous cleanup.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_add_async_cleanup_hook(napi_env env,
                                                    napi_async_cleanup_hook hook,
                                                    void* arg,
                                                    napi_async_cleanup_hook_handle* remove_handle);

/**
 * @brief Unregisters the asynchronous clean-up hook.
 *
 * @param remove_handle Optional handle that refers to the asynchronous cleanup.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_remove_async_cleanup_hook(napi_async_cleanup_hook_handle remove_handle);

/**
 * @brief Creates an asynchronous context. The capabilities related to 'async_hook' are not supported currently.
 *
 * @param env Current running virtual machine context.
 * @param async_resource Object associated with the async work that will be passed to possible 'async_hook'.
 * @param async_resource_name Identifier for the kind of resource that is being provided for diagnostic information
 *                            exposed by the async_hooks API.
 * @param result The initialized async context.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_async_init(napi_env env,
                                        napi_value async_resource,
                                        napi_value async_resource_name,
                                        napi_async_context* result);

/**
 * @brief Destroys the previously created asynchronous context. The capabilities related to 'async_hook' are not
 *        supported currently.
 *
 * @param env Current running virtual machine context.
 * @param async_context The async context to be destroyed.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_async_destroy(napi_env env,
                                           napi_async_context async_context);

/**
 * @brief Opens a callback scope. The capabilities related to 'async_hook' are not supported currently.
 * @param env Current running virtual machine context.
 * @param resource_object The resource object to be passed to possible 'async_hook'.
 * @param context The context environment for the async operation.
 * @param result The generated callback scope.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_open_callback_scope(napi_env env,
                                                 napi_value resource_object,
                                                 napi_async_context context,
                                                 napi_callback_scope* result);

/**
 * @brief Closes the callback scope. The capabilities related to 'async_hook' are not supported currently.
 *
 * @param env Current running virtual machine context.
 * @param scope The callback scope to be closed.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_close_callback_scope(napi_env env,
                                                  napi_callback_scope scope);

/**
 * @brief Obtains the absolute path of the location, from which the addon is loaded.
 *
 * @param env Current running virtual machine context.
 * @param result The absolute path of the location of the loaded addon.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status node_api_get_module_file_name(napi_env env,
                                                      const char** result);

/**
 * @brief Create ArkTS Object with initial properties given by descriptors, note that property key must be String, and
 *        must can not convert to element_index, also all keys must not duplicate.
 *
 * @param env Current running virtual machine context.
 * @param result The created ArkTS object.
 * @param property_count Number of the property descriptors.
 * @param properties Array of property descriptors which are expected to be applied to the ArkTS object.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_create_object_with_properties(napi_env env,
                                                           napi_value* result,
                                                           size_t property_count,
                                                           const napi_property_descriptor* properties);

/**
 * @brief Create ArkTS Object with initial properties given by keys and values, note that property key must be String,
 *        and must can not convert to element_index, also all keys must not duplicate.
 *
 * @param env Current running virtual machine context.
 * @param result The absolute path of the location of the loaded addon.
 * @param property_count Number of the propertied which needs to be applied on the ArkTS object.
 * @param keys Array of the keys of the properties.
 * @param values Array of the values of the properties.
 *
 * @return Returns the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_create_object_with_named_properties(napi_env env,
                                                                 napi_value* result,
                                                                 size_t property_count,
                                                                 const char** keys,
                                                                 const napi_value* values);

/**
 * @brief This API sets native properties to a object and converts this ArkTS object to native binding object.
 *
 * @param env Current running virtual machine context.
 * @param js_object The ArkTS value to coerce.
 * @param detach_cb Native callback that can be used to detach the ArkTS object and the native object.
 * @param attach_cb Native callback that can be used to bind the ArkTS object and the native object.
 * @param native_object User-provided native instance to pass to thr detach callback and attach callback.
 * @param hint Optional hint to pass to the detach callback and attach callback.
 *
 * @return Return the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_coerce_to_native_binding_object(napi_env env,
                                                             napi_value js_object,
                                                             napi_native_binding_detach_callback detach_cb,
                                                             napi_native_binding_attach_callback attach_cb,
                                                             void* native_object,
                                                             void* hint);

/**
 * @brief Adds a 'napi_finalize' callback, which will be called when the ArkTS object is garbage-collected.
 *
 * @param env Current running virtual machine context.
 * @param js_object The ArkTS object value.
 * @param native_object Native object to bind with the ArkTS object.
 * @param finalize_cb Native callback that can be used to free the native object
 *                    when the ArkTS object is garbage-collected.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 * @param result Optional reference of the ArkTS object.
 *
 * @return Return the function execution status.
 * @since 11
 */
NAPI_EXTERN napi_status napi_add_finalizer(napi_env env,
                                           napi_value js_object,
                                           void* native_object,
                                           napi_finalize finalize_cb,
                                           void* finalize_hint,
                                           napi_ref* result);

/**
 * @brief The module is loaded through the NAPI. By default, the default object is exported from the module.
 *
 * @param env Current running virtual machine context.
 * @param path Path name of the module to be loaded, like @ohos.hilog.
 * @param module_info Path names of bundle and module, like com.example.application/entry.
 * @param result Result of loading a module, which is an exported object of the module.
 *
 * @return Returns the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_load_module_with_info(napi_env env,
                                                   const char* path,
                                                   const char* module_info,
                                                   napi_value* result);

/**
 * @brief Create the ark runtime.
 *
 * @param env Indicates the ark runtime environment.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_create_ark_runtime(napi_env* env);

/**
 * @brief Destroy the ark runtime.
 *
 * @param env Indicates the ark runtime environment.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_destroy_ark_runtime(napi_env* env);

/**
 * @brief Defines a sendable class.
 *
 * @param env The environment that the API is invoked under.
 * @param utf8name Name of the ArkTS constructor function.
 * @param length The length of the utf8name in bytes, or NAPI_AUTO_LENGTH if it is null-terminated.
 * @param constructor Callback function that handles constructing instances of the class.
 * @param data Optional data to be passed to the constructor callback as the data property of the callback info.
 * @param property_count Number of items in the properties array argument.
 * @param properties Array of property descriptors describing static and instance data properties, accessors, and
 *                   methods on the class. See napi_property_descriptor.
 * @param parent A napi_value representing the Superclass.
 * @param result A napi_value representing the constructor function for the class.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_define_sendable_class(napi_env env,
                                                   const char* utf8name,
                                                   size_t length,
                                                   napi_callback constructor,
                                                   void* data,
                                                   size_t property_count,
                                                   const napi_property_descriptor* properties,
                                                   napi_value parent,
                                                   napi_value* result);

/**
 * @brief Queries a napi_value to check if it is sendable.
 *
 * @param env The environment that the API is invoked under.
 * @param value The napi_value to be checked.
 * @param result Boolean value that is set to true if napi_value is sendable, false otherwise.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_is_sendable(napi_env env,
                                         napi_value value,
                                         bool* result);

/**
 * @brief Defines a sendable object.
 *
 * @param env The environment that the API is invoked under.
 * @param property_count The count of object properties.
 * @param properties Object properties.
 * @param result The created sendable object.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_create_sendable_object_with_properties(napi_env env,
                                                                    size_t property_count,
                                                                    const napi_property_descriptor* properties,
                                                                    napi_value* result);

/**
 * @brief Wraps a native instance in an ArkTS object.
 *
 * @param env The environment that the API is invoked under.
 * @param js_object The ArkTS object that will be the wrapper for the native object.
 * @param native_object The native instance that will be wrapped in the ArkTS object.
 * @param finalize_cb Optional native callback that can be used to free the native instance when the ArkTS object
 * has been garbage-collected.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_wrap_sendable(napi_env env,
                                           napi_value js_object,
                                           void* native_object,
                                           napi_finalize finalize_cb,
                                           void* finalize_hint);

/**
 * @brief Wraps a native instance in an ArkTS object.
 *
 * @param env The environment that the API is invoked under.
 * @param js_object The ArkTS object that will be the wrapper for the native object.
 * @param native_object The native instance that will be wrapped in the ArkTS object.
 * @param finalize_cb Optional native callback that can be used to free the native instance when the ArkTS object
 *                    has been garbage-collected.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 * @param native_binding_size The size of native binding.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_wrap_sendable_with_size(napi_env env,
                                                     napi_value js_object,
                                                     void* native_object,
                                                     napi_finalize finalize_cb,
                                                     void* finalize_hint,
                                                     size_t native_binding_size);

/**
 * @brief Retrieves a native instance that was previously wrapped in an ArkTS object.
 *
 * @param env The environment that the API is invoked under.
 * @param js_object The object associated with the native instance.
 * @param result Pointer to the wrapped native instance.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_unwrap_sendable(napi_env env,
                                             napi_value js_object,
                                             void** result);

/**
 * @brief Retrieves a native instance that was previously wrapped in an ArkTS object and removes the wrapping.
 *
 * @param env The environment that the API is invoked under.
 * @param js_object The object associated with the native instance.
 * @param result Pointer to the wrapped native instance.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_remove_wrap_sendable(napi_env env,
                                                  napi_value js_object,
                                                  void** result);

/**
 * @brief Create a sendable array.
 *
 * @param env The environment that the API is invoked under.
 * @param result A napi_value representing a sendable array.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_create_sendable_array(napi_env env,
                                                   napi_value* result);

/**
 * @brief Create a sendable array with length.
 *
 * @param env The environment that the API is invoked under.
 * @param length The initial length of the sendable array.
 * @param result A napi_value representing a sendable array.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_create_sendable_array_with_length(napi_env env,
                                                               size_t length,
                                                               napi_value* result);

/**
 * @brief Create a sendable arraybuffer.
 *
 * @param env The environment that the API is invoked under.
 * @param byte_length The length in bytes of the sendable arraybuffer to create.
 * @param data Pointer to the underlying byte buffer of the sendable arraybuffer.
 * @param result A napi_value representing a sendable arraybuffer.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_create_sendable_arraybuffer(napi_env env,
                                                         size_t byte_length,
                                                         void** data,
                                                         napi_value* result);

/**
 * @brief Create a sendable typedarray.
 *
 * @param env The environment that the API is invoked under.
 * @param type Scalar datatype of the elements within the sendable typedarray.
 * @param length Number of elements in the typedarray.
 * @param arraybuffer Sendable arraybuffer underlying the sendable typedarray.
 * @param byte_offset The byte offset within the sendable arraybuffer from which to start projecting the
 *                     sendable typedarray.
 * @param result A napi_value representing a sendable typedarray.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_create_sendable_typedarray(napi_env env,
                                                        napi_typedarray_type type,
                                                        size_t length,
                                                        napi_value arraybuffer,
                                                        size_t byte_offset,
                                                        napi_value* result);

/**
 * @brief Run the event loop by the given env and running mode in current thread.
 *
 * Support to run the native event loop in an asynchronous native thread with the specified running mode.
 *
 * @param env Current running virtual machine context.
 * @param mode Indicates the running mode of the native event loop.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_run_event_loop(napi_env env,
                                            napi_event_mode mode);

/**
 * @brief Stop the event loop in current thread.
 *
 * Support to stop the running event loop in current native thread.
 *
 * @param env Current running virtual machine context.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_stop_event_loop(napi_env env);

/**
 * @brief Serialize an ArkTS object.
 *
 * @param env Current running virtual machine context.
 * @param object The ArkTS object to serialize.
 * @param transfer_list List of data to transfer in transfer mode.
 * @param clone_list List of Sendable data to transfer in clone mode.
 * @param result Serialization result of the ArkTS object.
 *
 * @return Returns the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_serialize(napi_env env,
                                       napi_value object,
                                       napi_value transfer_list,
                                       napi_value clone_list,
                                       void** result);

/**
 * @brief Restore serialization data to an ArkTS object.
 *
 * @param env Current running virtual machine context.
 * @param buffer Data to deserialize.
 * @param object ArkTS object obtained by deserialization.
 *
 * @return Returns the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_deserialize(napi_env env,
                                         void* buffer,
                                         napi_value* object);

/**
 * @brief Delete serialization data.
 *
 * @param env Current running virtual machine context.
 * @param buffer Data to delete.
 *
 * @return Returns the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_delete_serialization_data(napi_env env,
                                                       void* buffer);

/**
 * @brief Dispatch a task with specified priority from a native thread to an ArkTS thread, the task will execute
 *        the given thread safe function.
 *
 * @param func Indicates the thread safe function.
 * @param data Indicates the data anticipated to be transferred to the ArkTS thread.
 * @param priority Indicates the priority of the task dispatched.
 * @param isTail Indicates the way of the task dispatched into the native event queue. When "isTail" is true,
 *               the task will be dispatched to the tail of the native event queue. Conversely, when "isTail" is
 *               false, the tasks will be dispatched to the head of the native event queue.
 *
 * @return Return the function execution status.
 * @since 12
 */
NAPI_EXTERN napi_status napi_call_threadsafe_function_with_priority(napi_threadsafe_function func,
                                                                    void *data,
                                                                    napi_task_priority priority,
                                                                    bool isTail);

/**
 * @brief Throws UncaughtException to ArkTS.
 * @param env Current running virtual machine context.
 * @param err Error object which is passed to 'UncaughtException'.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) err is nullptr;\n
 *                                   If the param err is not an ArkTS Error value.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before execution.\n
 * @since 12
 */
NAPI_EXTERN napi_status napi_fatal_exception(napi_env env,
                                             napi_value err);

/**
 * @brief Allows an ArkTS function to be called in the asynchronous context. The capabilities related to 'async_hook'
 *        are not supported currently.
 * @param env Current running virtual machine context.
 * @param async_context The context environment for the async operation.
 * @param recv The 'this' pointer of the function.
 * @param func ArkTS function to be called.
 * @param argc Size of the argument array which is passed to 'func'.
 * @param argv Argument array.
 * @param result Result returned by the ArkTS function.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, func and(or) recv is nullptr;\n
 *                                   If the param argc is greater than 0 but argv is nullptr.\n
 *         {@link napi_object_expected } If the param recv is not an ArkTS Object.\n
 *         {@link napi_function_expected } If the param func is not an ArkTS Function.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 11
 */
NAPI_EXTERN napi_status napi_make_callback(napi_env env,
                                           napi_async_context async_context,
                                           napi_value recv,
                                           napi_value func,
                                           size_t argc,
                                           const napi_value* argv,
                                           napi_value* result);

/**
 * @brief Creates an ArkTS ArrayBuffer object of the specified size.
 *
 * @param env Current running virtual machine context.
 * @param length Bytes size of the underlying arraybuffer.
 * @param data Raw pointer to the underlying arraybuffer.
 * @param result Created ArkTS ArrayBuffer object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, data or result is nullptr, or length is larger than 2097152,
 *                                   or length is less than zero.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_buffer(napi_env env,
                                           size_t length,
                                           void** data,
                                           napi_value* result);

/**
 * @brief Creates a deferred object and an ArkTS promise.
 * @param env Current running virtual machine context.
 * @param deferred The created deferred object which will be passed to 'napi_resolve_deferred()' or
 *                 'napi_reject_deferred()' to resolve or reject the promise.
 * @param promise The ArkTS promise which is associated with the deferred object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, deferred or resolution is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 *         {@link napi_generic_failure } If create promise failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_promise(napi_env env,
                                            napi_deferred* deferred,
                                            napi_value* promise);

/**
 * @brief Resolves a promise by way of the deferred object associated.
 * @param env Current running virtual machine context.
 * @param deferred The deferred object which is utilized to resolve the promise.
 * @param resolution The resolution value used to resolve the promise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, deferred or resolution is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_resolve_deferred(napi_env env,
                                              napi_deferred deferred,
                                              napi_value resolution);

/**
 * @brief Rejects a promise by way of the deferred object associated.
 * @param env Current running virtual machine context.
 * @param deferred The deferred object which is utilized to reject the promise.
 * @param rejection The rejection value used to reject the promise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, deferred or rejection is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_reject_deferred(napi_env env,
                                             napi_deferred deferred,
                                             napi_value rejection);

/**
 * @brief Checks whether the given 'napi_value' is a promise object.
 * @param env Current running virtual machine context.
 * @param value The 'napi_value' to be checked.
 * @param is_promise Boolean value that is set to true if the 'value' is a promise object, false otherwise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or is_promise is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_promise(napi_env env,
                                        napi_value value,
                                        bool* is_promise);

/**
 * @brief Obtains the current libuv loop instance.
 * @param env Current running virtual machine context.
 * @param loop Libuv event loop.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or loop is nullptr.\n
 *         {@link napi_generic_failure } If env is invalid.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_uv_event_loop(napi_env env,
                                               struct uv_loop_s** loop);

/**
 * @brief Creates a thread-safe function.
 * @param env Current running virtual machine context.
 * @param func ArkTS function to be called.
 * @param async_resource An optional Object associated with the async work that will be passed to possible
 *                       'async_hooks'.
 * @param async_resource_name An ArkTS string to provide an identifier for the kind of resource that is being
 *                            provided for diagnostic information exposed by the `async_hooks` interface.
 * @param max_queue_size Maximum size of the event queue in the thread-safe function.
 * @param initial_thread_count Initial thread count of the thread-safe function.
 * @param thread_finalize_data Data passed to the finalize callback.
 * @param thread_finalize_cb Finalize callback function which will be triggered when the thread-safe function is
 *                           released.
 * @param context Optional data is passed to 'call_js_cb'.
 * @param call_js_cb Callback function which will be triggered after 'napi_call_threadsafe_function()' is called.
 * @param result The created thread-safe function.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, async_resource_name or result is nullptr; max_queue_size is less than 0;\n
 *                                   initial_thread_count is greater than 128 or less than 0; func and call_js_cb are\n
 *                                   nullptr at same time.\n
 *         {@link napi_generic_failure } If create thread-safe function failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_threadsafe_function(napi_env env,
                                                        napi_value func,
                                                        napi_value async_resource,
                                                        napi_value async_resource_name,
                                                        size_t max_queue_size,
                                                        size_t initial_thread_count,
                                                        void* thread_finalize_data,
                                                        napi_finalize thread_finalize_cb,
                                                        void* context,
                                                        napi_threadsafe_function_call_js call_js_cb,
                                                        napi_threadsafe_function* result);

/**
 * @brief Obtains the context of a thread-safe function.
 * @param func The created thread-safe function.
 * @param result Pointer pointer to the context of the thread-safe function.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If func or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_threadsafe_function_context(napi_threadsafe_function func,
                                                             void** result);

/**
 * @brief Calls a thread-safe function.
 * @param func The created thread-safe function.
 * @param data Data passed to the callback function 'call_js_cb' which is registered by calling
 *             'napi_create_threadsafe_function()'.
 * @param is_blocking If true, this function blocks until the event queue is not full. If false, return directly.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If func is nullptr.\n
 *         {@link napi_queue_full } If event queue is full.\n
 *         {@link napi_closing } If the thread-safe function is closing.\n
 *         {@link napi_generic_failure } If call thread-safe function failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_call_threadsafe_function(napi_threadsafe_function func,
                                                      void* data,
                                                      napi_threadsafe_function_call_mode is_blocking);

/**
 * @brief Acquires a thread-safe function.
 * @param func The created thread-safe function.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If func is nullptr.\n
 *         {@link napi_generic_failure } If acquire thread-safe function failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_acquire_threadsafe_function(napi_threadsafe_function func);

/**
 * @brief Releases a thread-safe function.
 * @param func The created thread-safe function.
 * @param mode Value of mode can be either 'napi_tsfn_release' to indicate that no more calls should be made
 *             to the thread-safe function from current thread or 'napi_tsfn_abort' to indicate that the queue
 *             of the thread-safe function will be closed and 'napi_closing' will be return when calling
 *             'napi_call_threadsafe_function()' under the circumstance.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If func is nullptr.\n
 *         {@link napi_generic_failure } If release thread-safe function failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_release_threadsafe_function(napi_threadsafe_function func,
                                                         napi_threadsafe_function_release_mode mode);

/**
 * @brief Indicates that the event loop running on the main thread may exit before the thread-safe function
 *        is destroyed.
 * @param env Current running virtual machine context.
 * @param func The created thread-safe function.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or func is nullptr.\n
 *         {@link napi_generic_failure } If unref thread-safe function failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_unref_threadsafe_function(napi_env env,
                                                       napi_threadsafe_function func);

/**
 * @brief Indicates that the event loop running on the main thread should not exit until the thread-safe
 *        function is destroyed.
 * @param env Current running virtual machine context.
 * @param func The created thread-safe function.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or func is nullptr.\n
 *         {@link napi_generic_failure } If ref thread-safe function failed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_ref_threadsafe_function(napi_env env,
                                                     napi_threadsafe_function func);

/**
 * @brief Creates an ArkTS 'Date' object from C double data
 * @param env Current running virtual machine context.
 * @param time ArkTS time value in milliseconds format since 01 January, 1970 UTC.
 * @param result Created ArkTS data object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_date(napi_env env,
                                         double time,
                                         napi_value* result);

/**
 * @brief Checks whether the given ArkTS value is a 'Date' object. You can use this API to check the type
 *        of the parameter passed from ArkTS.
 * @param env Current running virtual machine context.
 * @param value ArkTS data object.
 * @param is_date Boolean value that is set to true if the 'value' is a 'Date' object, false otherwise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or is_date is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_date(napi_env env,
                                     napi_value value,
                                     bool* is_date);

/**
 * @brief Obtains the C equivalent of the given ArkTS 'Date' object.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS data object.
 * @param result C time value in milliseconds format since 01 January, 1970 UTC.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or result is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 *         {@link napi_date_expected } If the 'value' is not a 'Date' object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_date_value(napi_env env,
                                            napi_value value,
                                            double* result);

/**
 * @brief Creates an ArkTS BigInt from C int64 data.
 *
 * @param env Current running virtual machine context.
 * @param value C int64 data.
 * @param result Created ArkTS BigInt object from C int64 data.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_bigint_int64(napi_env env,
                                                 int64_t value,
                                                 napi_value* result);

/**
 * @brief Creates an ArkTS BigInt from C int64 data.
 *
 * @param env Current running virtual machine context.
 * @param value C int64 data.
 * @param result Created ArkTS BigInt object from C int64 data.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_bigint_uint64(napi_env env,
                                                  uint64_t value,
                                                  napi_value* result);

/**
 * @brief Creates a single ArkTS BigInt from a C uint64 array.
 *
 * @param env Current running virtual machine context.
 * @param sign_bit Sign bit of the BigInt. If sign_bit is 0, the BigInt is positive, otherwise it is negative.
 * @param word_count The size of the words array.
 * @param words C uint64 array in little-endian 64-bit format.
 * @param result Created ArkTS BigInt object from C int64 array.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, words or result is nullptr or word_count is larger than 2147483647.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_bigint_words(napi_env env,
                                                 int sign_bit,
                                                 size_t word_count,
                                                 const uint64_t* words,
                                                 napi_value* result);

/**
 * @brief Obtains a signed 64-bit integer from an ArkTS BigInt object.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS BigInt object.
 * @param result Pointer points to the location where store the C signed 64-bit integer value.
 * @param lossless Indicates whether the conversion is lossless. If lossless is true, the conversion is lossless,
 *                 false otherwise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value, result or lossless is nullptr or word_count is larger than\n
 *                                   2147483647.\n
 *         {@link napi_bigint_expected } If the 'value' is not an ArkTS bigint object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_bigint_int64(napi_env env,
                                                    napi_value value,
                                                    int64_t* result,
                                                    bool* lossless);

/**
 * @brief Obtains an unsigned 64-bit integer from an ArkTS BigInt object.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS BigInt object.
 * @param result Pointer points to the location where store the C unsigned 64-bit integer value.
 * @param lossless Indicates whether the conversion is lossless. If lossless is true, the conversion is lossless,
 *                 false otherwise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value, result or lossless is nullptr or word_count is larger than\n
 *                                   2147483647.\n
 *         {@link napi_bigint_expected } If the 'value' is not an ArkTS bigint object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_bigint_uint64(napi_env env,
                                                     napi_value value,
                                                     uint64_t* result,
                                                     bool* lossless);

/**
 * @brief Obtains the underlying 64-bit unsigned (uint64) byte data from an ArkTS BigInt object.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS BigInt object.
 * @param sign_bit Sign bit of the BigInt. If sign_bit is 0, the BigInt is positive, otherwise it is negative.
 * @param word_count The size of the words array.
 * @param words C uint64 array in little-endian 64-bit format.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or word_count is nullptr or word_count is larger than 2147483647.\n
 *         {@link napi_bigint_expected } If the 'value' is not an ArkTS bigint object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_bigint_words(napi_env env,
                                                    napi_value value,
                                                    int* sign_bit,
                                                    size_t* word_count,
                                                    uint64_t* words);

/**
 * @brief Creates an ArkTS ArrayBuffer object of the specified size and initializes it with the given data.
 *
 * @param env Current running virtual machine context.n
 * @param length Bytes size of the given data.
 * @param data Given data.
 * @param finalize_cb Optional native callback that can be used to free the given data when the ArkTS ArrayBuffer
 *                    object has been garbage-collected.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 * @param result Created ArkTS ArrayBuffer object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, data or result is nullptr, or length is larger than 2097152,
 *                                   or length is less than or equal to zero.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_external_buffer(napi_env env,
                                                    size_t length,
                                                    void* data,
                                                    napi_finalize finalize_cb,
                                                    void* finalize_hint,
                                                    napi_value* result);

/**
 * @brief Creates an ArkTS ArrayBuffer object of the specified size and initializes it with the given data.
 *
 * @param env Current running virtual machine context.
 * @param length Bytes size of the given data.
 * @param data Given data.
 * @param result_data Raw pointer to the underlying arraybuffer.
 * @param result Created ArkTS ArrayBuffer object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, data or result is nullptr, or length is larger than 2097152,
 *                                   or length is less than or equal to zero.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_buffer_copy(napi_env env,
                                                size_t length,
                                                const void* data,
                                                void** result_data,
                                                napi_value* result);

/**
 * @brief Checks whether the given ArkTS value is a 'ArrayBuffer' object.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS ArrayBuffer object.
 * @param result Boolean value that is set to true if the 'value' is a 'ArrayBuffer' object, false otherwise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_buffer(napi_env env,
                                       napi_value value,
                                       bool* result);

/**
 * @brief Obtains the underlying data of 'ArrayBuffer' and its length.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS ArrayBuffer object.
 * @param data Raw pointer to the underlying arraybuffer.
 * @param length Bytes size of the underlying arraybuffer.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or result is nullptr.\n
 *         {@link napi_arraybuffer_expected } If the 'value' is not an ArkTS array buffer object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_buffer_info(napi_env env,
                                             napi_value value,
                                             void** data,
                                             size_t* length);

/**
 * @brief Freezes an ArkTS object. Once an object is frozen, its properties are immutable.
 *
 * @param env Current running virtual machine context.
 * @param object The given ArkTS object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or object is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_object_freeze(napi_env env,
                                           napi_value object);

/**
 * @brief Seals an ArkTS object. Once an object is sealed, its properties cannot be added or deleted, but property
 *        values can be modified.
 *
 * @param env Current running virtual machine context.
 * @param object The given ArkTS object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or object is nullptr.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_object_seal(napi_env env,
                                         napi_value object);

/**
 * @brief Detaches the underlying data from an 'ArrayBuffer' object. After the data is detached, you
 *        can operate the data in C/C++.
 *
 * @param env Current running virtual machine context.
 * @param arraybuffer ArkTS ArrayBuffer object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or arraybuffer is nullptr, if 'arraybuffer' is not an ArrayBuffer object.\n
 *         {@link napi_object_expected } If the 'arraybuffer' is not an ArkTS object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_detach_arraybuffer(napi_env env,
                                                napi_value arraybuffer);

/**
 * @brief Checks whether the given 'ArrayBuffer' has been detached.
 *
 * @param env Current running virtual machine context.
 * @param value ArkTS ArrayBuffer object.
 * @param result Boolean value that is set to true if the 'value' has been detached, false otherwise.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_detached_arraybuffer(napi_env env,
                                                     napi_value value,
                                                     bool* result);

/**
 * @brief Obtains the names of all properties of an ArkTS object.
 *
 * @param env Current running virtual machine context.
 * @param object ArkTS object.
 * @param key_mode Key collection mode. If key_mode is napi_key_include_prototypes, the result includes properties on
 *                 prototypes. If key_mode is napi_key_own_only, the result includes only properties directly on own
 *                 object.
 * @param key_filter Which properties to be collected.
 * @param key_conversion Key conversion mode. If key_conversion is napi_key_keep_numbers, the numbered property keys
 *                       will keep number type. If key_conversion is napi_key_numbers_to_strings, the numbered property
 *                       keys will be convert to string type.
 * @param result An array of ArkTS object that represent the property names of the object.
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, object or result is nullptr;\n
 *                                   key_mode is not enumeration value of napi_key_collection_mode;\n
 *                                   key_conversion is not enumeration value of napi_key_conversion.\n
 *         {@link napi_pending_exception } If an ArkTS exception existed when the function was called.\n
 *         {@link napi_object_expected } If object is not object type and function type.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_all_property_names(napi_env env,
                                                    napi_value object,
                                                    napi_key_collection_mode key_mode,
                                                    napi_key_filter key_filter,
                                                    napi_key_conversion key_conversion,
                                                    napi_value* result);

/**
 * @brief Registers a native module.
 *
 * @param mod Native module of type 'napi_module' to be registered.
 * @since 10
 */
NAPI_EXTERN void napi_module_register(napi_module* mod);

/**
 * @brief Obtains the napi_extended_error_info struct, which contains the latest error information.
 *
 * @param env Current running virtual machine context.
 * @param result The error info about the error.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_last_error_info(napi_env env,
                                                 const napi_extended_error_info** result);

/**
 * @brief Throws an ArkTS error.
 * @param env Current running virtual machine context.
 * @param error The ArkTS error to be thrown.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or error is nullptr, or error is not an error object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_throw(napi_env env, napi_value error);

/**
 * @brief Throws an ArkTS Error with text information.
 * @param env Current running virtual machine context.
 * @param code Optional error code to set on the error.
 * @param msg C string representing the text to be associated with the error.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or msg is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_throw_error(napi_env env,
                                         const char* code,
                                         const char* msg);

/**
 * @brief Throws an ArkTS TypeError with text information.
 * @param env Current running virtual machine context.
 * @param code Optional error code to set on the error.
 * @param msg C string representing the text to be associated with the error.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or msg is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_throw_type_error(napi_env env,
                                              const char* code,
                                              const char* msg);

/**
 * @brief Throws an ArkTS RangeError with text information.
 * @param env Current running virtual machine context.
 * @param code Optional error code to set on the error.
 * @param msg C string representing the text to be associated with the error.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or msg is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_throw_range_error(napi_env env,
                                               const char* code,
                                               const char* msg);

/**
 * @brief Checks whether a 'napi_value' is an error object.
 * @param env Current running virtual machine context.
 * @param value The value to check
 * @param result Boolean value that is set to true if the value represents an error object, false otherwise.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_error(napi_env env,
                                      napi_value value,
                                      bool* result);

/**
 * @brief Creates an ArkTS Error with text information.
 * @param env Current running virtual machine context.
 * @param code Optional error code to set on the error.
 * @param msg napi_value representing the EcmaScript string to be associated with the error.
 * @param result napi_value representing the error created.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, msg or result is nullptr, code is not string and number type or msg is\n
 *                                   not a string type.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_error(napi_env env,
                                          napi_value code,
                                          napi_value msg,
                                          napi_value* result);

/**
 * @brief Creates an ArkTS TypeError with text information.
 * @param env Current running virtual machine context.
 * @param code Optional error code to set on the error.
 * @param msg napi_value representing the EcmaScript string to be associated with the error.
 * @param result napi_value representing the error created.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, msg or result is nullptr, code is not string and number type or msg is\n
 *                                   not a string type.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_type_error(napi_env env,
                                               napi_value code,
                                               napi_value msg,
                                               napi_value* result);

/**
 * @brief Creates an ArkTS RangeError with text information.
 * @param env Current running virtual machine context.
 * @param code Optional error code to set on the error.
 * @param msg napi_value representing the EcmaScript string to be associated with the error.
 * @param result napi_value representing the error created.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, msg or result is nullptr, code is not string and number type or msg is\n
 *                                   not a string type.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_range_error(napi_env env,
                                                napi_value code,
                                                napi_value msg,
                                                napi_value* result);

/**
 * @brief Checks whether an exception occurs.
 * @param env Current running virtual machine context.
 * @param result Boolean value that is true if there is a pending exception.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_exception_pending(napi_env env, bool* result);

/**
 * @brief Obtains and clears the latest exception.
 * @param env Current running virtual machine context.
 * @param result The exception if there is a pending exception; otherwise return a null value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_and_clear_last_exception(napi_env env,
                                                          napi_value* result);

/**
 * @brief Raises a fatal error to terminate the process immediately.
 * @param location Optional location for the error occurrence.
 * @param location_len The byte length of the location, or NAPI_AUTO_LENGTH if it is terminated by a null character.
 * @param message The message associated with the error.
 * @param message_len The byte length of the message, or NAPI_AUTO_LENGTH if it is terminated by a null character.
 *
 * @since 10
 */
NAPI_EXTERN NAPI_NO_RETURN void napi_fatal_error(const char* location,
                                                 size_t location_len,
                                                 const char* message,
                                                 size_t message_len);

/**
 * @brief Opens a scope.
 * @param env Current running virtual machine context.
 * @param result napi_value representing the new scope.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_open_handle_scope(napi_env env,
                                               napi_handle_scope* result);

/**
 * @brief Closes the scope passed in. After the scope is closed, all references declared in it are closed.
 * @param env Current running virtual machine context.
 * @param scope The scope to close.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or scope is nullptr.\n
 *         {@link napi_handle_scope_mismatch } If there is no scope still existed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_close_handle_scope(napi_env env,
                                                napi_handle_scope scope);

/**
 * @brief Opens an escapable handle scope from which the declared values can be returned to the outer scope.
 * @param env Current running virtual machine context.
 * @param result The new scope.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_open_escapable_handle_scope(napi_env env,
                                                         napi_escapable_handle_scope* result);

/**
 * @brief Closes the escapable handle scope passed in.
 * @param env Current running virtual machine context.
 * @param scope The scope to close.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or scope is nullptr.\n
 *         {@link napi_handle_scope_mismatch } If there is no scope still existed.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_close_escapable_handle_scope(napi_env env,
                                                          napi_escapable_handle_scope scope);

/**
 * @brief Promotes the handle to the input ArkTS object so that it is valid for the lifespan of its outer scope.
 * @param env Current running virtual machine context.
 * @param scope Current scope.
 * @param escapee The ArkTS object to be escaped.
 * @param result The handle to the escaped object in the outer scope.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, scope, escapee or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_escape_handle(napi_env env,
                                           napi_escapable_handle_scope scope,
                                           napi_value escapee,
                                           napi_value* result);

/**
 * @brief Creates a reference for an object to extend its lifespan. The caller needs to manage the reference lifespan.
 * @param env Current running virtual machine context.
 * @param value The napi_value that is being referenced.
 * @param initial_refcount The initial count for the new reference.
 * @param result napi_ref pointing to the new reference.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, value or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_reference(napi_env env,
                                              napi_value value,
                                              uint32_t initial_refcount,
                                              napi_ref* result);

/**
 * @brief Deletes the reference passed in.
 * @param env Current running virtual machine context.
 * @param ref The napi_ref to be deleted.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or ref is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_delete_reference(napi_env env, napi_ref ref);

/**
 * @brief Increments the reference count for the reference passed in and returns the count.
 * @param env Current running virtual machine context.
 * @param ref The napi_ref whose reference count will be incremented.
 * @param result The new reference count.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or ref is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_reference_ref(napi_env env,
                                           napi_ref ref,
                                           uint32_t* result);

/**
 * @brief Decrements the reference count for the reference passed in and returns the count.
 * @param env Current running virtual machine context.
 * @param ref The napi_ref whose reference count will be decremented.
 * @param result The new reference count.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env or ref is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_reference_unref(napi_env env,
                                             napi_ref ref,
                                             uint32_t* result);

/**
 * @brief Obtains the ArkTS Object associated with the reference.
 * @param env Current running virtual machine context.
 * @param ref The napi_ref of the value being requested.
 * @param result The napi_value referenced by the napi_ref.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If env, ref or result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_reference_value(napi_env env,
                                                 napi_ref ref,
                                                 napi_value* result);

/**
 * @brief Check if the given ArkTS Object has the named own property or not.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param key The name of the property to check.
 * @param result Whether the own property exists on the object or not.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, key and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurs in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_has_own_property(napi_env env,
                                              napi_value object,
                                              napi_value key,
                                              bool* result);

/**
 * @brief Defines an ArkTS class, including constructor function and properties.
 * @param env Current running virtual machine context.
 * @param utf8name Name of the ArkTS constructor function.
 * @param length The length of the utf8name in bytes, or NAPI_AUTO_LENGTH if it is null-terminated.
 * @param constructor Callback function that handles constructing instances of the class.
 * @param data Optional data to be passed to the constructor callback as the data property of the callback info.
 * @param property_count Number of items in the properties array argument.
 * @param properties Array of property descriptors.
 * @param result A napi_value representing the constructor function for the class.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n execution.\n
 *         {@link napi_invalid_arg } If the param env, utf8name and(or) result is nullptr. If napi_property_descriptor
 *                                   is nullptr but property_count greater than 0.\n
 *         {@link napi_function_expected } If the param func is not an ArkTS Function.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurs in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_define_class(napi_env env,
                                          const char* utf8name,
                                          size_t length,
                                          napi_callback constructor,
                                          void* data,
                                          size_t property_count,
                                          const napi_property_descriptor* properties,
                                          napi_value* result);

/**
 * @brief Creates an ArkTS symbol.
 * @param env Current running virtual machine context.
 * @param description Optional napi_value representing an ArkTS string to describe the symbol.
 * @param result A napi_value representing an ArkTS symbol.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr;\n
 *                                   If the param description is not nullptr and is not an ArkTS String.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_symbol(napi_env env,
                                           napi_value description,
                                           napi_value* result);

/**
 * @brief Create an ArkTS function. This is the primary mechanism to call back into native code from ArkTS.
 * @param env Current running virtual machine context.
 * @param utf8name The visible within ArkTS as the new function's name property.
 * @param length The length oh the utf8name, or NAPI_AUTO_LENGTH if it is null-terminated.
 * @param cb The native function which should be called when this function object is called.
 * @param data User-provided data context. This will be passed back into the function when invoked.
 * @param result The newly created ArkTS function.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, cb and(or) result is nullptr.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_function(napi_env env,
                                             const char* utf8name,
                                             size_t length,
                                             napi_callback cb,
                                             void* data,
                                             napi_value* result);

/**
 * @brief Similar to typeof operation, support external value, detects null as a separate type.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value whose type expects to query.
 * @param result The type of ArkTS value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_typeof(napi_env env,
                                    napi_value value,
                                    napi_valuetype* result);

/**
 * @brief Obtains the double value corresponding to the given ArkTS value.
 * @param env Current running virtual machine context.
 * @param value ArkTS number.
 * @param result The C primitive equivalent of ArkTS value as double.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 *         {@link napi_number_expected } If a non-number ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_double(napi_env env,
                                              napi_value value,
                                              double* result);

/**
 * @brief Obtains the int32_t value corresponding to the given ArkTS value.
 * @param env Current running virtual machine context.
 * @param value ArkTS number.
 * @param result The C primitive equivalent of ArkTS value as int32_t.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 *         {@link napi_number_expected } If a non-number ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_int32(napi_env env,
                                             napi_value value,
                                             int32_t* result);

/**
 * @brief Obtains the uint32_t value corresponding to the given ArkTS value.
 * @param env Current running virtual machine context.
 * @param value ArkTS number.
 * @param result The C primitive equivalent of ArkTS value as uint32_t.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 *         {@link napi_number_expected } If a non-number ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_uint32(napi_env env,
                                              napi_value value,
                                              uint32_t* result);

/**
 * @brief Obtains the int64_t value corresponding to the given ArkTS value.
 * @param env Current running virtual machine context.
 * @param value ArkTS number.
 * @param result The C primitive equivalent of ArkTS value as int64_t.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 *         {@link napi_number_expected } If a non-number ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_int64(napi_env env,
                                             napi_value value,
                                             int64_t* result);

/**
 * @brief Obtains the C Boolean equivalent of an ArkTS Boolean value.
 * @param env Current running virtual machine context.
 * @param value A napi_value representing ArkTS Boolean.
 * @param result The C boolean equivalent of the ArkTS Boolean.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 *         {@link napi_boolean_expected } If a non-boolean ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_bool(napi_env env,
                                            napi_value value,
                                            bool* result);

/**
 * @brief Obtains the ISO-8859-1-encoded string corresponding to the given ArkTS value.
 * @param env Current running virtual machine context.
 * @param value ArkTS string.
 * @param buf Destination buffer that will be filled with the provided ISO-8859-1-encoded string.
 * @param bufsize The size of the buffer 'buf'.
 * @param result The length of the string in ISO-8859-1-encoded format.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) value is nullptr;\n
 *                                   If the param buf and result both are nullptr.\n
 *         {@link napi_string_expected } If a non-string ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_string_latin1(napi_env env,
                                                     napi_value value,
                                                     char* buf,
                                                     size_t bufsize,
                                                     size_t* result);

/**
 * @brief Obtains the UTF8-encoded string corresponding to the given ArkTS value.
 * @param env Current running virtual machine context.
 * @param value ArkTS string.
 * @param buf Destination buffer that will be filled with the provided UTF8-encoded string.
 * @param bufsize The size of the buffer 'buf'.
 * @param result The length of the string in UTF8-encoded format.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) value is nullptr;\n
 *                                   If the param buf and result both are nullptr.\n
 *         {@link napi_string_expected } If a non-string ArkTS value passed in it.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_string_utf8(napi_env env,
                                                   napi_value value,
                                                   char* buf,
                                                   size_t bufsize,
                                                   size_t* result);

/**
 * @brief Obtains the ArkTS undefined value.
 * @param env Current running virtual machine context.
 * @param result The ArkTS undefined value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the parameter env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_undefined(napi_env env, napi_value* result);

/**
 * @brief Obtains the ArkTS null value.
 * @param env Current running virtual machine context.
 * @param result The ArkTS null value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_null(napi_env env, napi_value* result);

/**
 * @brief Obtains the ArkTS global object.
 * @param env Current running virtual machine context.
 * @param result The ArkTS global Object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_global(napi_env env, napi_value* result);

/**
 * @brief Obtains the ArkTS singleton value corresponding to given C primitive boolean value.
 * @param env Current running virtual machine context.
 * @param value C primitive boolean value.
 * @param result The ArkTS singleton value equivalent of C primitive boolean value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_boolean(napi_env env,
                                         bool value,
                                         napi_value* result);

// Methods to create Primitive types/Objects

/**
 * @brief Creates a default ArkTS object.
 * @param env Current running virtual machine context.
 * @param result napi_value representing an ArkTS object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_object(napi_env env, napi_value* result);

/**
 * @brief Creates an ArkTS array.
 * @param env Current running virtual machine context.
 * @param result napi_value representing an ArkTS Array.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_array(napi_env env, napi_value* result);

/**
 * @brief Creates an ArkTS array of the specified length.
 * @param env Current running virtual machine context.
 * @param length The length of the Array.
 * @param result napi_value representing an ArkTS Array.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_array_with_length(napi_env env,
                                                      size_t length,
                                                      napi_value* result);

/**
 * @brief Creates an ArkTS number from C double data.
 * @param env Current running virtual machine context.
 * @param value The double value to be represented in ArkTS.
 * @param result A napi_value representing an ArkTS number.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_double(napi_env env,
                                           double value,
                                           napi_value* result);

/**
 * @brief Creates an ArkTS number from C int32_t data.
 * @param env Current running virtual machine context.
 * @param value The int32 value to be represented in ArkTS.
 * @param result A napi_value representing an ArkTS number.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_int32(napi_env env,
                                          int32_t value,
                                          napi_value* result);

/**
 * @brief Creates an ArkTS number from C uint32_t data.
 * @param env Current running virtual machine context.
 * @param value The uint32 value to be represented in ArkTS.
 * @param result A napi_value representing an ArkTS number.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_uint32(napi_env env,
                                           uint32_t value,
                                           napi_value* result);

/**
 * @brief Creates an ArkTS number from C int64_t data.
 * @param env Current running virtual machine context.
 * @param value The int64 value to be represented in ArkTS.
 * @param result A napi_value representing an ArkTS number.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_int64(napi_env env,
                                          int64_t value,
                                          napi_value* result);

/**
 * @brief Creates an ArkTS string from an ISO-8859-1-encoded C string.
 * @param env Current running virtual machine context.
 * @param str C string encoded in ISO-8859-1-encoded format.
 * @param length The length of the C string 'str'.
 * @param result Result of the ArkTS string from the ISO-8859-1-encoded C string.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, str and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_string_latin1(napi_env env,
                                                  const char* str,
                                                  size_t length,
                                                  napi_value* result);

/**
 * @brief Creates an ArkTS string from a UTF8-encoded C string.
 * @param env Current running virtual machine context.
 * @param str C string encoded in UTF8 format.
 * @param length The length of the C string 'str'.
 * @param result Result of the ArkTS string from the UTF8-encoded C string.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, str and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_string_utf8(napi_env env,
                                                const char* str,
                                                size_t length,
                                                napi_value* result);

/**
 * @brief Checks if the ArkTS value is an ArkTS ArrayBuffer.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to check.
 * @param result Whether the given ArkTS value is an ArkTS ArrayBuffer.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_arraybuffer(napi_env env,
                                            napi_value value,
                                            bool* result);
/**
 * @brief Creates an ArkTS ArrayBuffer of the specified size.
 * @param env Current running virtual machine context.
 * @param byte_length The length in bytes of the array buffer.
 * @param data The byte buffer of the ArrayBuffer.
 * @param result A napi_value representing an ArkTS ArrayBuffer.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, data and(or) result is nullptr.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_arraybuffer(napi_env env,
                                                size_t byte_length,
                                                void** data,
                                                napi_value* result);

/**
 * @brief Allocates a JS value with external data.
 * @param env Current running virtual machine context.
 * @param data Allocates a JS value that references external data.
 * @param finalize_cb Optional callback to call when the external value is being collected.
 * @param finalize_hint Optional hint that can be passed to the finalize callback function during the garbage
 *                      collection process.
 * @param result A napi_value representing an external value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env or result is nullptr.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_external(napi_env env,
                                             void* data,
                                             napi_finalize finalize_cb,
                                             void* finalize_hint,
                                             napi_value* result);

/**
 * @brief The underlying data that ArrayBuffer point to.
 * @param env Current running virtual machine context.
 * @param external_data Allocates an ArkTS value that references external data.
 * @param byte_length The length in bytes of the underlying buffer.
 * @param finalize_cb Optional callback to call when the ArrayBuffer is being collected.
 * @param finalize_hint Optional hint that can be passed to the finalize callback function during the garbage
 *                      collection process.
 * @param result A napi_value representing an ArkTS ArrayBuffer.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, external_data, finalize_cb and(or) result is nullptr.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_external_arraybuffer(napi_env env,
                                                         void* external_data,
                                                         size_t byte_length,
                                                         napi_finalize finalize_cb,
                                                         void* finalize_hint,
                                                         napi_value* result);

/**
 * @brief Obtains the underlying data buffer of ArrayBuffer and its length.
 * @param env Current running virtual machine context.
 * @param arraybuffer The napi_value representing the ArrayBuffer being queried.
 * @param data The underlying data buffer of the ArrayBuffer.
 * @param byte_length Length in bytes of the underlying data buffer.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, arraybuffer and(or) byte_length is nullptr.\n
 *         {@link napi_arraybuffer_expected } If the param is neither ArkTS TypedArray nor SendableArrayBuffer.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_arraybuffer_info(napi_env env,
                                                  napi_value arraybuffer,
                                                  void** data,
                                                  size_t* byte_length);

/**
 * @brief Checks if the ArkTS value is an ArkTS TypedArray.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to check.
 * @param result Whether the given ArkTS value is an ArkTS TypedArray.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_typedarray(napi_env env,
                                           napi_value value,
                                           bool* result);
/**
 * @brief Creates an ArkTS TypeArray from an existing ArrayBuffer.
 * @param env Current running virtual machine context.
 * @param type The element datatype of the TypedArray.
 * @param length Number of elements in the TypedArray.
 * @param arraybuffer The underlying ArrayBuffer that supports the TypedArray.
 * @param byte_offset The byte offset within the ArrayBuffer from which to start projecting the TypedArray.
 * @param result A napi_value representing an ArkTS TypedArray.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, arraybuffer and(or) result is nullptr;\n
 *                                   If param type is not a valid napi_typedarray_type.\n
 *         {@link napi_arraybuffer_expected } If a non-arraybuffer ArkTS value passed in it.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_typedarray(napi_env env,
                                               napi_typedarray_type type,
                                               size_t length,
                                               napi_value arraybuffer,
                                               size_t byte_offset,
                                               napi_value* result);

/**
 * @brief Obtains properties of a TypedArray.
 * @param env Current running virtual machine context.
 * @param typedarray The napi_value for the TypedArray whose properties are being checked.
 * @param type The datatype of elements in the TypedArray.
 * @param length The number of elements in the TypedArray.
 * @param data The data buffer underlying the TypedArray adjusted by the byte_offset.
 * @param arraybuffer The ArrayBuffer underlying the TypedArray.
 * @param byte_offset The byte offset within the underlying arraybuffer
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) typedarray is nullptr;\n
 *                                    If the param typedarray is neither ArkTS TypedArray nor SendableTypedArray.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_typedarray_info(napi_env env,
                                                 napi_value typedarray,
                                                 napi_typedarray_type* type,
                                                 size_t* length,
                                                 void** data,
                                                 napi_value* arraybuffer,
                                                 size_t* byte_offset);

/**
 * @brief Creates an ArkTS DataView from an existing ArrayBuffer.
 * @param env Current running virtual machine context.
 * @param length Number of elements in the DataView.
 * @param arraybuffer The underlying ArrayBuffer that supports the DataView.
 * @param byte_offset The byte offset within the ArrayBuffer from which to start projecting the DataView.
 * @param result A napi_value representing an ArkTS DataView.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, arraybuffer and(or) result is nullptr.\n
 *         {@link napi_arraybuffer_expected } If a non-arraybuffer ArkTS value passed in it.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 *                                         If the sum of byte_length and length is greater than the byte length of\n
 *                                         the arraybuffer.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_dataview(napi_env env,
                                             size_t length,
                                             napi_value arraybuffer,
                                             size_t byte_offset,
                                             napi_value* result);

/**
 * @brief Checks if the ArkTS value is an ArkTS DataView.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to check.
 * @param result Whether the given ArkTS value is an ArkTS DataView.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_dataview(napi_env env,
                                         napi_value value,
                                         bool* result);

/**
 * @brief Obtains properties of a DataView.
 * @param env Current running virtual machine context.
 * @param dataview The napi_value for the DataView whose properties are being checked.
 * @param bytelength The number of elements in the DataView.
 * @param data The data buffer underlying the DataView.
 * @param arraybuffer The ArrayBuffer underlying the DataView.
 * @param byte_offset The byte offset within the underlying arraybuffer
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) dataview is nullptr;\n
 *                                   If non-dataview ArkTS value passed in.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_dataview_info(napi_env env,
                                               napi_value dataview,
                                               size_t* bytelength,
                                               void** data,
                                               napi_value* arraybuffer,
                                               size_t* byte_offset);

/**
 * @brief Obtains the array length.
 * @param env Current running virtual machine context.
 * @param value The napi_value representing the ArkTS Array being queried.
 * @param result The length of the array.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr;\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_array_length(napi_env env,
                                              napi_value value,
                                              uint32_t* result);
/**
 * @brief  Obtains the prototype of an ArkTS object.
 * @param env Current running virtual machine context.
 * @param object The napi_value representing an ArkTS Object whose prototype to return.
 * @param result A napi_value representing prototype of the object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object or result is nullptr;\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_prototype(napi_env env,
                                           napi_value object,
                                           napi_value* result);

/**
 * @brief Obtains the external data pointer previously passed through napi_create_external().
 * @param env Current running virtual machine context.
 * @param value JavaScript external value.
 * @param result The data wrapped by the JavaScript external value.

 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value or result is nullptr;\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_value_external(napi_env env,
                                                napi_value value,
                                                void** result);

/**
 * @brief Coerce the given ArkTS value to an ArkTS boolean value.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to coerce.
 * @param result The coerced ArkTS boolean value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_coerce_to_bool(napi_env env,
                                            napi_value value,
                                            napi_value* result);

/**
 * @brief Coerce the given ArkTS value to an ArkTS number value.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to coerce.
 * @param result The coerced ArkTS number value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_coerce_to_number(napi_env env,
                                              napi_value value,
                                              napi_value* result);

/**
 * @brief Coerce the given ArkTS value to an ArkTS object value.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to coerce.
 * @param result The coerced ArkTS object value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_coerce_to_object(napi_env env,
                                              napi_value value,
                                              napi_value* result);

/**
 * @brief Coerce the given ArkTS value to an ArkTS string value.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to coerce.
 * @param result The coerced ArkTS string value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_coerce_to_string(napi_env env,
                                              napi_value value,
                                              napi_value* result);

/**
 * @brief Invoke instanceof operation on the object.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object to check.
 * @param constructor The ArkTS constructor function to check against.
 * @param result Set to true if the given ArkTS object instanceof constructor.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, constructor and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS object value.\n
 *         {@link napi_function_expected } If the param constructor is not an ArkTS function value.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_instanceof(napi_env env,
                                        napi_value object,
                                        napi_value constructor,
                                        bool* result);

/**
 * @brief Checks if the ArkTS value is an ArkTS Array.
 * @param env Current running virtual machine context.
 * @param value The ArkTS value to check.
 * @param result Whether the given ArkTS value is an ArkTS Array.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_is_array(napi_env env,
                                      napi_value value,
                                      bool* result);

/**
 * @brief Checks if the two ArkTS values are equal.
 * @param env Current running virtual machine context.
 * @param lhs The ArkTS value to check.
 * @param rhs The ArkTS value to check against.
 * @param result Whether the two given ArkTS values are equal.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_strict_equals(napi_env env,
                                           napi_value lhs,
                                           napi_value rhs,
                                           bool* result);

/**
 * @brief Obtains the names of the enumerable properties of object as an Array of Strings. The keys that are symbols
 *        will not be included.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object from which the property is retrieved.
 * @param result An ArkTS Array that contains the attribute names of the object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, value and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_property_names(napi_env env,
                                                napi_value object,
                                                napi_value* result);

/**
 * @brief Set a property on the given ArkTS Object.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param key The name of the property to set.
 * @param value The property value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, key and(or) value is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_set_property(napi_env env,
                                          napi_value object,
                                          napi_value key,
                                          napi_value value);

/**
 * @brief Get the requests property of the given ArkTS Object.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param key The name of the property to get.
 * @param result The value of the property.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, key and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_property(napi_env env,
                                          napi_value object,
                                          napi_value key,
                                          napi_value* result);

/**
 * @brief Check if the given ArkTS Object has the named property or not.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param key The name of the property to check.
 * @param result Whether the property exists on the object or not.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, key and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_has_property(napi_env env,
                                          napi_value object,
                                          napi_value key,
                                          bool* result);
/**
 * @brief Delete the named property of the given ArkTS Object.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param key The name of the property to delete.
 * @param result Whether the execution is succeed or not. Can optionally be ignored by passing nullptr.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object and(or) key is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_delete_property(napi_env env,
                                             napi_value object,
                                             napi_value key,
                                             bool* result);

/**
 * @brief Set a property on the given ArkTS Object.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param utf8name The name of the property to set.
 * @param value The property value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, utf8name and(or) value is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_set_named_property(napi_env env,
                                                napi_value object,
                                                const char* utf8name,
                                                napi_value value);

/**
 * @brief Get the requests property of the given ArkTS Object.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param utf8name The name of the property to get.
 * @param result The value of the property.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, utf8name and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_named_property(napi_env env,
                                                napi_value object,
                                                const char* utf8name,
                                                napi_value* result);

/**
 * @brief Check if the given ArkTS Object has the named property or not.
 * @param env Current running virtual machine context.
 * @param object The ArkTS object.
 * @param utf8name The name of the property to check.
 * @param result Whether the property exists on the object or not.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object, utf8name and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_has_named_property(napi_env env,
                                                napi_value object,
                                                const char* utf8name,
                                                bool* result);

/**
 * @brief Set a element on the given ArkTS Array.
 * @param env Current running virtual machine context.
 * @param object The ArkTS Array.
 * @param index The index of the element to set.
 * @param value The element value.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object and(or) value is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_set_element(napi_env env,
                                         napi_value object,
                                         uint32_t index,
                                         napi_value value);

/**
 * @brief Get the requests element of the given ArkTS Array.
 * @param env Current running virtual machine context.
 * @param object The ArkTS Array.
 * @param index The index of the element to get.
 * @param result The value of the element.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_element(napi_env env,
                                         napi_value object,
                                         uint32_t index,
                                         napi_value* result);

/**
 * @brief Check if the given ArkTS Array has an element at the requested index.
 * @param env Current running virtual machine context.
 * @param object The ArkTS Array.
 * @param index The name of the property to check.
 * @param result Whether the property exists on the Array or not.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_has_element(napi_env env,
                                         napi_value object,
                                         uint32_t index,
                                         bool* result);

/**
 * @brief Delete the special index from the given ArkTS Array.
 * @param env Current running virtual machine context.
 * @param object The ArkTS Array.
 * @param index The index of the element to delete.
 * @param result Whether the execution is succeed or not. Can optionally  be ignored by passing nullptr.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object and(or) key is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_delete_element(napi_env env,
                                            napi_value object,
                                            uint32_t index,
                                            bool* result);

/**
 * @brief Efficient define multiple properties on the given ArkTS Object by napi_property_descriptor.
 * @param env Current running virtual machine context.
 * @param object The ArkTS Object.
 * @param property_count The count of elements in the properties array.
 * @param properties The properties array.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, object and(or) properties is nullptr.\n
 *         {@link napi_object_expected } If the param object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_define_properties(napi_env env,
                                               napi_value object,
                                               size_t property_count,
                                               const napi_property_descriptor* properties);

/**
 * @brief Invoke an ArkTS function. This is the primary mechanism to call back into JavaScript.
 * @param env Current running virtual machine context.
 * @param recv The this value passed to the called function
 * @param func The ArkTS function to be invoked.
 * @param argc The count of elements in the argv array.
 * @param argv ArkTS values passed in as arguments to the function.
 * @param result Whether the provided 'type_tag' is matched with the tag on the ArkTS object 'value'.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) func is nullptr. If argv is nullptr but argc greater\n
 *                                   than 0.\n
 *         {@link napi_function_expected } If the param func is not an ArkTS Function.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_call_function(napi_env env,
                                           napi_value recv,
                                           napi_value func,
                                           size_t argc,
                                           const napi_value* argv,
                                           napi_value* result);

/**
 * @brief Obtains callback details about the call like arguments, this from given callback info.
 * @param env Current running virtual machine context.
 * @param cbinfo The callback info.
 * @param argc Size of the argv array.
 * @param argv The Array which arguments will be copied to. If there are more arguments than the provided count, only
 *             the requested number of arguments are copied. If there are fewer arguments provided, the rest argv is
 *             filled with undefined. Can optionally be ignored by passing nullptr.
 * @param this_arg Receives the ArkTS this argument for the call. Can optionally be ignored by passing nullptr.
 * @param data Receives the data pointer for the callback. Can optionally be ignored by passing nullptr.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) cbinfo is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_cb_info(napi_env env,
                                         napi_callback_info cbinfo,
                                         size_t* argc,
                                         napi_value* argv,
                                         napi_value* this_arg,
                                         void** data);

/**
 * @brief Obtains callback details about the call like arguments, this from given callback info.
 * @param env Current running virtual machine context.
 * @param cbinfo The callback info.
 * @param result The new.target of the constructor call.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, cbinfo and(or) result is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_get_new_target(napi_env env,
                                            napi_callback_info cbinfo,
                                            napi_value* result);

/**
 * @brief Instantiate a new ArkTS value using a given napi_value that represents the constructor for the object.
 * @param env Current running virtual machine context.
 * @param constructor The ArkTS function to be invoked as a constructor.
 * @param argc The count of elements in the argv array.
 * @param argv Array of ArkTS values representing the arguments to the constructor. If argc is 0 this parameter may
 *             be omitted by passing in nullptr.
 * @param result The ArkTS object returned, which in this case is the constructed object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) func is nullptr. If argv is nullptr but argc greater\n
 *                                   than 0.\n
 *         {@link napi_function_expected } If the param func is not an ArkTS Function.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_new_instance(napi_env env,
                                          napi_value constructor,
                                          size_t argc,
                                          const napi_value* argv,
                                          napi_value* result);

/**
 * @brief Wraps a native instance in a ArkTS object. The native instance can be retrieved later using napi_unwrap.
 * @param env Current running virtual machine context.
 * @param js_object The ArkTS object that will be the wrapper for the native object.
 * @param native_object The native instance that will be wrapped in the ArkTS object.
 * @param finalize_cb Native callback that can be used to free the native instance when the JavaScript object has
 *                    been garbage-collected.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 * @param result Optional reference to the wrapped object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, js_object, native_object and(or) finalize_cb is nullptr.\n
 *         {@link napi_object_expected } If the param js_object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_wrap(napi_env env,
                                  napi_value js_object,
                                  void* native_object,
                                  napi_finalize finalize_cb,
                                  void* finalize_hint,
                                  napi_ref* result);

/**
 * @brief Retrieves a native instance that was previously wrapped in an ArkTS object using napi_wrap.
 * @param env Current running virtual machine context.
 * @param js_object The ArkTS object.
 * @param result Pointer to the wrapped native instance.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, js_object and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param js_object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_unwrap(napi_env env,
                                    napi_value js_object,
                                    void** result);

/**
 * @brief Retrieves a native instance that was previously wrapped in the ArkTS object js_object using napi_wrap
 *        and removes the wrapping.
 * @param env Current running virtual machine context.
 * @param js_object The ArkTS object.
 * @param result Pointer to the wrapped native instance.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, js_object and(or) result is nullptr.\n
 *         {@link napi_object_expected } If the param js_object is not an ArkTS Object.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurred in execution.\n
 * @since 10
*/
NAPI_EXTERN napi_status napi_remove_wrap(napi_env env,
                                         napi_value js_object,
                                         void** result);

/**
 * @brief Allocate a work object that is used to execute logic asynchronously.
 * @param env Current running virtual machine context.
 * @param async_resource Not supported, can be ignored by passing nullptr.
 * @param async_resource_name  Identifier for the kind of resource that is being provided for diagnostic information
 *                             exposed by the HiTrace.
 * @param execute The native function which should be called to execute the logic asynchronously. The given function
 *                is called from a worker pool thread and can execute in parallel with the main event loop thread.
 * @param complete The native function which will be called when the asynchronous logic is completed or is cancelled.
 *                 The given function is called from the main event loop thread.
 * @param data User-provided data context. This will be passed back into the execute and complete functions.
 * @param result The handle to the newly created async work.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, async_resource_name, execute, complete and(or) result is\n
 *                                   nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_create_async_work(napi_env env,
                                               napi_value async_resource,
                                               napi_value async_resource_name,
                                               napi_async_execute_callback execute,
                                               napi_async_complete_callback complete,
                                               void* data,
                                               napi_async_work* result);

/**
 * @brief Free a previously allocated work object.
 * @param env Current running virtual machine context.
 * @param work The handle returned by the call to napi_create_async_work.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) work is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_delete_async_work(napi_env env,
                                               napi_async_work work);

/**
 * @brief Requests that the previously allocated work be scheduled for execution. Once it returns successfully,
 *        this API must not be called again with the same napi_async_work item or the result will be undefined.
 * @param env Current running virtual machine context.
 * @param work The handle returned by the call to napi_create_async_work.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) work is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_queue_async_work(napi_env env,
                                              napi_async_work work);

/**
 * @brief Cancels queued work if it has not yet been started. If it has already started executing, it cannot be
 *        cancelled. If successful, the complete callback will be invoked with a status value of napi_cancelled.
 *        The work should not be deleted before the complete callback invocation, even if it has been successfully
 *        cancelled.
 * @param env Current running virtual machine context.
 * @param work The handle returned by the call to napi_create_async_work.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env and(or) work is nullptr.\n
 * @since 10
 */
NAPI_EXTERN napi_status napi_cancel_async_work(napi_env env,
                                               napi_async_work work);

/**
 * @brief Wraps a native instance in an ArkTS object.
 * @param env The environment that the API is invoked under.
 * @param js_object The ArkTS object that will be the wrapper for the native object.
 * @param native_object The native instance that will be wrapped in the ArkTS object.
 * @param finalize_cb Optional native callback that can be used to free the native instance when the ArkTS object
 * has been garbage-collected.
 * @param async_finalizer A boolean value to determine that finalize_cb execute async or not.
 * @param finalize_hint Optional contextual hint that is passed to the finalize callback.
 * @param native_binding_size The size of native binding.
 * @param result Optional reference to the wrapped object.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env, js_object or native_object is nullptr.\n
 *         {@link napi_object_expected } If the param js_object is not an ArkTS Object or Function.\n
 *         {@link napi_pending_exception } There is an uncaught exception occurred before(in) execution.\n
 * @since 18
 */
NAPI_EXTERN napi_status napi_wrap_enhance(napi_env env,
                                          napi_value js_object,
                                          void* native_object,
                                          napi_finalize finalize_cb,
                                          bool async_finalizer,
                                          void* finalize_hint,
                                          size_t native_binding_size,
                                          napi_ref* result);

/**
 * @brief To create a new virtual machine context.
 * @param env Current running virtual machine context.
 * @param newEnv New generated virtual machine context which is expected to be used later.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env is nullptr.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurs in execution.\n
 * @since 20
 */
NAPI_EXTERN napi_status napi_create_ark_context(napi_env env, napi_env *newEnv);

/**
 * @brief To switch a virtual machine context which is expected to be used later.
 * @param env Designated Virtual machine context which is expected to be used as the current virtual machine context.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env is nullptr.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurs in execution.\n
 * @since 20
 */
NAPI_EXTERN napi_status napi_switch_ark_context(napi_env env);

/**
 * @brief To destroy a virtual machine context which will not be used again.
 * @param env Virtual machine context expected to be destroyed.
 *
 * @return Returns the function execution status.
 *         {@link napi_ok } If the function executed successfully.\n
 *         {@link napi_invalid_arg } If the param env is nullptr.\n
 *         {@link napi_pending_exception } If have uncaught exception, or exception occurs in execution.\n
 * @since 20
 */
NAPI_EXTERN napi_status napi_destroy_ark_context(napi_env env);
#ifdef __cplusplus
}
#endif
/** @} */
#endif /* FOUNDATION_ACE_NAPI_INTERFACES_KITS_NAPI_NATIVE_API_H */
