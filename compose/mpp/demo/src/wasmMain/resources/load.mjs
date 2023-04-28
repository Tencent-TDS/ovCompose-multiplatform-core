import { instantiate } from './mpp-demo.uninstantiated.mjs';

await wasmSetup;
instantiate({ skia: Module['asm'] });