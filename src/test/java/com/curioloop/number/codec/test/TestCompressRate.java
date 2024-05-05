/*
 * Copyright © 2024 CurioLoop (curioloops@gmail.com)
 *
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
package com.curioloop.number.codec.test;

import com.curioloop.number.codec.CodecBuffer;
import com.curioloop.number.codec.delta2.Delta2Codec;
import com.curioloop.number.codec.stream.FloatGetter;
import com.curioloop.number.codec.stream.IntGetter;
import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.gorilla.GorillaCodec;
import com.curioloop.number.codec.stream.DoubleGetter;
import com.curioloop.number.codec.varint.VarIntCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.curioloop.number.codec.test.TestDataSample.*;

public class TestCompressRate {

    static final String[] TRADE_SAMPLES = {"SEHK_2023-12-20_trade.gz", "NBBO_2023-12-15_trade.gz"};
    static final String[] KLINE_SAMPLES = {"SEHK_2023-12-20_kline.gz", "NBBO_2023-12-15_kline.gz"};

    @Test void testCompressKline() {
        for (String file : KLINE_SAMPLES) {
            Map<String, List<KLine>> sample = loadKline(file);
            for (Map.Entry<String, List<KLine>> entry : sample.entrySet()) {
                List<KLine> data = entry.getValue();
                LongGetter timestamp = i -> data.get(i).getTime();
                FloatGetter open = i -> data.get(i).getOpen();
                FloatGetter high = i -> data.get(i).getHigh();
                FloatGetter low = i -> data.get(i).getLow();
                FloatGetter close = i -> data.get(i).getClose();
                LongGetter volume = i -> data.get(i).getVolume();
                DoubleGetter amount = i -> data.get(i).getAmount();

                CodecBuffer buffer = CodecBuffer.newBuffer(1000);
                byte[] timeBytes = Delta2Codec.encode(timestamp, data.size(), true, buffer.forgetPos()).toArray();
                byte[] openBits = GorillaCodec.encode32(open, data.size(), buffer.forgetPos()).toArray();
                byte[] highBits = GorillaCodec.encode32(high, data.size(), buffer.forgetPos()).toArray();
                byte[] lowBits = GorillaCodec.encode32(low, data.size(), buffer.forgetPos()).toArray();
                byte[] closeBits = GorillaCodec.encode32(close, data.size(), buffer.forgetPos()).toArray();
                byte[] volumeBytes = VarIntCodec.encode64( volume, data.size(), true, buffer.forgetPos()).toArray();
                byte[] amountBits = GorillaCodec.encode64(amount, data.size(), buffer.forgetPos()).toArray();

                int beforeBytes = KLine.BYTES * data.size();
                int afterBytes = timeBytes.length +
                        openBits.length +
                        highBits.length +
                        lowBits.length +
                        closeBits.length +
                        volumeBytes.length +
                        amountBits.length;

                System.out.printf("%s\t:\t%d -> %d\t(%.2f)\n", entry.getKey(),
                        beforeBytes, afterBytes, (double) afterBytes / beforeBytes);

                Assertions.assertTrue(afterBytes < beforeBytes);

                CodecSlice slice = new CodecSlice();
                List<KLine> recover = new ArrayList<>();
                Delta2Codec.decode(slice.wrap(timeBytes), (i, v) -> {
                    KLine line = new KLine();
                    line.time = v;
                    recover.add(line);
                }, true);

                GorillaCodec.decode32(slice.wrap(openBits), (i, v) -> recover.get(i).setOpen(v));
                GorillaCodec.decode32(slice.wrap(highBits), (i, v) -> recover.get(i).setHigh(v));
                GorillaCodec.decode32(slice.wrap(lowBits), (i, v) -> recover.get(i).setLow(v));
                GorillaCodec.decode32(slice.wrap(closeBits), (i, v) -> recover.get(i).setClose(v));
                VarIntCodec.decode64(slice.wrap(volumeBytes), (i, v) -> recover.get(i).setVolume(v), true);
                GorillaCodec.decode64(slice.wrap(amountBits), (i, v) -> recover.get(i).setAmount(v));

                Assertions.assertEquals(data, recover, entry.getKey());
            }
        }
    }

    @Test void testCompressTrade() {
        for (String file : TRADE_SAMPLES) {
            Map<String, List<Trade>> sample = loadTrade(file);
            for (Map.Entry<String, List<Trade>> entry : sample.entrySet()) {
                List<Trade> data = entry.getValue();
                LongGetter timestamp = i -> data.get(i).getTime();
                FloatGetter price = i -> data.get(i).getPrice();
                IntGetter size = i -> data.get(i).getSize();

                CodecBuffer buffer = CodecBuffer.newBuffer(1000);
                byte[] timeBytes = Delta2Codec.encode(timestamp, data.size(), true, buffer.forgetPos()).toArray();
                byte[] priceBits = GorillaCodec.encode32(price, data.size(), buffer.forgetPos()).toArray();
                byte[] sizeBytes = VarIntCodec.encode32(size, data.size(), true, buffer.forgetPos()).toArray();

                int beforeBytes = Trade.BYTES * data.size();
                int afterBytes = timeBytes.length +
                        priceBits.length +
                        sizeBytes.length;

                System.out.printf("%s\t:\t%d -> %d\t(%.2f)\n", entry.getKey(),
                        beforeBytes, afterBytes, (double) afterBytes / beforeBytes);

                Assertions.assertTrue(afterBytes < beforeBytes);

                CodecSlice slice = new CodecSlice();
                List<Trade> recover = new ArrayList<>();
                Delta2Codec.decode(slice.wrap(timeBytes), (i, v) -> {
                    Trade trade = new Trade();
                    trade.time = v;
                    recover.add(trade);
                }, true);

                GorillaCodec.decode32(slice.wrap(priceBits), (i, v) -> recover.get(i).setPrice(v));
                VarIntCodec.decode32(slice.wrap(sizeBytes), (i, v) -> recover.get(i).setSize(v), true);
                Assertions.assertEquals(data, recover, entry.getKey());
            }
        }
    }


}
