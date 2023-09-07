/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const path = require("path");
const fs = require('fs');

config.browserConsoleLogOptions.level = "debug";

const basePath = config.basePath;
const wasmPath = path.resolve(basePath, "..", "kotlin")

var wasmTestMjsFile;
var wasmTestFile;
try {
    const files = fs.readdirSync(wasmPath);
    wasmTestMjsFile = files.filter(file => file.endsWith('-wasm-js-test.mjs'))[0];
    wasmTestFile = files.filter(file => file.endsWith('-wasm-js-test.wasm'))[0];
} catch (error) {
    console.error('[karma-config] Error:', error);
}


const wasmTestsMjs = path.resolve(basePath, "..", "kotlin", wasmTestMjsFile)
const wasmTestsWasm = path.resolve(basePath, "..", "kotlin", wasmTestFile)
const wasmTestsLoaderWasm = path.resolve(basePath, "..", "kotlin", "load-test-template.mjs")

const debug = message => console.log(`[karma-config] ${message}`);

debug(`karma basePath: ${basePath}`);
debug(`karma wasmPath: ${wasmPath}`);


config.browsers = ["ChromeHeadlessWasmGc"];
config.customLaunchers = {
    ChromeHeadlessWasmGc: {
        base: 'ChromeHeadless',
        flags: ['--js-flags=--experimental-wasm-gc']
    }
};

config.proxies = {
    "/wasm/": wasmPath,
    ["/" + wasmTestMjsFile]: wasmTestsMjs,
    ["/" + wasmTestFile]: wasmTestsWasm,
    "/resources": path.resolve(basePath, "..", "kotlin")
}

config.preprocessors[wasmTestsLoaderWasm] = ["webpack"];

const staticLoadMJs = path.resolve(basePath, "..", "static", "load.mjs")
config.files = config.files.filter((x) => x !== wasmTestsMjs);
config.files = config.files.filter((x) => x !== staticLoadMJs);

config.files = [
    path.resolve(wasmPath, "skiko.js"),
    {pattern: path.resolve(wasmPath, "skiko.wasm"), included: false, served: true, watched: false},
    {pattern: wasmTestsMjs, included: false, served: true, watched: false},
    {pattern: wasmTestsWasm, included: false, served: true, watched: false},
].concat(config.files);

config.files.push({pattern: wasmTestsLoaderWasm, type: 'module'});