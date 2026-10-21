package com.kooo.evcam.camera;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;

/** MediaCodec output polling: bounded, nonblocking during capture; bounded waiting only for EOS. */
final class CodecOutputDrain {
    private CodecOutputDrain() {}

    static boolean run(boolean eos, int batchSize, long eosWaitMs, LongSupplier uptimeMs,
            IntUnaryOperator dequeue, Runnable formatChanged, IntPredicate consumeAndRelease) {
        long deadline = uptimeMs.getAsLong() + eosWaitMs;
        int polls = 0;
        boolean output = false;
        while (eos ? uptimeMs.getAsLong() < deadline : polls < batchSize) {
            // A normal callback must not wait for a future encoded frame while camera buffers queue.
            int index = dequeue.applyAsInt(eos ? 10_000 : 0);
            polls++;
            if (index == -1) { // MediaCodec.INFO_TRY_AGAIN_LATER
                if (!eos) break;
            } else if (index == -2) { // MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                formatChanged.run();
                output = true;
            } else if (index >= 0) {
                output = true;
                if (consumeAndRelease.test(index)) break;
            }
        }
        return output;
    }
}
